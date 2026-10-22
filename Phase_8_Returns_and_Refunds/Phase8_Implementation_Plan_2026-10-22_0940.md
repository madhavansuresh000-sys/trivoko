# Phase 8 - Returns & refunds: implementation plan

Spec: `Phase8_Design_Spec_2026-10-22_0935.md` · Branch `phase-8-returns-and-refunds` · Test-first for every backend task.
★ = Madhavan types it (Claude explains first, then reviews).

| # | Task | Files (main) | Test |
|---|---|---|---|
| 1 | V10: `return_requests`, `return_events`, `seller_ledger.return_id` + new unique key; entities + repositories | `V10__returns.sql`, `returns/ReturnRequest`, `ReturnEvent`, repos | Flyway + `ddl-auto: validate` in any Spring test |
| 2 | ★ `ReturnStatus` enum with `allowedNext()` and `ReturnService.moveTo(ret, next, actor, note)` (writes the event) | `returns/ReturnStatus`, `ReturnService` | ★ step 3 |
| 3 | ★ Tests for allowed + illegal moves | `ReturnStatusTest` | itself |
| 4 | Ask for a return: delivered, 7 days, once per item, own item, reason + photo | `ReturnService.request`, `CustomerReturnController`, `OrderService` (read-only view of an item) | `ReturnRequestRulesTest` |
| 5 | Seller approve / reject; job auto-approves after 2 days; customer cancel | `SellerReturnController`, `ReturnAutoApproveJob` | `ReturnSellerTest` |
| 6 | Escalate once; admin approve / close | `AdminReturnController` | `ReturnEscalationTest` |
| 7 | Picked up -> partial refund (key `refund-return-<id>`), REFUND_FAILED + retry | `ReturnService.pickedUp`, `PaymentService.refundPart` | `RefundMathsTest`, `DoubleRefundTest` |
| 8 | REFUNDED -> stock back, ledger -90% line, bell + email | `LedgerService.recordRefund`, `ReturnNotifications` | `ReturnFlowTest` |
| 9 | Frontend: return form + timeline + My returns, seller returns page, admin disputes, return-rate card | `api/returns.js`, pages | Vitest |
| 10 | Return-rate report, security sweep, README + decision D9, Postman folder 11, full verify, Stripe test-mode check (done-when) | | `mvnw verify`, Vitest, lint, build |

Order: 1 -> (Madhavan: 2, 3) -> 4 ... 10. Tasks 4-8 can start against an interface while Madhavan writes 2-3, but
they are not merged until his state machine is in.
