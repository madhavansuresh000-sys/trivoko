# Phase 5 Seller Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Sellers ship their packages (state machine + timeline), cancel with a partial refund, earn ledger lines, manage products with photos, and see a dashboard and charts.

**Architecture:** Package moves live in the `order` module (`PackageService`, `PackageStatus.allowedNext()`, `package_events`); the seller's money in the `seller` module (`LedgerService`, `seller_ledger`). The seller back office in React under `/seller` uses those APIs. Partial refunds go through `PaymentGateway.refund(session, amount, key)`.

**Tech Stack:** Spring Boot 4.1, Java 25, MySQL 8.4, Flyway V7, Caffeine cache; React 19, Recharts.

**Spec:** `Phase_5_Seller_Dashboard/Phase5_Design_Spec_2026-10-05_1425.md`

## Global Constraints

- Moves only via `PackageStatus.allowedNext()`; illegal move = 409 with a plain message. `@Version` conflict = 409.
- Tracking number `^[A-Za-z0-9]{6,30}$`; cancel reason 5-200 chars (seller only).
- Ledger SALE = +90% of package total, HALF_UP to paise, written once per package (on DELIVERED).
- Refund on cancel = package total; idempotency key `refund-package-<id>`.
- Low stock < 5. Photos max 8, URLs must start with `https://res.cloudinary.com/<cloud-name>/`.
- Seller URLs: active seller (APPROVED shop) + package/product ownership; customer cancel: own order only.
- Commits `Phase 5 step N: ...` + Co-Authored-By line.

## Review Focus

- Customer cancels at the same moment the seller ships → one wins, other 409 (Task 2 test `cancelVsShipRace`).
- DELIVERED twice (double click) → one ledger line only (Task 3 test `deliverTwiceOneLedgerLine`).
- Another shop's package id in the URL → 403, nothing changes (Task 2 test `otherShopsPackageIsForbidden`).
- Cancelling the last packages one by one → the refunds add up to at most the payment amount (Task 2 test `twoCancelsRefundBothPackages`).
- A photo URL from another website → 400 (Task 5 test `foreignPhotoUrlIsRefused`).

---

### Task 1: Schema V7 + state machine + events
**Files:** `db/migration/V7__packages_ledger_events.sql`; `order/PackageStatus.java` (+PACKED, SHIPPED, DELIVERED, CANCELLED, `Set<PackageStatus> allowedNext()`); `order/PackageEvent.java`, `PackageEventRepository.java`; `OrderPackage` (+trackingNumber, cancelReason, placedAt, deliveredAt, `moveTo(PackageStatus, actor, note)` which checks allowedNext and adds an event); `payment/Payment` (+refundedAmount). Test `order/PackageStatusTest.java`.
- [ ] RED: allowedNext table (PLACED→{PACKED,CANCELLED}, PACKED→{SHIPPED,CANCELLED}, SHIPPED→{DELIVERED}, DELIVERED/CANCELLED/EXPIRED→{}, PENDING_PAYMENT→{PLACED,EXPIRED}). GREEN. Existing markPaid/expire use `moveTo` (events "Placed", "Expired"). Full suite green. Commit.

### Task 2: Seller package moves + customer cancel + refund
**Files:** `order/PackageService.java` (`listForSeller(sellerId, status, pageable)`, `pack(sellerId, id)`, `ship(sellerId, id, tracking)`, `deliver(sellerId, id)`, `cancelBySeller(sellerId, id, reason)`, `cancelByCustomer(userId, number, id)`), `order/SellerPackageController.java`, `OrderController` (+cancel), `payment/PaymentGateway.refund(String session, BigDecimal amount, String key)` (Fake records; Stripe `RefundCreateParams.setAmount(paise)`), `PaymentService.refundPart(orderId, amount, key)`. Seller id from `SellerAccess.currentSellerId()`. Test `order/PackageMovesTest.java`.
- [ ] RED tests: full happy path pack→ship(AWB12345)→deliver with events; ship without tracking 400; `otherShopsPackageIsForbidden`; cancel after SHIPPED 409; seller cancel needs reason; cancel returns stock + payment refundedAmount = package total; `twoCancelsRefundBothPackages` (payment REFUNDED when sum = amount); customer cancel of own package; `cancelVsShipRace` (two threads, exactly one 2xx). GREEN. Commit.

### Task 3: Ledger + customer timeline + notifications
**Files:** `seller/SellerLedger.java`, `LedgerRepository`, `LedgerService` (`recordSale(sellerId, packageId, packageTotal)`, `balance(sellerId)`, `lines(sellerId, pageable)`), `seller/SellerLedgerController` (`GET /api/seller/ledger`); `OrderViews.PackageDetail` (+trackingNumber, events); notifications per spec section 6 (`NotificationKind` +PACKAGE_PACKED, PACKAGE_SHIPPED, PACKAGE_DELIVERED, PACKAGE_CANCELLED). Test `seller/LedgerTest.java`, extend `PackageMovesTest`.
- [ ] RED: deliver → ledger +90% (e.g. total 898.20 → 808.38); `deliverTwiceOneLedgerLine`; balance = sum; order detail shows 4 events; shipped → customer bell + email queued. GREEN. Commit.

### Task 4: Dashboard, low stock, sales (backend)
**Files:** `seller/SellerDashboardController` (`GET /api/seller/dashboard`, `/api/seller/sales?days=`, `/api/seller/products/low-stock`) + order/catalog service queries; `@Cacheable("sellerSales")` 5 min. Test `seller/DashboardTest.java`.
- [ ] RED: dashboard numbers for Kovai after 1 placed + 1 delivered package; low stock lists stock 3 variant; sales days=7 returns 7 points with today's total; days=5 → 400. GREEN. Commit.

### Task 5: Product photos API
**Files:** `catalog/ProductService.replaceImages(productId, List<ImageInput>)`, `SellerProductController` (+`PUT /{id}/images`), `catalog/dto/ImageInput(url, altText)`. Test `seller/ProductImagesTest.java`.
- [ ] RED: 2 photos saved in order (sortOrder 0,1) and first becomes card image; 9 photos 400; `foreignPhotoUrlIsRefused`; other shop's product 403. GREEN. Commit.

### Task 6: Frontend seller back office
**Files:** `pages/seller/{SellerLayout,SellerDashboardPage,SellerPackagesPage,SellerProductsPage,ProductEditorPage,EarningsPage,AnalyticsPage}.jsx`, `api/seller.js`, `components/seller/*`, routes (guard `isActiveSeller`), account menu "My shop"; `npm i recharts`. Test `pages/seller/ProductEditorPage.test.jsx` (add/remove variant rows; live product locks name), `SellerPackagesPage.test.jsx` (ship asks for tracking).
- [ ] RED → GREEN, lint, build. Commit.

### Task 7: Customer timeline + cancel button
**Files:** `components/order/PackageTimeline.jsx`, `OrderDetailPage` (timeline per package, AWB, "Cancel this package" with confirm modal). Test `components/order/PackageTimeline.test.jsx`.
- [ ] RED → GREEN. Commit.

### Task 8: Browser check, docs, verify
- [ ] Test seller in dev DB (approve an application via admin API needs DEMO_PASSWORD; otherwise jdbc-free route: register test seller + apply + approve with admin login if available - else note it). Seller packs → ships → customer timeline. README Phase 5, `docs/decisions.md` D6 (state machine + ledger as lines), `mvnw verify` + frontend green. Ledger + final self-review.
