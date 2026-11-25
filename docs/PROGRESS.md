# PROJECT PROGRESS LOG (PROGRESS.md)
**Project:** AI Study Companion – Grade 6 (`SRS-ASC-G6-001`)  
**Current Phase:** Phase 3 In Progress (`[TASK-BE.1b]` Completed — All 14 Flyway Schema Tables Ready)  
**Last Updated:** 2026-11-25  

---

## 1. Document Single Source of Truth Status

| Document | Version | Status | Summary |
| :--- | :---: | :---: | :--- |
| [`docs/PRD.md`](./PRD.md) | `v1.0` | ✅ Baseline | `AI Study Companion – Grade 6`: 5 fixed subjects, `REVIEW` $\rightarrow$ `PREVIEW`, `850–2,000` chars, max `3` post-evaluation revisions, Brave Search + MiMo v2.6 Flash via CKEY, 1-time 8-char Guardian Link. |
| [`docs/ARCHITECTURE.md`](./ARCHITECTURE.md) | `v1.0` | ✅ Baseline | Modular Monolith (`com.asc.studycompanion`, Spring Boot 3.4 / Java 21) + Next.js 14 App Router + PostgreSQL 16 + Brave/CKEY Adapter Orchestration. |
| [`docs/DESIGN.md`](./DESIGN.md) | `v1.1` | ✅ Baseline | Minimal-Text Student & Guardian UI/UX (**Ocean Blue** `#0284C7` / `#0369A1` + White `#FFFFFF`, Light/Dark Top Bar, `>= 44px` THCS tap targets), 14-table PostgreSQL ERD (`SRS §7.1`), and REST API Contracts. |
| [`docs/RULES.md`](./RULES.md) | `v1.0` | ✅ Baseline | Domain & Engineering Red Lines (`BR-01..BR-24`, zero PII to AI, identical `BR-12` char counter, `ddl-auto=validate`, anti-IDOR, Atomic Commits). |
| [`docs/TASKS.md`](./TASKS.md) | `v2.2` | ✅ Active | Backlog cấu trúc `<= 3 subtasks / Task`, mỗi subtask chỉ `2 / 3 / 5 SP` (`Phase 0`–`Phase 6`). |

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
- **[DONE] `[TASK-CLEAN.3]` (`b4ca192`):** Removed all legacy SEP490 frontend routes/components (`invite`, `(dashboard)/projects`, `supervisor`, `projects/*`, `workspace/*`, `seed-data.ts`, `project*` services/types), updated `api.ts`, `auth.ts`, `api-client.ts`, `auth-token.ts`, `mock-runtime.ts`, `StatusBadge`, `UiReviewToolbar`, `/login`, `/register`, and verified `npm run typecheck`, `npm run lint` (`0` warnings/errors), and `npm run build` (`Compiled successfully`).
- **[DONE] `[TASK-UI.1]` (`55770b6`):** Applied Ocean Blue (`#0284C7`/`#0369A1`) + White (`#FFFFFF`) design tokens across `tailwind.config.ts`, `globals.css`, `button.tsx`, `badge.tsx`, and `docs/DESIGN.md` & `docs/RULES.md`; implemented strict Study Companion DTOs (`timetable`, `ai`, `study`, `guardian`), `study-utils.ts` (`BR-12` `850–2,000` normalized char counter & `Asia/Ho_Chi_Minh` next-day helper), updated `StatusBadge` (all task/AI enums in child-friendly Vietnamese), and added AI async/error scenarios (`evaluating`, `evaluation_failed`, `insufficient_evidence`) to `MockRuntime` & `UiReviewToolbar`. Verified `typecheck`, `lint`, `build` passed cleanly.
- **[DONE] `[TASK-UI.2]` (`ef21113`):** Created `link.service.ts` (+ `linkMockService` & `linkHttpService`), polished `/login` and `/register` (`h-11` THCS tap targets, `STUDENT`/`GUARDIAN` role switcher, quick mock role login buttons, Ocean Blue + White styling), and built `/link-student` (`LinkStudentView` for 8-character one-time code generation/copying for Students and code redemption for Guardians with `AUTH_007`/`AUTH_008` validation). Verified `typecheck`, `lint`, `build` passed cleanly.
- **[DONE] `[TASK-UI.3]` (`9185caa`):** Built `study-seed.ts` (Grade 6 timetable, BR-12-compliant `850–2,000` char Vietnamese submissions & `<= 600` char AI evaluations), `timetable.service.ts` (+ `timetableMockService` & `timetableHttpService`), `StudentShell` (`app/student/layout.tsx` Ocean Blue + White responsive header), and `/student/timetable` (`TimetableView` 7-day × 5-subject desktop grid & mobile day cards with `periodNote` and history-preserving versioning). Verified `typecheck`, `lint`, `build` passed cleanly.
- **[DONE] `[TASK-UI.4]` (`8fdeee6`):** Built `study.service.ts` (+ `studyMockService` & `studyHttpService`), `/student/today` (`NextDayPlanView` showing tomorrow's `Asia/Ho_Chi_Minh` subject plan with `REVIEW` -> `PREVIEW` sequential gate & empty schedule state), and `/student/tasks/[taskId]` (`TaskWorkspaceView` with live `850–2,000` normalized char bar, idempotent submission, async `EVALUATING` polling, `<= 600` char AI feedback + clickable sources, retry on `EVALUATION_FAILED`, and up to 3 post-evaluation immutable revisions). Verified `typecheck`, `lint`, `build` passed cleanly.
- **[DONE] `[TASK-UI.5]`:** Built `guardian.service.ts` (+ `guardianMockService` & `guardianHttpService`), `GuardianShell` (`/guardian/layout.tsx`), `/guardian/dashboard` (`GuardianDashboardView` with date & subject filters, completion summary, desktop `DataTable` + mobile cards), and `/guardian/submissions/[submissionId]` (`SubmissionDetailView` read-only immutable student version inspector with Accuracy/Completeness, incorrect/missing points, clickable `sources` + `retrievedAt`, and zero `AIUsageLog`/cost leaks). Verified `typecheck`, `lint`, `build` (`10` App Router routes compiled with `0` errors/warnings).

---

## 3. Next Steps Queue (Phase 3 — Backend Database & Auth/Link)
1. `[TASK-BE.1]` Create Flyway `V1__init_study_companion_schema.sql` (14 PostgreSQL tables per `DESIGN.md` + 5 seeded Grade 6 `subjects`) and verify JPA `ddl-auto=validate` alignment.
2. `[TASK-BE.2]` Upgrade `auth` module (`STUDENT`/`GUARDIAN` roles, `StudentProfile`, `GuardianLink`, 8-char 1-time Link Code APIs, and `StudyAccessGuard` anti-IDOR protection).
3. Proceed sequentially through `[TASK-BE.3]`–`[TASK-BE.6]`.
