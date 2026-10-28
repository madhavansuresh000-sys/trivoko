# TriVoKo - decisions log

Short records of technical decisions: what we chose, why, and what we checked.
Newest at the bottom. (Bigger decisions are in the Master Plan.)

---

## D1 - Library check on Spring Boot 4.1 (Phase 0, 3 Oct 2026)

**Question:** Do the libraries planned for later phases work with Spring Boot 4.1.1 / Hibernate ORM 7.4 / Java 25,
before we depend on them?

**How we checked:** a throwaway project (`tools/spikes/library-check`) with one test per library,
run against a real MySQL 8.4 (Testcontainers). Run it with
`../../../backend/mvnw -f pom.xml test` from that folder.

| Library | Version | Used in | Result |
|---|---|---|---|
| Hibernate Search (mapper-orm + Lucene backend) | 8.4.0.Final | Phase 10 smart search | ✅ Starts with Boot's Hibernate 7.4.5. Fuzzy search "iphnoe" → *Apple iPhone 15 silicone case* ranked first. Brand facets with counts work. |
| stripe-java | 34.0.0 | Phase 4 payments | ✅ Builds a Checkout Session request (API version 2026-09-30). EventHub used 29.2.0 - code will need small updates. |
| cloudinary-http5 | 2.5.0 | Phase 1 photo upload | ✅ Signs an upload. Note: `apiSignRequest(params, secret, 2)` needs a mutable `Map<String,Object>` and the signature version. |
| OpenPDF | 3.0.5 | Phase 4 invoices | ✅ Writes a PDF. **Package renamed:** `org.openpdf.text` (EventHub's 2.2.2 used `com.lowagie.text`). |
| ArchUnit (junit5) | 1.5.1 | Phase 1 module rule test | ✅ Reads our classes. |

**Things learned for later phases:**

- Phase 10: fuzzy distance 2 also matches short words ("iphnoe" ↔ "phone"). Tune with a prefix length,
  distance 1 for short words, or boost exact matches - and test the ranking, not only the hit list.
- Phase 10: a real analyzer (lower-case, ASCII folding, edge n-grams for autocomplete) must be defined
  in a `LuceneAnalysisConfigurer`; the name `english` is not built in.
- Phase 10: set `hibernate.search.backend.lucene_version` to stop a start-up warning.

**Decision:** keep the Master Plan stack. No library needs replacing.

---

## D2 - Separate ports from EventHub (Phase 0, 3 Oct 2026)

TriVoKo's MySQL runs on **3307** and Mailpit on **1026 / 8026**, so EventHub (3306 / 1025 / 8025) and TriVoKo
can run at the same time - useful when copying a working pattern from EventHub.
The backend (8080) and Vite (5173) keep the normal ports: only one of the two apps is developed at a time.

---

## D3 - Roles come from the database on every request (Phase 2, 4 Oct 2026)

The login cookie holds a JWT with the user id **and** the roles (the frontend uses the roles to show menus).
But the server does **not** trust the roles inside the token. On every request `JwtCookieFilter` checks the
signature, takes the user id, and loads the user again: still enabled? which roles now?

Why: a token lives 8 hours. If the server trusted its roles, an admin who **blocks** a shop or **approves**
a new seller would have to wait up to 8 hours for it to take effect (or the person would have to log out and in).
Reading the database makes every change instant - like a college ID card: the card shows your name,
but the gate still checks the register to see if you are still allowed in.

Cost: one small primary-key lookup per logged-in request. Fine for this project; if the k6 test in Phase 7
shows it matters, cache the user for a few seconds (Redis or Caffeine) - not before.

---

## D4 - Guest cart in the browser, prices only from the server (Phase 3, 5 Oct 2026)

A visitor can fill a cart before logging in. Their cart lives in the browser (localStorage key `trivoko.cart`)
as `[{ variantId, quantity }]` - **only what and how many, never a price**. To show it, the browser sends the
lines to `POST /api/cart/preview`, and the server prices them with the same `CartPricing` class it uses for a
saved cart. After login, `POST /api/cart/merge` adds the lines to the user's saved cart (the same variant =
quantities added, capped at 10 and the stock) and the browser copy is cleared.

