# Phase 6 - Admin panel

**Time:** 1 week  ·  **Group:** Core  ·  **Folder:** `Phase_6_Admin_Panel\`

**Goal:** The admin controls the marketplace: approves sellers and products, blocks bad actors, creates coupons and reads reports.

>> The mall office: it decides which shops may open, can close a shop that cheats customers, prints discount coupons and reads the daily sales register.

## Steps

@widths 600 6926 1500
| # | Step | Who |
| 1 | Admin layout + overview cards | Claude |
| 2 | Seller approvals (approve / reject with reason, email to the seller) | Claude |
| 3 | Product approvals (approve / reject with reason) | Claude |
| 4 | Search users, sellers and orders with paging | Claude |
| 5 | Block a seller → their products disappear from the shop; block a user → they cannot log in | Claude |
| 6 | Coupons page (create, edit, end) | Claude |
| 7 | Reports: sales per day, top products, top sellers, return rate (filled in Phase 8) | Claude |
| 8 | Activity log page from the audit log | Claude |
| 9 | Tests for every admin action and that non-admins get 403 | Claude |

## Output of this phase

- Admin panel with approvals, users, coupons, reports, activity log

## Done when

- Admin blocks a seller → their products disappear from the shop
- CI green

## Explain it back

- How is "blocked" applied to every shop query without forgetting one?
- What is written to the audit log and why?

**Skills you practise:** Role-based pages, reporting queries (GROUP BY), audit trails

**Where your work goes:** `frontend\src\pages\admin`, `backend\...\admin`, `...\analytics`
