# 7. Viva and interview questions

Read the question, answer it **aloud in your own words**, then compare with the short answer. You do not need the exact words - you need the idea and one example from TriVoKo.

## 7.1 The project

### Q1. Tell me about your project.
Use the two-minute answer in chapter 1.2: problem, product, four standouts, quality, live.

### Q2. Why did you build a marketplace and not a simple shop?
A marketplace has harder, more interesting problems: one payment split across many sellers, each seller's earnings, and stock that many people buy at once. Those are the problems real companies have.

### Q3. What was the hardest part?
Keeping money exact. The coupon is shared across the items by price, each item stores its share, and a return refunds exactly that item minus its share. Money, stock and the seller ledger must always agree, and tests check each case.

### Q4. Which part did you write yourself?
The three key pieces of logic: the order split, the flash-sale claim and the return state machine. (Answer honestly about how you built and reviewed them.)

## 7.2 Java and Spring Boot

### Q5. What is a modular monolith?
One application, one deployment, but split into modules with clear borders. In TriVoKo a module may call another module's service, never its repository or tables. An ArchUnit test fails the build if the rule is broken.

### Q6. Why not microservices?
At this size they add cost without benefit: network calls, many deployments, distributed transactions. The module borders keep the option open to split later.

### Q7. What does @Transactional do?
It runs the method in one database transaction: all changes are saved together or none are. Example: a refund puts stock back, writes the minus line in the seller ledger and changes the return status in one transaction.

### Q8. What is the N+1 problem and how did you avoid it?
Loading 24 products and then making one extra query for each product's variants = 25 queries. TriVoKo sets `hibernate.default_batch_fetch_size: 50`, so the variants of 50 products come in one query. Ratings are stored on the product row, so cards need no extra query.

### Q9. Why Flyway and not ddl-auto=update?
Flyway gives a numbered, reviewed history of every schema change, the same on every machine. `ddl-auto=update` guesses changes, can never drop or rename safely, and leaves no history. TriVoKo uses Flyway to build and Hibernate only to `validate`.

### Q10. How do you handle errors in the API?
Services throw a few own exceptions (`ResourceNotFoundException`, `BusinessRuleException` ...). One `GlobalExceptionHandler` turns them into RFC 7807 `ProblemDetail` JSON with the right status (404, 409, 400). The React app reads `detail` and shows a friendly message.

### Q11. What is optimistic locking? Where did you use it?
A `@Version` column: an update succeeds only if the version is still the one you read. If two people change the same package at once, one wins and the other gets 409 - no silent overwrite. Used on packages and returns.

## 7.3 Database and concurrency

### Q12. How does the flash sale never oversell?
One atomic statement: `UPDATE flash_sale_items SET sold = sold + 1 WHERE id = ? AND sold < quantity AND <live>`. MySQL locks the row and checks and adds in one step. 1 row changed = won, 0 = sold out. Plus `UNIQUE(item, user)` and `CHECK (sold <= quantity)`.

### Q13. Why not read the stock in Java, check it, then save?
Two requests can both read "1 left" before either saves, and both buy. That is a race condition. The check must happen inside the same statement that changes the number.

### Q14. How did you prove it?
A Java test with many threads, and a k6 load test: 5,000 users at the same instant, 100 units, exactly 100 winners, 0 errors, and the database agreed afterwards.

### Q15. Why is money DECIMAL and not double?
`double` cannot store values like 0.1 exactly, so sums drift by tiny amounts. `DECIMAL` (and `BigDecimal` in Java) is exact - required for money.

### Q16. What indexes did you add and why?
Examples: `price_history (variant_id, changed_at)` for "lowest price in 30 days", a unique key on reviews (one per customer per product), and an index for the admin activity log. An index is like the index at the back of a book - find the page without reading every page.

## 7.4 Security

### Q17. Where is the login token stored and why?
In an HttpOnly, Secure, SameSite=Lax cookie. JavaScript cannot read it, so a script injected into the page cannot steal it. That is safer than localStorage.

### Q18. What is CSRF and how is it stopped?
Another website making the user's browser send a request to our site with the user's cookie. TriVoKo requires a token from the `XSRF-TOKEN` cookie in the `X-XSRF-TOKEN` header on every POST/PUT/DELETE - another site cannot read our cookie, so it cannot send the header.

