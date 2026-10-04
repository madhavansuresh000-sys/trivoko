# Phase 2 - Accounts & Roles: Design Spec

**Date:** 4 October 2026 · **Status:** APPROVED by Madhavan (4 Oct 2026), incl. section 8 custom 404 page
**Branch:** `phase-2-accounts-and-roles` · **Plan source:** Phase Plan, Phase 2 (steps 1-9)

## 1. Goal

Safe login for customers, sellers and admins. A customer can apply to become a seller. A seller can only
touch their **own** products.

> Every person in the mall gets the right card: shoppers can enter the shop floor, shopkeepers can enter
> only their own storeroom, and the mall office can open every door.

### Decisions made while planning (4 Oct 2026)

| # | Decision | Why |
|---|---|---|
| P2-1 | Scope = plan as written. **No** forgot-password or email verification now; forgot-password moves to Phase 4 (emails are built there) | Write email code once |
| P2-2 | Ownership = **guard bean `SellerAccess`** + `@PreAuthorize` (403), and the seller's own list is filtered by seller id | Matches done-when (403); same pattern as EventHub `ClubAccess`; easy to explain |
| P2-3 | Every one of the 9 sample shops gets an owner user; `sellers.user_id` is `NOT NULL UNIQUE` (one user = one shop) | No half-linked data |
| P2-4 | Demo logins advertised: admin, customers Ravi and Kavya, sellers Chennai Mobiles and Kovai Sports | From the Phase Plan |
| P2-5 | Seller "submit" -> product PENDING -> admin approve -> ACTIVE (or reject with reason). Admin endpoints now, admin pages in Phase 6 | The whole flow can be tested |
| P2-6 | REJECTED seller may apply again; BLOCKED seller may not | |
| P2-7 | Audit log lives in module `admin` and is built now (package-info said Phase 6 - updated) | Phase Plan step 7 |
| P2-8 | JWT proves **who** you are; the JWT filter reads the user's **current roles + enabled flag from the database** on every request | Approval and blocking take effect immediately, no re-login |
| P2-9 | Login lasts 8 hours (like EventHub), no refresh token | Simple; enough for a shop |
| P2-10 | In Phase 2 a seller creates products with basic variants (label, size/colour, price, MRP, stock). Photos + full editor = Phase 5 | A product needs a price to be sellable |
| P2-11 | ACTIVE products: seller may change **only price and stock**; name/description/category edits only in DRAFT or REJECTED | Live products cannot be secretly renamed |

## 2. Database - Flyway `V3__accounts_and_roles.sql`

```
users                         user_roles                addresses (max 5 per user)
─────────────────────         ──────────────────        ─────────────────────────
id                            user_id  -> users.id      id
email      (unique)           role  CUSTOMER |          user_id -> users.id
password_hash (BCrypt)              SELLER | ADMIN      name, phone
full_name                     PK (user_id, role)        line1, line2, city,
phone                                                   state, pincode (6 digits)
enabled    (admin can block)                            is_default
created_at                                              created_at

sellers (existing; new columns)                       audit_log
──────────────────────────────                        ─────────────────────────────
+ user_id   -> users.id  UNIQUE NOT NULL              id, actor_user_id -> users.id
+ gstin     VARCHAR(15) NULL                          action (e.g. SELLER_APPROVED)
+ reject_reason VARCHAR(300) NULL                     entity_type, entity_id
                                                      details VARCHAR(500)
                                                      created_at
```

Migration order for `sellers.user_id`: add column NULL -> insert the 9 owner users -> link each shop to
its owner -> `ALTER ... NOT NULL` + unique + foreign key.

Seed users in V3 (all emails `@trivoko.test`): `admin`, `ravi`, `kavya`, and one owner per shop
(e.g. `chennai.mobiles@trivoko.test`). Their `password_hash` is a placeholder that can never match.
`DevDataSeeder` (profile `dev` only, copied from EventHub) sets the real BCrypt hash from `DEMO_PASSWORD`
in `.env` at start-up. The real password is never in Git.

Roles: Ravi and Kavya = CUSTOMER; admin = CUSTOMER + ADMIN; approved shop owners = CUSTOMER + SELLER;
Erode Organics' owner (shop PENDING) = CUSTOMER only.

## 3. Modules

| Module | Contents |
|---|---|
| `user` | `User`, `Role` enum, `Address`, repositories, `UserService` (find, roles, add/remove role, enable/disable), `AddressService`, `AddressController` |
| `auth` (new package) | `JwtService`, `AuthCookies`, `JwtCookieFilter`, `CsrfCookieFilter`, `LoginAttemptService`, `TooManyLoginAttemptsException`, `PasswordConfig`, `CurrentUser`, `AuthService`, `AuthController`, `DevDataSeeder` - copied from EventHub `com.eventhub.auth` and renamed |
| `seller` | `SellerService` (apply, status, approve/reject/block), `SellerAccess` guard, `SellerApplicationController`, `SellerProductController` |
| `catalog` | `ProductService` gains `createDraft`, `updateDraft`, `updateLive` (price + stock), `submit`, `approve`, `reject`, `findForSeller` |
| `admin` | `AuditLog` entity + repo, `AuditService.record(...)`, `AdminSellerController`, `AdminProductController` |

