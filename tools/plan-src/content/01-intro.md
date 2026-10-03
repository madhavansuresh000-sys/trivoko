# 1. What TriVoKo is and why we are building it

**TriVoKo** is an online marketplace, like a small Flipkart. Many **sellers** open shops on TriVoKo and list their products. **Customers** search, put items from different sellers into one cart, pay once, and track every package. The **admin** team keeps the marketplace safe and running.

>> Think of Flipkart or Amazon. When you buy a phone from one shop and shoes from another shop in one order, you pay once, but two different shops pack and ship two different boxes. TriVoKo does exactly this.

## 1.1 The goal: a job portfolio project

TriVoKo is Madhavan's **main portfolio project**. It will be shown to recruiters and interviewers. That decides three things:

- It must be **live online**, so an interviewer can click a link and use it.
- It must look **polished** on laptop and phone.
- It needs **standout features** that are easy to demo and easy to explain in an interview.

## 1.2 Decisions taken (planning session, 1 October 2026)

| Question | Decision |
| Why are we building it? | **Job portfolio** - live, polished, explainable |
| What kind of shop? | **Marketplace** - many sellers, one cart, orders split per seller |
| What is sold? | **Everything** - electronics, fashion, home, books (general, like Flipkart) |
| Standout features | **Flash sale**, **Returns & refunds**, **Price-drop alerts**, **Smart search** |
| Hosting | **Decide at launch** - the app is built portable (Docker + environment variables) so free or paid hosting both work |
| How we build | **Claude builds each step and explains it**; for the 3 core pieces (flash-sale stock, order split, return state machine) **Madhavan types the key logic** with guidance |
| Backend structure | **Modular monolith** - one Spring Boot app with walled modules; Redis only if the load test proves it is needed |

## 1.3 What "success" means

TriVoKo is finished when all of these are true:

1. An interviewer opens the **live link**, logs in with a demo account and **buys from two sellers in one cart** with a Stripe test card.
2. They watch a **flash sale** start with a countdown, and the stock never goes below zero - proven by a **load test with 5,000 virtual users**.
3. They **return an item** and see the refund appear.
4. GitHub shows **CI green**, about **80% test coverage**, and a README with diagrams, GIFs and the load-test graph.
5. Madhavan can **explain every standout feature** in his own words: how it works, why it was built that way, and what would break without it.

## 1.4 What we reuse from EventHub

EventHub was the practice. Most building blocks of TriVoKo already exist there, so we copy the pattern and make it bigger.

| EventHub feature | Becomes in TriVoKo |
| Seat hold for 10 minutes + `@Version` | Stock hold at checkout + flash-sale stock |
| 100-thread "no overbooking" test | 1,000-thread flash-sale test + k6 load test |
| Event approval state machine (DRAFT → PENDING → PUBLISHED) | Seller approval, product approval, package states, return states |
| Clubs with organizers | Sellers with their own shop dashboard |
| Stripe gateway switch + webhook + idempotency | Same, plus **partial refunds** for returns |
| Waitlist offer + notification bell + Mailpit emails | Price-drop alerts + order emails |
| JWT cookie + CSRF + roles | Same, with roles CUSTOMER / SELLER / ADMIN |
| Analytics with Recharts + `@Cacheable` | Seller sales charts + admin reports |
| Testcontainers, JaCoCo, Vitest, CI | Same, plus Playwright end-to-end tests |
| Docker image + Render/Vercel files | Same launch path, decided at Phase 12 |

> New things to learn in TriVoKo: product variants (size/colour), image upload to Cloudinary, splitting one order into packages, a money ledger, Stripe partial refunds, Hibernate Search (Lucene), k6 load testing and Playwright.
