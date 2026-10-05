# Phase 3 - Shop frontend: design spec

Date: 5 Oct 2026 · Branch `phase-3-shop-frontend` · Status: **written by Claude with default decisions**.
Madhavan asked Claude to "continue all" and to change things later, so every choice below that he did not make
himself is marked **[default]** - any of them can be changed after he reviews the shop.

## 1. Goal (from the Phase Plan)

A customer can browse and search the catalogue, choose a size or colour and fill ONE cart from many sellers -
on laptop and phone, light and dark. No payment yet (Phase 4).

**Done when:** a guest adds 3 items from 2 sellers, logs in, and the cart is kept; every page passes the
phone (375 px) + dark-mode check.

```
 Home ──► Listing (/products?category=..&brand=..) ──► Product page ──► Add to cart
   ▲                                                                     │
   └──────────── Navbar: search · categories · cart count · account ◄────┘
 Guest cart (browser) ──login──► merged into the server cart ──► Cart page grouped by seller
```

## 2. Pages and URLs

| URL | Page | Who |
|---|---|---|
| `/` | Home: flash-sale banner place (Phase 7), category tiles, "Top deals" (biggest % off), "New arrivals" | everyone |
| `/products` | Listing: filters + sort in the URL, pagination, skeleton cards, empty state | everyone |
| `/products/:slug` | Product page: gallery, variant picker, price, seller box, add to cart | everyone |
| `/shops/:slug` | Seller shop page: shop header + its products (same grid) | everyone |
| `/cart` | Cart grouped by seller ("packages"), quantity +/- , remove, price details, Checkout button ("coming in Phase 4") | everyone |
| `/login`, `/register` | Copied from EventHub, TriVoKo texts; after login the guest cart is merged | guests |
| `/sell` | Become a seller: form, or the application status (PENDING / APPROVED / REJECTED + reason / BLOCKED) | logged in |
| `/account/addresses` | List, add, edit, delete, make default (max 5) | logged in |
| `/style-guide`, `*` (404) | unchanged | everyone |

Navbar: logo, search box (submits to `/products?q=`), "All categories" menu (tree from `/api/categories`),
cart icon with count, account menu (Hi, Ravi · My addresses · Become a seller / My shop status · Log out) or
"Login". Phone: search on its own row, menu drawer with the same links. Category chips row under the navbar
on the home page only **[default]**.

## 3. Backend additions

### 3.1 Simple text search `q` on `GET /api/products` **[default]**
`q` = words matched with `LIKE %word%` against product name and brand (every word must match).
Smart search with typo tolerance replaces it in Phase 10; the URL `?q=` stays the same, so the frontend does not change.

### 3.2 Brand list for the filter **[default]**
`GET /api/products/brands?category=phones` → `[{ "brand": "Volta", "count": 6 }, ...]` - visible products only,
sorted by name. Used for the brand checkboxes. (Counts that react to every other filter = Phase 10 facets.)

### 3.3 Cart module (`com.trivoko.cart`, Flyway V4)

```
carts      (id, user_id UNIQUE -> users, updated_at)
cart_items (id, cart_id -> carts, variant_id -> product_variants, quantity 1..10, added_at,
            UNIQUE (cart_id, variant_id))
```

- Only logged-in users have a server cart. A guest cart lives in the browser (localStorage) as
  `[{ variantId, quantity }]` - **no prices are stored in the browser**.
- Prices, names, stock and the seller grouping always come from the server, through ONE calculation
  (`CartPricing`) used by both kinds of cart:

| Endpoint | Who | What |
|---|---|---|
| `GET /api/cart` | logged in | my cart as a `CartView` |
| `PUT /api/cart/items/{variantId}` `{quantity}` | logged in | set the quantity (adds the line if new) |
| `DELETE /api/cart/items/{variantId}` | logged in | remove a line |
| `POST /api/cart/merge` `{items:[{variantId, quantity}]}` | logged in | add the guest lines to my cart (same variant: quantities added) |
| `POST /api/cart/preview` `{items:[...]}` | everyone | the `CartView` for a guest cart, nothing saved |

- **Rules:** quantity 1 to 10 per line, and never more than the stock (capped, with a note on the line);
  at most 30 lines per cart **[default]**. A variant whose product is not ACTIVE or whose shop is not APPROVED
  stays in the cart but is shown "No longer available" and does not count in the totals.
  A sold-out variant: same, "Out of stock". Unknown variant id in PUT → 404; in merge/preview → skipped.
- **Shipping rule (shown now, charged in Phase 4):** per package (= per seller), free when that package's items
  total ≥ ₹499, else ₹40.
