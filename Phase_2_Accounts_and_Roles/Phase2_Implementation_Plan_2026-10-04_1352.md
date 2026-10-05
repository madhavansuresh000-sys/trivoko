# Phase 2 - Accounts & Roles Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Login for customers, sellers and admins; seller application + approval; sellers manage only their own products; custom 404 page.

**Architecture:** Copy EventHub's cookie-JWT auth (`com.eventhub.auth`) into a new `com.trivoko.auth` package, store users/roles/addresses in module `user`, put the ownership guard `SellerAccess` in module `seller`, approvals + audit log in module `admin`. Modules talk through services only (ArchUnit).

**Tech Stack:** Spring Boot 4.1 / Java 25, Spring Security (oauth2-jose for Nimbus JWT), Flyway, MySQL 8.4 (Testcontainers), React 19 + Vitest.

**Spec:** `Phase_2_Accounts_and_Roles/Phase2_Design_Spec_2026-10-04_1347.md`

## Global Constraints

- Cookie name `TRIVOKO_TOKEN`; JWT HS256, issuer `trivoko`, expiry `8h`; secret >= 32 chars.
- Login lock: 5 failures per (email + IP) -> 15 minutes -> HTTP 429 with `Retry-After`.
- Roles: `CUSTOMER`, `SELLER`, `ADMIN`. Every new account = CUSTOMER. SELLER added on seller approval.
- JWT filter loads **current roles + enabled** from the DB each request (spec P2-8).
- Seed emails end in `@trivoko.test`; demo password only from `DEMO_PASSWORD` (dev) - never in Git.
- Max 5 addresses; pincode 6 digits; phone 10 digits. Max 10 variants per product; `mrp >= price`; `stock >= 0`.
- Never edit V1/V2; new migration is `V3__accounts_and_roles.sql`.
- Errors via existing exceptions (`ResourceNotFoundException` 404, `BadRequestException` 400, `BusinessRuleException` 409) + `GlobalExceptionHandler`.
- Tests that change data are `@Transactional` (MockMvc runs in the test thread -> rolled back).
- Plain-English "why" comments, like the rest of the codebase. One commit per task: `Phase 2 step N: ...`.

## Review Focus

- Email case/spaces: `" Ravi@TriVoKo.test "` must log in as `ravi@trivoko.test` (normalise on register + login) - test in Task 2.
- A JWT for a user who was **disabled** after login must be refused at once (401) - test in Task 2.
- Seller edits an ACTIVE product sending the **same** name but new price -> allowed (only *changed* locked fields are refused) - test in Task 6.
- Deleting the default address when others exist -> newest remaining becomes default - test in Task 3.
- Shop name that slugifies to an existing slug (e.g. "Chennai Mobiles!") -> `chennai-mobiles-2` - test in Task 5.

---

### Task 1 (plan step 1): Flyway V3 + user module entities

**Files:**
- Create: `backend/src/main/resources/db/migration/V3__accounts_and_roles.sql`
- Create: `backend/src/main/java/com/trivoko/user/{User,Role,Address,UserRepository,AddressRepository}.java`
- Modify: `backend/src/main/java/com/trivoko/seller/Seller.java` (+ `ownerId` Long column `user_id`, `gstin`, `rejectReason`)
- Modify: `backend/src/main/java/com/trivoko/admin/package-info.java` ("Built in Phase 2 (audit log) and 6")
- Test: `backend/src/test/java/com/trivoko/user/AccountsSeedTest.java`

**Interfaces - Produces:**
- `enum Role { CUSTOMER, SELLER, ADMIN }`
- `User` (id, email, passwordHash, fullName, phone, enabled, createdAt, `Set<Role> roles` as `@ElementCollection` on `user_roles(user_id, role)`)
- `UserRepository.findByEmail(String)`, `existsByEmail(String)`
- `Seller.getOwnerId()` (plain `Long` column, no entity link across modules), `SellerRepository.findByOwnerId(Long)`

