# PROJECT PROGRESS LOG (PROGRESS.md)
**Project:** AI Study Companion – Grade 6 (`SRS-ASC-G6-001`)  
**Current Phase:** Phase 0 Completed $\rightarrow$ Ready for Phase 1 (`Legacy Domain Pruning`)  
**Last Updated:** 2026-11-25  

---

## 1. Document Single Source of Truth Status

| Document | Version | Status | Summary |
| :--- | :---: | :---: | :--- |
| [`docs/PRD.md`](./PRD.md) | `v1.0` | ✅ Baseline | `AI Study Companion – Grade 6`: 5 fixed subjects, `REVIEW` $\rightarrow$ `PREVIEW`, `850–2,000` chars, max `3` post-evaluation revisions, Brave Search + MiMo v2.6 Flash via CKEY, 1-time 8-char Guardian Link. |
| [`docs/ARCHITECTURE.md`](./ARCHITECTURE.md) | `v1.0` | ✅ Baseline | Modular Monolith (`com.asc.studycompanion`, Spring Boot 3.4 / Java 21) + Next.js 14 App Router + PostgreSQL 16 + Brave/CKEY Adapter Orchestration. |
| [`docs/DESIGN.md`](./DESIGN.md) | `v1.0` | ✅ Baseline | Minimal-Text Student & Guardian UI/UX (`#E85D04`, Light/Dark Top Bar), 14-table PostgreSQL ERD (`SRS §7.1`), and REST API Contracts. |
| [`docs/RULES.md`](./RULES.md) | `v1.0` | ✅ Baseline | Domain & Engineering Red Lines (`BR-01..BR-24`, zero PII to AI, identical `BR-12` char counter, `ddl-auto=validate`, anti-IDOR, Atomic Commits). |
| [`docs/TASKS.md`](./TASKS.md) | `v2.0` | ✅ Active | 6-Phase anti-hallucination migration roadmap (`Phase 0`–`Phase 6`). |

---

## 2. Detailed Chronological Log

### 2026-11-25 — Domain Pivot Initialization (`SRS-ASC-G6-001`)
- **[DONE] Phased Migration Plan Approved:**
  - Confirmed clean removal of legacy SEP490 code/migrations and Java package rename to `com.asc.studycompanion`.
  - Confirmed continuing UI-First (`Phase 2` Mock Prototype before `Phase 3–5` Backend), 1-time 8-char Student–Guardian Link Code (`O-01`), and up to **3 post-evaluation revisions** per task (`O-04`).
- **[DONE] `[TASK-DOC.1]` (`3aabf56`):** Rewrote `docs/PRD.md` and `docs/ARCHITECTURE.md` for `AI Study Companion – Grade 6`.
- **[DONE] `[TASK-DOC.2]` (`6b9363a`):** Rewrote `docs/DESIGN.md`, `docs/RULES.md`, `docs/TASKS.md`, `docs/PROGRESS.md`, and `README.md`.
- **[DONE] `[TASK-CLEAN.1]`:** Updated `docker-compose.yml`, `.env.example`, and `application.yml` (`study_companion` DB, `Asia/Ho_Chi_Minh`, `CKEY`/`MiMo` & `Brave Search` config; removed SEP490 storage/multipart config).

---

## 3. Next Steps Queue (Phase 1 — Legacy Cleanup)
1. `[TASK-CLEAN.2]` Rename backend package to `com.asc.studycompanion`, remove legacy `module/project/**` & placeholders, update `ErrorCode.java`, and verify `./mvnw.cmd test`.
2. `[TASK-CLEAN.3]` Remove legacy SEP490 frontend routes/components (`invite`, `projects`, `workspace`) and verify `npm run typecheck && npm run lint && npm run build`.
