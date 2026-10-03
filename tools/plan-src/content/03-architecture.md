# 3. Architecture and technology

![TriVoKo architecture](architecture.png)

## 3.1 Why a modular monolith?

Three ways to structure the backend were compared:

| Option | Good | Bad | Decision |
| **1. Modular monolith** (one Spring Boot app, one package per module) | One app to host (free or cheap); easy to debug; clear module walls; can be split later | Cannot scale one module alone | **Chosen** |
| 2. Microservices (product, order, payment services + gateway + Kafka) | Big buzzword | 5+ apps to host - impossible on free hosting; weeks lost on plumbing instead of features | Rejected |
| 3. Monolith + Redis from day one | Strong "high traffic" story | One more thing to host and learn before we know we need it | Only **if** the k6 load test shows MySQL is too slow |

>> A modular monolith is like one big mall building: every shop (module) has its own walls and its own counter (service), and customers go through the counter - nobody walks into another shop's storeroom (tables). Later, a shop can move into its own building (microservice) without changing how customers talk to it.

**Interview answer:** "I chose a modular monolith because it is the right size for the traffic and the team. Each module only talks to others through its service interface, so splitting a module into a microservice later is a packaging change, not a rewrite. I measured the flash sale with k6 before deciding whether Redis was needed."

## 3.2 Technology list

| Layer | Technology | Why |
| Frontend | React 19 + Vite, React Router, Redux Toolkit, Tailwind CSS v4, Axios | Same as EventHub - already known |
| Frontend extras | Recharts (charts), the useForm hook and UI kit copied from EventHub | Charts for seller and admin; less new code |
| Backend | Java 25, Spring Boot 4, Spring Web, Spring Data JPA, Spring Security, Validation | Same as EventHub |
| Database | MySQL 8.4 (Docker locally), Flyway migrations | Same as EventHub |
| Search | Hibernate Search 8 with the Lucene backend (index stored inside the app) | Typo tolerance + autocomplete without an extra server |
| Payments | Stripe Checkout (test mode), webhooks, partial refunds | Real payment flow - the carried-over EventHub to-do is done here |
| Images | Cloudinary free tier (signed upload from the browser) | Photos are not stored in MySQL or in the app |
| Email | Spring Mail; Mailpit locally; Brevo/SMTP free tier when live | Order and alert emails |
| PDF | OpenPDF | Invoices (like EventHub certificates) |
| Testing | JUnit 5, Mockito, Testcontainers, Vitest + React Testing Library, **Playwright** (end-to-end), **k6** (load) | Proof for the README |
| Quality | JaCoCo coverage, oxlint/ESLint, GitHub Actions CI | Green badge on GitHub |
| Delivery | Docker image, environment variables, `render.yaml` / `vercel.json` or a VPS | Hosting decided at Phase 12 |
| Optional | Redis | Only if the load test needs it |

## 3.3 Backend modules

Every module is one Java package under `com.trivoko`, with its own `controller`, `service`, `repository`, `entity` and `dto` classes.

| Module | Owns (tables) | Main job |
| `user` | users, user_roles, addresses | Register, login, profile, addresses |
| `seller` | sellers, seller_ledger | Seller applications, shop profile, earnings |
| `catalog` | categories, products, product_variants, product_images, price_history | Products and stock |
| `cart` | carts, cart_items | Cart for guests and users, merge on login |
| `order` | orders, packages, order_items | Checkout, order split, package states |
| `payment` | payments, refunds, processed_payment_events | Stripe Checkout, webhooks, refunds |
| `flashsale` | flash_sales, flash_sale_items, flash_sale_claims | Scheduled sales with atomic stock |
| `returns` | return_requests | Return state machine |
| `review` | reviews | Verified-buyer reviews and rating average |
| `wishlist` | wishlist_items | Wishlist and price-drop alerts |
| `coupon` | coupons, coupon_redemptions | Coupon rules |
| `search` | (Lucene index) | Search, autocomplete, also-bought |
| `notify` | notifications | Bell + emails (after commit, with retry) |
| `admin`, `analytics` | audit_log | Approvals, reports, audit |
| `common` | - | Errors (ProblemDetail), money helpers, paging |

> The module rule: a module may call another module's **service**, never its **repository** or entity tables. Modules announce changes with Spring events (for example `PackageDelivered`, `PriceChanged`) so that `notify`, `search` and `analytics` can react without being called directly. A small ArchUnit test checks this rule in CI.

## 3.4 Folder layout

```
03_TriVoKo_Main_Project/
  00_Project_Documents/        master plan, phase plan, guides
  Phase_0_Setup/ ... Phase_12_Launch/   one checklist document per phase
  backend/                     Spring Boot app (package com.trivoko.*)
  frontend/                    React app
  docker-compose.yml           MySQL + Mailpit for development
  load-tests/                  k6 scripts and results
  e2e/                         Playwright tests
  tools/                       document generators, demo scripts
```
