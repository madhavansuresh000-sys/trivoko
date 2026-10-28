<p align="center">
  <img src="docs/trivoko-logo-mark.svg" width="72" alt="TriVoKo logo" />
</p>

<h1 align="center">TriVoKo</h1>
<p align="center"><b>One cart. Many sellers. One payment.</b><br/>A multi-seller marketplace (a small Flipkart) built with React + Spring Boot.</p>

<p align="center">
  <a href="../../actions/workflows/ci.yml"><img src="../../actions/workflows/ci.yml/badge.svg" alt="CI" /></a>
</p>

> **Status: Phase 10 - Smart search** (October 2026): one cart from many sellers, one payment, one package per seller (Phase 4); sellers ship, earn 90% and see their sales (Phase 5); the admin runs the marketplace (Phase 6); a flash sale hands out exactly the right number of units (Phase 7); a delivered item can be returned for exactly what was paid (Phase 8); verified-buyer reviews, a wishlist and ONE alert per real price drop (Phase 9). Now search forgives typos ("iphnoe" finds the iPhone case first), suggests while you type, counts results per brand, category and price, and shows what customers also bought.

## What it will do

- **Customers** search the catalogue, fill one cart from many sellers, pay once with Stripe, and track one package per seller.
- **Sellers** (approved by the admin) list products with sizes and colours, ship their own packages and see their earnings.
- **Admins** approve sellers and products, run flash sales, create coupons and read reports.

### Four standout features

| Feature | The rule it proves |
|---|---|
| ⚡ **Flash sale** | Thousands click at once, exactly the right number win - one atomic SQL update, proven with a 5,000-user k6 load test |
| ↩️ **Returns & refunds** | A return state machine and an exact Stripe *partial* refund; money, stock and the seller ledger always match |
| 🔔 **Price-drop alerts** | Wishlisted customers get one alert per real drop - never spam |
| 🔍 **Smart search** | Typo tolerance ("iphnoe" → iPhone), autocomplete and filter counts with Hibernate Search + Lucene |

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | React 19, Vite, React Router, Redux Toolkit, Tailwind CSS v4, Axios |
| Backend | Java 25, Spring Boot 4 (modular monolith, package `com.trivoko.*`), Spring Security, Spring Data JPA, Flyway |
| Database | MySQL 8.4 (Docker) |
| Payments | Stripe Checkout (test mode), webhooks, partial refunds |
| Testing | JUnit 5, Testcontainers, Vitest, Playwright, k6 |
| Delivery | Docker, GitHub Actions CI |

## Run it on your laptop

You need: Java 25, Node.js 24, Docker Desktop.

```bash
cp .env.example .env              # then choose your own passwords in .env
docker compose up -d              # MySQL on :3307, Mailpit inbox on http://localhost:8026
cd backend && ./mvnw spring-boot:run      # API on http://localhost:8080 (Swagger: /swagger-ui.html)
cd frontend && npm install && npm run dev # shop on http://localhost:5173
```

Tests: `cd backend && ./mvnw verify` (needs Docker - the tests start their own MySQL) and `cd frontend && npm test`.

API checks (backend running): `npx newman run postman/TriVoKo.postman_collection.json --env-var demoPassword=<DEMO_PASSWORD>` - or import the file in Postman.

### Catalogue API (Phase 1, public - no login)

| URL | What it returns |
|---|---|
| `GET /api/products?category=phones&sort=price` | Product cards. Filters: `category` (a top category includes its sub-categories), `brand` (one or more), `minPrice`, `maxPrice`, `inStock=true`, `seller`; `sort=newest\|price`, `dir=asc\|desc`, `page`, `size` (max 48) |
| `GET /api/products/{slug}` | One product: variants (price, MRP, % off, in stock, "only N left"), photos, seller, breadcrumb |
| `GET /api/categories` | The category tree (10 top categories x 3) |
| `GET /api/sellers/{slug}` | A shop page (approved shops only) |
| `POST /api/uploads/signature` | Logged-in only: a signature to upload one product photo straight to Cloudinary |

Also: `GET /api/products?q=volta case` (every word must be in the name or brand; typo-tolerant search comes in
Phase 10) and `GET /api/products/brands?category=phones` (brands with counts for the filter).

