# Phase 5 - Seller dashboard

**Time:** 1.5 weeks  ·  **Group:** Core  ·  **Folder:** `Phase_5_Seller_Dashboard\`

**Goal:** Sellers run their own shop: add products with photos, ship their packages, and see what they earn.

>> Each shopkeeper gets their own back office: a stock register, a packing table, and an account book that says how much the mall owes them.

## Steps

@widths 600 6926 1500
| # | Step | Who |
| 1 | Seller layout + dashboard: today's packages, low stock, earnings, products waiting for approval | Claude |
| 2 | Products list + product editor: variants table, photo upload to Cloudinary (drag to reorder, max 8, 5 MB), submit for approval; price change writes `price_history` | Claude |
| 3 | Low-stock warnings (default below 5) | Claude |
| 4 | `PackageStatus` enum with `allowedNext()` + `@Version`; Packed → Shipped (tracking number) → Delivered (demo button) | Claude |
| 5 | Cancel before shipping (customer or seller): refund that package, stock back; race test cancel-vs-ship → one wins, other gets 409 | Claude |
| 6 | `seller_ledger`: DELIVERED adds +90%; earnings page with the ledger lines | Claude |
| 7 | Customer order detail: tracking timeline per package; email + bell at every step | Claude |
| 8 | Sales charts for 7 / 30 / 90 days (Recharts, cached) | Claude |
| 9 | Tests: ownership, every package move, ledger amounts | Claude |

## Output of this phase

- Seller back office
- Package tracking timelines
- Earnings ledger and sales charts

## Done when

- A seller ships a package and the customer sees the timeline update
- Cancel-vs-ship race test passes

## Explain it back

- What is a state machine and why use `allowedNext()`?
- What does `@Version` do when two people change the same package?
- Why is the seller balance the sum of ledger lines instead of one number?

**Skills you practise:** State machines, optimistic locking, file upload, charts, caching

**Where your work goes:** `frontend\src\pages\seller`, `backend\...\order`, `...\seller`