Module rule (ArchUnit) still holds: `seller` and `admin` call `ProductService` / `SellerService` /
`UserService`, never another module's repository.

## 4. Login and security

### Login flow

```
GET  /api/auth/csrf            -> XSRF-TOKEN cookie (readable by JS)
POST /api/auth/login  + X-XSRF-TOKEN header
     1. CSRF header == cookie?            no  -> 403
     2. LoginAttemptService locked?        yes -> 429 (15 min after 5 fails, per email + IP)
     3. BCrypt matches?                    no  -> 401, count a failure
     4. user.enabled?                      no  -> 403
     5. JWT {sub=id, email, name, roles}, HS256, 8 h, issuer "trivoko"
     <- Set-Cookie TRIVOKO_TOKEN=<jwt>; HttpOnly; SameSite=Lax; Path=/; Secure in prod
Later requests: JwtCookieFilter verifies the JWT, then loads current roles + enabled from the DB (P2-8).
```

### Auth endpoints (`/api/auth`)

| Endpoint | Behaviour |
|---|---|
| `POST /register` | `{email, password (min 8), fullName, phone?}` -> CUSTOMER, logged in (cookie). Duplicate email -> 409 |
| `POST /login` | as above |
| `POST /logout` | expired empty cookie |
| `GET /me` | id, email, fullName, roles, `seller: {id, shopName, slug, status} or null`; guest -> 401 |
| `GET /csrf` | sets the CSRF cookie |

### URL rules (`SecurityConfig`)

| URLs | Who |
|---|---|
| `/api/health`, GET `/api/products/**` `/api/categories/**` `/api/sellers/**`, `/api/auth/register` `/login` `/csrf` | everyone |
| `/api/me/**`, `/api/seller/apply`, `/api/seller/application`, `/api/auth/logout`, `/api/auth/me` | logged in |
| `/api/seller/**` (rest) | role SELLER **and** `SellerAccess.isActiveSeller()` (shop APPROVED) |
| `/api/admin/**`, Swagger (`/swagger-ui/**`, `/v3/api-docs/**`) | ADMIN |
| anything else | logged in (401 otherwise) |

### Config and keys

`application.yml`: `app.jwt.secret: ${JWT_SECRET}`, `app.jwt.expiry: 8h`, `app.cookie.secure` (false dev / true
prod), `app.login.max-failures: 5`, `app.login.lock-time: 15m`, `app.demo.password: ${DEMO_PASSWORD:}`.
`.env.example` gets `JWT_SECRET` (min 32 chars) and `DEMO_PASSWORD` with comments. Tests use fixed test
values from `src/test/resources`.

## 5. Seller and admin flows

```
Seller:  customer --apply--> PENDING --approve--> APPROVED (+SELLER role) --block--> BLOCKED
                                 └--reject(reason)--> REJECTED --apply again--> PENDING
Product: DRAFT --submit--> PENDING --approve--> ACTIVE
           ^                  └--reject(reason)--> REJECTED --edit--> (submit again)
```

### Seller endpoints

| Endpoint | Behaviour |
|---|---|
| `POST /api/seller/apply` | `{shopName, city, description?, gstin?}` -> PENDING; slug from shop name (unique, `-2` suffix if taken). Already PENDING/APPROVED -> 409; BLOCKED -> 403; REJECTED -> back to PENDING with new details |
| `GET /api/seller/application` | my status + reject reason; none -> 404 |
| `POST /api/seller/products` | `{name, categoryId (sub-category), brand, description, variants[1..10]{label, size?, colour?, price, mrp >= price, stock >= 0}}` -> DRAFT, sku generated |
| `GET /api/seller/products?status=&page=` | **my** products only, all statuses, paged (max 48) |
| `GET /api/seller/products/{id}` | one of mine, exact stock shown |
| `PUT /api/seller/products/{id}` | DRAFT/REJECTED: full edit. ACTIVE: price + stock only (other field changed -> 409); price via `changePrice` (writes `price_history`). PENDING -> 409 |
| `POST /api/seller/products/{id}/submit` | DRAFT/REJECTED -> PENDING |

Every `{id}` method: `@PreAuthorize("@sellerAccess.ownsProduct(#id)")`. Product exists but belongs to
another shop -> 403. Product id does not exist -> 404. (403 tells A that B's product id exists; that is
fine here, because every ACTIVE product is public anyway.)

