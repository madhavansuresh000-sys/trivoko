<p align="center">
  <img src="docs/trivoko-logo-mark.svg" width="72" alt="TriVoKo logo" />
</p>

<h1 align="center">TriVoKo</h1>
<p align="center"><b>One cart. Many sellers. One payment.</b><br/>A multi-seller marketplace (a small Flipkart) built with React + Spring Boot.</p>

<p align="center">
  <a href="../../actions/workflows/ci.yml"><img src="../../actions/workflows/ci.yml/badge.svg" alt="CI" /></a>
</p>

> **Status: Phase 3 - Shop frontend** (October 2026): browse, search and filter 120 sample products from 8 shops, pick a size or colour, and fill ONE cart from many sellers - the cart is grouped into one package per seller and is kept when a guest logs in. Works on phones and in dark mode. Payment comes in Phase 4. Built in 13 phases; see `00_Project_Documents/TriVoKo_Phase_Plan_*.pdf`.

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

- Real Stripe test in test mode (Phase 4): `sk_test` key in `.env`, pay with card 4242 4242 4242 4242, receive a real signed webhook with the Stripe CLI.

---

Built by **Madhavan Suresh** as a Java full stack portfolio project. Practice project before this: [EventHub](https://github.com/madhavansuresh000-sys/eventhub).
