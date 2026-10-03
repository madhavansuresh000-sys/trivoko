# 6. Database, API and pages

## 6.1 Main tables

![Main tables and how they are linked](er-diagram.png)

Important database rules:

- **Order items copy** the product title, variant text and price. An old invoice never changes.
- **Stock lives on the variant** (size M of a T-shirt has its own stock), not on the product.
- Rows that many people change at once (`product_variants`, `packages`, `flash_sale_items`) have a **version** column or use a single atomic `UPDATE ... WHERE`.
- Status columns are `VARCHAR` + `CHECK` (not MySQL `ENUM`) - the EventHub rule.
- Every table change is a **Flyway migration** (V1, V2, ...). Nothing is changed by hand.

## 6.2 API overview

| Group | Examples | Who |
| Auth | `POST /api/auth/register`, `/login`, `/logout`, `GET /api/auth/me` | anyone |
| Catalogue | `GET /api/products?q=&category=&brand=&min=&max=&sort=`, `GET /api/products/{slug}`, `GET /api/categories` | anyone |
| Search | `GET /api/search?q=`, `GET /api/search/suggest?q=` | anyone |
| Cart | `GET/PUT/DELETE /api/cart/items`, `POST /api/cart/merge` | customer |
| Checkout | `POST /api/checkout` → order + Stripe URL, `POST /api/payments/{session}/verify` | customer |
| Orders | `GET /api/orders`, `GET /api/orders/{number}`, `POST /api/packages/{id}/cancel`, `GET /api/orders/{number}/invoice.pdf` | customer (own) |
| Returns | `POST /api/order-items/{id}/returns`, `POST /api/returns/{id}/escalate` | customer (own) |
| Wishlist & reviews | `POST/DELETE /api/wishlist/{variantId}`, `PUT /api/order-items/{id}/review` | customer |
| Flash sales | `GET /api/flash-sales/live`, `POST /api/flash-sales/{id}/items/{itemId}/claim` | anyone / customer |
| Seller | `/api/seller/products`, `/api/seller/packages/{id}/pack, /ship, /deliver`, `/api/seller/returns/{id}/approve, /reject, /picked-up`, `/api/seller/earnings`, `/api/seller/analytics` | approved seller (own data only) |
| Admin | `/api/admin/sellers/{id}/approve`, `/api/admin/products/pending`, `/api/admin/coupons`, `/api/admin/flash-sales`, `/api/admin/returns/{id}/decide`, `/api/admin/reports` | admin |
| Payments | `POST /api/payments/stripe/webhook` (signed) | Stripe only |

All errors use the EventHub **ProblemDetail** format (400 / 401 / 403 / 404 / 409 / 429) with a clear message for each field. Swagger shows the full list (admin only when live).

## 6.3 Frontend pages

| Area | Pages |
| Shop (public) | Home (deals, flash-sale banner, categories), Search/listing with filters, Product page, Seller shop page, Cart, Login, Register, Become a seller |
| Customer | Checkout, Payment result, My orders, Order detail (tracking timelines, invoice, cancel, return, review), Wishlist, Addresses, Notifications |
| Seller | Dashboard (today's orders, low stock, earnings), Products list + product editor (variants, photos), Orders to ship, Returns, Flash-sale offers, Earnings ledger, Analytics |
| Admin | Overview, Seller approvals, Product approvals, Users, Orders, Coupons, Flash sales, Return disputes, Reports, Activity log |
| Other | 404 page, Style guide (dev only), dark mode on every page, mobile layout on every page |

> Design: a fresh TriVoKo look (teal + saffron colours, new logo), built on the EventHub UI kit (Button, Card, Badge, Modal, FormField, Toaster). Sketches of the 6 key pages are made in Phase 0 and approved before coding the frontend.