Why: anyone can edit localStorage or a request. If the browser kept prices, a customer could make a ₹12,999
phone cost ₹1. With only ids and quantities in the browser, the worst they can do is change a quantity - which
the server caps anyway. In Phase 4 checkout, the server calculates every amount again from these same lines.
Like a shopping trolley: you push it around, but the cashier scans every item's price at the counter.

Other choices (can be changed later): a cart line keeps a sold-out or blocked item (shown, not counted) so the
customer sees what happened; delivery is per seller package (free from ₹499, else ₹40) and is only shown in
Phase 3 - Phase 4 charges it.

---

## D5 - The order split: one payment, one package per seller, coupon share per item (Phase 4, 5 Oct 2026)

One checkout = one payment = one order, split by `OrderSplitter` (pure Java, unit-tested) into one package per
seller. Delivery is per package (free from Rs 499 of that package's items before the coupon, else Rs 40).
The coupon discount is shared across ITEMS in proportion to their price, rounded to paise, with the last item
taking the remainder - so the shares always add up exactly, and each item stores its own share.

Why per item: in Phase 8 a returned item is refunded exactly `line_total - discount_share`; nothing has to be
recalculated from a coupon that may have changed or expired since. Like a restaurant bill split by dish.

Other choices made with it:

- **Stock is held at "Pay"**, not at "add to cart": one atomic `UPDATE ... SET stock = stock - :qty WHERE stock >= :qty`
  per item, in variant-id order (no deadlocks), all in the order's transaction - one item missing undoes every hold.
  Proven by `StockConcurrencyTest` (100 customers, 10 units, exactly 10 orders).
- **One unpaid order per customer**: a second "Pay" (another tab) returns the same payment page instead of
  holding the stock twice. After 10 minutes the order EXPIRES and the stock goes back.
- **A late payment** re-takes all the stock if it is still there, otherwise it is refunded automatically.
- **Stripe sees one line** (the grand total), because coupon shares and delivery are already inside it.
- **Emails go out after the commit, on a background thread** (found in the browser check: a slow mail server
  made "Pay" time out although the order was paid), with retries every minute.

---

## D6 - Packages as a state machine, money as ledger lines (Phase 5, 5 Oct 2026)

**State machine.** `PackageStatus.allowedNext()` is the ONE place that says which move is legal; `OrderPackage.moveTo`
checks it, and every move also writes a `package_events` row - so the customer's tracking timeline is simply the list
of moves. An illegal move (ship before pack, cancel after shipping) is 409 with a plain message.

**Two people, one package.** The packages row has `@Version`. Cancel and ship at the same moment: the first commit
wins, the second gets 409. The cancel saves (flushes) the status FIRST and only then gives stock and money back, so a
lost race never refunds anything (`cancelVsShipRace`).

**Ledger, not a balance column.** A delivered package adds a `SALE` line of +90% of what the customer paid for it;
the balance is the SUM of the lines. A stored balance can drift and cannot be explained; lines can always be added up
again and each rupee points to an order. A UNIQUE (package, kind) key makes "Mark delivered" twice harmless.

**Partial refunds.** Cancelling one package refunds that package's total (its coupon share already subtracted)
through `PaymentGateway.refundPart` with the idempotency key `refund-package-<id>`; the payment remembers how much
was refunded and never gives back more than was paid.

Found on the way: `@EnableCaching` was missing (so `@Cacheable` silently did nothing), and the shared `Modal`
re-focused itself after every keystroke when its parent re-rendered.

## D7 - Blocking: one reason, one place, takes effect at once (Phase 6, 13 Oct 2026)

**Block is a state, not a delete.** A blocked shop keeps `status = BLOCKED` plus `block_reason` (V8); an unblocked shop
goes straight back to `APPROVED` (no second review - the admin already trusted it once). Nothing is removed, so
orders, ledger lines and the audit log still point at real rows.

**Products vanish from one place.** Every shop query goes through `ProductSpecifications.visibleInShop()` (ACTIVE
product of an APPROVED shop). Blocking a shop therefore hides its products on the listing, product page and brands
without touching a single product row - and unblocking brings them back.

**Blocked users stop at once.** `JwtCookieFilter` loads the user through `findActiveById` on every request (D3), so
`users.enabled = false` kills the cookie they already hold on their next click - no token blacklist needed. An
ADMIN account cannot be blocked from the panel (409), so the admin can never lock everyone out.

**Reports are `GROUP BY`, not loops.** `ReportRepository` lets MySQL add up the day / product / shop totals and send
back only those rows. A "sale" is a package that is not cancelled or expired, the same rule as the seller chart.

**Likes helper.** Search boxes go through `Likes.contains(text)`, which escapes `%` and `_` so typing `100%` searches
for that text and cannot turn into a wildcard.

## D8 - Flash sale: one atomic UPDATE in MySQL; Redis not added yet (Phase 7, 22 Oct 2026)

**The claim.** `UPDATE ... SET sold = sold + 1 WHERE id = ? AND sold < quantity AND <sale is live>` is a check and a
change in one locked step. The wrong way - read `sold`, compare in Java, write `sold + 1` - lets two requests read 99
at the same moment and both write 100: an oversell. Three walls stop a mistake: the WHERE clause, `UNIQUE (item, user)`
(one per person) and `CHECK (sold <= quantity)` (the database itself refuses).

**Fast "no".** Once sold out, the other thousands are answered by a plain, non-locking read of `quantity - sold`, so
they do not queue on the locked row. It can be out of date for a moment after a give-back; the UPDATE is still the only
thing that hands out a unit.

**Measured (k6, 15 Oct 2026, one laptop, app + MySQL + k6 all in Docker):** 100 winners, 0 oversell, 0 errors in both
shapes. p95 = 1,082 ms when all 5,000 press at the same instant (median 681 ms), 517 ms at 1,000 a second (median 2.4 ms).

**Redis?** The plan said: if p95 is above 500 ms, try Redis `DECR` and measure again. The p95 is above, but the cost
is everyone waiting in line for one hot row on a single laptop that also runs k6 and MySQL - the answer was always
right. **Decision: keep MySQL only for now** (one fewer service to host, no second copy of the stock that could drift).
**Still to do together:** run the `ceiling` shape (5,000 at `/api/health`) to see how much of the 1 s is the laptop
itself; only if the claim is clearly slower than that ceiling, build the Redis `DECR` version and record both graphs.

## D9 - Returns: a state machine, money fixed at asking, refund in its own transaction (Phase 8, 22 Oct 2026)

**One door.** Every return move goes through `ReturnService.moveTo`, which asks `ReturnStatus.allowedNext()` and then
`saveAndFlush`es with `@Version`. "Escalate once" needs no flag: ESCALATED never leads back to REJECTED.

**Whole line, amount fixed when asked.** A return covers one order line (UNIQUE `order_item_id`) and its refund is
`line_total - discount_share`, copied onto the return when the customer asks - later price or coupon changes cannot
move it. Returning "1 of 3 identical items" and refunding delivery were left out on purpose (more maths, little to
learn); both are easy to add on top.

**The refund is its own transaction.** `ReturnPickup` is not transactional: PICKED_UP is saved first (the item really
is with the seller), then `ReturnService.refund` does REFUNDED + Stripe + stock + ledger + bell all-or-nothing. A
Stripe error thrown through a joined transaction would mark the whole thing rollback-only, so the failure is caught
outside and REFUND_FAILED is written in a fresh transaction. Retries reuse the key `refund-return-<id>`.

**Ledger key change (V10).** V7's `UNIQUE (package_id, kind)` allowed one REFUND per package; it is now
`UNIQUE (package_id, kind, once_key)` with `once_key = COALESCE(return_id, 0)` - MySQL treats NULLs in a unique key
as all different, so SALE lines (no return) use 0 and still cannot repeat.

**Snapshot (V11).** A return copies order number, product, variant, quantity and shop from the bill, like
`order_items` itself, so the seller and admin lists need no lookups per row.

## D10 - Price-drop alerts: mark on change, decide in a job (Phase 9, 27 Oct 2026)

**Why not alert at once?** A seller often saves a price several times in a row (typo, second thought). Alerting inside
`changePrice` would send one message per save. Instead the `PriceChanged` event only sets `price_changed = TRUE` on that
product's wishlist rows, after the commit (`@TransactionalEventListener(AFTER_COMMIT)` + `REQUIRES_NEW`, so a
rolled-back edit marks nothing). `PriceAlertJob` (every 10 minutes) reads only the marked rows and the price as it is
NOW - five edits become one alert with the final price.