### Q19. How do passwords stay safe?
BCrypt hashes (slow on purpose, with a salt). Wrong email and wrong password give the same message, and 5 wrong tries lock that login for 15 minutes.

### Q20. Why read the roles from the database on every request?
So a block or a role change works at once. If the roles were trusted from the token, a blocked user could keep using the site until the token expired (8 hours).

### Q21. How do you stop a seller from editing another seller's product?
Every seller URL checks ownership (`@sellerAccess`): another shop's product or package = 403. Tests check this for each kind of object.

### Q22. Where are the secrets?
Only in environment variables on the server and in a `.env` file on the laptop that is never committed. gitleaks scans every commit in CI.

## 7.5 Payments

### Q23. How does Stripe tell you the payment worked?
A webhook: Stripe calls `/api/payments/stripe/webhook`. The server checks the signature, then marks the order paid. If the same message arrives twice, the second changes nothing (idempotent). The return page also asks Stripe directly, in case the webhook is late.

### Q24. How do you make sure a refund never happens twice?
Each refund uses an idempotency key (`refund-return-<id>`) - Stripe returns the first result for a repeated key. And `@Version` stops a second click before any money moves.

### Q25. What happens if the customer never pays?
The stock is held for 10 minutes. A job marks the order EXPIRED and gives the stock back. A late payment re-takes the stock or is refunded.

## 7.6 React

### Q26. Why Redux Toolkit?
For state many pages share: the logged-in user, the cart badge, the wishlist hearts. Page-only state stays in the component with `useState`.

### Q27. How does the guest cart work?
It is stored in localStorage. After login the lines are sent to `/api/cart/merge` and joined with the saved cart; prices always come from the server.

### Q28. How did you make the site accessible?
Labels on every field, keyboard support (the search box is an ARIA combobox), colour contrast fixed until axe reported no serious problems, in light and dark mode.

## 7.7 Testing and DevOps

### Q29. What is Testcontainers?
A library that starts real services (here MySQL 8.4) in Docker for the tests, so tests run against the same database as production.

### Q30. What is in your CI pipeline?
Backend build and tests with coverage gate, frontend lint/tests/build, Playwright end-to-end with accessibility, and a security scan - on every push. Merge only when all are green.

### Q31. What does the Dockerfile do?
Two stages: build the JAR with Maven, then copy only the JAR into a small Java runtime image that runs as a non-root user. Settings come from environment variables, so the same image runs anywhere.

### Q32. How did you deploy it for free?
React on Vercel, Spring Boot in Docker on Render, MySQL on Aiven. Vercel passes `/api` calls to Render so everything is one address. An uptime check keeps the free server awake.

### Q33. Your app sits behind proxies. What problem does that create?
The app would see the proxy's address for every visitor, so one person's wrong passwords would lock everyone. `forward-headers-strategy: framework` reads the real address from `X-Forwarded-For`.

## 7.8 Smart search and alerts

### Q34. How does the search handle typos?
Lucene fuzzy matching: 1 typo allowed from 5 letters, 2 from 8, and the first letter must be right - so "iphnoe" finds iPhone but does not drag in "phone" for every word.

### Q35. Why both Lucene and MySQL in one search?
Lucene is best at text and ranking; MySQL has the facts that change every minute (stock, price, blocked shops). Lucene returns ranked ids, MySQL filters and counts them.

### Q36. How do price alerts avoid spam?
The price-change event only marks wishlist rows. A job every 10 minutes sends one alert if the latest price is at least 1% below the last price the customer was told. Five edits in a minute = one alert.

## 7.9 Reflection

### Q37. What would you change if you started again?
Answer honestly. Ideas: decide the hosting earlier, add Redis only if measurements demand it (as done), write the end-to-end test from Phase 3 instead of Phase 11.

### Q38. What did you learn?
Pick two: thinking about races and money exactly; testing against a real database; production settings (memory, proxies, secrets); writing decisions down (`docs/decisions.md`).

### Q39. How would it scale to 100 times more users?
Several copies of the app behind a load balancer; then move what lives inside one copy - the search index and the rate limits - to shared services (Elasticsearch, Redis); a read replica for MySQL; photos on a CDN.

### Q40. Which part are you most proud of, and why?
Your own answer - see chapter 1.4.
