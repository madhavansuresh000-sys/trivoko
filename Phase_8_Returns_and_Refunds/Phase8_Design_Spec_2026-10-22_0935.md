# Phase 8 - Returns & refunds: design spec

Date: 22 Oct 2026 · Branch `phase-8-returns-and-refunds` (stacked on `phase-7-flash-sale`, not pushed yet) ·
Status: **written by Claude while Madhavan studies for his model exam; defaults marked [default] - change any of
them before step 2 starts.**

## 1. Goal and "done when"

Standout #2: a customer returns ONE item and gets back exactly the right money through a Stripe *partial* refund,
while stock and the seller ledger stay correct.
(You return one shirt from a bill of three. The shop checks it, puts it back on the shelf and gives back only that
shirt's money - minus its part of the discount you used.)

**Done when:** return one item of a 2-item order -> the exact partial refund appears in the Stripe test dashboard ·
ledger and stock match · a double click on "Refund" gives one refund.

## 2. The return state machine (★ Madhavan types `ReturnStatus` + the move method - steps 2-3)

```
 REQUESTED --seller approves--> APPROVED --courier picks up--> PICKED_UP --refund ok--> REFUNDED
     |   \--2 days, no answer (job)-->--/                              \--refund call fails--> REFUND_FAILED --retry--> REFUNDED
     |
     +--seller rejects (reason)--> REJECTED --customer escalates (once)--> ESCALATED --admin--> APPROVED
     |                                                                         \--admin--> CLOSED (final no)
     +--customer cancels--> CANCELLED
```

| From | Allowed next | Who |
|---|---|---|
| REQUESTED | APPROVED, REJECTED, CANCELLED | seller (approve/reject), job (auto-approve after 2 days), customer (cancel) |
| APPROVED | PICKED_UP | seller (simulated courier: "Mark picked up") **[default]** |
| PICKED_UP | REFUNDED, REFUND_FAILED | system (refund call) |
| REFUND_FAILED | REFUNDED | admin "Retry refund" |
| REJECTED | ESCALATED | customer, once, within 3 days of the rejection **[default]** |
| ESCALATED | APPROVED, CLOSED | admin (final) |
| REFUNDED, CLOSED, CANCELLED | - (end states) | |

- `ReturnStatus.allowedNext()` decides every move (same shape as `PackageStatus` from Phase 5). Anything else ->
  `BusinessRuleException` -> **409** "A REFUNDED return cannot move to APPROVED".
- `@Version` on `return_requests`: two clicks at once -> one wins, the other 409.
- Every move writes a **return event** (time, new status, who, note) -> the customer's return timeline.

## 3. Rules for asking

- Only items of a **DELIVERED** package, within **7 days** of `deliveredAt`.
- **Once per order item** (UNIQUE `order_item_id`), for the **whole line quantity** **[default]** (asking for 1 of 3
  identical items is left out: it complicates the maths with little to learn).
- Reason (one of: DAMAGED, WRONG_ITEM, NOT_AS_DESCRIBED, SIZE_FIT, CHANGED_MIND, OTHER) + note 5-300 chars + optional
  photo (Cloudinary, same signed upload as product photos).
- **Flash-sale items** can be returned like any other (refund = the flash price they paid) **[default]**.

## 4. Money: how much comes back

```
refund = lineTotal - discountShare          (both are already stored on order_items since Phase 4)
       = unitPrice x quantity - this line's part of the coupon
```

- Delivery fee is **not** refunded **[default]** (unless the whole package is returned - also not refunded, keep it
  simple; written in the README).
- Paid through `PaymentService.refundPart(orderId, amount, key)` with key **`refund-return-<returnId>`**. The key makes
  Stripe ignore a repeat, and the state machine makes the second call impossible on our side
  (PICKED_UP -> REFUNDED happens once; a second click finds REFUNDED and gets 409).
- `refundPart` already never gives back more than was paid in total (`refundedAmount`).
- A refund that throws (Stripe down) -> REFUND_FAILED, the admin retries with the **same key**.

## 5. After REFUNDED (step 8) - all in the same transaction as the status change

1. **Stock back:** `ProductService.releaseStock({variantId: quantity})` (the item was checked by the seller at pickup).
2. **Seller ledger:** a `REFUND` line of **-90% of the refund** (the seller only ever got 90%), rounded HALF_UP like
   the SALE line. **Needs migration V10:** the V7 key `UNIQUE (package_id, kind)` allows only one REFUND per package,
   but two items of one package can be returned. V10 adds `return_id` (NULL for SALE lines) and a generated column
   `once_key = COALESCE(return_id, 0)`, and replaces the key with `UNIQUE (package_id, kind, once_key)`. (A plain key
   on `return_id` would not work: MySQL treats every NULL as different, so SALE lines could repeat.)
3. **Bell + email** to the customer ("Refund of Rs 1,499.00 started - 5-7 days to reach your card") and a bell to the
   seller at every step (requested, auto-approved, escalated, decided).

## 6. Data (V10)

`return_requests (id, order_item_id UNIQUE, package_id, order_id, customer_id, seller_id, status, reason, note,
photo_url, refund_amount, reject_reason, escalated_at, decided_by, version, created_at, updated_at)`
`return_events (id, return_id, status, actor_role, actor_id, note, created_at)` - the timeline.
`seller_ledger`: + `return_id`, new unique key (section 5).
Owner: module `returns` (as its `package-info` says). It calls `OrderService`, `PaymentService`, `ProductService`,
`LedgerService`, `NotificationService` - services only (ArchUnit rule).

## 7. API

| URL | Who | What |
|---|---|---|
| `POST /api/me/returns` `{orderItemId, reason, note, photoUrl}` | customer | Ask |
| `GET /api/me/returns`, `GET /api/me/returns/{id}` | customer | My returns + timeline |
| `POST /api/me/returns/{id}/cancel` - `escalate {note}` | customer | |
| `GET /api/seller/returns?status=&page=` | seller | Returns for my shop |
| `POST /api/seller/returns/{id}/approve` - `reject {reason}` - `picked-up` | seller | `picked-up` triggers the refund |
| `GET /api/admin/returns?status=ESCALATED&page=` | admin | Disputes |
| `POST /api/admin/returns/{id}/approve` - `close {reason}` - `retry-refund` | admin | Final word |
| `GET /api/admin/reports/returns?days=` | admin | Return rate = returned items / delivered items, per shop |

## 8. Frontend

Customer: "Return this item" button on a delivered line (hidden after 7 days) -> form; return timeline on the order
page and a "My returns" list. Seller: `/seller/returns` (queue with approve / reject / picked up). Admin:
`/admin/returns` (disputes + failed refunds) and a return-rate card in Reports.

## 9. Tests (step 10 + ★ step 3)

- ★ `ReturnStatusTest` (Madhavan): every allowed move works, every other move throws.
- `RefundMathsTest`: 2-item order with a coupon -> refund = line - its coupon share, to the paisa; seller ledger -90%.
- `ReturnFlowTest`: ask -> approve -> picked up -> REFUNDED: stock +qty, payment.refundedAmount, ledger line, bell.
- `DoubleRefundTest`: 10 threads press "picked up" at once -> exactly one refund call to the (fake) gateway.
- Rules: not delivered / day 8 / second request / someone else's item -> 409/404. Auto-approve job after 2 days.
- Security sweep: every new URL x wrong role -> 403.

## 10. Open questions for Madhavan (answer any time; defaults are used until then)

1. Whole line only, or also "1 of 3 identical items"? [default: whole line]
2. Refund the delivery fee when the whole package comes back? [default: no]
3. Who marks "picked up": seller button (simulated courier) or an admin? [default: seller]
