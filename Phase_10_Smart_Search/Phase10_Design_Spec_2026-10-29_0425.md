# Phase 10 - Smart search: design spec

Date: 29 Oct 2026 · Branch `phase-10-smart-search` (started from `phase-9-reviews-wishlist-alerts`, which is not merged
yet) · Status: **written by Claude after "continue"; defaults marked [default] - MADHAVAN approves or changes them** ·
No new table, so probably no Flyway migration (next would be **V13**).

## 1. Goal and "done when"

Standout #4: search that forgives typos, suggests while you type, and shows filters with counts.
(A good shop assistant understands "iphnoe" means iPhone, and says "we have 12 Samsung and 8 Apple phones" before
you ask.)

**Done when:** "iphnoe" finds the iPhone case (ranked first, not a random "phone") · suggestions appear after 2 letters
while typing · CI green.

## 2. The big choice: Lucene finds and ranks, MySQL filters and counts **[default]**

```
 "iphnoe case"  ──►  Lucene index (name, brand, description, category name)
                       fuzzy match + relevance score  ──►  up to 500 product ids, best first
                                                              │
 filters (category, brand, price, in stock, live shop) ◄──────┘
 applied in MySQL on those ids (the same ProductSpecifications as the listing)
        │
        ├──► one page of cards, in Lucene's order (most relevant first)
        └──► facet counts with SQL GROUP BY: brands (Volta 6, Nimbus 2 ...), categories, price bands
```

**Why not keep stock, price and shop status in Lucene too?** Because they change all the time WITHOUT Hibernate
seeing it: stock moves through bulk `UPDATE ... SET stock = stock - ?` statements (checkout, flash sale), and a shop
is blocked on the `sellers` row, not the product. Hibernate Search only re-indexes what Hibernate saves, so those
filters would go stale ("in stock" for a sold-out phone). Text (name, brand, description) changes only through normal
saves, so Lucene always has it right. Result: typo-tolerant ranking from Lucene, always-fresh filters and counts from
MySQL. Limit: only the best 500 matches are filtered (TriVoKo has 120 products; documented in D11).

## 3. Index (step 1-2)

- Hibernate Search **8.4.0.Final**, Lucene backend (verified in D1). `@Indexed` on `catalog.Product`; fields:
  `name` (boost 3), `brand` (boost 2), `categoryName`, `description` (boost 1) with an analyzer
  `trivoko_text` = standard tokenizer + lower-case + ASCII folding; `name_suggest` / `brand_suggest` with an
  edge n-gram analyzer (2-15 letters) for suggestions, searched with a plain analyzer.
- **Index folder:** `app.search.index-dir`, default `../data/search-index` (git-ignored) in dev **[default]**;
  tests use `local-heap` (in memory, nothing on disk). Set `lucene_version` to stop the start-up warning (D1).
- **Rebuilt on start** with the MassIndexer (120 products = well under a second) - so the folder can always be
  deleted safely, and hosting can use a plain temp folder.

## 4. Search API (step 3)

`GET /api/search?q=&category=&brand=&minPrice=&maxPrice=&inStock=&page=` (public)

```json
{ "results": { ...PageResponse of ProductCard... },
  "facets": { "brands": [{"value":"Volta","count":6}], "categories": [{"value":"phones","name":"Mobile phones","count":8}],
              "prices": [{"label":"Under ₹500","count":3}, ...] },
  "didYouMean": null }
```

- **Fuzzy, but sensible** (the D1 lesson: distance 2 lets "iphnoe" also hit "phone"): words of 1-4 letters
  must match exactly or by prefix; 5+ letters allow 1 typo, 8+ letters 2 typos; exact matches get a boost, so
  "iPhone 15 case" outranks "Phone stand". **The test checks the ranking, not only the hit list.**
- Sort: relevance (default when `q` is given), or price low/high, newest - the same choices as the listing.
- No `q` -> the normal listing (`/api/products`) is used; the search page only adds what Lucene is good at.
- Facet counts come from the same filtered id set, so "Volta (6)" always means 6 cards if you tick it.

## 5. Suggest API (step 4)

`GET /api/search/suggest?q=ip` (public) -> up to 8 suggestions `[{type:"product", text, slug}, {type:"brand", text}]`
after **2 letters** (shorter -> empty list), only live products, cached nowhere (fast enough: <10 ms on 120 items).

## 6. Frontend (step 5)

- The navbar search box shows a suggestion list while typing: **debounce 250 ms**, arrow keys + Enter + Escape,
  `role="combobox"` / `listbox` for screen readers; a product suggestion opens the product, a brand opens
  `/search?brand=...`, Enter opens `/search?q=...`.
- `/search?q=...` page: the cards + a facet panel with counts (brand checkboxes, categories, price bands) + sort.
  "No results for 'xyz'" with a tip when empty.

## 7. "Customers also bought" (step 6)

- On the product page: up to 6 products that appear in the same PAID orders as this one, most-often first.
- SQL in the **order** module (it owns `order_items`): returns product slugs/ids; catalog builds the cards (only
  live ones). Cached **1 hour** in a separate Caffeine cache `alsoBought` (the global spec is 60 s).
- Fewer than 2 results -> the section is hidden **[default]** (no "random" fillers).

## 8. Decision D11 (step 7, together)

Keep Lucene (the index lives inside the app: no extra server, rebuilt on start) **vs** MySQL FULLTEXT (no typo
tolerance, but nothing extra to host). Claude's suggestion: **keep Lucene** - it is the whole point of standout #4
and it costs nothing at hosting time because it runs inside the same Java process. To be decided together.

## 9. Tests

- Backend: "iphnoe" -> the iPhone case FIRST · "volta" finds every Volta product · a blocked shop's product never
  appears · sold-out product hidden with "in stock" ticked, right after a checkout (fresh stock) · brand facet
  counts match the cards · suggest: "i" -> [], "ip" -> iPhone case · price band counts · also-bought from paid
  orders only, cached.
- Frontend (Vitest): suggestions after 2 letters only (debounced), arrow keys + Enter, facet tick changes the URL.

## 10. Explain-it-back (from the Phase Plan)

1. What is an index, and why is search not a SQL `LIKE` query?
2. How does fuzzy matching find "iphnoe"?
