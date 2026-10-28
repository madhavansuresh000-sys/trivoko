# Phase 10 - Smart search: implementation plan

Spec: `Phase10_Design_Spec_2026-10-29_0425.md` · Branch `phase-10-smart-search` · Test-first for every backend task
(**Docker must be running**) · MADHAVAN creates the PR and clicks Merge.

| # | Phase Plan step | Task | Main files | Test |
|---|---|---|---|---|
| 1 | step 1 | Add `hibernate-search-mapper-orm` + `hibernate-search-backend-lucene` 8.4.0.Final; config (index dir, local-heap in tests, lucene_version); MassIndexer on start | `pom.xml`, `application.yml`, `search/SearchIndexer` | `SearchIndexTest` (index has all live products after start) |
| 2 | step 2 | Map `Product`: text fields + boosts, `trivoko_text` and edge n-gram analyzers | `catalog/Product`, `search/SearchAnalysis` | `SearchIndexTest` |
| 3 | step 3 | Search API: sensible fuzzy query -> ranked ids (max 500) -> SQL filters + page in that order + facet counts | `search/SearchService`, `SearchController`, `catalog/ProductService.cardsInOrder`, facet queries | `SmartSearchTest` ("iphnoe" first, blocked shop hidden, fresh stock, facet counts) |
| 4 | step 4 | Suggest API (2+ letters, max 8, live only) | `search/SearchService.suggest` | `SuggestTest` |
| 5 | step 5 | Frontend: combobox with debounce + keyboard; `/search` page with facets and sort | `components/search/SearchBox`, `pages/SearchPage` | Vitest |
| 6 | step 6 | "Customers also bought" (order module SQL, `alsoBought` cache 1 h) + product page section | `order/AlsoBoughtService`, `config/CacheConfig` | `AlsoBoughtTest` |
| 7 | step 7 + docs | Decision D11 (together), README Phase 10, Postman folder 12, full verify + npm checks, browser check | | |

PR split: **PR A** = tasks 1-4 (search backend), **PR B** = tasks 5-7 (pages, also-bought, docs).
