# Phase 0 - Setup

**Time:** 3-4 days  ·  **Group:** Core  ·  **Folder:** `Phase_0_Setup\`

**Goal:** Create the TriVoKo project so that the backend, the frontend and the database all run on your laptop and on GitHub, and agree on the look of the shop before any page is coded.

>> Before a new shop opens in a mall, the owner gets the keys, puts up the name board, paints the walls in the brand colours and draws where every shelf will go. No goods are sold yet - but everything is ready.

## Steps

@widths 600 6926 1500
| # | Step | Who |
| 1 | Approve this Phase Plan (or ask for changes) | You |
| 2 | Turn `03_TriVoKo_Main_Project` into a Git repository: folders `backend/`, `frontend/`, `load-tests/`, `e2e/`, `docs/`, `tools/`; `.gitignore`, `.gitattributes`, README | Claude |
| 3 | Spring Boot 4 + Java 25 skeleton, package `com.trivoko`, same versions as EventHub; dev and prod profiles; `.env` (git-ignored) for passwords | Claude |
| 4 | `docker-compose.yml`: MySQL 8.4 and Mailpit, with new ports (3307, 8026) so EventHub and TriVoKo never clash | Claude |
| 5 | Copy the EventHub base: ProblemDetail error handler, `/api/health`, Swagger, base SecurityConfig, Flyway, Testcontainers MySQL for tests | Claude |
| 6 | React 19 + Vite skeleton: Tailwind v4 with TriVoKo teal + saffron colour tokens, dark mode, React Router, Redux Toolkit, Axios, Vite proxy; footer shows "Backend: UP" | Claude |
| 7 | Copy the EventHub UI kit (Button, Card, Badge, Modal, Loader, FormField, Toaster) and recolour it; `/style-guide` page | Claude |
| 8 | TriVoKo logo (SVG) + favicon | Claude |
| 9 | GitHub Actions CI: backend build + tests, frontend lint + build + Vitest | Claude |
| 10 | Create the public GitHub repository `trivoko` (or say yes and Claude creates it) and push | You |
| 11 | Library check (small spikes): Hibernate Search 8, Stripe Java, Cloudinary, OpenPDF and Testcontainers all work with Spring Boot 4 - results written in `docs/decisions.md` | Claude |
| 12 | Sketches of the 6 key pages: Home, Search/listing, Product, Cart, Checkout, Seller dashboard | Claude |
| 13 | Sample-data plan: 10 categories, 8 sellers, 120 products with variants; where product photos come from | Claude |
| 14 | Approve the sketches and the sample-data plan (or ask for changes) | You |

## Output of this phase

- Spring Boot app at localhost:8080 with Swagger
- React app at localhost:5173 in TriVoKo colours
- MySQL + Mailpit in Docker
- GitHub repository `trivoko` with a green CI tick
- Approved sketches and sample-data plan

## Done when

- The React page shows "Backend: UP"
- `docker compose up -d` starts MySQL and Mailpit with one command
- CI is green on GitHub
- Madhavan has approved the sketches

## Explain it back

- What is a modular monolith, and why did we not choose microservices?
- Why does the database run in Docker instead of being installed on Windows?
- What does CI do on every push?

**Skills you practise:** Project setup, Maven, npm, Docker, environment variables, CI/CD, design tokens

**Where your work goes:** `backend\`, `frontend\`, `docker-compose.yml`, `.github\workflows\`, `docs\`
