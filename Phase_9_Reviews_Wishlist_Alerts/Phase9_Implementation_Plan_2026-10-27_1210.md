# Phase 9 - Reviews, wishlist, alerts: implementation plan

Spec: `Phase9_Design_Spec_2026-10-27_1207.md` · Branch `phase-9-reviews-wishlist-alerts` · Test-first for every
backend task (Docker must be running) · One PR per finished part, MADHAVAN clicks Merge.

| # | Phase Plan step | Task | Main files | Test |
|---|---|---|---|---|
| 1 | step 1 | V12: reviews, wishlist_items, products rating columns; entities + repositories | `V12__reviews_wishlist.sql`, `review/Review`, `wishlist/WishlistItem` | `ReviewWishlistSchemaTest` |
| 2 | step 2 | Reviews: may-review check (order module `hasDelivered`), create/edit/delete mine, summary + page, `ProductService.updateRating` | `review/ReviewService`, `ReviewController`, `order/OrderService` | `ReviewTest` |
| 3 | step 3 | Wishlist API (add idempotent, remove, list with price change, ids) | `wishlist/WishlistService`, `WishlistController` | `WishlistTest` |
| 4 | step 4 | `PriceChanged` event from `changePrice`; listener flags rows; `PriceAlertJob` (latest price, >= 1% below reference, live + in stock) -> bell + email `PRICE_DROP` | `catalog/PriceChanged`, `wishlist/PriceAlertService`, `PriceAlertJob` | `PriceAlertTest` (5 changes -> 1 alert) |
| 5 | step 5 | Lowest price in 30 days + `lowestNow` on the product page; rating on cards | `catalog/ProductService`, `CatalogMapper`, DTOs | `LowestPriceTest` |
| 6 | steps 2-5 UI | Frontend: rating summary + reviews list + form, heart button, `/wishlist` page, "write a review" on delivered items, lowest-price line | `components/review/*`, `WishlistButton`, `WishlistPage` | Vitest |
| 7 | step 6 + docs | README Phase 9 + decision D10, Postman folder, full `mvnw verify` + npm checks, browser check | | |

PR split: **PR A** = tasks 1-3 (reviews + wishlist API), **PR B** = tasks 4-5 (alerts + lowest price), **PR C** = task 6-7 (pages + docs).
