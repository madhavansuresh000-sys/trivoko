# Phase 6 - Admin panel: implementation plan

Spec: `Phase6_Design_Spec_2026-10-05_2308.md` · Branch `phase-6-admin-panel` · Test-first for every backend task.

| # | Task | Files (main) | Test |
|---|---|---|---|
| 1 | Paged admin lists with search: sellers, products (update the 2 old tests to `$.content`) | `SellerService`, `SellerRepository`, `ProductService`, `Admin*Controller` | `AdminListsTest` |
| 2 | Seller block with reason + unblock; notifications to the owner on approve / reject / block; product approve / reject notifications | `SellerService`, `ProductService`, `NotificationKind`, new `admin/AdminNotices` | `SellerBlockTest` |
| 3 | Users: search (paged), block / unblock (not an admin) | `UserService`, `UserRepository`, `admin/AdminUserController` | `AdminUsersTest` |
| 4 | Orders search (paged) for the admin | `OrderService`/`OrderRepository`, `order/AdminOrderController` | `AdminOrdersTest` |
| 5 | Coupons edit + end | `CouponService`, `CouponUpdateRequest`, `AdminCouponController` | `CouponAdminTest` |
| 6 | Overview + reports (sales/day, top products, top sellers) | `order/ReportService`, `analytics/AdminReportController`, `analytics/AdminOverviewService` | `ReportsTest` |
| 7 | Activity log endpoint (with actor email) | `AuditService`, `AuditLogRepository`, `AdminActivityController` | `ActivityLogTest` |
| 8 | Security sweep: every `/api/admin/**` URL -> 403 for customer + seller | - | `AdminSecurityTest` |
| 9 | Frontend: api/admin.js, AdminLayout + 8 pages, routes, account menu link | `frontend/src/pages/admin/*` | Vitest |
| 10 | Browser check (admin login needs `DEMO_PASSWORD`), README + decisions D7, Postman, full verify | | `mvnw verify`, Vitest, lint, build |
