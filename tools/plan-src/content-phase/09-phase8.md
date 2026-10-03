# Phase 8 - Returns & refunds

**Time:** 1.5 weeks  ·  **Group:** Core  ·  **Folder:** `Phase_8_Returns_and_Refunds\`

**Goal:** Standout #2: a customer returns one item and gets back exactly the right money through a Stripe partial refund, while stock and the seller ledger stay correct.

>> You return one shirt from a bill of three. The shop checks it, takes it back to the shelf, and returns only that shirt's money - minus its part of the discount you used.

## Steps

@widths 600 6926 1500
| # | Step | Who |
| 1 | Flyway: `return_requests`, `refunds` | Claude |
| 2 | ★ You type the RETURN STATE MACHINE: `ReturnStatus` enum with `allowedNext()` and the service method that moves a return | ★ Madhavan |
| 3 | ★ You write tests for allowed and illegal moves | ★ Madhavan |
| 4 | Request a return: DELIVERED items only, within 7 days, once per item, reason + optional photo | Claude |
| 5 | Seller approve / reject with reason; auto-approve job after 2 days | Claude |
| 6 | Escalate once; admin makes the final decision | Claude |
| 7 | Picked up → Stripe partial refund of (price × qty − coupon share), one idempotency key per return | Claude |
| 8 | Refund done → stock back, minus line in the seller ledger, email + bell | Claude |
| 9 | Frontend: return form + timeline (customer), returns page (seller), disputes page (admin); return rate in admin reports | Claude |
| 10 | Tests: refund maths, double click = one refund | Claude |

## Output of this phase

- Complete returns flow for 3 roles
- Stripe partial refunds
- Your ReturnStatus state machine + tests

## Done when

- Return one item of a 2-item order → the exact partial refund appears in the Stripe test dashboard
- Ledger and stock match

## Explain it back

- Draw the return states and say who can make each move
- How is the refund amount calculated?
- Why can a refund never happen twice?

**Skills you practise:** State machines, Stripe refunds, idempotency, scheduled jobs

**Where your work goes:** `backend\...\returns`, `...\payment`, return pages in `frontend`
