# Phase 1 - Catalogue API

**Time:** 1 week  ·  **Group:** Core  ·  **Folder:** `Phase_1_Catalogue_API\`

**Goal:** Products exist in the database and anyone can browse them through the API: categories, filters, sorting, paging and product details.

>> The shelves are filled and every product has a price tag and a label. Customers can walk the aisles, but the billing counter is not open yet.

## Steps

@widths 600 6926 1500
| # | Step | Who |
| 1 | Flyway V1: `categories` (tree with parent), `sellers` (shop name, slug, city, status), `products` (status DRAFT / PENDING / ACTIVE / REJECTED / BLOCKED), `product_variants` (size, colour, price, MRP, stock, version), `product_images`, `price_history` | Claude |
| 2 | Flyway V2 seed data from the sample-data plan: 10 categories, 8 sellers, 120 products with variants and photos | Claude |
| 3 | Entities and repositories in modules `catalog` and `seller`; money as `BigDecimal` / `DECIMAL(12,2)` | Claude |
| 4 | ArchUnit test for the module rule (a module may call another module's service, never its repository) - runs in CI from now on | Claude |
| 5 | DTO records + mapper; shared `PageResponse` | Claude |
| 6 | `GET /api/products` with filters (category incl. sub-categories, brand, min/max price, in stock), sort (price, newest) and paging (max 48) | Claude |
| 7 | `GET /api/products/{slug}` (variants, images, seller), `GET /api/categories` (tree), `GET /api/sellers/{slug}` (shop page) | Claude |
| 8 | Price-change service method that always writes `price_history` (used by price alerts in Phase 9) | Claude |
| 9 | Create a free Cloudinary account and put the 3 keys in `.env` | You |
| 10 | Cloudinary signed upload: `POST /api/uploads/signature` (images only, max 5 MB) - tried with one real photo | Claude |
| 11 | Tests: repository tests, service rules, every endpoint (Testcontainers MySQL) | Claude |
| 12 | Postman collection with checks, run with newman | Claude |

## Output of this phase

- Catalogue REST API in Swagger
- 120 sample products in MySQL
- Postman collection
- ArchUnit module rule in CI

## Done when

- `GET /api/products?category=phones&sort=price` returns the correct pages
- All Postman checks pass
- CI green

## Explain it back

- Why does stock live on the variant and not on the product?
- What is a Flyway migration and why do we never change the database by hand?
- How do filters become one SQL query (Specifications)?

**Skills you practise:** JPA entities, relationships, Flyway, Specifications, paging, DTOs, testing with Testcontainers

**Where your work goes:** `backend\src\main\java\com\trivoko\catalog`, `...\seller`, `db\migration`, `postman\`