### Shop pages (Phase 3)

| Page | What you can do |
|---|---|
| `/` | Category chips and tiles, top deals, new arrivals |
| `/products` | Filters (category, brand, price, in stock) and sort - all kept in the URL, so links can be shared |
| `/products/{slug}` | Choose colour and size (sold-out sizes are crossed out), quantity, add to cart |
| `/shops/{slug}` | A seller's shop page |
| `/cart` | One box per seller (= one package), delivery per package: free from ₹499, else ₹40 |
| `/login`, `/register` | A guest's cart is merged into the saved cart after login |
| `/sell`, `/account/addresses` | Apply to become a seller; save up to 5 delivery addresses |

### Seller back office (Phase 5)

```
 PLACED --seller--> PACKED --tracking no.--> SHIPPED --> DELIVERED (+90% into the seller's ledger)
    └──── customer or seller, before shipping ────> CANCELLED (that package refunded, stock back)
```

| URL | Who | What it does |
|---|---|---|
| `GET /api/seller/dashboard`, `GET /api/seller/sales?days=7\|30\|90` | active seller | To pack, low stock, balance, today's sales; daily sales for the chart (cached a minute) |
| `GET /api/seller/packages?status=`, `POST /api/seller/packages/{id}/pack` · `ship {trackingNumber}` · `deliver` · `cancel {reason}` | owner seller | The packing table; another shop's package = 403; an illegal move = 409 |
| `POST /api/orders/{number}/packages/{id}/cancel` | the customer | Cancel one package before shipping (partial refund) |
| `GET /api/seller/ledger` | active seller | Balance (sum of the lines) and the lines |
| `GET /api/seller/products/low-stock`, `PUT /api/seller/products/{id}/images` | owner seller | Variants with fewer than 5 left; the photo list (max 8, only our Cloudinary photos) |

Pages: `/seller` (dashboard), `/seller/orders`, `/seller/products` (+ editor with photos), `/seller/earnings`,
`/seller/analytics`. The customer's order page shows a tracking timeline per package. Two people changing the same
package at once: `@Version` makes one of them get 409 (`PackageMovesTest.cancelVsShipRace`).

### Admin panel (Phase 6)

The admin is the mall office: decides which shops may open, closes a shop that cheats, prints coupons, reads the
sales register. Every URL below is ADMIN only (a customer or seller gets 403 - `AdminSecurityTest`), lists are paged
(20 a page, `PageResponse`) and every change writes one line in the activity log.

| URL | What it does |
|---|---|
| `GET /api/admin/overview` | Cards: shops and products waiting, customers, approved shops, today's orders and sales |
| `GET /api/admin/sellers?status=&q=&page=`, `POST .../{id}/approve` - `reject {reason}` - `block {reason}` - `unblock` | Shops queue. Block/reject need a reason (5-300 chars) that the owner sees; a blocked shop's products leave every shop page at once |
| `GET /api/admin/products?status=&q=&page=`, `POST .../{id}/approve` - `reject {reason}` | Product review queue |
| `GET /api/admin/users?q=&page=`, `POST .../{id}/block` - `unblock` | Search users; a blocked user cannot log in and their cookie stops working on the next click. An admin cannot be blocked (409) |
| `GET /api/admin/orders?q=&status=&page=`, `GET .../{number}` | Find an order by number or customer email |
| `GET/POST /api/admin/coupons`, `PUT .../{id}`, `POST .../{id}/end` | Edit (not the code or type) or end a coupon; ended coupons are kept because redemptions point to them |
| `GET /api/admin/reports/sales?days=` - `top-products?days=` - `top-sellers?days=` | `GROUP BY` reports, cached 60 s |
| `GET /api/admin/activity?action=&page=` | The audit log, newest first, with who did it |

Shop owners get a bell + email when their shop is approved, rejected, blocked or unblocked, and when a product is rejected
(approved products: bell only). Pages: `/admin` (overview), `/admin/shops`, `/admin/products`, `/admin/users`,
`/admin/orders`, `/admin/coupons`, `/admin/reports`, `/admin/activity`. Migration `V8__admin_panel.sql` adds the
block reason and an index for the activity log.

### Smart search (Phase 10)

