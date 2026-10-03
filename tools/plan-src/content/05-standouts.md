# 5. The four standout features

These are the features that make TriVoKo different from a tutorial clone. Each one has a clear rule, a clear proof, and a clear interview story.

## 5.1 Flash sale (standout #1)

![The flash sale that never oversells](flash-sale.png)

**What the user sees:** a "Flash sale starts in 00:04:59" banner on the home page. At 12:00 the button turns on, a live "73 left" counter drops, and when it reaches 0 the button changes to "Sold out".

**How it works:**

1. The admin creates a flash sale (start time, end time). Sellers offer variants with a sale price and a fixed quantity (for example 100). That quantity is **taken out of normal stock** when the sale is scheduled, so normal buyers and flash buyers never fight over the same units.
2. "Buy now" calls `POST /api/flash-sales/{id}/items/{itemId}/claim`.
3. The server runs **one atomic update**: `UPDATE flash_sale_items SET sold = sold + 1 WHERE id = ? AND sold < quantity AND NOW() BETWEEN start AND end`. One row changed = you got one. Zero rows = sold out (or not started / ended).
4. A row in `flash_sale_claims` with **UNIQUE(sale_item, customer)** stops anyone buying twice.
5. The winner gets a normal 10-minute checkout hold at the sale price. If they do not pay, the expiry job does `sold = sold - 1` and the unit is back in the sale.
6. When the sale ends, unsold units go back to normal stock.

**Proof:**

- Java test (like EventHub's `BookingConcurrencyTest`): 1,000 threads, 100 units → exactly 100 claims, `sold` = 100, never 101.
- **k6 load test**: 5,000 virtual users hit the running app at once. The README shows the result: requests per second, p95 response time, number of winners (must be exactly 100) and zero errors.
- **Only if** p95 is too slow (target: under 500 ms on a laptop), the counter moves to Redis (`DECR`), and we measure again and show both graphs.

> You type this part: the claim SQL, the unique check and the expiry give-back. Claude explains first and reviews after.

**Interview story:** "Instead of reading stock and then writing it - which lets two people both see 1 left - I let MySQL do check-and-decrement in a single statement, so the database itself guarantees no oversell. I proved it with a 1,000-thread test and a 5,000-user k6 run."

## 5.2 Returns and refunds (standout #2)

The return states are in the diagram in chapter 4.

**Rules:**

- A return can be requested only for a **DELIVERED** item, within **7 days**, once per order item, with a reason (list) and an optional photo (Cloudinary).
- The seller must answer within 2 days; if not, the request is **auto-approved** by a job (marketplace protects the buyer).
- If the seller rejects, the customer can **escalate once**; the admin makes the final decision.
- On **PICKED_UP**, the server calls **Stripe refund** for `item price × qty − item's coupon share` (a **partial refund** of the original payment). The refund is idempotent: a unique key per return request, so a double click or a retried webhook never refunds twice.
- Refund done → stock +qty, seller ledger minus line, customer email + bell.

> You type this part: the `ReturnStatus` enum with `allowedNext()`, and the service method that moves states. Claude writes the Stripe refund call and the tests around it.

**Interview story:** "Returns are a state machine. Every move is validated, every refund is idempotent, and money always matches: the ledger line, the Stripe refund and the stock change happen together."

## 5.3 Price-drop alerts (standout #3)

![Price-drop alerts and smart search](price-search.png)

- Every price change writes a row to `price_history` and publishes a `PriceChanged` Spring event.
- A listener finds wishlist entries for that variant where the **new price is lower** than both the price when added and the last alerted price.
- One notification + email per customer per drop (stored `last_alert_price` stops spam if the seller changes the price 5 times in a minute - alerts are sent by a job every 10 minutes using the latest price).
- The product page shows **"Lowest price in 30 days"** using `price_history` - useful and honest.

## 5.4 Smart search (standout #4)

- **Hibernate Search** keeps a **Lucene index** of products inside the app (no Elasticsearch server to host). It updates automatically when a product changes.
- **Typo tolerance**: fuzzy matching ("iphnoe" → iPhone). **Autocomplete**: suggestions after 2 letters.
- **Filters and facets**: category, brand, price range, rating, in-stock - with counts ("Samsung (12)").
- **Sort**: relevance, price low/high, newest, rating.
- **Customers also bought**: products that appear together in paid orders most often (a SQL query, cached for 1 hour).

> If Hibernate Search causes trouble on free hosting (the index needs disk space), the fallback is MySQL FULLTEXT search without typo tolerance. This decision is checked in Phase 10.