**SQL decisions:** `users.email VARCHAR(160) UNIQUE`, `password_hash VARCHAR(100)`, `enabled BOOLEAN DEFAULT TRUE`;
`addresses` with FK + index on user_id; `audit_log` (id, actor_user_id FK, action VARCHAR(40), entity_type VARCHAR(30),
entity_id BIGINT, details VARCHAR(500), created_at). Seed users ids 1-12: 1 admin, 2 ravi, 3 kavya, 4-12 owners of
sellers 1-9 (`chennai.mobiles@`, `kovai.sports@`, `bengaluru.gadgets@`, `madurai.handlooms@`, `mumbai.style@`,
`salem.steel@`, `pondy.books@`, `tirupur.kids@`, `erode.organics@trivoko.test`). Placeholder hash `{noop}!locked` -
cannot match any BCrypt check. Roles: admin CUSTOMER+ADMIN; ravi, kavya CUSTOMER; owners 4-11 CUSTOMER+SELLER; 12 CUSTOMER.
`sellers.user_id = id + 3`, then NOT NULL + UNIQUE + FK.

- [ ] Step 1: Write `AccountsSeedTest` (`@DataJpaTest`-style `@SpringBootTest`): 12 users; `findByEmail("admin@trivoko.test").roles == {CUSTOMER, ADMIN}`; every seller has an owner and owner ids are unique; owner of `erode-organics` has only CUSTOMER.
- [ ] Step 2: Run `mvnw test -Dtest=AccountsSeedTest` -> FAIL (no table).
- [ ] Step 3: Write V3 + entities + repositories.
- [ ] Step 4: Run test + `ModuleRulesTest` + `CatalogSeedTest` -> PASS.
- [ ] Step 5: Commit `Phase 2 step 1: Flyway V3 users, roles, addresses, audit_log; shop owners`.

### Task 2 (plan steps 2, 3, 8): Auth - JWT cookie, login, register, lock, demo accounts

**Files:**
- Create in `backend/src/main/java/com/trivoko/auth/`: `JwtService`, `AuthCookies`, `JwtCookieFilter`, `CsrfCookieFilter`, `LoginAttemptService`, `TooManyLoginAttemptsException`, `PasswordConfig`, `AuthUser`, `AuthService`, `AuthController`, `DevDataSeeder`, `package-info`, `dto/{RegisterRequest,LoginRequest,MeResponse}` - copied from `../eventhub/backend/src/main/java/com/eventhub/auth` and renamed.
- Create: `backend/src/main/java/com/trivoko/user/UserService.java`
- Modify: `config/SecurityConfig.java`, `common/GlobalExceptionHandler.java` (429), `application.yml`, `pom.xml` (if `spring-boot-starter-oauth2-resource-server` / jose missing), `.env.example`
- Modify: `backend/src/test/java/com/trivoko/MySqlTestDatabase.java` (add `app.demo.password=Test-Demo-Pass-1`)
- Create test helper: `backend/src/test/java/com/trivoko/support/Logins.java`
- Tests: `auth/AuthFlowTest`, `auth/LoginLockTest` (unit, fake `Clock`), `auth/CsrfTest`

**Interfaces - Produces:**
- `record AuthUser(Long id, String email, String fullName, Set<Role> roles)` = the `@AuthenticationPrincipal`; `static Optional<AuthUser> current()`
- `UserService`: `User register(String email, String rawPassword, String fullName, String phone)`, `Optional<User> findActiveById(Long)`, `User getByEmail(String)`, `void addRole(Long userId, Role)`, `static String normalizeEmail(String)`
- `SellerService.findByOwner(Long userId): Optional<SellerSummary>` (record id, shopName, slug, status) - used by `/me`
- `MeResponse(Long id, String email, String fullName, List<String> roles, SellerSummary seller)`
- Test helper `Logins.as(MockMvc mvc, String email): Cookie` (POST `/api/auth/login` with csrf + `Test-Demo-Pass-1`) and `Logins.PASSWORD`
- Authorities = `ROLE_<role>` so `hasRole("ADMIN")` works.

