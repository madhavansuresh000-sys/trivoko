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
