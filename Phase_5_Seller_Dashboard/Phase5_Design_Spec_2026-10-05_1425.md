# Phase 5 - Seller dashboard: design spec

Date: 5 Oct 2026 · Branch `phase-5-seller-dashboard` (built on top of `phase-4-checkout-and-payments`, which is
pushed but not merged yet) · Status: **written by Claude; Madhavan said "continue"; defaults marked [default]**.

## 1. Goal and "done when"

Sellers run their own shop: add products with photos, ship their packages, and see what they earn.
(Each shopkeeper gets a back office: a stock register, a packing table and an account book.)

**Done when:** a seller ships a package and the customer sees the timeline update · the cancel-vs-ship race test
passes (one wins, the other gets 409).

| Part | Contents |
|---|---|
| **5A** | Package state machine, seller "orders to ship", cancel + refund of one package, seller ledger (+90% on DELIVERED), customer tracking timeline, bell/email at every step |
| **5B** | Seller layout + dashboard (sketch 6), products list + editor with variants and photos (Cloudinary), low-stock warnings, earnings page, sales charts 7/30/90 days |

## 2. Package state machine (spec of the Master Plan, section 4.4)

```
 PENDING_PAYMENT --paid--> PLACED --seller--> PACKED --seller + tracking no.--> SHIPPED --seller/demo--> DELIVERED
        └-- 10 min --> EXPIRED      └────────── customer or seller ──────────> CANCELLED (refund that package, stock back)
```

- `PackageStatus.allowedNext()` decides every move; anything else = 409 "A SHIPPED package cannot be cancelled".
- `@Version` on packages (already there): customer cancels while the seller ships -> one wins, the other gets 409.
- Every move writes a **package event** (time, new status, who, note) -> the customer's timeline.
- **Tracking number**: 6-30 letters/digits, required for SHIPPED **[default]**. DELIVERED: a "Mark delivered" button
  for the seller (simulated courier) **[default]**.
- **Cancel**: before SHIPPED only. Customer: from the order page. Seller: with a reason (5-200 characters).
  Effect: package CANCELLED, its stock back, **refund = package total** (items - its coupon share + its delivery),
  as a PARTIAL refund of the order's payment (`PaymentGateway.refund(session, amount, key)`, idempotency key =
  `refund-package-<id>`). The coupon is not given back **[default]**.

## 3. Money: the seller ledger (V7)

`seller_ledger (id, seller_id, package_id, kind, amount, note, created_at)`.
- On **DELIVERED**: `SALE` line = **+90% of the package total**, rounded HALF_UP; TriVoKo keeps 10%.
- A package refunded AFTER delivery (returns, Phase 8) will add a matching minus line; a cancel before shipping adds
  nothing (the money was never earned).
- **Balance = sum of the seller's ledger lines** (never one stored number - every rupee can be explained).
  No bank payouts (out of scope).

## 4. Data (Flyway V7)

```
package_events (id, package_id, status, actor, note, created_at)   -- the timeline
seller_ledger  (id, seller_id, package_id, kind, amount, note, created_at)
packages       + tracking_number, cancel_reason, placed_at, delivered_at
payments       + refunded_amount (partial refunds add up; status REFUNDED when it reaches amount)
product_images (exists) - seller photos are saved here (max 8 per product)
```

## 5. API

| URL | Who | What |
|---|---|---|
| `GET /api/seller/dashboard` | active seller | to pack, low stock count, balance, products waiting for approval, today's sales |
| `GET /api/seller/packages?status=` | active seller | my packages (newest first, paged), items + ship-to city |
| `POST /api/seller/packages/{id}/pack` · `/ship {trackingNumber}` · `/deliver` · `/cancel {reason}` | owner seller | package moves |
| `POST /api/orders/{number}/packages/{id}/cancel` | the customer | cancel one package before shipping |
| `GET /api/orders/{number}` | the customer | now with each package's `events` (timeline) and tracking number |
| `GET /api/seller/ledger` | active seller | balance + lines (paged) |
| `GET /api/seller/sales?days=7|30|90` | active seller | one point per day: sales total + packages; cached 5 minutes **[default]** |
| `GET /api/seller/products/low-stock` | active seller | variants of my ACTIVE products with stock < 5 **[default threshold 5]** |
| `PUT /api/seller/products/{id}/images` `[{url, altText}]` | owner seller | replace the photo list (order = position, max 8; URLs must be our Cloudinary folder) |

Ownership: every `/api/seller/packages/{id}` call checks the package's seller = my APPROVED shop (403 otherwise),
like `SellerAccess.ownsProduct` in Phase 2.

## 6. Messages (bell + email, after commit, background)

| Event | To | Email? |
|---|---|---|
| PACKED | customer | bell only **[default]** |
| SHIPPED (with tracking no.) | customer | yes |
| DELIVERED | customer | yes |
| CANCELLED by seller | customer | yes (with the reason and the refund amount) |
| CANCELLED by customer | seller | yes |

## 7. Frontend

- `/seller` layout (sidebar on desktop, tabs on phone): **Dashboard** (sketch 6: 4 stat cards + packages to ship),
  **Orders to ship**, **Products**, **Earnings**, **Analytics**. Guard: active seller (others see "Become a seller").
- Products: list with status badges + "New product"; editor: name, sub-category, brand, description, variants table
  (label, size, colour, price, MRP, stock), photos (upload with the Phase 1 signature, max 8, 5 MB, move up/down
  **[default instead of drag-and-drop - works with keyboard and on phones]**), "Save draft" / "Submit for approval".
  A live product: only price and stock are editable (Phase 2 rule).
- Earnings: balance card + ledger table. Analytics: bar chart (Recharts) of daily sales, 7/30/90 switch.
- Customer order detail: per package a **timeline** (Placed -> Packed -> Shipped (AWB) -> Delivered) and a
  "Cancel this package" button while it is PLACED or PACKED.

## 8. Testing

Unit: `PackageStatus.allowedNext`, ledger 90% rounding. Integration: every move by the owner, someone else's package
403, ship without tracking 400, cancel after shipping 409, cancel refunds the package total (fake gateway records
it) and returns stock, DELIVERED writes the ledger line once, **race: cancel vs ship in two threads -> exactly one
wins, the other 409**, sales endpoint sums, images max 8 + foreign URL refused. Vitest: timeline, editor variant rows.
Browser: seller (Kovai Sports needs DEMO_PASSWORD, or a test seller) packs -> ships -> customer timeline updates.

## 9. Not in Phase 5

Returns (Phase 8), admin pages (Phase 6), flash sales (Phase 7), bank payouts (never).
