# Phase 11 - Testing & polish

**Time:** 1 week  ·  **Group:** Always  ·  **Folder:** `Phase_11_Testing_and_Polish\`

**Goal:** Portfolio quality: the whole demo story runs as an automated browser test, the code is well covered, safe, fast and pleasant on every screen.

>> Before the grand opening, an inspector walks through the whole shop as a customer, checks every fire exit, and the cleaners polish every glass door.

## Steps

@widths 600 6926 1500
| # | Step | Who |
| 1 | Playwright end-to-end test of the demo story (buy from 2 sellers, seller ships, customer returns) in CI | Claude |
| 2 | JaCoCo coverage about 80% on services; coverage badge | Claude |
| 3 | Security review: ownership tests, OWASP checklist, dependency check, secret scan of the Git history | Claude |
| 4 | Accessibility (axe), keyboard use, phone + dark mode on every page | Claude |
| 5 | Speed: lazy-loaded pages, Cloudinary image sizes; Lighthouse ≥ 90 on Home | Claude |
| 6 | Vitest for the main components | Claude |
| 7 | README draft | Claude |

## Output of this phase

- E2E tests in CI
- Coverage report
- Security and accessibility fixes

## Done when

- CI green including the E2E test
- Lighthouse ≥ 90 on the home page

## Explain it back

- What is the difference between a unit, an integration and an end-to-end test?
- What did the security review find, and how was it fixed?

**Skills you practise:** Playwright, coverage, security review, accessibility, performance

**Where your work goes:** `e2e\`, test folders, `README.md`