**Config:** `app.jwt.secret: ${JWT_SECRET}` (prod), dev fallback `dev-only-secret-not-for-production-0123456789`; `app.jwt.expiry: 8h`; `app.cookie.secure: false` (prod true); `app.login.max-failures: 5`; `app.login.lock-time: 15m`; `app.demo.password: ${DEMO_PASSWORD:}`.
`DevDataSeeder`: profile `dev` (also active in tests - default profile), sets BCrypt(demo password) on the 12 seed users when the password is non-empty.

- [ ] Step 1: Write tests. `AuthFlowTest`: register `{email:" New@Shop.test ", password:"longenough1", fullName:"New User"}` -> 201 + `Set-Cookie TRIVOKO_TOKEN` HttpOnly; `/me` with cookie -> email `new@shop.test`, roles `["CUSTOMER"]`, seller null; logout -> cookie Max-Age=0; register same email -> 409; password `short` -> 400; login wrong password -> 401; login `" Ravi@TriVoKo.test "` -> 200; disabled user's old cookie -> `/me` 401. `LoginLockTest`: 5 `failed` -> `checkAllowed` throws; clock + 15 min -> allowed; different IP not locked. `CsrfTest`: logged-in POST `/api/auth/logout` without csrf -> 403.
- [ ] Step 2: Run -> FAIL.
- [ ] Step 3: Copy + adapt the EventHub classes; `JwtCookieFilter` reads `sub`, calls `UserService.findActiveById` -> no user / disabled -> no authentication (401 later).
- [ ] Step 4: Run new tests + all old tests -> PASS (old `UploadSignatureTest` uses `with(user(...))` - keep working).
- [ ] Step 5: Commit `Phase 2 steps 2-3, 8: cookie JWT login, register, lock, roles, demo accounts`.

### Task 3 (plan step 4): Addresses API

**Files:** Create `user/AddressService.java`, `user/AddressController.java`, `user/dto/{AddressRequest,AddressView}.java`; Test `user/AddressTest.java`.

**Interfaces - Produces:** `GET/POST /api/me/addresses`, `PUT/DELETE /api/me/addresses/{id}`, `PUT /api/me/addresses/{id}/default`. `AddressRequest(name, phone @Pattern("\\d{10}"), line1, line2, city, state, pincode @Pattern("\\d{6}"))`.

- [ ] Step 1: `AddressTest` (logged in as ravi): first address -> isDefault true; 5 ok, 6th -> 409; set default on #3 -> only #3 default; delete default -> newest remaining is default; kavya GET/PUT/DELETE ravi's address -> 404; pincode `12345` -> 400.
- [ ] Step 2: Run -> FAIL. Step 3: implement. Step 4: Run -> PASS.
- [ ] Step 5: Commit `Phase 2 step 4: addresses API (max 5, one default)`.

### Task 4 (plan step 7): Audit log

**Files:** Create `admin/AuditLog.java`, `admin/AuditLogRepository.java`, `admin/AuditService.java`; Test `admin/AuditServiceTest.java`.

**Interfaces - Produces:** `AuditService.record(String action, String entityType, Long entityId, String details)` - actor = `AuthUser.current()` id (null allowed for system). Actions used: `SELLER_APPROVED`, `SELLER_REJECTED`, `SELLER_BLOCKED`, `PRODUCT_APPROVED`, `PRODUCT_REJECTED`, `PRICE_CHANGED`. `List<AuditLog> AuditService.latestFor(String entityType, Long entityId)`.

- [ ] Step 1: Test: record with an authenticated admin in the SecurityContext -> one row with actor 1, action, entity. Step 2: FAIL. Step 3: implement. Step 4: PASS.
- [ ] Step 5: Commit `Phase 2 step 7: audit log service`.

### Task 5 (plan step 5): Seller application + admin seller approvals