- `CartView`:
  ```
  { packages: [ { seller: {name, slug, city}, items: [CartLine], itemsTotal, shippingFee } ],
    itemCount, itemsTotal, shippingTotal, total }
  CartLine = { variantId, productSlug, productName, variantLabel, imageUrl, price, mrp,
               quantity, maxQuantity, available, note }   // note e.g. "Only 2 left - quantity lowered"
  ```
  Packages are ordered by the first line added; lines by the time they were added.
- The cart module reads products through a new `ProductService.cartVariants(ids)` (module rule: never the
  catalogue's repositories). Security: `/api/cart/preview` is public, the rest of `/api/cart/**` needs login;
  every write needs the CSRF token (as all POST/PUT/DELETE).

### 3.4 Why the server recalculates (explain-it-back answer)
The browser's numbers are only for showing. Anyone can edit localStorage or a request, so in Phase 4 checkout
the server takes ONLY variant ids + quantities and computes every price itself.

## 4. Frontend structure

```
src/api/        client.js (exists) · auth.js · catalog.js · cart.js · account.js (addresses, seller apply)
src/store/      authSlice (copied from EventHub, TriVoKo roles) · cartSlice · notificationsSlice (exists)
src/lib/        money.js (₹ format) · productFilters.js (URL <-> filter object) · guestCart.js (localStorage)
src/hooks/      useAsync (exists) · useForm (copy) · utils/validation (copy)
src/components/ auth/ (RequireAuth, AuthCard) · catalog/ (ProductCard, ProductGrid, Skeletons, FilterPanel,
                ProductImage placeholder art, VariantPicker, PriceTag) · cart/ (CartLine, PriceDetails)
src/pages/      Home, Products, ProductDetail, Shop, Cart, Login, Register, BecomeSeller, Addresses
```

- **Auth on start:** `loadSession` calls `GET /api/auth/me`; 401 = visitor. Axios already sends the
  `XSRF-TOKEN` cookie back as the `X-XSRF-TOKEN` header (same site), so no extra code.
- **Cart state:** `cartSlice` holds the last `CartView` from the server + status. Guests: changes go to
  `guestCart.js` (localStorage) and then `POST /api/cart/preview`. Logged in: `PUT/DELETE` and the answer
  replaces the view. After login/register: if the guest cart has lines → `POST /api/cart/merge`, then the
  guest cart is cleared. Logout → cart view emptied (the server keeps the user's cart for next login).
- **Navbar count** = `itemCount` (sum of quantities of available lines).
- **Filters in the URL:** `productFilters.js` turns `?category=phones&brand=Volta&brand=Arc&minPrice=100&inStock=true&sort=price&dir=asc&page=2&q=case`
  into an object and back; the page reads only the URL, so Back/Forward and shared links work. Page numbers
  in the URL start at 1 (humans), the API's at 0 **[default]**.
- **Photos:** the seed products have no photos yet, so `ProductImage` draws a placeholder (category-coloured
  tile with an icon and the brand) when `imageUrl` is empty **[default]**. Real photos: later task (Cloudinary).
- Money shown as `₹2,499` (Indian grouping, `en-IN`), whole rupees when there are no paise.

## 5. Errors and empty states
- Lists: skeleton cards while loading; EmptyState "No products match" with a "Clear filters" button.
- Unknown product or shop slug → the friendly 404 content.
- Server down → the existing describeError message with a "Try again" button.
- Cart line problems shown on the line (amber note), never as a blocking error.
- 401 on a cart write (cookie ran out) → session cleared, toast "Please log in again".

## 6. Testing
- **Backend (Testcontainers):** cart service rules (cap by stock, max 10, unavailable lines excluded, shipping
  per package ₹499 rule, merge adds quantities, preview saves nothing), cart controller security (preview public,
  others 401 without login, CSRF), brands endpoint, `q` search. Postman folder "Cart" via the generator.
- **Frontend (Vitest):** `productFilters` (parse/serialize round trip, defaults dropped, page 1-based), `guestCart`
  (add same variant adds quantity, cap 10, remove), `money` format, cart slice merge flow (mocked API),
  Products page reads filters from the URL, VariantPicker disables sold-out sizes.
- **Phone + dark scan:** every page at 375 px and in dark mode in the browser pane; fix what it finds.

## 7. Out of scope (later phases)
Checkout and payment (4), photos upload and seller pages (5), admin pages (6), flash-sale logic (7),
ratings shown on cards (9), typo-tolerant search and facet counts (10), Playwright (11).
