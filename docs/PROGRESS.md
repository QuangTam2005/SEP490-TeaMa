# PROJECT PROGRESS LOG (PROGRESS.md)
**Project:** AI Study Companion – Grade 6 (`SRS-ASC-G6-001`)  
**Current Phase:** Phase 1 Completed $\rightarrow$ Ready for Phase 2 (`Frontend UI-First Prototype`)  
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

### 2026-11-25 — Phase 0 & Phase 1: Domain Pivot & Clean Teardown (`SRS-ASC-G6-001`)
- **[DONE] Phased Migration Plan Approved:**
  - Confirmed clean removal of legacy SEP490 code/migrations and Java package rename to `com.asc.studycompanion`.
  - Confirmed continuing UI-First (`Phase 2` Mock Prototype before `Phase 3–5` Backend), 1-time 8-char Student–Guardian Link Code (`O-01`), and up to **3 post-evaluation revisions** per task (`O-04`).
- **[DONE] `[TASK-DOC.1]` (`3aabf56`):** Rewrote `docs/PRD.md` and `docs/ARCHITECTURE.md` for `AI Study Companion – Grade 6`.
- **[DONE] `[TASK-DOC.2]` (`6b9363a`):** Rewrote `docs/DESIGN.md`, `docs/RULES.md`, `docs/TASKS.md`, `docs/PROGRESS.md`, and `README.md`.
- **[DONE] `[TASK-CLEAN.1]` (`eaf8fac`):** Updated `docker-compose.yml`, `.env.example`, and `application.yml` (`study_companion` DB, `Asia/Ho_Chi_Minh`, `CKEY`/`MiMo` & `Brave Search` config; removed SEP490 storage/multipart config).
- **[DONE] `[TASK-CLEAN.2]` (`a787098`):** Migrated backend root package from `com.fptu.sep490` to `com.asc.studycompanion` (`StudyCompanionApplication`, `pom.xml`), removed legacy `module/project/**`, SEP490 migrations (`V1`–`V3`), and placeholders, added `timetable`/`study`/`ai`/`guardian` module stubs, updated `ErrorCode.java`, and verified `.\mvnw.cmd clean test` (`Tests run: 29, Failures: 0` — `BUILD SUCCESS`).
- **[DONE] `[TASK-CLEAN.3]`:** Removed all legacy SEP490 frontend routes/components (`invite`, `(dashboard)/projects`, `supervisor`, `projects/*`, `workspace/*`, `seed-data.ts`, `project*` services/types), updated `api.ts`, `auth.ts`, `api-client.ts`, `auth-token.ts`, `mock-runtime.ts`, `StatusBadge`, `UiReviewToolbar`, `/login`, `/register`, and verified `npm run typecheck`, `npm run lint` (`0` warnings/errors), and `npm run build` (`Compiled successfully`).

---

## 3. Next Steps Queue (Phase 2 — Frontend UI-First Prototype)
1. `[TASK-UI.1]` Build Study Companion strict DTOs (`src/types/{auth,timetable,study,ai,guardian}.ts`), `study-utils.ts` (`BR-12` char counter `850–2,000`, `Asia/Ho_Chi_Minh` next-day helper), and `MockRuntime` (`EVALUATING`/`EVALUATION_FAILED`/`INSUFFICIENT_EVIDENCE`).
2. `[TASK-UI.2]` Build Auth & 1-Time Link Code Flow (`/login`, `/register`, `/link-student` 8-char code generator/acceptor).
3. `[TASK-UI.3]` Build Student Workspace Shell & Weekly Timetable (`/student/layout.tsx`, `/student/timetable`).
4. `[TASK-UI.4]` Build Next-Day Plan & Review/Preview Submission Workspace (`/student/today`, `/student/tasks/[taskId]`).
5. `[TASK-UI.5]` Build Guardian Read-Only Dashboard (`/guardian/dashboard`) & Immutable Submission Inspector (`/guardian/submissions/[submissionId]`).
