# Phase 4 Checkout & Payments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Pay once for a cart from many sellers; the order is split into one package per seller, stock is held safely, coupons apply, emails go out and a PDF invoice can be downloaded.

**Architecture:** New modules `order`, `payment`, `coupon`, `notify` (module rule: services only). The money split is one pure class, `order/OrderSplitter` (★ Madhavan). Stock is held with one atomic UPDATE per line in `catalog`. The payment company sits behind `payment/PaymentGateway` (Fake for dev, Stripe when a key is set), copied from EventHub.

**Tech Stack:** Spring Boot 4.1, Java 25, MySQL 8.4, Flyway, stripe-java 34.0.0, OpenPDF 3.0.5 (`org.openpdf.text`), Spring Mail + Mailpit; React 19 + Redux Toolkit.

**Spec:** `Phase_4_Checkout_and_Payments/Phase4_Design_Spec_2026-10-05_1030.md`

## Global Constraints

- Money `BigDecimal` scale 2, HALF_UP; Stripe gets paise (`Money.toPaise`).
- Delivery per package: items total (before discount) ≥ ₹499 → 0, else ₹40. Quantity ≤ 10 per item.
- Hold time 10 minutes; expiry job every 60 s (`app.order.expiry-check`), off in tests (`app.scheduling.enabled=false`).
- Order number `TV-` + 6 digits. Coupons upper-case. One redemption per (coupon, user).
- Module rule: `order` calls `CartService`, `ProductService`, `CouponService`, `AddressService`/`UserService`, `PaymentService`; never their repositories.
- Commits: `Phase 4 step N: ...` + Co-Authored-By line. ★ task = Madhavan's code; Claude only reviews it.

## Review Focus

- Two tabs press "Pay" at once → only one PENDING order; the second gets the same payment link (Task 5 test `secondCheckoutReusesPendingOrder`).
- Price changed between preview and place → 409 with the new preview, nothing held (Task 5 test `priceChangedIsRefused`).
- One line of three runs out during the hold → whole checkout rolled back, other two lines' stock unchanged (Task 5 test `ranOutRollsBackEveryHold`).
- Webhook for an order that already EXPIRED and whose stock is gone → refunded, order stays EXPIRED (Task 6 test `latePaymentWithoutStockIsRefunded`).
- A coupon used in a paid order cannot be used again by the same customer, but works for another customer (Task 2 test `onePerCustomer`).

---

### Task 1: V5 schema, entities, Money

**Files:** Create `db/migration/V5__orders_payments_coupons.sql`; `order/{Order,OrderStatus,Package,PackageStatus,OrderItem,OrderRepository}.java`; `payment/{Payment,PaymentStatus,PaymentProvider,PaymentRepository,ProcessedPaymentEvent,ProcessedPaymentEventRepository}.java`; `coupon/{Coupon,CouponType,CouponRedemption,CouponRepository,CouponRedemptionRepository}.java`; `common/Money.java`. Test `common/MoneyTest.java`, `order/OrderSchemaTest.java`.
**Interfaces:** `Money.toPaise(BigDecimal) -> long`, `Money.round(BigDecimal) -> BigDecimal` (scale 2 HALF_UP), `Money.ZERO`. Tables exactly as spec section 3; seed WELCOME10 (PERCENT 10, max 500, min 499, valid 1 year) and FLAT100 (FLAT 100, min 999).
- [ ] Step 1: MoneyTest: `toPaise(15400.00) == 1540000`, `toPaise(99.5) == 9950`, `round(99.805) == 99.81`, `toPaise(null)` throws IllegalArgumentException. OrderSchemaTest: Flyway V5 runs, both coupons present.
- [ ] Step 2: FAIL → implement → PASS (`mvnw test -Dtest=MoneyTest,OrderSchemaTest,ModuleRulesTest`). Commit `Phase 4 step 1`.

### Task 2: Coupons

