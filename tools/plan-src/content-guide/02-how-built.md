# 2. How TriVoKo is built

## 2.1 The big picture

![Architecture](architecture.png)

```
  Browser (phone / laptop)
     |  https://trivoko.vercel.app  - one address
     v
  Vercel ── React app (built by Vite)
     |  /api/*  is passed on (rewrite)
     v
  Render ── Spring Boot 4 in Docker (Java 25, 512 MB memory)
     |      17 modules, Lucene search index rebuilt on start
     v
  Aiven ── MySQL 8.4 (tables built by Flyway)
```

**Why one address?** The browser only talks to Vercel. Vercel quietly passes every `/api/...` call to Render. So for the browser the shop and the API are the same website, and the login cookie works as a normal first-party cookie - no cross-site cookie problems, no CORS in normal use.

## 2.2 Frontend (React)

- **React 19 + Vite** for the pages, **React Router** for the URLs, **Tailwind CSS** for the look (light and dark mode).
- **Redux Toolkit** keeps what many pages need: who is logged in, the cart, the wishlist hearts.
- **One door for the server:** every call goes through `src/api/client.js` (Axios). Errors from the server come back as one standard shape (RFC 7807 `ProblemDetail`) and `describeError()` turns them into friendly sentences.
- **Guest cart:** a visitor's cart lives in the browser (localStorage). At login it is merged into the saved cart on the server.

## 2.3 Backend (Spring Boot) - a modular monolith

>> A mall has one building, one entrance and one security guard, but every shop has its own locked storeroom. A shop may ask another shop's counter for something, but may never walk into its storeroom.

TriVoKo is **one Spring Boot application** (one building) split into **modules** (shops): `catalog`, `seller`, `cart`, `order`, `payment`, `coupon`, `flashsale`, `returns`, `review`, `wishlist`, `search`, `notify`, `analytics`, `admin`, `user`, `auth`, `health`.

**The rule:** a module may call another module's **service**, never its **repository** or its tables. An **ArchUnit** test checks this on every build - if someone breaks the rule, the build fails.

**Why not microservices?** One person, one small database, one deployment. Microservices would add network calls, many deployments and distributed transactions with no benefit at this size. The module rule keeps the code ready to be split later if it ever must be.

Inside each module the layers are always the same:

```
  Controller  (URL, JSON in/out, validation)
      |
  Service     (the business rules, @Transactional)
      |
  Repository  (Spring Data JPA)  ->  MySQL
```

## 2.4 Database

- **MySQL 8.4.** Money is `DECIMAL`, never `double`.
- **Flyway owns the schema:** files `V1__...sql` to `V12__...sql` build every table. Hibernate is set to `validate` - it only checks that the Java classes match. An applied migration is never edited; a change is a new file.
- **Important tables:** `products` and `product_variants` (one row per size/colour with its own price and stock), `orders` -> `packages` (one per seller) -> `order_items`, `payments`, `seller_ledger`, `flash_sale_items` + `flash_sale_claims`, `return_requests`, `price_history`, `wishlist_items`, `reviews`, `audit_log`.

![Database tables](er-diagram.png)

## 2.5 Security

- **Login:** password stored with **BCrypt**. On login the server puts a **JWT** in an **HttpOnly, Secure, SameSite=Lax** cookie for 8 hours. JavaScript cannot read it, so an injected script cannot steal it.
- **Roles are read from the database on every request** (not trusted from the token), so when the admin blocks someone, it works on their very next click.
- **CSRF:** every POST/PUT/DELETE must send the `XSRF-TOKEN` cookie value back in the `X-XSRF-TOKEN` header.
- **Login lock:** 5 wrong passwords = 15 minutes locked, counted per email and per visitor address.
- **Ownership checks:** a seller can only touch their own products and packages (`@sellerAccess`), otherwise 403.
- **Headers on the live site:** Content-Security-Policy (scripts only from our own address), HSTS, nosniff, no framing.
- **Secrets** (database password, JWT secret, API keys) are environment variables - never in Git. `gitleaks` checks every commit.
