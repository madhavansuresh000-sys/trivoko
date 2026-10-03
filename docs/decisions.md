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
