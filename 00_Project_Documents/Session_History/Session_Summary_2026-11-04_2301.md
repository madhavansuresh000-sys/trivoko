# Session Summary - 4 Nov 2026

## Done
- Branch phase-12-launch-part2 (pushed, CI green, NOT merged): one-click demo logins (DEMO_ONE_CLICK, off by default, D13 draft), uptime.yml, launch README, Final Guide + viva Q&A, resume/LinkedIn/video drafts.
- Branch phase-12-after-launch-extras (stacked on part2, pushed, NOT merged): recently viewed, compare page, PWA (installable), product Q&A (V14), DemoGuard + nightly demo restock (V13). README/decisions D13-D14 updated.
- CI backend failure found from the GitHub log: CatalogSeedTest saw 0 price-history rows because LowestPriceTest and PriceAlertTest deleted ALL price_history on cleanup (test-order bug). Fixed in b4ad1e7 with support/PriceHistorySnapshot (save + restore); local verify 413 green, both orders checked. CI result on b4ad1e7 not yet seen at session end.

## Next (in order)
1. Check CI on b4ad1e7 is green.
2. MADHAVAN: Render trivoko-api -> Environment -> add DB_PASSWORD (reset Aiven password first) + DEMO_PASSWORD -> Save -> Manual Deploy. Watch /api/health = UP.
3. Vercel: he signs up; import repo, root frontend; fix FRONTEND_URL/CORS if the URL differs.
4. Merge part2, then extras (he clicks Merge). uptime.yml only after Vercel is live.
5. Phone live test, Hosting_Values file, D13 decision, video, resume/LinkedIn, tag v1.0, Brevo emails.
- Change the weak DEMO_PASSWORD before sharing the link. Delete Aiven pg-228c49d2 before 2 Dec 2026.
- REMIND: MADHAVAN promised a NAME - ask for it.
- Next Flyway migration: V15.
