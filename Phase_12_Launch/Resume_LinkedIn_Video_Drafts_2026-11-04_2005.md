# TriVoKo - resume, LinkedIn and demo video (Phase 12 steps 7-8) - DRAFTS

Drafts for MADHAVAN to change into his own words ("Together" steps). Fill the live link once Vercel is up, and
change the demo password before sharing anything (see the session notes).

---

## 1. Resume

**Project title line**

> **TriVoKo - Multi-seller Marketplace** | React, Spring Boot 4, Java 25, MySQL, Stripe, Docker | [live link] · [GitHub]

**Bullet points** (pick 4-5; each = what + how + proof)

- Built a multi-seller e-commerce marketplace (customer, seller, admin) where one cart and one Stripe payment are
  split into a package per seller, with a 90/10 seller ledger - React 19 + Spring Boot 4 modular monolith (17 modules,
  module borders enforced by ArchUnit).
- Designed a flash sale that never oversells using a single atomic SQL update plus UNIQUE/CHECK constraints; a k6 load
  test with 5,000 concurrent users produced exactly 100 winners for 100 units with 0 errors.
- Implemented returns as a state machine with exact Stripe partial refunds (item minus its coupon share), idempotency
  keys and optimistic locking - 10 simultaneous clicks refund once.
- Added typo-tolerant search with Hibernate Search + Lucene (facets from MySQL) and price-drop alerts that send one
  alert per real drop (event + scheduled job).
- Tested with ~400 JUnit tests on real MySQL (Testcontainers, 91.7% service coverage gate), 100+ Vitest tests and a
  Playwright end-to-end story with axe accessibility checks, all in GitHub Actions with gitleaks and npm audit.
- Deployed on free hosting: Spring Boot in Docker on Render tuned for 512 MB, MySQL on Aiven, React on Vercel with
  CSP/HSTS headers, HttpOnly JWT cookies, CSRF protection and an uptime check.

**Skills line**

> Java, Spring Boot, Spring Security, JPA/Hibernate, Flyway, MySQL, React, Redux Toolkit, Tailwind CSS, REST APIs,
> JUnit, Testcontainers, Playwright, Docker, GitHub Actions, Stripe

---

## 2. LinkedIn post

> I just launched **TriVoKo** - a multi-seller marketplace (think a small Flipkart) that I built from an empty folder
> to a live website as my main Java full stack project. 🚀
>
> 🛒 One cart from many sellers -> one payment -> one package per seller, each with its own tracking.
>
> The four parts I'm proudest of:
> ⚡ **Flash sale** - 5,000 users at the same second, 100 units, exactly 100 winners. One atomic SQL statement does
> the check and the update together, so the database itself prevents overselling.
> ↩️ **Returns** - a state machine with exact Stripe partial refunds; money, stock and the seller's earnings always match.
> 🔔 **Price-drop alerts** - one alert per real drop, never spam.
> 🔍 **Smart search** - "iphnoe" still finds the iPhone (Hibernate Search + Lucene).
>
> 🧪 ~400 backend tests on a real MySQL, a Playwright browser test of the whole story, accessibility and security
> scans on every push.
> ☁️ Runs on Render (Docker) + Aiven (MySQL) + Vercel - Rs 0 hosting.
>
> Try it (one-click demo login, test payments only): [live link]
> Code: https://github.com/madhavansuresh000-sys/trivoko
>
> Tech: Java 25 · Spring Boot 4 · React 19 · MySQL · Stripe · Docker · GitHub Actions
>
> I'm looking for Java full stack developer roles - happy to walk anyone through the design!
>
> #Java #SpringBoot #React #FullStack #OpenToWork

---

## 3. Demo video (3 minutes, English; Tamil version optional)

Record the live site at 1080p (OBS Studio or the Windows Game Bar `Win+Alt+R`). Speak slowly; it is fine to record each
part separately and join them.

| Time | Screen | Say (roughly) |
|---|---|---|
| 0:00-0:15 | Home page | "Hi, I'm Madhavan. This is TriVoKo, a multi-seller marketplace I built with React and Spring Boot." |
| 0:15-0:45 | Search "iphnoe", filters, product page | "Search understands typos and shows counts per brand. Each product has sizes and colours with their own stock." |
| 0:45-1:15 | Add from two shops, cart, checkout, test payment | "One cart from two sellers - here are two packages - and one payment. After paying, the order splits per seller." |
| 1:15-1:40 | Log in as Chennai Mobiles: pack, ship | "The seller sees only their own package, packs it and adds a tracking number. Delivered = 90% to their ledger." |
| 1:40-2:05 | Customer: return one item | "A customer can return one item. The refund is exactly that item minus its share of the coupon - a Stripe partial refund." |
| 2:05-2:30 | Flash sale page + README load-test table | "The flash sale: 5,000 users at once, 100 units, exactly 100 winners - one atomic SQL update, proven with k6." |
| 2:30-2:50 | GitHub Actions green checks | "Every push runs about 400 backend tests on real MySQL, a browser test of this whole story, and security scans." |
| 2:50-3:00 | Home page | "It's live on free hosting - the link is below. Thanks for watching!" |

Tips: log in as the customer before recording (to skip typing), use dark mode for one shot, and keep the mouse slow.
