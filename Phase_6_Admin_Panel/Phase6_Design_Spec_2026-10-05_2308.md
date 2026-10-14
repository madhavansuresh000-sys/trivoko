# Phase 6 - Admin panel: design spec

Date: 5 Oct 2026 · Branch `phase-6-admin-panel` (built on top of `phase-5-seller-dashboard`, which sits on
`phase-4-checkout-and-payments`; PR #4 is not created yet) · Status: **written by Claude; Madhavan said "continue";
defaults marked [default]**.

## 1. Goal and "done when"

The admin controls the marketplace: approves sellers and products, blocks bad actors, creates coupons and reads
reports. (The mall office: it decides which shops may open, can close a shop that cheats customers, prints
discount coupons and reads the daily sales register.)

**Done when:** the admin blocks a seller -> their products disappear from the shop · every admin URL answers 403
to a customer and a seller · CI green.

## 2. What already exists (Phase 2 + 4)

| Already there | Phase 6 adds |
|---|---|
| `GET /api/admin/sellers?status=` (plain list), approve, reject, block | paging + search box, **unblock**, block **with a reason**, bell + email to the shop owner |
| `GET /api/admin/products?status=` (plain list), approve, reject | paging + search, bell + email to the seller |
| `GET/POST /api/admin/coupons` | **edit** and **end** a coupon |
| `audit_log` + `AuditService.record` | the activity log page (paged, filter by action) |
| `users.enabled` (the JWT filter already refuses a disabled user) | block / unblock a user from the admin panel |
| Shop queries already show only `ACTIVE` products of `APPROVED` shops (`ProductSpecifications.visibleInShop`) | a test that proves "block seller -> products gone" on listing, product page and brands |

## 3. Rules

- **Block a seller** (any status except BLOCKED) -> `BLOCKED` + reason. Their products vanish from every shop
  query at once (all of them go through `visibleInShop()` - one place, nothing can be forgotten). Their packages
  that are already paid stay as they are (customers paid; the admin handles them by hand) **[default]**.
- **Unblock a seller** -> `APPROVED` again (products come back) **[default: no "re-review" step]**.
- **Block a user** -> `enabled = false`: they cannot log in, and their current cookie stops working on the next
  request (the JWT filter reads the user from the database every time). The admin cannot block an ADMIN
  (also not themselves) -> 409.
- **Unblock a user** -> `enabled = true`.
- **Coupons**: edit = description, value, max discount, min order, valid until (the code and type never change -
  customers already know the code) **[default]**. **End** = `active = false` (never deleted: redemptions point to it).
- Every admin action writes one audit line (`SELLER_UNBLOCKED`, `USER_BLOCKED`, `USER_UNBLOCKED`, `COUPON_UPDATED`,
  `COUPON_ENDED`, plus the Phase 2 ones).
- Reasons (reject / block): 5-300 characters, shown to the seller.

## 4. Messages (bell + email, after commit, background - the Phase 4 notify module)

| Event | To | Email? |
|---|---|---|
| Shop approved | shop owner | yes |
| Shop rejected (with reason) | shop owner | yes |
| Shop blocked (with reason) | shop owner | yes |
| Product approved | shop owner | bell only **[default]** |
| Product rejected (with reason) | shop owner | yes |

New `NotificationKind` values: `SHOP_APPROVED`, `SHOP_REJECTED`, `SHOP_BLOCKED`, `PRODUCT_APPROVED`, `PRODUCT_REJECTED`.

## 5. Reports (read-only, from the order tables)

A "sale" = a package that was PLACED and not cancelled/expired (same rule as the seller chart, Phase 5).

| Report | Query idea |
|---|---|
| Sales per day (7 / 30 / 90 days) | `GROUP BY DATE(placed_at)` over packages -> total + package count; empty days = 0 |
| Top products (10) | `order_items` of sold packages `GROUP BY product_slug` -> units, revenue |
| Top sellers (10) | packages `GROUP BY seller_id` -> packages, revenue, TriVoKo fee (10%) |
| Return rate | shown as "after Phase 8" (returns do not exist yet) |

Cached 60 s like the seller chart (existing Caffeine spec) **[default]**.

## 6. API (all ADMIN only - `SecurityConfig` already guards `/api/admin/**`)

| URL | What |
|---|---|
| `GET /api/admin/overview` | pending shops, pending products, customers, approved shops, today's orders + sales |
| `GET /api/admin/sellers?status=&q=&page=` | paged; q = shop name / city / slug |
| `POST /api/admin/sellers/{id}/block {reason}` · `/unblock` | block now needs a reason |
| `GET /api/admin/products?status=&q=&page=` | paged; q = product name / brand |
| `GET /api/admin/users?q=&page=` | paged; q = email / name / phone; shows roles + blocked |
| `POST /api/admin/users/{id}/block` · `/unblock` | |
| `GET /api/admin/orders?q=&status=&page=` | paged; q = order number or customer email |
| `PUT /api/admin/coupons/{id}` · `POST /api/admin/coupons/{id}/end` | |
| `GET /api/admin/reports/sales?days=` · `/top-products?days=` · `/top-sellers?days=` | |
| `GET /api/admin/activity?action=&page=` | audit log, newest first, with the actor's email |

Page size 20 (the existing `PageResponse`). This also closes the Phase 2 later-note "admin lists unpaged".

## 7. Frontend (`/admin/*`, like the seller back office)

`AdminLayout` (side menu on laptop, tab row on phone; non-admins see "not for you") with pages:
Overview (stat cards + links to the queues) · Shops (tabs Pending / Approved / Blocked / Rejected, search, approve,
reject/block with a reason modal, unblock) · Products (Pending queue with approve / reject) · Users (search, block /
unblock) · Orders (search by number or email) · Coupons (list, create, edit, end) · Reports (sales chart + two top-10
tables) · Activity log. Account menu gets "Admin panel" for admins.

## 8. Tests

- Backend: every new admin URL answers 403 to a customer and to a seller; block seller -> listing, product page and
  brands no longer show the products, unblock -> back; blocked user cannot log in and their old cookie gets 401;
  admin cannot block an admin; coupon edit + end (ended coupon refused at checkout quote); reports maths on a known
  order; activity log lists the actions newest first; approval sends a notification to the owner.
- Frontend (Vitest): admin layout guard, shops page reject asks for a reason, coupons form.

## 9. Not in this phase

Payouts, editing users' details, deleting anything, the return rate (Phase 8), flash-sale scheduling (Phase 7).
