# 1. TriVoKo in two minutes

This guide is written for **you, MADHAVAN**, to read before an interview or a viva. Chapters 1-6 explain the finished project in simple words. Chapter 7 is a list of questions an interviewer may ask, each with a short answer you can say in your own words.

>> When a friend asks "what does your shop sell?", you do not read the stock register to them. You say one sentence, then two or three things that make your shop special. This chapter is that sentence and those things.

## 1.1 The 20-second answer

"TriVoKo is a multi-seller marketplace - a small Flipkart - that I built from an empty folder to a live website with React and Spring Boot. A customer fills one cart from many shops and pays once; the order is split into one package per seller, and each seller packs and ships their own package. I added four harder features: a flash sale that never oversells, returns with exact partial refunds, price-drop alerts, and a typo-tolerant search."

## 1.2 The two-minute answer

Say it in this order - **problem, product, four standouts, quality, live**:

- **Problem.** A marketplace is harder than a single shop: one payment must be split between many sellers, and money, stock and each seller's earnings must always agree.
- **Product.** Three roles. Customers search, fill one cart, pay once and track one package per seller. Sellers - approved by the admin - list products with sizes and colours, ship, and see their earnings (90%; TriVoKo keeps 10%). The admin approves shops and products, runs flash sales and coupons, and reads reports.
- **Four standouts.**
  - **Flash sale:** 100 units, 5,000 people - exactly 100 winners. One atomic SQL `UPDATE ... WHERE sold < quantity`; proven with a k6 load test.
  - **Returns:** a state machine and a Stripe **partial** refund of exactly the returned item minus its share of the coupon.
  - **Price-drop alerts:** one alert per real drop, never spam - an event marks rows, a job sends the alert.
  - **Smart search:** "iphnoe" finds the iPhone case - Hibernate Search with Lucene, with counts for brands and prices.
- **Quality.** About 400 backend tests on a real MySQL (Testcontainers), 100+ React tests, a Playwright test that clicks through the whole story in a real browser, accessibility and security scans - all in GitHub Actions on every push.
- **Live.** It runs in Docker on Render, with MySQL on Aiven and the React app on Vercel - all on free plans - and a recruiter can log in with one click as a demo customer or seller.

## 1.3 The numbers to remember

@widths 3200 5826
| Number | What it means |
| 13 phases | Phase 0 (setup) to Phase 12 (launch), one Git branch and pull request per phase |
| 17 modules | `catalog`, `order`, `payment`, `flashsale`, `returns`, `search` ... in one Spring Boot app |
| 12 migrations | Flyway files V1-V12 build every table; Hibernate only checks them |
| 100 of 5,000 | flash-sale load test: 100 units, 5,000 users, exactly 100 winners, 0 errors, 0 oversell |
| 91.7% | line coverage of the service classes; the build fails below 80% |
| 92 | Lighthouse performance score of the home page on a phone (accessibility 100) |
| Rs 0 | hosting cost: Render + Aiven + Vercel free plans |

## 1.4 The part you are most proud of

Pick one and be ready to explain it deeply (chapters 3 and 7 help). A good choice is the **flash sale**, because it has a clear risk (overselling), a clear one-line fix (the atomic update), and a clear proof (the load test with exactly 100 winners). Another good choice is the **order split**, because you can draw it on a whiteboard in one minute.