A good shop assistant understands "iphnoe" means iPhone, and says "we have 3 cases and 2 chargers" before you ask.

```
 "iphnoe case" --> Lucene index (inside the app; name, brand, category name, description)
                   typo-tolerant, best match first --> up to 500 product ids
                                                           |
 MySQL <---------------------------------------------------+
   live? in stock? category / brand / price?   (always fresh - stock moves by bulk UPDATEs Lucene never sees)
   --> one page in Lucene's order + counts per brand, category and price band
```

| URL | Who | What it does |
|---|---|---|
| `GET /api/search?q=&category=&brand=&minPrice=&maxPrice=&inStock=&sort=relevance\|price\|newest&page=` | anyone | Results (cards) + facets: brands, categories, price bands, each counted without its own tick |
| `GET /api/search/suggest?q=` | anyone | From 2 letters: up to 8 suggestions, brands first, then products (live shops only) |
| `GET /api/products/{slug}/also-bought` | anyone | Up to 6 products from the same PAID orders, most often first (ranking cached 1 hour) |

How it stays sensible:

- **Typos by word length**: 1-4 letters exact (or the start of a word), 5-7 letters 1 typo, 8+ letters 2 typos, and
  the first letter must be right - so "iphnoe" finds the iPhone case but never drags in "Mobile phones"
  (`SmartSearchTest` checks the ranking, not only the hit list). Exact words count 4x, the name more than the brand.
- **Analyzers** (`SearchAnalysis`): lower-case + accents removed; suggestions use edge n-grams ("vol" -> Volta).
- **Index rebuilt on every start** (`SearchIndexer`, 120 products in ~0.3 s); folder `SEARCH_INDEX_DIR`
  (default `../data/search-index`, git-ignored); tests keep it in memory. A saved product is re-indexed at once.
- **Pages**: the navbar box suggests while typing (250 ms pause, arrow keys, Enter, Esc, ARIA combobox);
  `/products?q=...` uses the smart search with "Best match" and the counts; the product page shows
  "Customers also bought" when there are at least 2. Decision: D11 (draft - to be agreed).

### Reviews, wishlist & price-drop alerts (Phase 9)

You tell the shopkeeper "call me if this TV gets cheaper". He calls you once when it does - not five times because
he changed the price tag five times in a minute.

```
 seller saves a price --> ProductService.changePrice --> price_history row + PriceChanged event
                                                              | (only after the save is committed)
                                                              v
                       wishlist_items.price_changed = TRUE for that product   (5 edits = still ONE mark)
                                                              |
  every 10 min  PriceAlertJob --------------------------------+
     price NOW at least 1% below the last price the customer heard from us (or the price when saved)?
        yes -> ONE bell + email PRICE_DROP, remember this price        always -> clear the mark
```

| URL | Who | What it does |
|---|---|---|
| `GET /api/products/{slug}/reviews?page=` | anyone | Average (1 decimal), count, 5..1 bars + 10 reviews per page, newest first ("Ravi K.", Verified buyer) |
| `GET /api/products/{slug}/reviews/mine` | customer | `canReview` (a DELIVERED package of mine holds this product) + my review |
| `PUT /api/products/{slug}/reviews/mine` `{rating, title, body}` | customer who received it | Write, or edit (one review per product - UNIQUE key); else 403 |
| `DELETE /api/products/{slug}/reviews/mine` | author | Delete my review |
| `GET /api/me/wishlist`, `GET /api/me/wishlist/ids` | customer | My saved products with "Rs X lower than when you saved it"; the ids fill the hearts |
| `PUT /api/me/wishlist/{productId}`, `DELETE ...` | customer | Save (twice = still one row, at most 100) / remove |
| `GET /api/products/{slug}` | anyone | now also `ratingAvg`, `ratingCount`, `lowestPrice30Days`, `lowestNow` (cards get the stars too) |

How it stays honest and quiet:

- **Verified buyer**: the review module asks the order module `hasDelivered(user, variants of this product)` - it never
  reads the order tables (module rule). A later return does not take it away: they did receive and try it.
- **Stars on cards without extra queries**: after every review write the average and count are saved on
  `products.rating_avg / rating_count`, so a listing of 24 cards reads them from the product row.
