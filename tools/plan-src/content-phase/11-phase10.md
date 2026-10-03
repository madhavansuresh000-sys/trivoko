# Phase 10 - Smart search

**Time:** 1 week  ·  **Group:** Shine  ·  **Folder:** `Phase_10_Smart_Search\`

**Goal:** Standout #4: search that forgives typos, suggests while you type, and shows filters with counts.

>> A good shop assistant understands "iphnoe" means iPhone, and says "we have 12 Samsung and 8 Apple phones" before you ask.

## Steps

@widths 600 6926 1500
| # | Step | Who |
| 1 | Spike: Hibernate Search 8 with the Lucene backend on Spring Boot 4; index folder; rebuild on start | Claude |
| 2 | Map products: full-text fields (title, brand, description), keyword fields for facets | Claude |
| 3 | Search API: fuzzy match, filters, facets with counts, sort | Claude |
| 4 | Suggest API: suggestions after 2 letters | Claude |
| 5 | Frontend: search box with suggestions (debounce, keyboard), result page with facet counts | Claude |
| 6 | "Customers also bought" (SQL, cached 1 hour) | Claude |
| 7 | Decision: keep Lucene or fall back to MySQL FULLTEXT for hosting - written in `docs/decisions.md` | Together |

## Output of this phase

- Smart search with typo tolerance, suggestions and facets

## Done when

- "iphnoe" finds iPhone
- Suggestions appear while typing

## Explain it back

- What is an index, and why is search not a SQL LIKE query?
- How does fuzzy matching find "iphnoe"?

**Skills you practise:** Full-text search, Lucene, analyzers, facets, debounce

**Where your work goes:** `backend\...\search`, search components in `frontend`
