# TriVoKo - Session Summary 1 Nov 2026 21:35

## Decisions (MADHAVAN, "Yes, both")
- **D11 decided:** keep Lucene smart search on the live site (typo-tolerant, no extra server).
- **D12 hosting:** FREE - Vercel (React shop) + Render free (Spring Boot in Docker, Singapore, `trivoko-api`) + Aiven free (MySQL).

## Built today - Phase 12 step 2 (production settings), branch `phase-12-launch` (pushed; NOT merged, no PR yet)
Commits 289c9a8, 636c599, 90c3940.
- `backend/Dockerfile`: multi-stage (builds the JAR itself), non-root user, 512 MB tuning, index in /tmp, Lucene native access.
- `render.yaml` (repo root): Render Blueprint; secrets `sync: false` (typed in the dashboard).
- `frontend/vercel.json`: `/api/*` rewrite to Render (one address -> cookies first-party) + Content-Security-Policy and security headers.
- `application.yml` prod: forward-headers (real visitor address), 40 threads, 5 DB connections.
- Dark-mode script moved from `index.html` to `public/theme.js` (CSP forbids inline scripts); `npm run preview` sends the live headers.
- `docs/decisions.md`: D11 decided, D12 added.

## Checked
- Prod image on an empty MySQL with a 512 MB limit: 12 migrations, started 7.8 s, health UP, secure cookie, HSTS,
  Swagger 401, login lock per real address (IP A locked, IP B OK), "iphnoe" search, full purchase via test payment
  (no CSP blocks), memory 426/512 MB.
- Backend `mvnw verify`: 388 tests green + coverage gate; Vitest 101 green; frontend build OK.

## Next time
1. Check CI for `phase-12-launch` on GitHub.
2. Phase 12 step 3 - MADHAVAN creates free accounts and signs in himself: Aiven (MySQL), Render (Blueprint from repo),
   Vercel (root `frontend`). Claude guides each screen. Then FRONTEND_URL / CORS / DEMO_PASSWORD / Cloudinary keys.
3. Steps 4-10: demo data live, uptime check, final README, demo video, resume + LinkedIn, guide + viva Q&A, tag v1.0.
4. Older open items: explain-it-back Phases 9-11, Stripe test-dashboard check, k6 ceiling run.
