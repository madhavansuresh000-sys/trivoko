# Phase 9 - Reviews, wishlist, price-drop alerts: design spec

Date: 27 Oct 2026 · Branch `phase-9-reviews-wishlist-alerts` (started from `phase-8-returns-and-refunds`, the same code
as PR #13) · Status: **written by Claude; Madhavan said "start Phase 9"; defaults marked [default]** · Next Flyway: **V12**.

## 1. Goal and "done when"

Standout #3: verified-buyer reviews, a wishlist, and price-drop alerts that are useful and never spammy.
(You ask the shopkeeper, "Call me if this TV gets cheaper." He calls once when it does - not five times when he
changes the tag five times in a minute.)

**Done when:** a seller lowers a price -> a customer who wishlisted it gets ONE alert (bell + email), not five ·
the test "5 price changes in one minute -> one alert" passes · CI green.

## 2. Reviews (module `review`, owns `reviews`)

- **Who may review:** a customer with at least one **DELIVERED** package containing this product (any variant).
  That is how we know the reviewer really bought it - the order module answers `hasDelivered(userId, productId)`.
  A returned item still counts as bought **[default]**.
- **One review per customer per product** (UNIQUE `(product_id, user_id)`); the author can edit or delete it
  **[default]**. Rating 1-5 (required), title up to 80 characters (optional), text 10-1000 characters.
- **Product page:** average (1 decimal), count, star breakdown 5★..1★ with counts, list newest first (10 per page),
  every review marked "Verified buyer" and shows the first name only ("Ravi K.") **[default]**.
- **Product cards and listing:** small stars + count. Kept on `products.rating_avg` / `rating_count` (two new columns),
  updated by the review module through `ProductService.updateRating(...)` after each write - so the listing needs no
  extra query per card.
- Where a customer writes it: the order page shows "Write a review" on each delivered item; the product page shows
  the form when the logged-in customer may review.
- Not in this phase: photos in reviews, "helpful" votes, admin hiding reviews (Phase 11 polish if wanted).

## 3. Wishlist (module `wishlist`, owns `wishlist_items`)

- Saved **per product** (not per variant) **[default]**; the tracked price is the product's **"from" price**
  (cheapest variant), the same number the card shows.
- `wishlist_items (id, user_id, product_id, price_when_added, last_alerted_price, price_changed, created_at)`,
  UNIQUE `(user_id, product_id)`. At most 100 items per customer **[default]**.
- API: `GET /api/me/wishlist` (cards + "₹X lower than when you saved it"), `PUT /api/me/wishlist/{productId}` (add,
  idempotent), `DELETE /api/me/wishlist/{productId}`, `GET /api/me/wishlist/ids` (for the hearts).
- UI: a heart button on product cards and the product page (a guest is sent to login), and a `/wishlist` page.
  A product that is no longer live stays in the list marked "Not available" **[default]**.

## 4. Price-drop alerts - one alert per real drop

```
 seller saves price --> ProductService.changePrice --> price_history row + PriceChanged event
                                                            |  (after commit)
                                                            v
                         wishlist listener: UPDATE wishlist_items SET price_changed = TRUE WHERE product_id = ?
                                                            |
                    every 10 minutes  PriceAlertJob --------+
                       for each flagged row: current "from" price  vs  reference
                       reference = last_alerted_price, or price_when_added if never alerted
                       drop >= 1% of the reference ?  -> bell + email, last_alerted_price = current
                       always: price_changed = FALSE
```

- **Why a job and not at once:** five changes in one minute reach the job as ONE flagged row, and the job reads only
  the **latest** price -> one alert with the final price. The event only marks rows, so the job never scans the whole
  wishlist table.
- **Never spammy:** an alert only when the price goes below the last price we told the customer about, by at least
  **1%** **[default]**. 999 -> 899 alerts; 899 -> 950 -> 899 does not alert again.
- Only for products that are live (ACTIVE product, APPROVED shop) and in stock.
- Email + bell, kind `PRICE_DROP`: "Kovai Run Pro is now ₹2,249 (was ₹2,499)", link to the product.
- Job interval `app.wishlist.alert-every: 10m` (tests call the job directly).

## 5. "Lowest price in 30 days" (catalog, from `price_history`)

- `ProductDetail.lowestPrice30Days` = the lowest "from" price the product had in the last 30 days: the minimum of
  the current price and every `old_price` / `new_price` of its variants' changes in those 30 days.
- Product page: "Lowest price in 30 days: ₹X", plus a green badge **"Lowest in 30 days"** when today's price IS that
  lowest and the price changed in the window (no badge for a price that never moved) **[default]**.

## 6. Data (Flyway V12)

```
reviews        (id, product_id, user_id, rating 1-5, title, body, created_at, updated_at)  UNIQUE(product_id, user_id)
wishlist_items (id, user_id, product_id, price_when_added, last_alerted_price, price_changed, created_at)
               UNIQUE(user_id, product_id), INDEX(price_changed)
products       + rating_avg DECIMAL(2,1) NULL, rating_count INT NOT NULL DEFAULT 0
price_history  (no change: V1 already has INDEX(variant_id, changed_at) for the 30-day question)
```

## 7. API summary

| URL | Who | What |
|---|---|---|
| `GET /api/products/{slug}/reviews?page=` | public | summary (avg, count, breakdown) + page of reviews |
| `GET /api/products/{slug}/reviews/mine` | customer | my review, and `canReview` |
| `PUT /api/products/{slug}/reviews/mine` | customer who received it | create or edit `{rating, title, body}` |
| `DELETE /api/products/{slug}/reviews/mine` | author | delete |
| `GET/PUT/DELETE /api/me/wishlist...` | customer | see section 3 |
| `GET /api/products/{slug}` | public | now also `ratingAvg`, `ratingCount`, `lowestPrice30Days`, `lowestNow` |

`/api/products/**` is public for GET only, so the `mine` URLs need a login check of their own (SecurityConfig rule).

## 8. Tests

- Backend: may not review without a delivered package (403) · one review per product (edit keeps one row) · average
  and breakdown maths · wishlist add twice = one row · **5 price changes in one minute -> one alert** · price up then
  back = no second alert · drop below 1% = no alert · blocked shop's product = no alert · lowest price in 30 days
  ignores changes older than 30 days.
- Frontend (Vitest): heart toggles (guest -> login), review form rules (rating needed, 10 characters), rating summary bars.

## 9. Explain-it-back (from the Phase Plan)

1. Why do alerts come from a job every 10 minutes instead of at once?
2. How do we know a reviewer really bought the product?