### Admin endpoints (each writes `audit_log`)

- `GET /api/admin/sellers?status=PENDING`, `POST /api/admin/sellers/{id}/approve | /reject {reason} | /block`
- `GET /api/admin/products?status=PENDING`, `POST /api/admin/products/{id}/approve | /reject {reason}`
- Approve seller = status APPROVED + add SELLER role. Block seller = status BLOCKED (role kept; guard refuses).
  Approve product needs the shop to be APPROVED.

### Addresses (`/api/me/addresses`)

`GET`, `POST`, `PUT /{id}`, `DELETE /{id}`, `PUT /{id}/default`. Max 5 (6th -> 409). First address is
default; exactly one default; deleting the default makes the newest remaining one default. Another user's
address -> 404. Pincode = 6 digits, phone = 10 digits.

### Errors

Use the existing exceptions + `GlobalExceptionHandler` (ProblemDetail): `ResourceNotFoundException` 404,
`BadRequestException` 400, `BusinessRuleException` 409, new `TooManyLoginAttemptsException` 429 (with
`Retry-After`), security 401/403 via `SecurityProblems`.

## 6. Tests

| Test | Proves |
|---|---|
| **SellerOwnershipTest** | Seller A read/edit/submit on B's product -> 403; B's row unchanged; A's list has no B products |
| **UnapprovedSellerTest** | PENDING / REJECTED / BLOCKED seller create product -> 403 |
| AuthFlowTest | register -> login -> me -> logout -> me 401; duplicate 409; weak password 400 |
| LoginLockTest | 5 wrong -> 6th (right password) 429; after 15 min (fake clock) OK |
| CsrfTest | logged-in POST without CSRF header -> 403 |
| RoleChangeTest | approve -> same cookie reaches seller URLs; block -> refused at once |
| SellerApplicationTest | apply, double apply 409, re-apply after reject, blocked 403 |
| ProductLifecycleTest | DRAFT -> PENDING -> ACTIVE -> visible publicly; ACTIVE rename 409; price change writes price_history |
| AddressTest | max 5, one default, other user's address 404 |
| AuditLogTest | each admin action = one row (actor, action, entity) |
| SecurityRulesTest | Swagger 401 guest / 403 customer / 200 admin; public browsing still open |
| ArchUnit | module rule still green |

Postman generator: new folder "Accounts & roles" (login, apply, approve, create, submit, approve product,
403 check); newman all green. README: endpoint table + demo logins (password from `.env`).

## 7. Done when

- SellerOwnershipTest proves 403; UnapprovedSellerTest proves an unapproved seller cannot create products
- All tests pass (49 from Phase 1 + new) and newman green
- PR CI green and merged into `main` (with Madhavan's yes)
- Madhavan answers: httpOnly cookie vs localStorage, what CSRF protects against, how ownership is checked

## 8. Extra: custom 404 page (added at approval, 4 Oct 2026)

Madhavan asked for a custom 404 page and picked the **"helpful shop 404"** style.

**Frontend - `pages/NotFoundPage.jsx` (rewrite):**

```
      [ empty-shelf drawing, inline SVG, teal + saffron ]
                         404
                 This shelf is empty
  The link may be wrong, or the product may have been removed.
  [ Search products...              ] [Search]   -> /products?q=<text>
  Popular:  (first 6 top categories from GET /api/categories, as buttons -> /products?category=<slug>)
            [ Go home ]   [ <- Go back ]  (Go back only when there is browser history)
```

- Categories come from the real API; while loading show nothing, on error hide the "Popular" row (the
  page must never break).
- Works in dark mode and at phone width; `document.title` = "Page not found - TriVoKo"; the drawing has
  `aria-hidden`, the heading is the page's `h1`.
- New route `products` -> `ComingSoonPage` until Phase 3 builds the listing, so the search box and the
  category buttons do not lead to another 404.
- Test (Vitest): unknown URL shows the 404 heading; categories from a mocked API appear as links with
  the right `href`; API error -> page still shows, no "Popular" row; search submits to `/products?q=`.

**Backend - unknown `/api/...` URL:**

- Logged-in user calling a URL that does not exist -> **404 ProblemDetail** with
  `detail: "There is no API at /api/<path>"` (customised from Spring's `NoResourceFoundException`).
- Guest -> stays **401** (we do not tell strangers which URLs exist).
- Test: `UnknownApiUrlTest` (logged-in 404 JSON, guest 401).

## 9. Out of scope (later phases)

Forgot password (Phase 4), product photos + full editor + ledger (Phase 5), admin pages + user blocking UI
(Phase 6), frontend login pages (Phase 3), refresh tokens and Redis-backed login lock (only if needed at launch).
