# 8. The phase plan

![The 13 phases](roadmap.png)

Each phase gets its own folder (`Phase_N_Name`) with a checklist document - like EventHub. The table below is the summary; the detailed step checklists are written next, after this master plan is approved.

## 8.1 Phases at a glance

@widths 1250 1350 3926 2500
| Phase | Goal | Main work | Done when |
| **0 Setup** | Project ready to build | Git repo `trivoko` on GitHub, Spring Boot + React skeletons, docker-compose (MySQL, Mailpit), CI, `.env`, logo and colours, sketches of 6 key pages, sample-data plan (8 sellers, 120 products, 10 categories) | React shows "Backend: UP"; CI green; sketches approved |
| **1 Catalogue API** | Products exist | Tables categories / products / variants / images / price_history; seed data; listing with filters, paging and sorting; product detail; Cloudinary signed upload | `GET /api/products?category=phones&sort=price` returns correct pages; Postman checks pass |
| **2 Accounts & roles** | Safe login for 3 roles | Register/login/logout/me (EventHub auth), addresses, "Become a seller" + admin approval, ownership checks, demo accounts | Seller cannot touch another seller's product (test proves 403); unapproved seller cannot sell |
| **3 Shop frontend** | Customer can shop (no payment yet) | Home, listing with filters in the URL, product page with variant picker and gallery, cart grouped by seller, guest cart + merge on login, dark mode, mobile | Guest adds 3 items from 2 sellers, logs in, cart is kept |
| **4 Checkout & payments** | Real orders | Stock hold, order + packages split, coupons, Stripe Checkout + webhook + verify fallback, expiry job, late-payment rule, emails, PDF invoice, **real Stripe 4242 test with Stripe CLI** (EventHub to-do) | Pay once for 2 sellers → 2 packages PLACED; 100-thread stock test passes |
| **5 Seller dashboard** | Sellers run their shop | Product editor with variants + photos, stock warnings, orders to ship (Packed → Shipped → Delivered), earnings ledger, analytics charts | Seller ships a package and the customer sees the timeline update |
| **6 Admin panel** | Admin controls the marketplace | Seller + product approvals, users/orders search, block, coupons, reports, audit log | Admin blocks a seller → their products disappear from the shop |
| **7 Flash sale** | Standout #1 | Schedule sale, reserve stock, atomic claim, one per customer, countdown + live counter, rate limit, give-back job, 1,000-thread test, **k6 5,000 users**, Redis only if needed | k6 report: exactly 100 winners, 0 oversell, p95 recorded in README |
| **8 Returns & refunds** | Standout #2 | ReturnStatus state machine, photo upload, seller approve/reject, auto-approve job, escalate + admin decision, Stripe partial refund, stock + ledger update | Return one item of a 2-item order → exact partial refund in Stripe test dashboard |
| **9 Reviews, wishlist, alerts** | Standout #3 | Verified-buyer reviews + rating average, wishlist, price_history, price-drop job + email + bell, "lowest in 30 days" | Lower a price → wishlisted customer gets one alert (not five) |
| **10 Smart search** | Standout #4 | Hibernate Search + Lucene, fuzzy match, autocomplete, facets with counts, "customers also bought" | "iphnoe" finds iPhone; suggestions appear while typing |
| **11 Testing & polish** | Portfolio quality | Playwright demo-story test in CI, coverage ~80%, ArchUnit module rule, security review, accessibility + phone check, speed (lazy pages, image sizes), README draft | CI green with E2E; Lighthouse ≥ 90 on home page |
| **12 Launch** | Live and shown | Choose hosting (free or paid), deploy backend + DB + frontend, real email, demo accounts, README with GIFs + architecture + k6 graph, 3-minute demo video, resume bullets, LinkedIn post, tag v1.0 | A friend opens the link on a phone, buys, returns, and it all works |

## 8.2 Who types what

@widths 5526 3500
| Part | Built by |
| Setup, configuration, copies of EventHub patterns (auth, errors, UI kit, Stripe gateway, emails) | Claude builds and explains line by line |
| Catalogue, cart, seller and admin pages, reports | Claude builds and explains; Madhavan reviews and asks for changes |
| **Order split** (grouping cart lines into packages, coupon share per item) | **Madhavan types the key method** with guidance, Claude reviews |
| **Flash-sale claim** (atomic SQL, one-per-customer, give-back) | **Madhavan types** with guidance, Claude reviews |
| **Return state machine** (`ReturnStatus.allowedNext`, state-move service) | **Madhavan types** with guidance, Claude reviews |
| Tests for the three parts above | Written together - Madhavan writes at least one test for each |

## 8.3 Must-have core and stretch

- **Must-have core = Phases 0-8.** This is already a complete marketplace with the two strongest standouts (flash sale + returns).
- **Shine = Phases 9-10.** If time is short, 9 and 10 can be made smaller (for example search without facets) without breaking anything else.
- **Always = Phases 11-12.** A portfolio project that is not tested, not live and not explained is not finished.
