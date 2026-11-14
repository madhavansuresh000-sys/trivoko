# How to use this sheet

Each phase ends with "explain it back" questions. For every question there is a **short answer** (say it in about 30
seconds, in your own words), a **picture** to draw on paper, and the **interview follow-up** you may get next.
Practise like a viva: cover the answer, say it aloud, then check.

>> Like the RTO driving test: the inspector does not ask you to recite the rules - he watches you drive. An interviewer is the same: they check that you understand WHY, not that you remember words. Always say the why.

# Phase 9 - Reviews, wishlist, price-drop alerts

## Q1. Why do alerts come from a job every 10 minutes instead of at once?

**Short answer:** A seller often saves a price several times in a row - a typo, then a correction. If we sent an
alert inside the price change, Ravi would get one message for every save. So the price change only puts a mark
("this price changed") on the wishlist rows. A job runs every 10 minutes, reads only the marked rows and the price
as it is NOW, and sends at most one alert per real drop of at least 1%.

```
 Seller saves:  14,999 -> 13,999 -> 13,499   (3 saves in 2 minutes)
                  |         |         |
                  mark      mark      mark        (no messages yet)
                                          \
 Every 10 min:  PriceAlertJob  ->  "Volta V12 5G is now 13,499"   (ONE alert)
```

**Extra detail:** the mark is set only after the database commit (`@TransactionalEventListener(AFTER_COMMIT)`), so a
price change that fails and rolls back marks nothing. The reference is the last price we told Ravi about, so going
up and back down to the same price is not a new drop.

**Follow-up you may get:** "Why not a message queue like Kafka?" -> One app, a few hundred products; a database
flag plus a scheduled job gives the same "collect, then decide" behaviour with nothing extra to run or pay for.

## Q2. How do we know a reviewer really bought the product?

**Short answer:** Before saving a review, the review module asks the order module: "Did this customer get a package
with this product DELIVERED?" (`OrderService.hasDelivered`). Only then can they review. One review per customer per
product - a UNIQUE key in MySQL; writing again edits the old one.

```
 Ravi -> POST review -> ReviewService --asks--> OrderService.hasDelivered(ravi, product)
                                                    | yes -> save review, update average stars
                                                    | no  -> 403 "Only buyers can review"
```

**Why ask the service and not read the table?** Our module rule: a module may call another module's service, never
its tables. ArchUnit checks this on every build, so modules stay separate like shops in a mall.

**Follow-up:** "Can a returned item be reviewed?" -> Yes, it was delivered; an unhappy buyer's review is honest
feedback.

# Phase 10 - Smart search

## Q3. What is an index, and why is search not a SQL LIKE query?

**Short answer:** An index is like the index at the back of a textbook: for every word, the list of pages where it
appears. Lucene builds one for product names, brands and descriptions. To find "wireless earbuds" it looks up two
words and gets the products at once, already ranked by how well they match.

`LIKE '%earbuds%'` must read every row one by one (the `%` at the start stops MySQL from using its normal index),
it cannot rank results, and it cannot forgive spelling mistakes.

```
 LIKE:    row1? row2? row3? ... row120?            (reads everything, no ranking, no typos)
 Lucene:  "earbuds" -> [17, 42, 88]                 (one lookup, best match first)
```

**How TriVoKo splits the work:** Lucene finds and ranks (top 500 ids); MySQL then filters live, in-stock products by
category, brand and price and makes the counts. Stock changes too often to keep it in the index.

## Q4. How does fuzzy matching find "iphnoe"?

**Short answer:** Lucene allows a small number of letter changes (edit distance). "iphnoe" -> "iphone" is one swap,
so it matches. Our rules depend on word length: up to 4 letters must be exact, 5-7 letters allow 1 change, 8+ allow 2.
The first letter must match, and exact matches get a higher score so "iphone" typed correctly still ranks first.

```
 i p h n o e
 i p h o n e     <- swap "n" and "o" = 1 change -> allowed for a 6-letter word
```