- **One alert per real drop**: the event only marks rows; the job reads the LATEST price. 999 -> 899 alerts once;
  899 -> 950 -> 899 does not alert again; a drop under 1%, a blocked shop or an empty shelf sends nothing
  (`PriceAlertTest`: 5 changes in a minute -> 1 alert).
- **Lowest price in 30 days**: the smallest of today's price and every old / new price of the product's changes in
  the last 30 days (`price_history`, index `(variant_id, changed_at)`). The green badge shows only if the price moved.
- Pages: stars + reviews on the product page, a heart on every card, `/wishlist`, "Write a review" on delivered order
  lines. Migration: `V12__reviews_wishlist.sql`. Job interval: `app.wishlist.alert-every` (10m).

### Returns & refunds (Phase 8)

You return one shirt from a bill of three: the shop checks it, puts it back on the shelf and gives back only that
shirt's money - minus its part of the coupon you used.

```
 REQUESTED --seller / 2 days no answer--> APPROVED --seller "picked up"--> PICKED_UP --refund ok--> REFUNDED
     |                                                                         \--refund fails--> REFUND_FAILED --admin retry--> REFUNDED
     +--seller "no" + reason--> REJECTED --customer, once, within 3 days--> ESCALATED --admin--> APPROVED | CLOSED
     +--customer cancels--> CANCELLED
```

| URL | Who | What it does |
|---|---|---|
| `POST /api/me/returns` `{orderItemId, reason, note}` | customer | Ask: own line only, package DELIVERED, within 7 days, once per line (UNIQUE key) |
| `GET /api/me/returns`, `POST .../{id}/cancel`, `POST .../{id}/escalate {note}` | customer | My returns with their timeline; cancel before the seller answers; ask TriVoKo to review a "no" once |
| `GET /api/seller/returns?status=`, `POST .../{id}/approve` - `reject {reason}` - `picked-up` | seller | My shop's returns; "picked up" refunds at once |
| `GET /api/admin/returns?status=ESCALATED\|REFUND_FAILED`, `POST .../{id}/approve` - `close {reason}` - `retry-refund` | admin | The final word on disputes; retry a failed refund |

How the money stays right:

- **One door for every move**: `ReturnStatus.allowedNext()` + `ReturnService.moveTo()` (illegal move = 409), and
  `@Version` + `saveAndFlush` stop a second click before any money moves (10 clicks at once = 1 refund, `ReturnRefundTest`).
- **Exact amount, fixed when asked**: `refund = line total - the line's coupon share` (both stored on the bill since
  Phase 4). Delivery is not refunded.
- **Stripe partial refund** with the idempotency key `refund-return-<id>`; `PaymentService` never gives back more
  than was paid. A failure rolls the refund back and shows REFUND_FAILED; the admin retries with the same key.
- **After the refund**, in the same transaction: the item goes back in stock, the seller's ledger gets a line of
  **-90%** of the refund (they only ever got 90%), and the customer gets a bell + email. V10 changed the ledger key
  so two items of one package give two refund lines.
- A seller who does not answer in **2 days** has said yes (`ReturnJob`, every 10 minutes).
- Pages: "Return" on a delivered order line, `/account/returns`, `/seller/returns`, `/admin/returns`.
  Migrations: `V10__returns.sql`, `V11__return_snapshot.sql`.

### Flash sale (Phase 7)

100 TVs, 5,000 people at the door: the guard hands out exactly 100 tokens, one per person. Token 101 does not exist.

| URL | Who | What it does |
|---|---|---|
| `GET /api/flash-sales/current`, `GET /api/flash-sales/{id}` | anyone | The live (or next) sale, its deals and "N left"; the server's clock is sent so the countdown cannot be cheated |
| `POST /api/flash-sales/items/{itemId}/claim` | customer | Claim one unit: held at the sale price for 10 minutes, then checkout |
| `GET /api/me/flash-claims` | customer | My held / ordered / paid deals |
| `GET/POST /api/admin/flash-sales` | admin | Schedule a sale (start, end) |
| `GET /api/seller/flash-sales`, `GET .../offers`, `POST .../{saleId}/offers`, `DELETE .../offers/{itemId}` | seller | Offer a variant with a sale price and quantity (taken out of normal stock at once) |

How it stays correct:

