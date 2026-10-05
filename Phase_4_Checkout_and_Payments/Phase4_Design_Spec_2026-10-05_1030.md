# Phase 4 - Checkout & payments: design spec

Date: 5 Oct 2026 · Branch `phase-4-checkout-and-payments` · Status: **written by Claude; defaults marked [default]**.
Madhavan chose "all of Phase 4" in one go and earlier asked Claude to decide defaults he can change later.
The rules here come from the Master Plan (section 4) and the Phase Plan; only the gaps are **[default]**.

## 1. Goal and "done when"

One cart, one payment, one order split into one package per seller - with stock held safely, coupons, emails and
a PDF invoice. (Food court: pay once at the counter; the dosa stall and the juice stall each get their own token.)

**Done when:** pay once for items from 2 sellers → 2 packages PLACED · the 100-thread stock test passes ·
a real Stripe 4242 payment is confirmed by a signed webhook (Madhavan's Stripe test account).

Built in three parts on one branch, one PR at the end:

| Part | Contents |
|---|---|
| **4A** | Flyway V5, `Money`, coupons, **★ OrderSplitter (Madhavan types it + its test)**, checkout with stock hold, fake payment page, expiry job, late payment, checkout / payment result / My orders / order detail pages |
| **4B** | Real Stripe Checkout behind the same `PaymentGateway`, signed webhook, verify-on-return, idempotency, 100-thread stock test, Madhavan's 4242 test with the Stripe CLI |
| **4C** | `notify` module (bell + emails after commit, with retry), order email to the customer, new-package email to each seller, PDF invoice |

## 2. The flow

```
 Cart ──► /checkout ──► POST /api/checkout/preview   (address, coupon -> packages, amounts; nothing saved)
                │
                └──► POST /api/orders {addressId, couponCode, expectedTotal}
                        1. recheck: products ACTIVE, shops APPROVED, coupon valid, qty <= 10
                        2. ★ OrderSplitter -> packages, discount share per item, delivery per package
                        3. total != expectedTotal -> 409 "prices changed" (+ the new preview)
                        4. hold stock: UPDATE product_variants SET stock = stock - :qty
                                       WHERE id = :id AND stock >= :qty   (0 rows -> roll back, name the item)
                        5. order PENDING_PAYMENT (10 min) + packages + items (copies of name, label, price)
                        6. PaymentGateway.createCheckout -> redirect URL (Stripe page or the fake test page)
                ┌───────┴──────────────────────────────┐
     paid (webhook / verify / fake page)       not paid in 10 min (job, every minute)
     order PAID, packages PLACED,              order EXPIRED, stock back,
     coupon redemption, ordered lines           payment session closed
     removed from the cart, emails (4C)
                                               paid LATE -> re-take all stock if free -> PAID,
                                                            else full refund (order stays EXPIRED)
```

## 3. Data (Flyway V5, one migration)

| Table | Key columns |
|---|---|
| `orders` | id, `number` UNIQUE (**[default]** `TV-` + 6 digits, e.g. TV-100123), user_id, status (PENDING_PAYMENT / PAID / EXPIRED), items_total, discount_total, shipping_total, grand_total, coupon_code, address snapshot (name, phone, line1, line2, city, state, pincode), hold_expires_at, paid_at, created_at, version |
| `packages` | id, order_id, seller_id, seller_name (copy), status (PENDING_PAYMENT / PLACED / EXPIRED; PACKED / SHIPPED / DELIVERED / CANCELLED come in Phase 5), items_total, discount, shipping_fee, total, version |
| `order_items` | id, package_id, variant_id, product_slug, product_name, variant_label (copies), unit_price, mrp, quantity, line_total, discount_share |
| `payments` | id, order_id, provider (FAKE / STRIPE), session_id UNIQUE, amount, status (CREATED / PAID / REFUNDED / EXPIRED), created_at, paid_at |
| `processed_payment_events` | event_id PK, processed_at - a webhook seen twice changes nothing the second time |
| `coupons` | id, code UNIQUE (upper case), type (FLAT / PERCENT), value, max_discount (PERCENT only, optional), min_order, valid_from, valid_until, active |
| `coupon_redemptions` | id, coupon_id, user_id, order_id, UNIQUE (coupon_id, user_id) - one use per customer |

Seed: coupon **WELCOME10** - 10% off, max ₹500, minimum order ₹499, valid for one year **[default]**; and
**FLAT100** - ₹100 off from ₹999 **[default]**.

## 4. Money rules

- Stored as `DECIMAL(12,2)` rupees; `Money.toPaise(₹15,400.00) = 1540000` for Stripe; rounding HALF_UP to 2 places.
- **Coupon discount** (CouponService) is worked out on the items total (before delivery):
  FLAT = min(value, itemsTotal); PERCENT = itemsTotal × value / 100, rounded, capped by max_discount.
  Valid only if active, inside the dates, itemsTotal ≥ min_order, and this customer has no redemption and no
  other PENDING_PAYMENT order with it **[default]**.
- **★ OrderSplitter** (pure Java, no database - Madhavan types it):
  - group lines by seller, in the cart's order → one package each;
  - **delivery per package**: package items total (before discount) ≥ ₹499 → ₹0, else ₹40 **[default: before discount]**;
  - **discount share per item** = discount × lineTotal ÷ itemsTotal, rounded to paise; the **last item of the
    order** gets the remainder, so the shares always add up to the discount exactly;
  - package discount = sum of its items' shares; package total = items − discount + delivery;
    grand total = sum of package totals.
  - Why per item: in Phase 8 a returned item is refunded `line_total − discount_share`, exactly.
- Example the test must prove: Chennai Mobiles case ₹499 × 2 + Kovai Sports shoes ₹2,499 × 1, WELCOME10
  → items ₹3,497, discount ₹349.70 → case line ₹99.80 (998 × 349.70 / 3497 = 99.8), shoes ₹249.90 (remainder),
  delivery ₹0 + ₹0, pay **₹3,147.30**.

## 5. API

| URL | Who | What |
|---|---|---|
| `GET /api/coupons/{code}` → `{code, description}` | logged in | Does this code exist and is it active (amounts come from preview) |
| `POST /api/checkout/preview` `{addressId?, couponCode?}` | logged in | Packages, amounts, coupon result (`couponError` instead of failing), lines that cannot be bought |
| `POST /api/orders` `{addressId, couponCode?, expectedTotal}` | logged in | Places the order (section 2) → `{number, redirectUrl}`; 409 with a message for: prices changed, item ran out, cart empty, nothing buyable |
| `GET /api/orders` · `GET /api/orders/{number}` | owner | My orders (newest first, paged) · one order with packages and items |
| `GET /api/orders/{number}/invoice.pdf` | owner, PAID only | PDF invoice (4C) |
| `GET /api/payments/fake/{session}` · `POST .../complete` | owner, dev only | The built-in test payment page (no Stripe key) |
| `POST /api/payments/{session}/verify` | owner | Back from Stripe: ask Stripe directly (webhook may be late) |
| `POST /api/payments/stripe/webhook` | Stripe (signature) | `checkout.session.completed` → mark paid; bad signature 400 |
| `GET/POST /api/admin/coupons` | admin | List / create coupons (admin pages: Phase 6) |
| `GET /api/notifications`, `POST /api/notifications/read` | logged in | The bell (4C) |

- One order at a time per customer: a new checkout while another order is PENDING_PAYMENT returns that order's
  payment link instead of holding stock twice **[default]**.
- The payment gateway switch is copied from EventHub: `STRIPE_SECRET_KEY` set → Stripe, else the fake page;
  prod refuses to start without a key unless `PAYMENTS_TEST_PAGE=true`.

## 6. Frontend

- `/checkout` (sketch 5): address radio list (+ add address modal), packages review, "Price changed: X is now
  ₹499 (was ₹549)" notice (compares with the cart view the customer last saw), coupon box, price details,
  "Pay ₹3,147.30" button, "Stock is held for 10 minutes while you pay".
- `/test-payment/:session` (fake gateway page, clearly "TEST - no real money"), `/payment/success` (verify, then
  "Order TV-100123 placed - 2 packages"), `/payment/cancelled`.
- `/orders` (My orders) and `/orders/:number` (packages with status badges, items, amounts, invoice link).
- Navbar: "My orders" in the account menu; bell with unread count (4C).

## 7. Errors

- Item ran out while placing → 409 "Sorry, Kovai Run Pro (UK 8) has only 1 left" and nothing is held.
- Coupon invalid → preview shows the reason under the box; place-order with an invalid coupon → 409.
- Payment provider down → 503 "Could not start the payment", the order and stock hold are rolled back.
- Webhook with a bad signature → 400, nothing changes; a duplicate webhook → 200, nothing changes.

## 8. Testing

- Unit: `Money`, `CouponService` rules, **★ OrderSplitterTest (Madhavan's, the example in section 4)**.
- Integration (Testcontainers): place order happy path (2 sellers → 2 packages, stock down, cart lines removed
  after payment), price changed 409, ran out 409 (nothing held), expiry job (stock back), late payment both ways,
  duplicate webhook, bad signature, someone else's order 404, coupon one-use, **100 threads buy 10 units →
  exactly 10 orders** (4B).
- Vitest: checkout page price-changed notice, payment success flow. Browser: full fake-payment run.
- Postman: folder "Checkout" (fake gateway).

## 9. Not in Phase 4

Package moves after PLACED (Phase 5), cancellations and refunds of packages (Phase 5/8), seller ledger lines
(on DELIVERED, Phase 5), admin coupon pages (Phase 6), flash-sale prices (Phase 7).