**Why the first-letter rule?** Our spike showed that a plain "2 changes" rule let "iphnoe" also match "phone" -
wrong products. Fixing the first letter removed that noise. Tests check the ranking.

# Phase 11 - Testing and polish

## Q5. What is the difference between a unit, an integration and an end-to-end test?

**Short answer:**

- **Unit test** - one small piece alone, no database. Example: `OrderSplitter` splits a cart into one package per
  seller; `CartPricing` adds up the money. Fast (milliseconds).
- **Integration test** - several real pieces together: Spring + a real MySQL started by Testcontainers. Example:
  checkout really saves an order and reduces stock; a seller cannot edit another shop's product (403).
- **End-to-end test** - the whole app in a real browser, like a user. Playwright opens the shop, a customer buys from
  two sellers with one payment, sellers ship, it is delivered, returned and refunded.

```
          /\        E2E: few, slow, closest to the real user   (Playwright)
         /  \
        /----\      Integration: many, real DB                (most of the 413 JUnit tests)
       /      \
      /--------\    Unit: fastest, the most pieces            (+ 117 Vitest on the frontend)
```

**Follow-up:** "What is coverage?" -> The share of code lines the tests run: 91.6% of lines. CI fails if the
services drop below 80%. Coverage shows what is NOT tested; it does not prove the tests are good.

## Q6. What did the security review find, and how was it fixed?

**Short answer:** No passwords or keys were ever committed (gitleaks checked the whole Git history). The scans found
**18 known vulnerabilities** in libraries - 4 critical in Tomcat - fixed by upgrading Spring Boot 4.1.1 -> 4.1.2, and
2 in frontend dev tools fixed with `npm audit fix`. A CI "Security scan" job now repeats these checks on every push.

We also walked through the OWASP Top 10: for example, broken access control (every seller URL checks the shop owns
the item), injection (only parameterised queries; React escapes text), and login attacks (login locks for 15 minutes
after repeated wrong passwords).

**Lesson worth telling:** the first scan said "no issues" - but it had been throttled and checked only 7 libraries.
Scanning the full dependency list (SBOM, 151 libraries) found the real problems. Never trust a green tick you do not
understand.

# Phase 12 - Launch

## Q7. Explain TriVoKo in 2 minutes, as in an interview

**A shape you can follow (say it in your own words):**

1. **What:** "TriVoKo is a multi-seller marketplace - like a small Flipkart. A customer fills one cart from many
   sellers and pays once. The order is split into one package per seller; each seller packs and ships their own."
2. **Stack:** "React 19 with Redux and Tailwind on the front; Java 25 and Spring Boot 4 on the back, as a modular
   monolith with 18 modules; MySQL with Flyway; Stripe test payments."
3. **Four standout features:** flash sale (5,000 users, exactly 100 winners, proven with k6), returns with exact
   partial refunds, price-drop alerts that never spam, and typo-tolerant search with Lucene.
4. **Quality:** "413 backend tests, 117 frontend tests, a Playwright browser story, accessibility checks and a
   security scan on every push - 91.6% line coverage."
5. **Close:** "I built it phase by phase over two months; the part I am most proud of is ..." (your answer to Q8).

## Q8. Which part are you most proud of, and why?

**Pick ONE and tell its story - problem, idea, proof.** Example with the flash sale:

- **Problem:** 5,000 people click "Claim" in the same second for 100 items. A normal "read stock, then write stock"
  sells more than 100 - two people read "1 left" at the same time.
- **Idea:** one atomic SQL update: `UPDATE ... SET sold = sold + 1 WHERE id = ? AND sold < quantity`. MySQL lets only one
  update change the row at a time, so the 101st update changes nothing -> "sold out".
- **Proof:** a k6 load test - 5,000 users at once: exactly 100 winners, 0 oversold, 0 errors; the database agreed.

```
 5,000 clicks --> UPDATE ... WHERE sold < quantity (100) --> 100 rows changed  = 100 winners
                                              --> 4,900 changed 0   = "Sold out"
```

Other good choices: the returns state machine (10 clicks on "Refund" = exactly 1 refund), or the order split.