**Files:**
- Modify: `seller/SellerService.java` (`apply`, `myApplication`, `approve`, `reject`, `block`, `listByStatus`, `findByOwner`)
- Create: `seller/SellerAccess.java`, `seller/SellerApplicationController.java`, `seller/dto/{SellerApplyRequest,SellerApplicationView,SellerSummary}.java`, `admin/AdminSellerController.java`, `admin/dto/RejectRequest.java`
- Modify: `config/SecurityConfig.java` (rules table from spec section 4)
- Tests: `seller/SellerApplicationTest`, `seller/RoleChangeTest`

**Interfaces - Produces:**
- `SellerAccess` bean `sellerAccess`: `boolean isActiveSeller()` (current user owns an APPROVED shop), `boolean ownsProduct(Long productId)` (Task 6 fills it), `Long currentSellerId()` (throws 403 if none)
- `SellerApplyRequest(shopName 3-120, city 2-80, description <=500, gstin @Pattern 15 chars uppercase alnum or empty)`
- Admin: `GET /api/admin/sellers?status=`, `POST /api/admin/sellers/{id}/approve|reject|block`; reject body `RejectRequest(reason 5-300)`
- Approve = APPROVED + `UserService.addRole(ownerId, SELLER)` + audit.

- [ ] Step 1: `SellerApplicationTest` (ravi): apply -> 201 PENDING, slug `ravi-s-shop` style; apply again -> 409; admin rejects -> ravi re-applies -> PENDING; admin blocks a shop -> its owner re-apply -> 403; shop name "Chennai Mobiles!" -> slug `chennai-mobiles-2`; customer calls `/api/admin/sellers` -> 403. `RoleChangeTest`: ravi applies, logs in (cookie C); `GET /api/seller/products` with C -> 403; admin approves; same C -> 200; admin blocks -> same C -> 403; audit has SELLER_APPROVED + SELLER_BLOCKED rows.
- [ ] Step 2: FAIL. Step 3: implement (seller products list endpoint may return an empty page stub until Task 6). Step 4: PASS.
- [ ] Step 5: Commit `Phase 2 step 5: become a seller + admin approve/reject/block`.

### Task 6 (plan steps 6, 9): Seller products API + ownership guard

**Files:**
- Modify: `catalog/ProductService.java` (+ `createDraft`, `updateBySeller`, `submit`, `findForSeller`, `getForSeller`, `sellerIdOf`)
- Create: `catalog/dto/{SellerProductRequest,SellerVariantRequest,SellerProductView}.java`, `seller/SellerProductController.java`
- Modify: `seller/SellerAccess.java` (`ownsProduct`)
- Tests: `seller/SellerOwnershipTest` ⭐, `seller/UnapprovedSellerTest` ⭐, `seller/SellerProductTest`

**Interfaces - Produces:**
- `ProductService.createDraft(Long sellerId, SellerProductRequest): SellerProductView`
- `ProductService.updateBySeller(Long productId, SellerProductRequest): SellerProductView` - DRAFT/REJECTED full edit (variants replaced); ACTIVE: only price/mrp (via `changePrice`) and stock per existing variant id, any changed name/brand/description/category/variant set -> `BusinessRuleException`; PENDING -> `BusinessRuleException`
- `ProductService.submit(Long productId)`, `findForSeller(Long sellerId, ProductStatus status, Pageable): PageResponse<SellerProductView>`, `getForSeller(Long productId)`, `Optional<Long> sellerIdOf(Long productId)`
- `SellerProductRequest(name, categoryId, brand, description, List<SellerVariantRequest> variants 1..10)`; `SellerVariantRequest(Long id nullable, label, size, colour, price, mrp, stock)`; sku = `<seller slug prefix>-<product id>-<n>`
- `ownsProduct(id)`: `sellerIdOf(id)` empty -> **true** (let the service throw 404), else equals `currentSellerId()`.
- `categoryId` must be a sub-category (has parent) -> else 400.

