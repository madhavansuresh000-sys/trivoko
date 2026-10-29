# Phase 11 step 3 - Security review (29 Oct 2026)

Before the grand opening the inspector checks every lock, every fire exit and the alarm. This is that walk-through
for TriVoKo: automatic scans first, then the OWASP Top 10 (the 10 most common ways web apps are broken into).

## 1. What the scans found - and what was done

| Scan | Found | Fix |
|---|---|---|
| **Secrets in the Git history** (gitleaks 8.24.2, every commit of every branch, 3.1 MB of changes) | nothing | - (and `.env` was never committed) |
| **Java libraries** (OSV-Scanner on the exact list of 151 libraries, made by CycloneDX) | **18 known vulnerabilities**: Tomcat 11.0.24 (4 *critical*, CVSS 9.1-9.8), Jackson 2.21.5 / 3.1.5 (*high*) | Spring Boot **4.1.1 -> 4.1.2** (Tomcat 11.0.27, Jackson 2.21.7 / 3.1.7). Rescan: no issues. 392 tests green. |
| **npm** frontend (`npm audit`) | 2 *high*: vite 8.3.0-8.3.2 (dev server), undici (used by jsdom in tests) | `npm audit fix` -> vite 8.3.5, undici 8.12.3; 0 vulnerabilities |
| npm e2e | nothing | - |
| A first OSV run on `pom.xml` alone | "No issues" - **misleading**: Maven Central throttled it (HTTP 429), so it checked only 7 direct libraries | Lesson: scan the resolved dependency list (SBOM), not the pom |

**Kept that way:** a new CI job "Security scan" repeats all three scans on every push, so a leaked key or a newly
published vulnerability turns CI red instead of hiding.

## 2. OWASP Top 10 (2021) - where TriVoKo stands

| # | Risk | How TriVoKo handles it | Proof |
|---|---|---|---|
| A01 | Broken access control | Every `/api/seller/**` call checks the shop owns the thing (`@sellerAccess`); customers only see their own orders, returns, reviews, wishlist; admin URLs need ADMIN; roles re-read from the DB on every request (D3) | SellerOwnershipTest, SecurityRulesTest, AdminSecurityTest, ReturnSellerTest, ReviewTest (403s) |
| A02 | Cryptographic failures | Passwords hashed with BCrypt (max 72 bytes enforced); JWT signed, in an httpOnly + SameSite=Lax cookie, Secure in prod; prod refuses to start without a real `JWT_SECRET` | AuthFlowTest |
| A03 | Injection | Only JPQL / Specifications with parameters - no SQL built from text; `LIKE` wildcards escaped (`Likes.contains`); React escapes all text (no `dangerouslySetInnerHTML` anywhere), so a review cannot run a script | ProductSearchTest |
| A04 | Insecure design | Money moves go through state machines (packages, returns), idempotency keys (Stripe, refunds), `@Version` against double clicks, CHECK constraints in MySQL | ReturnRefundTest (10 clicks = 1 refund), FlashClaimConcurrencyTest |
| A05 | Security misconfiguration | Swagger only for ADMIN; actuator shows only health; no stack traces in errors (`GlobalExceptionHandler`); CORS allow-list; Spring Security headers (nosniff, X-Frame-Options DENY) | CsrfTest, SecurityRulesTest |
| A06 | Vulnerable components | Fixed today (section 1) + CI "Security scan" job | CI |
| A07 | Identification & auth failures | Login locked 15 min after repeated failures per email + address (not per email alone, so nobody can lock YOUR account); same "wrong email or password" message for both cases; blocked users stop at their next click | LoginLockTest |
| A08 | Software & data integrity | Stripe webhooks checked with the `Stripe-Signature` HMAC; each payment event processed once (`processed_payment_events`) | StripeWebhookTest |
| A09 | Logging & monitoring | Admin decisions and price changes in the audit log (`AuditService`); unexpected errors logged with details on the server only | AuditServiceTest |
| A10 | Server-side request forgery | The server never fetches a URL a user sends; photo URLs must be OUR Cloudinary uploads (`isOurPhoto`) | ProductImagesTest |

## 3. Left for later (written down, not forgotten)

- **Content-Security-Policy** header for the website: set on the hosting of the built frontend in Phase 12 (the API
  only returns JSON, where CSP does nothing).
- **HTTPS + HSTS**: Phase 12 hosting (Spring already sends HSTS on HTTPS requests).
- Dependabot / automatic update PRs: needs a GitHub setting MADHAVAN switches on (Settings -> Code security).

## 4. Explain-it-back

"What did the security review find, and how was it fixed?" - Short answer: no leaked secrets; 18 known holes in the
libraries Spring Boot 4.1.1 brought (the web server Tomcat and the JSON library Jackson) -> one version bump to 4.1.2;
2 in the frontend's dev tools -> `npm audit fix`; and a CI job so it is checked on every push from now on.