**Never spammy.** The reference is the last price we told the customer about (`last_alerted_price`), or the price when
they saved it. An alert needs a drop of at least 1% below it; going up and back down to the same price is not a new
drop. Only live, in-stock products alert. Known small gap: a price change that commits in the milliseconds while the
job is running can have its mark cleared by the job; it is picked up by the next change. Acceptable for a wishlist.

**Saved per product, not per variant.** The tracked number is the product's "from" price - the one on the card - so
the wishlist and the alert talk about the same price the customer saw.

**Verified buyer = a DELIVERED package**, asked through `OrderService.hasDelivered` (module rule: no reading
`order_items` from the review module). Returned items still count. One review per customer per product (UNIQUE
key); writing again edits it. The average and count are stored on `products` so cards need no extra query.

## D11 - Smart search: Lucene inside the app; it finds and ranks, MySQL filters and counts (Phase 10, 29 Oct 2026) - DRAFT

**Status: draft by Claude - the Phase Plan says "decide together"; MADHAVAN confirms or changes it.**

**Split of work.** Lucene (Hibernate Search 8.4, D1) only answers "which products match these words, best first"
(top 500 ids). MySQL then keeps the live, in-stock, category / brand / price matches and the page and counts are made
from those rows. Reason: stock changes through bulk `UPDATE ... stock = stock - ?` (checkout, flash sale) and a shop
is blocked on the `sellers` row - Hibernate Search never sees either, so filters kept in the index would go stale.
Text only changes through normal saves, which Hibernate Search re-indexes at once (`synchronization.strategy: sync`).
Limit: only the best 500 matches are filtered - far above TriVoKo's 120 products.

**Keep Lucene or fall back to MySQL FULLTEXT for hosting?** Suggestion: **keep Lucene.** It runs inside the same Java
process (no search server to pay for), the index is rebuilt from MySQL on every start (so a hosting platform's
temporary disk is fine), and typo tolerance is the whole point of standout #4 - MySQL FULLTEXT cannot forgive
"iphnoe". If the app ever runs as several copies, each copy keeps its own index (rebuilt on start; edits made on one
copy reach the others at their next start) - acceptable for a portfolio shop; a shared Elasticsearch would be the
next step, not FULLTEXT.

**Typo rules.** By word length (exact up to 4 letters, 1 typo from 5, 2 from 8), first letter fixed, exact matches
boosted - the D1 spike showed plain "distance 2" lets "iphnoe" match "phone". Tests check the ranking.

**Also bought** is plain SQL over paid orders in the order module, cached 1 hour (its own Caffeine cache; the rest
keep 60 s). Only the ranked ids are cached; cards are built fresh, so prices are never an hour old.