- [ ] Step 1: `SellerOwnershipTest` (kovai.sports vs chennai.mobiles product id from seed): GET/PUT/submit -> 403 each; DB row unchanged; kovai list contains no seller-1 product; unknown id 999999 -> 404. `UnapprovedSellerTest`: erode.organics (PENDING), a REJECTED shop, a BLOCKED shop -> POST `/api/seller/products` -> 403. `SellerProductTest`: create -> DRAFT with priceFrom = min price; mrp < price -> 400; 11 variants -> 400; top-level category -> 400; ACTIVE product same name + new price -> 200 and price_history row; ACTIVE rename -> 409; PENDING edit -> 409.
- [ ] Step 2: FAIL. Step 3: implement. Step 4: PASS + ArchUnit PASS.
- [ ] Step 5: Commit `Phase 2 step 6: seller products API with SellerAccess ownership guard`.

### Task 7: Admin product approvals + lifecycle

**Files:** `catalog/ProductService.java` (+ `approve`, `reject`, `listByStatus`), `admin/AdminProductController.java`; Test `admin/ProductLifecycleTest.java`, `admin/AuditLogTest` covered here.

**Interfaces - Produces:** `GET /api/admin/products?status=PENDING`, `POST /api/admin/products/{id}/approve|reject`.

- [ ] Step 1: Test: kovai creates -> submits -> admin approves -> public `GET /api/products/{slug}` 200; reject with reason -> seller sees `rejectionReason`, edits, resubmits; approve when shop BLOCKED -> 409; each admin action writes one audit row.
- [ ] Step 2: FAIL. Step 3: implement. Step 4: PASS.
- [ ] Step 5: Commit `Phase 2: admin product approve/reject + lifecycle test`.

### Task 8: Security rules, Swagger lock, unknown API 404

**Files:** `config/SecurityConfig.java`, `common/GlobalExceptionHandler.java` (override `handleNoResourceFoundException` -> detail `There is no API at /api/<path>`); Tests `config/SecurityRulesTest`, `common/UnknownApiUrlTest`.

- [ ] Step 1: Tests: `/v3/api-docs` guest 401, ravi 403, admin 200; `GET /api/products` guest 200; `GET /api/nope` guest 401, ravi 404 with `detail` "There is no API at /api/nope".
- [ ] Step 2: FAIL. Step 3: implement. Step 4: PASS.
- [ ] Step 5: Commit `Phase 2: lock Swagger to ADMIN, JSON 404 for unknown API URLs`.

### Task 9: Custom 404 page (frontend)

**Files:** Modify `frontend/src/pages/NotFoundPage.jsx`, `frontend/src/App.jsx` (route `products` -> `ComingSoonPage`); Create `frontend/src/pages/NotFoundPage.test.jsx`, `frontend/src/components/ui/EmptyShelfArt.jsx`.

- [ ] Step 1: Tests (mock `api/client` get): unknown route renders h1 "This shelf is empty"; categories `[{name:'Electronics',slug:'electronics'}, ...]` -> links `href="/products?category=electronics"` (max 6); API error -> no "Popular" text; typing "phone" + submit navigates to `/products?q=phone`.
- [ ] Step 2: `npx vitest run src/pages/NotFoundPage.test.jsx` FAIL. Step 3: implement (Go back via `navigate(-1)` shown only if `window.history.length > 1`; `document.title`). Step 4: PASS; `npm run lint` + `npm run build` PASS.
- [ ] Step 5: Commit `Phase 2: helpful custom 404 page`.

### Task 10: Postman, README, full verification

**Files:** `tools/postman-gen/make_collection.py` (folder "Accounts & roles"), regenerated `postman/TriVoKo.postman_collection.json`, `README.md` (endpoints, demo logins, status line), `docs/decisions.md` (D-entry: roles from DB per request).

- [ ] Step 1: Add requests: csrf, login as kovai (cookie jar), me, create product, submit, login admin, approve, login chennai, GET kovai product -> 403.
- [ ] Step 2: `mvnw verify` PASS; backend running with `.env` (JWT_SECRET, DEMO_PASSWORD) -> `npx newman run postman/TriVoKo.postman_collection.json` all green.
- [ ] Step 3: Commit `Phase 2: Postman accounts folder, README, decisions`.
- [ ] Step 4: Push branch; PR + merge only after Madhavan's yes.
