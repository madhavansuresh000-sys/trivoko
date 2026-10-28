# Session Summary - 27 Oct 2026 (Phase 9 built)

## What we did today

1. Checked the real status on GitHub: Phases 0-7 + Phase 8 steps 1-8 merged; **PR #13 (Phase 8 step 9) still open**
   - MADHAVAN said "merged" three times, but the API kept showing `merged: false` (the second button, "Confirm merge",
   was probably not clicked).
2. Planned Phase 9: design spec + implementation plan (`Phase_9_Reviews_Wishlist_Alerts\`). MADHAVAN kept all four
   defaults: wishlist per product, 1% minimum drop, one review per product, returned items still count as bought.
3. Built all 7 Phase 9 tasks test-first on branch `phase-9-reviews-wishlist-alerts` (pushed):

| Step | What | Test |
|---|---|---|
| 1 | V12: `reviews`, `wishlist_items`, `products.rating_avg / rating_count` | ReviewWishlistSchemaTest (4) |
| 2 | Verified-buyer reviews (delivered only, one per product, edit/delete, summary) | ReviewTest (7) |
| 3 | Wishlist API (idempotent add, max 100, list with price drop) | WishlistTest (7) |
| 4 | PriceChanged event -> mark rows; PriceAlertJob -> ONE alert per real drop | PriceAlertTest (6): 5 changes -> 1 alert |
| 5 | Lowest price in 30 days + badge; stars on cards | LowestPriceTest (5) |
| 6 | Pages: reviews, heart, /wishlist, lowest-price line, "Write a review" | Vitest (85 total) |
| 7 | README Phase 9, decision D10, Postman folder 11 | newman |

4. Checks: `mvnw verify` 368 green · Vitest 85 green, lint clean, build ok · newman: Phase 9 folder all green
   (one old Phase 4 coupon check fails on the used dev DB) · browser (guest) OK.
5. Added a random `DEMO_PASSWORD` to the local `.env` (git-ignored, value never shown) so demo accounts unlock.

## The idea to remember

```
 5 price edits in a minute -> 5 events -> ONE mark on the wishlist row
 job every 10 min -> reads the price NOW -> at least 1% below what the customer last heard? -> ONE message
```
"Call me once when the TV is cheaper - not every time you change the price tag."

## Things the tests caught

- TINYINT column vs Java `int`: Hibernate's start-up check refused to start -> column changed to INT.
- MySQL CHECK failure is a different Spring error type than a duplicate key (DataAccessException).
- The heart on product cards needed the Redux store in the old ProductsPage test.

## Next time

1. MADHAVAN: Confirm merge PR #13; create the Phase 9 PR (compare link in the Project Brief); Claude checks CI.
2. MADHAVAN: logged-in tour (heart, wishlist, price-drop alert in the bell + Mailpit).
3. Explain-it-back: why a job instead of at once? how do we know the reviewer bought it?
4. Phase 10 - Smart search. Next Flyway: V13.
