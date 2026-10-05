# Phase 3 Shop Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The shop website on real API data - browse, search, product page, one cart from many sellers that survives login, account pages - on phone and in dark mode.

**Architecture:** Backend gains a `cart` module (Flyway V4, `CartPricing` shared by the saved cart and the guest preview) plus `q` search and a brand list on the catalogue. Frontend gains auth + cart Redux slices, small pure helpers in `src/lib/` (tested with Vitest), and one page per URL from the spec.

**Tech Stack:** Spring Boot 4.1 / Java 25 / MySQL 8.4 / Testcontainers; React 19, React Router 7, Redux Toolkit, Tailwind v4, Vitest.

**Spec:** `Phase_3_Shop_Frontend/Phase3_Design_Spec_2026-10-05_0927.md`

## Global Constraints

- Module rule (ArchUnit): `cart` calls `ProductService`, never catalogue repositories.
- Cart rules: quantity 1..10 per line, capped by stock; max 30 lines; shipping per seller package: free when package items total >= ₹499, else ₹40.
- Guest cart in localStorage key `trivoko.cart` as `[{variantId, quantity}]` - never prices.
- Public: `GET` catalogue, `POST /api/cart/preview`. Login needed: rest of `/api/cart/**`, `/sell`, `/account/**`.
- Money as `BigDecimal` on the server; shown as `₹2,499` (`en-IN`, no `.00`).
- URL page numbers start at 1; API pages at 0.
- Copy style: plain English comments with the "why", like the existing code. Commit messages `Phase 3 step N: ...` + Co-Authored-By line.

## Review Focus

- Guest cart in localStorage edited by hand to junk (`"abc"`, negative quantity, unknown variant) → app must not crash; junk lines dropped. (Task 4 test `guestCart ignores junk`.)
- Same variant added twice, or merge pushing a line over 10 / over stock → capped, note shown. (Task 2 tests.)
- URL with nonsense filters (`?page=-3&minPrice=abc&sort=xyz`) → page falls back to defaults, no error screen. (Task 4 test `parseFilters drops bad values`.)
- Product whose shop gets BLOCKED while it sits in a cart → line shown "No longer available", excluded from totals. (Task 2 test.)
- `q` containing `%` or `_` → treated as literal text, not a wildcard. (Task 1 test.)

---

### Task 1: Catalogue `q` search + brand list

**Files:** Modify `catalog/ProductSearch.java`, `ProductSpecifications.java`, `ProductService.java`, `ProductController.java`, `ProductRepository.java`; Test `catalog/ProductSearchTest.java` (new).

**Interfaces:** Produces `GET /api/products?q=` and `GET /api/products/brands?category=` → `List<BrandCount(String brand, long count)>`.

