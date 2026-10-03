# How to use this plan

This is the **step-by-step plan** for building TriVoKo. The **Master Plan** (1 October 2026) explains *what* we build and *why*. This document says *in which order* and *how you know a phase is finished*.

>> A building plan has two papers: the architect's drawing (what the house looks like) and the contractor's schedule (foundation first, then walls, then roof). The Master Plan is the drawing. This Phase Plan is the schedule.

![The 13 phases](roadmap.png)

## 1.1 The 13 phases

@widths 800 2500 3626 1100 1000
| Phase | Name | Goal | Time | Group |
| **0** | Setup | Project ready to build | 3-4 days | Core |
| **1** | Catalogue API | Products exist | 1 week | Core |
| **2** | Accounts & roles | Safe login for 3 roles | 1 week | Core |
| **3** | Shop frontend | Customer can shop (no payment yet) | 1.5 weeks | Core |
| **4** | Checkout & payments | Real orders, paid once, split per seller | 2 weeks | Core |
| **5** | Seller dashboard | Sellers run their shop | 1.5 weeks | Core |
| **6** | Admin panel | Admin controls the marketplace | 1 week | Core |
| **7** | Flash sale | Standout #1 - never oversells | 1.5 weeks | Core |
| **8** | Returns & refunds | Standout #2 - exact partial refunds | 1.5 weeks | Core |
| **9** | Reviews, wishlist, alerts | Standout #3 - alerts without spam | 1 week | Shine |
| **10** | Smart search | Standout #4 - typo-tolerant search | 1 week | Shine |
| **11** | Testing & polish | Portfolio quality | 1 week | Always |
| **12** | Launch | Live and shown | 1 week | Always |

Total: about **15-16 weeks** at 2 hours per day. **Core** (Phases 0-8) is a complete marketplace with the two strongest standouts. **Shine** (9-10) can be made smaller if time is short. **Always** (11-12) is never skipped - a portfolio project that is not tested and not live is not finished.

## 1.2 Who does each step

Every step has a **Who** column:

@widths 1800 7226
| Who | Meaning |
| **Claude** | Claude builds it and explains it line by line. You read, run it, and ask for changes. |
| **★ Madhavan** | **You type the key logic.** Claude explains the idea first with a picture, gives you the method signature and the test, then reviews what you wrote. These are the parts interviewers will ask about. |
| **You** | Something only you can do: create an account (Stripe, Cloudinary, GitHub), approve a sketch, choose hosting. |
| **Together** | We do it side by side - for example the real Stripe test or the demo video. |

The three ★ parts are: the **order split** (Phase 4), the **flash-sale claim** (Phase 7) and the **return state machine** (Phase 8). For each one you also write at least one test.

## 1.3 The rules for every phase

1. Open the phase folder (`Phase_N_Name`) and its checklist document. Tick each step when it is done.
2. Do not start the next phase until **every "Done when" check passes** - shown working in the browser, not only in tests.
3. Every phase ends with the **finish routine**:

- All tests pass on the laptop and **CI is green** on GitHub
- Code committed and pushed with clear messages
- The "Done when" checks shown working
- **Explain it back**: you answer the questions at the end of the phase in your own words (spoken or written in "My notes")
- Session summary + SESSION_LOG entry written

> A phase may be split over many days. The checklist document is the place where you write the start date, the finish date and your own notes.

## 1.4 Where things live

@widths 3400 5626
| Folder | What is inside |
| `00_Project_Documents\` | Master Plan, this Phase Plan, guides, session history |
| `Phase_0_Setup\` ... `Phase_12_Launch\` | One checklist document per phase (tick boxes + notes) |
| `backend\` | Spring Boot code - one project that grows every phase |
| `frontend\` | React code - one project that grows every phase |
| `load-tests\`, `e2e\` | k6 scripts (Phase 7), Playwright tests (Phase 11) |
| `docs\`, `tools\` | Sketches, decisions, README diagrams; document generators, demo scripts |
