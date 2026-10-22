# Session Summary - 22 October 2026 (09:20)

**Project:** TriVoKo - `D:\\Java_FullStack_Journey\\03_TriVoKo_Main_Project` - https://github.com/madhavansuresh000-sys/trivoko

## Where we are

```
Phase 0-6  done, merged into main on GitHub (PR #4, #5, #6 - CI green each time)
Phase 7    Flash sale - built, committed be832b8 on branch phase-7-flash-sale, NOT pushed yet
Phase 8-12 next: Phase 8 Returns & refunds
```

## Phase 7 - Flash sale

- Claim = ONE atomic `UPDATE ... SET sold = sold + 1 WHERE sold < quantity AND sale is live`;
  `UNIQUE (item, user)` = one per person; `CHECK (sold <= quantity)` = the database refuses an oversell.
- 10-minute hold, give-back of unpaid claims, unsold units back to stock at sale end, 429 rate limit.
- k6 (5,000 users, 100 units): **100 winners, 0 oversell, 0 errors**. p95: burst 1,082 ms, spread 517 ms.
- Decision D8: MySQL only for now (no Redis). Still to do together: the `ceiling` run, then decide on Redis.
- Frontend: lint clean, 70 tests, build green. Backend tests: NOT run in this session (no Java 25 here) - CI will run them.

## To do (in this order)

| # | Task | Who |
|---|---|---|
| 1 | `git push origin phase-7-flash-sale` (PowerShell, in the project folder) | You |
| 2 | Open PR #7, wait for CI green, merge | Claude + You (merge click) |
| 3 | Delete the empty stray file `dcc` in the project folder (created 15 Oct, not in git) | You |
| 4 | Explain-it-back Phase 7: read-then-write vs one UPDATE; one per person; did we need Redis? | Together |
| 5 | Phase 8 - Returns & refunds (you type the return state machine) | Next phase |