- **The claim is one statement**: `UPDATE flash_sale_items SET sold = sold + 1 WHERE id = ? AND sold < quantity AND
  the sale is live`. MySQL locks the row, checks and adds in one step, so two people can never both get the last unit.
  1 row changed = you won, 0 = sold out / not live.
- **One per person**: `UNIQUE (item_id, user_id)` on `flash_sale_claims`; a second claim fails and its +1 is rolled back.
- **The database refuses an oversell** even if the code were wrong: `CHECK (sold <= quantity)`.
- **Give-back**: a claim not paid in 10 minutes (job every 30 s) returns its unit (`sold = sold - 1`, never below 0).
- **Sale end**: unsold units go back to normal stock once (`settled`).
- **Rate limit**: 5 claims per user and 200 per IP every 10 s, then `429` with `Retry-After`.
- Pages: home banner with a countdown, `/flash-sale` with a live "N left" counter and an instant "Sold out",
  `/seller/flash` (offers) and `/admin/flash-sales`.

**Load test** (k6 in Docker, app in Docker, one laptop; `powershell -File load-tests\run-flash-load.ps1 [-Shape burst|spread]`,
results in `load-tests/results`):

| Shape | Requests | Winners | Sold out | Errors | Oversell | p95 | median |
|---|---|---|---|---|---|---|---|
| burst - 5,000 users press at the same instant | 5,600 (~1,480/s) | **100** | 4,900 | **0** | **0** | 1,082 ms | 681 ms |
| spread - 1,000 users a second for 5 s | 5,000 (~1,000/s) | **100** | 4,900 | **0** | **0** | 517 ms | 2.4 ms |

The database agreed after both runs: `sold = 100`, 100 claim rows, 100 different people. Correctness is proven; speed in
the burst is above the 500 ms target (see decision D8 in `docs/decisions.md`).

### Checkout & payments (Phase 4)

```
 Cart -> /checkout (preview: packages, coupon, delivery) -> Pay
   -> server rechecks prices, holds stock (one atomic UPDATE per item), creates order + packages
   -> Stripe page (or the test page) -> paid: order PAID, packages PLACED, emails, invoice
   -> not paid in 10 minutes: EXPIRED, stock back (a late payment re-takes it or is refunded)
```

| URL | Who | What it does |
|---|---|---|
| `POST /api/checkout/preview` `{addressId?, couponCode?}` | logged in | Packages, coupon result, delivery and total - nothing saved |
| `POST /api/orders` `{addressId, couponCode?, expectedTotal}` | logged in | Places the order; 409 if prices changed or an item ran out; one unpaid order at a time |
| `GET /api/orders`, `GET /api/orders/{number}`, `GET /api/orders/{number}/invoice.pdf` | owner | My orders, one order, the PDF invoice (paid orders) |
| `POST /api/payments/stripe/webhook` | Stripe (signature) | Marks the order paid; a duplicate message changes nothing |
| `POST /api/payments/{session}/verify` | owner | Back from Stripe: check now (the webhook may be late) |
| `GET/POST /api/payments/fake/{session}[/complete]` | owner | The built-in test payment page (no Stripe key) |
| `GET /api/coupons/{code}`, `GET/POST /api/admin/coupons` | logged in / admin | Coupons: `WELCOME10` (10% up to Rs 500 from Rs 499), `FLAT100` (Rs 100 off from Rs 999) |
| `GET /api/notifications`, `POST /api/notifications/read-all` | logged in | The bell |

Money rules: the coupon is shared across items by price (the last item takes the rounding remainder), stored
per item for exact refunds later; delivery is per package - free from Rs 499 (before the coupon), else Rs 40.
Tested by `OrderSplitterTest`, and `StockConcurrencyTest` (100 customers, last 10 units -> exactly 10 orders).

**Real Stripe (test mode):** put `STRIPE_SECRET_KEY` (sk_test_...) in `.env`, run
`stripe listen --forward-to localhost:8080/api/payments/stripe/webhook`, copy the `whsec_...` it prints into
`STRIPE_WEBHOOK_SECRET`, restart the backend, and pay with card `4242 4242 4242 4242` (any future date, any CVC).

### Cart API (Phase 3)

