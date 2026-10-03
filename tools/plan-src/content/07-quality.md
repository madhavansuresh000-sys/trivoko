# 7. Security, testing and quality

## 7.1 Security checklist

| Risk | Protection |
| Stolen login | JWT in an **httpOnly, Secure, SameSite** cookie; BCrypt passwords; 5 failed logins → 15-minute lock (EventHub `LoginAttemptService`) |
| Forged requests from other sites | CSRF double-submit token (EventHub pattern) |
| Seller A reading Seller B's orders | Every seller query is filtered by the logged-in seller's id; `@PreAuthorize` ownership checks; tests that try to read another seller's package must get 403/404 |
| Customer changing prices in the browser | Server recalculates every price, discount and shipping fee at checkout |
| Fake payment confirmation | Stripe webhook **signature check**; idempotent processing (EventHub `processed_payment_events`) |
| Double refund | One refund key per return request; Stripe idempotency key |
| Bot spamming the flash sale | Rate limit per user/IP on `claim` (429 + Retry-After); one claim per customer |
| Bad file uploads | Signed Cloudinary uploads only, images only, max 5 MB, max 8 photos |
| Secrets in Git | `.env` git-ignored; secret scan of the history before launch |
| Admin tools exposed | Swagger and Actuator for ADMIN only in production |

## 7.2 Testing plan

| Level | Tool | What is tested | Target |
| Unit | JUnit 5 + Mockito | Money maths (coupon split, refund amount, ledger), state machines, rules | Every rule |
| Integration | `@SpringBootTest` + MockMvc + **Testcontainers MySQL** | Every endpoint, security per role, flows (checkout → pay → split → ship → return → refund) | Service coverage ~80% (JaCoCo) |
| Concurrency | Plain Java threads | Flash sale 1,000 threads / 100 units; checkout stock hold 100 threads / 10 units; cancel-vs-ship race | Never oversell, always one winner |
| Frontend | Vitest + React Testing Library | Cart maths, forms, countdown, key components | Main components |
| End-to-end | **Playwright** | The demo story in a real browser: buy from 2 sellers, seller ships, customer returns | Runs in CI |
| Load | **k6** | Flash sale with 5,000 virtual users; product listing under load | Exactly 100 winners, p95 < 500 ms locally |
| Manual | Phone + laptop, light + dark | Every page | No layout bugs |

> Every phase ends with: tests pass locally, **CI green on GitHub**, commit pushed, and the "done when" checks of that phase shown working in the browser.
