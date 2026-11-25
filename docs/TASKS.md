# TASK BACKLOG & WORK BREAKDOWN (TASKS.md)
**Project:** AI Study Companion – Grade 6 (`SRS-ASC-G6-001`)  
**Rule:** Every `[TASK-*]` corresponds to **1 atomic commit** and passes `typecheck`/`lint`/`build` (FE) or `./mvnw.cmd test` (BE) before committing.  
**Version:** 2.0 *(Phased Migration to AI Study Companion — Anti-Hallucination Schedule)*

---

## Phase 0: Specification & Documentation Reset (`docs/`)
- [x] `[TASK-DOC.1]` Rewrite `docs/PRD.md` & `docs/ARCHITECTURE.md` aligned 100% with `SRS-ASC-G6-001` v1.0 (`5` subjects, `REVIEW` $\rightarrow$ `PREVIEW`, max `3` post-evaluation revisions, Brave + MiMo/CKEY, 1-time 8-char Guardian link code)
- [x] `[TASK-DOC.2]` Rewrite `docs/DESIGN.md` (14-table ERD + REST APIs + Student/Guardian UI), `docs/RULES.md` (`BR-01..BR-24`), `docs/TASKS.md`, `docs/PROGRESS.md`, and `README.md`

---

## Phase 1: Legacy Domain Pruning & Package Rename (`com.asc.studycompanion`)
- [x] `[TASK-CLEAN.1]` Update `docker-compose.yml`, `.env.example`, and `application.yml` for `AI Study Companion` (`Asia/Ho_Chi_Minh`, Brave Search & CKEY/MiMo config; remove SEP490 storage/SMTP config)
- [x] `[TASK-CLEAN.2]` Rename Java package `com.fptu.sep490` $\rightarrow$ `com.asc.studycompanion` (`StudyCompanionApplication.java`, `pom.xml`), remove `module/project/**` & legacy SEP490 `.gitkeep` folders, update `ErrorCode.java` (`SCH_*`, `STUDY_*`, `AI_*`, `GRD_*`), and verify `./mvnw.cmd test` passes
- [x] `[TASK-CLEAN.3]` Remove legacy SEP490 frontend routes/components (`app/invite`, `app/(dashboard)/projects/**`, `components/projects/**`, `components/workspace/**`, SEP490 types & mocks), adapt `api-client.ts` (`/guardian/`), and verify `npm run typecheck && npm run lint && npm run build` passes

---

## Phase 2: Frontend UI-First Prototype (`frontend/` — Contract-First Mock Services)
- [ ] `[TASK-UI.1]` Build Study Companion strict DTOs (`src/types/{auth,timetable,study,ai,guardian}.ts`), `study-utils.ts` (`BR-12` char counter `850–2,000`, `Asia/Ho_Chi_Minh` next-day helper), updated `StatusBadge`, and `MockRuntime` (`STUDENT`/`GUARDIAN` roles + `EVALUATING`/`EVALUATION_FAILED`/`INSUFFICIENT_EVIDENCE` scenarios)
- [ ] `[TASK-UI.2]` Build Auth & 1-Time Link Code Flow (`/login`, `/register` with `STUDENT`/`GUARDIAN` role selector, and `/link-student` 8-char code generator/acceptor per `SRS O-01`)
- [ ] `[TASK-UI.3]` Build Student Workspace Shell & Weekly Timetable (`/student/layout.tsx`, `/student/timetable` 7-day × 5-subject grid with history preservation & Minimal-Text UI)
- [ ] `[TASK-UI.4]` Build Next-Day Plan & Review/Preview Submission Workspace (`/student/today`, `/student/tasks/[taskId]` — `REVIEW` locks `PREVIEW`, realtime `850–2,000` counter, async `EVALUATING` state, `<= 600` char AI feedback + sources, retry & max-3-revision flow)
- [ ] `[TASK-UI.5]` Build Guardian Read-Only Dashboard (`/guardian/dashboard` daily/weekly filters) & Immutable Submission Inspector (`/guardian/submissions/[submissionId]` version history, Accuracy/Completeness, incorrect/missing points, sources — no AI cost leaks) + Responsive/Light-Dark QA

---

## Phase 3: Database Baseline & Auth/Link Backend (`backend/`)
- [ ] `[TASK-BE.1]` Create Flyway `V1__init_study_companion_schema.sql` (14 PostgreSQL tables per `DESIGN.md` + 5 seeded Grade 6 `subjects`) and verify JPA `ddl-auto=validate` alignment
- [ ] `[TASK-BE.2]` Upgrade `auth` module (`STUDENT`/`GUARDIAN` roles, `StudentProfile`, `GuardianLink`, 8-char 1-time Link Code APIs, and `StudyAccessGuard` anti-IDOR protection)

---

## Phase 4: Timetable, Study Plan & Submission Backend (`backend/`)
- [ ] `[TASK-BE.3]` Implement `timetable` module (`Subject`, `Timetable`, `TimetableEntry` versioned CRUD preserving historical plans/submissions `BR-20`)
- [ ] `[TASK-BE.4]` Implement `study` module (`Asia/Ho_Chi_Minh` next-day `StudyPlan`/`StudyTask` generator `BR-04..BR-07`, `REVIEW`-before-`PREVIEW` gate, `BR-12` `850–2,000` char validation, max 3 revisions post-evaluation, `Idempotency-Key`, atomic `StudySubmission` + `EvaluationJob` persistence)

---

## Phase 5: Brave Search + CKEY/MiMo AI Pipeline & Guardian Backend (`backend/`)
- [ ] `[TASK-BE.5]` Implement `SearchProvider` (`BraveSearchAdapter`) & `AiEvaluationProvider` (`CkeyMimoAdapter`) with PII-free prompts, ~2,048 token context budget, max 3 sources, and strict JSON Schema + `<= 600` char feedback validator (plus PoC verification checklist)
- [ ] `[TASK-BE.6]` Implement `KnowledgeCache`, Async `EvaluationJob` Worker (rate limiter + exponential backoff retry + `AIUsageLog`) & `guardian` Read-Only Dashboard/Submission APIs (`FR-GRD-01..08`)

---

## Phase 6: API Integration & Acceptance Verification (`AC-01`..`AC-20`)
- [ ] `[TASK-INT.1]` Implement real HTTP Services (`frontend/src/services/http/*`) and flip `NEXT_PUBLIC_DATA_MODE=api`
- [ ] `[TASK-INT.2]` Verify all 20 SRS Acceptance Criteria (`AC-01`–`AC-20`) + PoC AI evaluation quality check across all 5 subjects