Prices are never stored in the browser or the cart table - the server prices the cart from the catalogue every
time (`CartPricing`), for the saved cart and the guest preview alike.

| URL | Who | What it does |
|---|---|---|
| `POST /api/cart/preview` `{items:[{variantId, quantity}]}` | anyone | Prices a guest cart from the browser; nothing is saved |
| `GET /api/cart` | logged in | My cart, grouped into packages per seller |
| `PUT /api/cart/items/{variantId}` `{quantity}` · `DELETE ...` | logged in | Set a quantity (1-10, capped by stock) or remove a line; at most 30 lines |
| `POST /api/cart/merge` | logged in | After login: the guest lines join my cart (same item = quantities added) |

### Accounts & roles API (Phase 2)

Login sets an **HttpOnly JWT cookie** (8 hours). Every `POST / PUT / DELETE` also needs the CSRF token:
call `GET /api/auth/csrf` once, then send the `XSRF-TOKEN` cookie value back in the `X-XSRF-TOKEN` header.
Roles are read from the database on every request, so an admin's approve or block works at once.

| URL | Who | What it does |
|---|---|---|
| `POST /api/auth/register` · `login` · `logout` | anyone | New customer account; login (5 wrong passwords = locked for 15 minutes); logout clears the cookie |
| `GET /api/auth/me` · `GET /api/auth/csrf` | logged in · anyone | Who am I (roles + my shop); get the CSRF cookie |
| `GET POST /api/me/addresses`, `PUT /{id}`, `PUT /{id}/default`, `DELETE /{id}` | customer | Delivery addresses (max 5, exactly one default) |
| `POST /api/seller/apply` · `GET /api/seller/application` | customer | Apply to open a shop; see the application status |
| `GET POST /api/seller/products`, `GET PUT /{id}`, `POST /{id}/submit` | approved seller | Own products only (another shop's product = 403); a new product is a draft until submitted |
| `GET /api/admin/sellers`, `POST /{id}/approve` · `reject` · `block` | admin | Shop applications; approve adds the SELLER role, block hides the shop |
| `GET /api/admin/products`, `POST /{id}/approve` · `reject` | admin | Products waiting for review |

Swagger (`/swagger-ui.html`) is for the admin only. Every admin action is written to the audit log.

### Demo logins

Set `DEMO_PASSWORD` (and `JWT_SECRET`, 32+ characters) in `.env`; on start-up the backend gives that
password to the demo accounts. Empty = the demo accounts stay locked. The password is never in Git.

| Email | Role | Use it for |
|---|---|---|
| `admin@trivoko.test` | ADMIN (+ customer) | Approve shops and products |
| `ravi@trivoko.test`, `kavya@trivoko.test` | CUSTOMER | Shopping, addresses |
| `chennai.mobiles@trivoko.test`, `kovai.sports@trivoko.test`, `bengaluru.gadgets@`, `madurai.handlooms@`, `mumbai.style@`, `salem.steel@`, `pondy.books@`, `tirupur.kids@` | SELLER | One login per approved shop |
| `erode.organics@trivoko.test` | CUSTOMER | Shop application still pending - try the admin approval |

Postman: `npx newman run postman/TriVoKo.postman_collection.json --env-var demoPassword=<your DEMO_PASSWORD>`

## Project folders

| Folder | What is inside |
|---|---|
| `backend/` | Spring Boot API - one package per module (`catalog`, `order`, `payment`, `flashsale` ...) |
| `frontend/` | React app |
| `00_Project_Documents/` | Master Plan and Phase Plan (Word + PDF) |
| `Phase_0_Setup/` ... `Phase_12_Launch/` | One checklist document per phase |
| `docs/` | Sketches, decisions, diagrams |
| `load-tests/`, `e2e/` | k6 load tests (Phase 7), Playwright tests (Phase 11) |
| `postman/` | Postman collection with automatic checks |
| `tools/` | Generators: plan documents, sample data (`seed-gen`), Postman collection (`postman-gen`) |

## To-do carried over from EventHub

- Real Stripe test in test mode: the code is ready (Phase 4); waiting for a Stripe test account - see "Real Stripe" above.

---

Built by **Madhavan Suresh** as a Java full stack portfolio project. Practice project before this: [EventHub](https://github.com/madhavansuresh000-sys/eventhub).
