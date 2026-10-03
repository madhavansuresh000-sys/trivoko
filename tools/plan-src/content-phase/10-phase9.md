# Phase 9 - Reviews, wishlist, alerts

**Time:** 1 week  ·  **Group:** Shine  ·  **Folder:** `Phase_9_Reviews_Wishlist_Alerts\`

**Goal:** Standout #3: verified-buyer reviews, a wishlist, and price-drop alerts that are useful and never spammy.

>> You ask the shopkeeper, "Call me if this TV gets cheaper." He calls once when it does - not five times when he changes the tag five times in a minute.

## Steps

@widths 600 6926 1500
| # | Step | Who |
| 1 | Flyway: `reviews`, `wishlist_items` (price when added, last alerted price) | Claude |
| 2 | Reviews only for delivered items, one per item; rating average and star breakdown on the product page | Claude |
| 3 | Wishlist API + heart button + wishlist page | Claude |
| 4 | `PriceChanged` event; alert job every 10 minutes using the latest price; one alert per drop; email + bell | Claude |
| 5 | "Lowest price in 30 days" on the product page from `price_history` | Claude |
| 6 | Tests: 5 price changes in one minute → one alert | Claude |

## Output of this phase

- Reviews, wishlist and price-drop alerts

## Done when

- Lower a price → a wishlisted customer gets one alert (not five)

## Explain it back

- Why do alerts come from a job every 10 minutes instead of at once?
- How do we know a reviewer really bought the product?

**Skills you practise:** Spring events, scheduled jobs, aggregate queries

**Where your work goes:** `backend\...\review`, `...\wishlist`, `...\notify`