- [ ] Step 1: Tests: `q=volta case` returns only products whose name or brand contains BOTH words (case-insensitive); `q=100%` matches nothing and does not error; brands for `category=phones` are sorted, counts > 0 and only visible products; unknown category → empty list.
- [ ] Step 2: Run, see FAIL.
- [ ] Step 3: `ProductSpecifications.matchesWords(String q)`: split on spaces, each word → `lower(name) LIKE %w% OR lower(brand) LIKE %w%` with `\` escape for `% _ \`. `ProductService.brands(String category)` via repository JPQL `select new BrandCount(p.brand, count(p)) ... group by p.brand order by p.brand` with the visible + category conditions. Route `/products/brands` is declared before `/products/{slug}`.
- [ ] Step 4: Run, PASS. Step 5: Commit `Phase 3 step 1: simple q search + brand list`.

### Task 2: Cart backend (V4, pricing, service, API)

**Files:** Create `db/migration/V4__carts.sql`, `cart/Cart.java`, `CartItem.java`, `CartRepository.java`, `CartPricing.java`, `CartService.java`, `CartController.java`, `cart/dto/{CartView, CartLineView, CartPackage, CartItemRequest, CartItemsRequest, QuantityRequest}.java`, `catalog/dto/CartVariant.java`; Modify `ProductService` (+`cartVariants`), `SecurityConfig`; Test `cart/CartPricingTest.java`, `cart/CartApiTest.java`.

**Interfaces:**
- `ProductService.cartVariants(Collection<Long> variantIds) -> Map<Long, CartVariant>`; `CartVariant(Long variantId, String productSlug, String productName, String variantLabel, String imageUrl, BigDecimal price, BigDecimal mrp, int stock, boolean available, Long sellerId, String sellerName, String sellerSlug, String sellerCity)`; `available` = product ACTIVE and shop APPROVED.
- `CartPricing.price(List<Line(variantId, quantity)>, Map<Long, CartVariant>) -> CartView` (pure, no DB).
- `CartView(List<CartPackage> packages, int itemCount, BigDecimal itemsTotal, BigDecimal shippingTotal, BigDecimal total)`; `CartPackage(SellerRef seller, List<CartLineView> items, BigDecimal itemsTotal, BigDecimal shippingFee)`; `CartLineView(variantId, productSlug, productName, variantLabel, imageUrl, price, mrp, quantity, maxQuantity, available, note)`.
- REST per spec 3.3. `CartService.view(userId)`, `setQuantity(userId, variantId, qty)`, `remove(userId, variantId)`, `merge(userId, items)`, `preview(items)`.

- [ ] Step 1: `CartPricingTest` (no Spring): two sellers → two packages in first-added order; package ₹498 → fee 40, ₹499 → 0; quantity 12 → 10 with note; stock 2, quantity 5 → 2 with note "Only 2 left - quantity lowered"; stock 0 → available=false, note "Out of stock", excluded from totals and itemCount; unavailable product → note "No longer available", excluded; unknown id → skipped.
- [ ] Step 2: `CartApiTest` (MockMvc + Logins helper): preview works without login and saves nothing; `GET /api/cart` without login 401; PUT twice then GET shows quantity of the last PUT; PUT unknown variant 404; PUT quantity 0 or 11 → 400; merge adds to an existing line and caps at 10; 31st line → 409 "at most 30"; blocking the shop (jdbc update) makes the line unavailable; another user's cart is never seen.
- [ ] Step 3: Run, FAIL. Step 4: Implement; `quantity` validated `@Min(1) @Max(10)`; merge/preview skip unknown ids and cap; `Cart` created lazily on first write. Step 5: Run full suite, PASS (ModuleRulesTest too). Step 6: Commit `Phase 3 step 6: cart module`.

### Task 3: Postman "Cart" folder

**Files:** Modify `tools/postman-gen/make_collection.py`, regenerate `postman/TriVoKo.postman_collection.json`.
- [ ] Step 1: Requests: preview (guest, 2 sellers → 2 packages), brands list, `q` search, login ravi, merge, GET cart, PUT qty, DELETE line. Step 2: regenerate; newman green when backend + DEMO_PASSWORD available (else note it). Step 3: Commit.

### Task 4: Frontend foundation (helpers, API, auth)

**Files:** Create `src/lib/money.js`, `src/lib/productFilters.js`, `src/lib/guestCart.js`, `src/api/{auth,catalog,cart,account}.js`, `src/store/authSlice.js`, `src/hooks/useForm.js`, `src/utils/validation.js` (copy EventHub), `src/components/auth/{RequireAuth,AuthCard}.jsx` (copy); Modify `store/index.js`, `main.jsx`/`App.jsx` (dispatch `loadSession` once); Tests `lib/*.test.js`.

**Interfaces:**
- `formatRupees(value) -> string` ("₹2,499", "₹99.50").
- `parseFilters(URLSearchParams) -> {q, category, brands[], minPrice, maxPrice, inStock, sort, dir, page}`; `toSearchParams(filters) -> URLSearchParams` (drops defaults: sort newest, dir asc... page 1); `toApiParams(filters)` (page-1, brand repeated).
- `guestCart.read() -> [{variantId, quantity}]`, `add(variantId, qty)`, `set(variantId, qty)`, `remove(variantId)`, `clear()`; caps 1..10, max 30 lines, junk dropped.
- `authSlice`: `loadSession, login, register, logout, sessionExpired`, selectors `selectUser, selectAuthStatus`; `isSeller(user)` = has an APPROVED shop in `user.seller`; `isAdmin`.

- [ ] Step 1: Tests: `formatRupees`; `parseFilters drops bad values` (`?page=-3&minPrice=abc&sort=xyz` → defaults); round trip of a full filter set; `toApiParams` page 2 → 1; `guestCart ignores junk` (localStorage `"abc"`, `[{variantId:"x"}]`, quantity -2 → read() returns []); add same variant twice adds; cap 10.
- [ ] Step 2: FAIL → implement → PASS (`npm test`), `npm run lint`. Step 3: Commit `Phase 3 step 4: auth slice, filters, guest cart helpers`.

### Task 5: Cart slice + navbar + layout

**Files:** Create `src/store/cartSlice.js`, `src/components/layout/{AccountMenu,CategoryMenu}.jsx`; Modify `Navbar.jsx` (search submits to `/products?q=`, categories, cart link with count badge, account menu / Login), `Footer.jsx` links; Test `store/cartSlice.test.js`.

**Interfaces:** thunks `loadCart()`, `addToCart({variantId, quantity})`, `setCartQuantity({variantId, quantity})`, `removeFromCart(variantId)`, `mergeGuestCart()`; state `{view: CartView|null, status}`; `selectCartCount`. Guest → guestCart + `previewCart`; user → server. `login/register.fulfilled` → app dispatches `mergeGuestCart()` then `loadCart()`; `logout.fulfilled` → view null.

- [ ] Step 1: Test with mocked `api/cart`: guest add calls preview with the stored lines; logged-in add calls PUT with existing quantity + new; merge posts guest lines then clears localStorage; empty guest cart → merge not called.
- [ ] Step 2: FAIL → implement → PASS. Step 3: Commit.

### Task 6: Catalogue components, Home, Listing, Shop page

**Files:** Create `components/catalog/{ProductImage,ProductCard,ProductGrid,ProductSkeleton,FilterPanel,PriceTag}.jsx`, `pages/{ProductsPage,ShopPage}.jsx`; Rewrite `pages/HomePage.jsx`; Modify `App.jsx` routes; Test `pages/ProductsPage.test.jsx`.
- [ ] Step 1: Test: rendering `/products?category=phones&brand=Volta&page=2` calls `fetchProducts` with `{category:'phones', brand:['Volta'], page:1, ...}`; ticking "In stock" updates the URL to include `inStock=true` and resets page.
- [ ] Step 2: FAIL → implement. Home: flash banner placeholder (Phase 7 text), category tiles (top categories), "Top deals" = products sorted client-side by `discountPercent` from `size=24` newest **[spec default]**, "New arrivals". Listing: sidebar filters (drawer on phones), sort select, chips for active filters, Pagination, skeletons, EmptyState with Clear filters. Shop page: `GET /api/sellers/:slug` header + grid with `seller=slug`; 404 → NotFound content.
- [ ] Step 3: PASS + lint. Commit.

### Task 7: Product page

**Files:** Create `pages/ProductPage.jsx`, `components/catalog/{VariantPicker,Gallery}.jsx`; Test `components/catalog/VariantPicker.test.jsx`.
- [ ] Step 1: Test: variants with colour Blue/Black and sizes 7/8/9 where Blue-9 is sold out → after choosing Blue the size 9 button is disabled and crossed; variants without size/colour show their labels as buttons.
- [ ] Step 2: FAIL → implement. Breadcrumb, gallery (placeholder art), brand, name, PriceTag for the chosen variant, "Only N left", quantity select 1..min(10), Add to cart (toast "Added to cart" + link), seller box (link to `/shops/:slug`, delivery rule text). Unknown slug → NotFound content.
- [ ] Step 3: PASS. Commit.

### Task 8: Cart page

**Files:** Create `pages/CartPage.jsx`, `components/cart/{CartLine,PriceDetails}.jsx`.
- [ ] Step 1: Implement per sketch 4: "Your cart (N items)", "comes in K packages", package cards with fee badge, +/- (disabled at 1 / maxQuantity), Remove, amber notes, Price details, Checkout button disabled with "Checkout opens in Phase 4". Empty cart → EmptyState + "Start shopping".
- [ ] Step 2: Manual check in the browser (Task 10). Commit.

### Task 9: Login, Register, Become a seller, Addresses

**Files:** Create `pages/{LoginPage,RegisterPage,BecomeSellerPage,AddressesPage}.jsx`, `components/account/AddressForm.jsx`; Modify `App.jsx` (RequireAuth group for `/sell`, `/account/addresses`); Test `pages/LoginPage.test.jsx` (copy EventHub's, adapted).
- [ ] Step 1: Login test: wrong password shows the server message; success navigates to `?next=` or `/`, and dispatches the merge. Register fields: fullName, email, password, phone (optional). Become a seller: shows status card when `/api/seller/application` returns one (404 → form). Addresses: list, add/edit in Modal with AddressForm (name, phone 10 digits, line1, line2, city, state select of Indian states, pincode 6 digits), delete with confirm, make default; "Add" disabled at 5.
- [ ] Step 2: FAIL → implement → PASS. Commit.

### Task 10: Phone + dark scan, docs, full verification

- [ ] Step 1: Start backend + frontend via `.claude/launch.json`; register a test customer; walk every page at 375 px and dark mode; guest adds 3 items from 2 sellers → login → cart kept. Fix findings.
- [ ] Step 2: README status + "Shop frontend" section + cart endpoints; `docs/decisions.md` D4 (guest cart in the browser, prices only from the server).
- [ ] Step 3: `mvnw verify` + `npm run lint && npm test && npm run build` green. Commit. Push/PR only after Madhavan's yes.
