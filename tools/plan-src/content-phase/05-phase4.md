# Phase 4 - Checkout & payments

**Time:** 2 weeks  ·  **Group:** Core  ·  **Folder:** `Phase_4_Checkout_and_Payments\`

**Goal:** One cart, one Stripe payment, one order split into one package per seller - with stock held safely, coupons, emails and a PDF invoice.

>> At a food court you pay once at the main counter, and your token is sent to the dosa stall and the juice stall separately. Each stall only sees its own part of your order.

## Steps

@widths 600 6926 1500
| # | Step | Who |
| 1 | Flyway: `orders`, `packages`, `order_items` (copy title, variant and price), `payments`, `processed_payment_events`, `coupons`, `coupon_redemptions` | Claude |
| 2 | `Money` helpers (rupees ↔ paise, rounding) with unit tests | Claude |
| 3 | Coupon rules: flat or %, minimum order, expiry, one use per customer; admin create endpoint | Claude |
| 4 | ★ You type the ORDER SPLIT: group cart lines by seller into packages, shipping fee per package (free above ₹499, else ₹40), coupon share per item (by price, rounding remainder on the last item) | ★ Madhavan |
| 5 | ★ You write at least one test for the order split (2 sellers + coupon → exact amounts) | ★ Madhavan |
| 6 | CheckoutService: recheck products, sellers, coupon and prices; hold stock with `UPDATE ... SET stock = stock - :qty WHERE stock >= :qty`; roll back and name the item that ran out | Claude |
| 7 | Copy the EventHub PaymentGateway switch (Fake when no Stripe key, Stripe Checkout when set) | Claude |
| 8 | Stripe webhook (signature check) + "verify on return" fallback + idempotency → order PAID, packages PLACED, coupon used, cart emptied | Claude |
| 9 | Expiry job: unpaid after 10 minutes → EXPIRED, stock back; late payment re-takes stock or is refunded automatically | Claude |
| 10 | `notify` module (copy from EventHub): bell + emails after commit with retry; order email to customer, new-package email to each seller | Claude |
| 11 | PDF invoice with OpenPDF: `GET /api/orders/{number}/invoice.pdf` | Claude |
| 12 | Frontend: checkout page (address, coupon, summary per package, "price changed" notice), payment result, My orders, order detail | Claude |
| 13 | Concurrency test: 100 threads try to buy 10 units → exactly 10 orders | Claude |
| 14 | Create the Stripe test account, put `sk_test` in `.env`, install the Stripe CLI | You |
| 15 | Real Stripe test: pay with 4242 4242 4242 4242 and receive a real signed webhook (the EventHub to-do) | Together |

## Output of this phase

- Working checkout with Stripe test mode
- Split orders with packages per seller
- Order emails in Mailpit and PDF invoices
- Your own OrderSplitter class + test

## Done when

- Pay once for items from 2 sellers → 2 packages PLACED
- The 100-thread stock test passes
- A real Stripe 4242 payment confirmed by a signed webhook

## Explain it back

- Walk through checkout from "Place order" to "PLACED" - which table changes at each step?
- How is the coupon shared between packages, and why store it on each item?
- Why can a webhook arrive twice, and what stops a double order?

**Skills you practise:** Transactions, atomic SQL updates, money maths, Stripe Checkout, webhooks, idempotency, scheduled jobs, PDF

**Where your work goes:** `backend\...\order`, `...\payment`, `...\coupon`, `...\notify`; checkout pages in `frontend`