**Files:** `coupon/CouponService.java`, `coupon/CouponController.java` (`GET /api/coupons/{code}`), `admin/AdminCouponController.java` (`GET/POST /api/admin/coupons`), `coupon/dto/*`. Test `coupon/CouponServiceTest.java`.
**Interfaces:** `CouponService.quote(String code, Long userId, BigDecimal itemsTotal, boolean usedInPendingOrder) -> CouponQuote(String code, BigDecimal discount, String error)` - error = null when valid, discount 0 when not; never throws for a bad code (the `order` module answers `usedInPendingOrder`, since it owns orders). `redeem(String code, Long userId, Long orderId)` - called when the order is PAID.
- [ ] Step 1: Tests: WELCOME10 on 3497 → 349.70; on 6000 → 500 (cap); on 400 → error "Add ₹99 more to use WELCOME10"; FLAT100 on 999 → 100; unknown → "This coupon does not exist"; expired → "This coupon has expired"; `onePerCustomer` (redeem for ravi → ravi error "You have already used WELCOME10", kavya fine); admin create lower-case code stored upper; non-admin POST 403.
- [ ] Step 2: FAIL → implement → PASS. Commit.

### Task 3: ★ OrderSplitter (Madhavan)

**Files:** Claude creates `order/OrderSplitter.java` skeleton with the records and a method that throws `UnsupportedOperationException("Madhavan writes this")`, plus `Phase_4_Checkout_and_Payments/OrderSplit_Lesson_<ts>.md` (diagram + the worked example). **Madhavan writes** the method body and `order/OrderSplitterTest.java`.
**Interfaces (fixed so Task 5 can use them):**
```java
public record SplitLine(Long variantId, Long sellerId, String sellerName, String productSlug, String productName,
        String variantLabel, BigDecimal unitPrice, BigDecimal mrp, int quantity) {}
public record ItemPlan(SplitLine line, BigDecimal lineTotal, BigDecimal discountShare) {}
public record PackagePlan(Long sellerId, String sellerName, List<ItemPlan> items, BigDecimal itemsTotal,
        BigDecimal discount, BigDecimal shippingFee, BigDecimal total) {}
public record SplitResult(List<PackagePlan> packages, BigDecimal itemsTotal, BigDecimal discountTotal,
        BigDecimal shippingTotal, BigDecimal grandTotal) {}
public static SplitResult split(List<SplitLine> lines, BigDecimal discount)
```
- [ ] Step 1 (★): Madhavan's test - spec section 4 example: packages Chennai then Kovai; case share 99.80, shoes 249.90; delivery 0 + 0; grand total 3147.30. Plus his choice of a second test (e.g. ₹200 package → ₹40 delivery).
- [ ] Step 2 (★): he runs it, sees FAIL, writes `split`, sees PASS.
- [ ] Step 3: Claude reviews; adds review-only tests if a rule is missed (shares sum exactly to discount; zero discount; one line). Commit `Phase 4 step 4-5 (Madhavan): order split`.

### Task 4: Stock hold + fake payment gateway

