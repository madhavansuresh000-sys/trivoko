# 4. How we know it works

## 4.1 The test pyramid

```
                 ▲  End-to-end: Playwright clicks the real app in a real browser
                ╱ ╲   buy from 2 sellers -> 1 payment -> ship -> deliver -> return -> refund
               ╱───╲
              ╱     ╲  Integration: ~400 JUnit tests, each on a real MySQL (Testcontainers)
             ╱       ╲   rules, security, money, races (10 clicks = 1 refund)
            ╱─────────╲
           ╱           ╲  Unit + component: 103 Vitest (React) + plain JUnit
          ╱             ╲   one function or one screen alone
         ───────────────────
```

>> A car factory tests every bolt (unit), then the engine on a bench (integration), then drives the whole car on the road (end-to-end). Many cheap bolt tests, a few expensive road tests.

**Why a real MySQL in tests?** An in-memory database (H2) behaves differently: locking, `CHECK` constraints and some SQL. The flash-sale and stock tests depend on real MySQL locking, so **Testcontainers** starts a throwaway MySQL 8.4 in Docker for the test run.

**Race tests:** `StockConcurrencyTest` (100 customers, last 10 units -> exactly 10 orders), the flash-sale claim test, `ReturnRefundTest` (10 clicks at once -> 1 refund).

## 4.2 CI - GitHub Actions on every push

@widths 2600 6426
| Job | What it checks |
| Backend | `mvnw verify`: all JUnit tests, ArchUnit module rule, JaCoCo coverage (fails below 80% of the service code) |
| Frontend | lint (oxlint), Vitest, production build |
| E2E | the Playwright demo story + accessibility (axe, WCAG 2.1 AA) in light/laptop and dark/phone |
| Security scan | `npm audit` (high), gitleaks (no secret in any commit) |
| Uptime (launch) | every 10 minutes: is the live API UP? a failure e-mails the owner |

## 4.3 Performance and accessibility

- **k6 load test** (Phase 7): 5,000 users at the same instant -> exactly 100 winners, 0 errors. Speed in that burst (p95 about 1 s on one laptop) is above the 500 ms target - correctness first; decision D8 explains when Redis would be added.
- **Lighthouse** on a phone profile: Performance 92, Accessibility 100, Best practices 96, SEO 100 (self-hosted font, lazy-loaded pages, no layout jump).

# 5. How it is launched

## 5.1 The three free services

@widths 1800 2600 4626
| Piece | Service | Notes |
| Database | Aiven free MySQL (Bengaluru) | Flyway creates all tables on the first start |
| API | Render free, Docker (Singapore) | `render.yaml` Blueprint; sleeps after 15 idle minutes, so the uptime check pings it |
| Shop | Vercel | `vercel.json`: `/api/*` to Render, other paths to `index.html`, security headers |

## 5.2 Making Spring Boot fit in 512 MB

- **Multi-stage Docker image:** stage 1 builds the JAR with Maven, stage 2 holds only the Java runtime and the JAR - smaller, and no source code on the server. It runs as a normal user, not root.
- **Java options:** `-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k` - Java may use 75% of the memory, the simplest garbage collector, smaller thread stacks.
- **prod profile:** 40 web threads (not 200), 5 database connections, search index in `/tmp`.
- Measured in Docker before going live: about 400-430 MB after a full purchase.

## 5.3 Production settings

- Everything comes from **environment variables**: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS`, `DEMO_PASSWORD`, Cloudinary and Stripe keys. The JWT secret has **no fallback** in prod - the server refuses to start without it.
- `server.forward-headers-strategy: framework`: behind Vercel and Render the app sees the visitor's real address (from `X-Forwarded-For`) and knows the request was HTTPS - needed for the login lock and the Secure cookie.

## 5.4 What a launch taught us

- A Render Blueprint form can drop secret fields that were typed in it - always **check the list of variables** after creating the service.
- "Access denied for user" from MySQL means the address was right and the password was wrong (or missing) - read the log line before changing anything.

# 6. What I would do next

- **Redis** for the flash-sale counter and the rate limit, if the burst must be faster than 500 ms or the app runs as several copies.
- **Elasticsearch / OpenSearch** instead of the in-app Lucene index if the app runs as several copies.
- **Real e-mail** through Brevo (the code is ready; only the SMTP settings are missing on the live server).
- **Seller payouts** (the ledger already knows each seller's balance).
- **Images resized on upload** and a CDN for the product photos.
