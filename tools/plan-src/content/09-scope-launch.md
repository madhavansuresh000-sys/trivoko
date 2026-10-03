# 9. Scope, risks and launch

## 9.1 Not included (on purpose)

| Left out | Why | What we do instead |
| Real courier API (Delhivery, Shiprocket) | Needs a business account | Seller clicks Shipped/Delivered; tracking timeline is simulated |
| Real bank payouts to sellers | Needs Stripe Connect + KYC | Earnings ledger shows what each seller would receive |
| Cash on delivery, UPI, EMI | More payment flows, little extra learning | Stripe test card only |
| Seller-created coupons | Doubles the coupon rules | Admin coupons only |
| Mobile app, multiple languages | Big extra work | Mobile-friendly website |
| Full GST invoicing | Tax rules are complex | Simple invoice PDF with price, discount, shipping, total |
| Chat between buyer and seller | Real-time features are a separate project | Return reason + seller reply text |

## 9.2 Risks and what we do about them

| Risk | Plan |
| Project is too big and never finishes | Core = Phases 0-8 first; every phase ends with a working, pushed app; 9-10 can shrink |
| Free hosting is slow to wake up | Hosting chosen at Phase 12; uptime pinger for free hosting, or a paid plan only during job-hunting months |
| Hibernate Search needs disk on the host | Index can be rebuilt on start-up (120-500 products takes seconds); fallback = MySQL FULLTEXT |
| Stripe test webhooks are hard locally | EventHub "verify on return" fallback; Stripe CLI in Phase 4 |
| Cloudinary or email free limits | Small sample images; emails only for important steps; Mailpit locally |
| Madhavan cannot explain a part in an interview | The 3 core parts are typed by him; every phase ends with a short "explain it back" check; viva Q&A in the final guide |
| Spring Boot 4 / Java 25 library gaps | Same versions as EventHub, which already work; check each new library (Hibernate Search, Playwright, k6) in a small spike first |

## 9.3 Launch and the portfolio outcome

![What you show a recruiter](outcome.png)

**Hosting choices at Phase 12** (the code is the same for all of them):

| Choice | Cost | First load | Notes |
| Free: Render + Aiven MySQL + Vercel | ₹0 | 30-60 s when asleep | Uptime pinger keeps it warm during job hunting |
| Small paid: Railway or a VPS | about ₹400-600 per month | 1-2 s | Can be paid only for the months of applying |

**Launch package:**

- Live link with **demo logins** for customer, seller and admin on the login page.
- GitHub README: what it is, GIFs of the demo story, architecture diagram, ER diagram, k6 graph, how to run locally, test coverage badge.
- **3-minute demo video** (English; Tamil version optional, like EventHub).
- Resume bullet and LinkedIn post.
- Final project guide + viva Q&A document (like the EventHub 105-page guide).
- Git tag **v1.0**.

## 9.4 Next steps after this plan

1. Madhavan reviews this master plan and asks for changes.
2. Claude writes the **TriVoKo Phase Plan** (step-by-step checklist for every phase, with "done when" checks) and the Phase folders.
3. Phase 0 starts: GitHub repo `trivoko`, skeleton apps, Docker, CI, logo and sketches.
