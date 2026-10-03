# 4. Core flow: cart, checkout and the split order

![One cart, many sellers, one payment, split packages](order-split.png)

## 4.1 Cart

- A **guest** can add to cart. The cart lives in the browser (localStorage) with a guest token.
- After **login**, the guest cart is **merged** into the user's saved cart (same variant = quantities added, capped by stock).
- The cart shows items **grouped by seller**, so the customer already sees "this will come in 2 packages".
- Prices in the cart are only for display. **The server always recalculates prices at checkout** - a changed price is shown to the customer before paying.

## 4.2 Checkout - step by step

1. Customer chooses an address (or adds one) and optionally a coupon.
2. Server checks: every product still ACTIVE, every seller still APPROVED, coupon valid, quantities ≤ 10 per item.
3. Server **holds the stock** for every item in one transaction:
   `UPDATE product_variants SET stock = stock - :qty WHERE id = :id AND stock >= :qty`
   If any line returns 0 rows, the whole checkout is rolled back and the customer is told which item ran out.
4. Server creates the **order** (status PENDING_PAYMENT, hold 10 minutes) and **one package per seller**. Order items **copy** the title, variant and price, so later product changes never change an old order.
5. Server creates a **Stripe Checkout session** for the full amount and the customer pays.
6. The Stripe **webhook** (or the "verify on return" fallback from EventHub) marks the order **PAID**; every package becomes **PLACED**; each seller gets an email; the coupon is marked used.
7. If nobody pays within 10 minutes, a job (every minute) marks the order **EXPIRED** and puts the stock back. A payment that arrives late re-takes the stock if still free, otherwise it is refunded automatically (same rule as EventHub).

>> IRCTC Tatkal works like this: when you click "Book", the seat is kept for you while you pay. If the payment page times out, the seat goes back to the pool.

## 4.3 Money rules

- All money is stored as `DECIMAL(12,2)` in rupees; Stripe receives **paise** (₹15,400.00 → 1540000).
- **Coupon discount** is split across packages and items in proportion to their price, and stored on each order item. This makes partial refunds exact later.
- When a package is **DELIVERED**, the seller ledger gets **+90%** of that package's paid amount; TriVoKo keeps 10%. A refund adds a matching **minus** line. The seller's balance = sum of the ledger. (No real bank payouts - out of scope.)
- Shipping fee: free above ₹499 per package, otherwise ₹40 per package (simple fixed rule).

## 4.4 Package and return states

![Package and return state machines](states.png)

| Move | Who | Rule |
| PENDING_PAYMENT → PLACED | system | Payment confirmed (webhook or verify) |
| PENDING_PAYMENT → EXPIRED | system job | 10 minutes passed, not paid - stock back |
| PLACED → PACKED | seller | Own package only |
| PACKED → SHIPPED | seller | Tracking number required |
| SHIPPED → DELIVERED | seller (simulated courier) or demo button | Starts the 7-day return window, adds ledger line |
| PLACED / PACKED → CANCELLED | customer or seller | Before shipping only; refund that package; stock back |

> Each state list is a Java `enum` with an `allowedNext()` method, exactly like EventHub's `EventStatus.nextAllowed`. Package rows have `@Version`, so if the customer cancels at the same moment the seller ships, only one wins and the other gets a clear 409 message.
