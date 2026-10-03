<p align="center">
  <img src="docs/trivoko-logo-mark.svg" width="72" alt="TriVoKo logo" />
</p>

<h1 align="center">TriVoKo</h1>
<p align="center"><b>One cart. Many sellers. One payment.</b><br/>A multi-seller marketplace (a small Flipkart) built with React + Spring Boot.</p>

<p align="center">
  <a href="../../actions/workflows/ci.yml"><img src="../../actions/workflows/ci.yml/badge.svg" alt="CI" /></a>
</p>

> **Status: Phase 0 - Setup** (October 2026). Built in 13 phases; see `00_Project_Documents/TriVoKo_Phase_Plan_*.pdf`.

## What it will do

- **Customers** search the catalogue, fill one cart from many sellers, pay once with Stripe, and track one package per seller.
- **Sellers** (approved by the admin) list products with sizes and colours, ship their own packages and see their earnings.
- **Admins** approve sellers and products, run flash sales, create coupons and read reports.

### Four standout features

| Feature | The rule it proves |
|---|---|
| ⚡ **Flash sale** | Thousands click at once, exactly the right number win - one atomic SQL update, proven with a 5,000-user k6 load test |
| ↩️ **Returns & refunds** | A return state machine and an exact Stripe *partial* refund; money, stock and the seller ledger always match |
| 🔔 **Price-drop alerts** | Wishlisted customers get one alert per real drop - never spam |
| 🔍 **Smart search** | Typo tolerance ("iphnoe" → iPhone), autocomplete and filter counts with Hibernate Search + Lucene |

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | React 19, Vite, React Router, Redux Toolkit, Tailwind CSS v4, Axios |
| Backend | Java 25, Spring Boot 4 (modular monolith, package `com.trivoko.*`), Spring Security, Spring Data JPA, Flyway |
| Database | MySQL 8.4 (Docker) |
| Payments | Stripe Checkout (test mode), webhooks, partial refunds |
| Testing | JUnit 5, Testcontainers, Vitest, Playwright, k6 |
| Delivery | Docker, GitHub Actions CI |

## Run it on your laptop

You need: Java 25, Node.js 24, Docker Desktop.

```bash
cp .env.example .env              # then choose your own passwords in .env
docker compose up -d              # MySQL on :3307, Mailpit inbox on http://localhost:8026
cd backend && ./mvnw spring-boot:run      # API on http://localhost:8080 (Swagger: /swagger-ui.html)
cd frontend && npm install && npm run dev # shop on http://localhost:5173
```

Tests: `cd backend && ./mvnw verify` (needs Docker - the tests start their own MySQL) and `cd frontend && npm test`.

## Project folders

| Folder | What is inside |
|---|---|
| `backend/` | Spring Boot API - one package per module (`catalog`, `order`, `payment`, `flashsale` ...) |
| `frontend/` | React app |
| `00_Project_Documents/` | Master Plan and Phase Plan (Word + PDF) |
| `Phase_0_Setup/` ... `Phase_12_Launch/` | One checklist document per phase |
| `docs/` | Sketches, decisions, diagrams |
| `load-tests/`, `e2e/` | k6 load tests (Phase 7), Playwright tests (Phase 11) |
| `tools/` | Generators for the plan documents |

## To-do carried over from EventHub

- Real Stripe test in test mode (Phase 4): `sk_test` key in `.env`, pay with card 4242 4242 4242 4242, receive a real signed webhook with the Stripe CLI.

---

Built by **Madhavan Suresh** as a Java full stack portfolio project. Practice project before this: [EventHub](https://github.com/madhavansuresh000-sys/eventhub).