**Files:** `catalog/ProductService` (+`holdStock(Map<Long,Integer>)`, `releaseStock(Map<Long,Integer>)`, `catalog/ProductVariantRepository` (+`@Modifying` take/give queries)); `payment/{PaymentGateway,FakePaymentGateway,PaymentConfig,PaymentProviderException,PaymentService}.java`. Test `catalog/StockHoldTest.java`.
**Interfaces:** `holdStock` throws `BusinessRuleException("Sorry, <name> (<label>) has only N left")` on the first line that cannot be taken (caller's transaction rolls back); `PaymentGateway.createCheckout(OrderPayment(orderNumber, email, BigDecimal amount, List<String> lineNames)) -> CheckoutSession(sessionId, redirectUrl)`, `isPaid`, `expireSession`, `refund`. `PaymentService.start(Order)`, `markPaid(eventId, sessionId) -> PaidResult`.
- [ ] Step 1: StockHoldTest: hold 2 of stock 5 → 3; hold 6 of 5 → exception names the item, stock 5; release adds back. FAIL → implement → PASS. Commit.

### Task 5: Checkout preview + place order

**Files:** `order/{CheckoutService,CheckoutController,OrderNumbers}.java`, `order/dto/{CheckoutRequest,PlaceOrderRequest,CheckoutPreview,PlacedOrder}.java`; `cart/CartService` (+`checkoutLines(userId) -> List<CartLine(variantId, quantity)>`, `removeVariants(userId, Set<Long>)`). Test `order/CheckoutTest.java`.
**Interfaces:** `POST /api/checkout/preview`, `POST /api/orders` per spec 5; preview reuses `OrderSplitter` and `CouponService.quote`.
- [ ] Step 1: Tests: happy path (ravi, 2 sellers, WELCOME10) → 201 `{number: TV-xxxxxx, redirectUrl: .../test-payment/...}`, order PENDING_PAYMENT, 2 packages, stock down; `priceChangedIsRefused`; `ranOutRollsBackEveryHold`; `secondCheckoutReusesPendingOrder`; empty cart 409; address of another user 404; coupon invalid 409.
- [ ] Step 2: FAIL → implement → PASS. Commit.

### Task 6: Payment confirm, expiry, late payment, orders API

**Files:** `order/OrderService` (`markPaid`, `expireOldHolds`, `list`, `get`), `order/OrderExpiryJob.java`, `order/OrderController.java`, `payment/PaymentController.java` (fake page endpoints + verify; webhook in Task 8). Test `order/PaymentLifecycleTest.java`, `order/OrdersApiTest.java`.
- [ ] Step 1: Tests: fake complete → order PAID, packages PLACED, coupon redeemed, ordered lines gone from cart, other lines stay; duplicate complete → no change; expiry after 10 min (clock) → EXPIRED, stock back; `latePaymentWithoutStockIsRefunded`; late payment with stock free → PAID; GET /api/orders newest first; other user's order 404.
- [ ] Step 2: FAIL → implement → PASS. Postman folder "Checkout". Commit.

### Task 7: Frontend checkout

**Files:** `api/orders.js`, `pages/{CheckoutPage,TestPaymentPage,PaymentResultPage,OrdersPage,OrderDetailPage}.jsx`, `components/order/*`, routes + account menu link. Test `pages/CheckoutPage.test.jsx`.
- [ ] Step 1: Test: preview line price differs from the cart view → "Price changed: X is now ₹499 (was ₹549)"; invalid coupon shows its error; Pay button shows the total. FAIL → implement → PASS + browser run (fake pay → success → My orders). Commit.

### Task 8 (4B): Real Stripe

**Files:** `payment/StripePaymentGateway.java` (copy, stripe-java 34 API), webhook endpoint, verify; `order/StockConcurrencyTest.java`.
- [ ] Step 1: Tests: bad signature 400; signed test event (Webhook.generateTestHeaderString) → PAID; same event twice → one change; 100 threads × 1 unit on stock 10 → exactly 10 PAID-able orders (PENDING) and stock 0.
- [ ] Step 2 (You): Stripe test account, `STRIPE_SECRET_KEY` + `STRIPE_WEBHOOK_SECRET` in `.env`, Stripe CLI `stripe listen --forward-to localhost:8080/api/payments/stripe/webhook`.
- [ ] Step 3 (Together): pay with 4242 4242 4242 4242 → order PAID by the signed webhook. Commit.

### Task 9 (4C): notify module

**Files:** `notify/*` copied from EventHub (Notification, EmailSender with retry, after-commit triggers), V6 `notifications`; templates for "Order TV-xxxx placed" (customer) and "New package to pack" (each seller); bell API + navbar bell. Test `notify/OrderEmailsTest.java` (GreenMail or Mailpit-free mock sender).
- [ ] Step 1: Test: paying an order sends 1 customer mail + 1 mail per seller, after commit only; failed send retried. FAIL → implement → PASS. Commit.

### Task 10 (4C): PDF invoice

**Files:** `order/InvoicePdf.java` (OpenPDF `org.openpdf.text`), `GET /api/orders/{number}/invoice.pdf`. Test `order/InvoiceTest.java` (PDF text contains number, items, totals; PENDING order → 409; other user 404).
- [ ] Step 1: FAIL → implement → PASS. Commit.

### Task 11: Browser check, docs, verify

- [ ] Full fake-payment run in the browser (phone + dark); README (Phase 4 status, checkout API, Stripe setup); `docs/decisions.md` D5 (order split + coupon share per item; one pending order per customer); `mvnw verify` + frontend checks green; push/PR only after Madhavan's yes.
