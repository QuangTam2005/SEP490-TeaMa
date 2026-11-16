# TASK BACKLOG & WORK BREAKDOWN (TASKS.md)
**Project:** SEP490 Capstone Copilot (`SEP490-TeaMa`)  
**Rule:** Every single `[TASK-X.Y]` corresponds to **1 atomic commit** (`feat`/`fix`/`docs`/`chore`) and maps to GitHub PR tracking.  
**Version:** 1.3  

---

## Sprint 0: Foundation, Specifications & Monorepo Boilerplate
- [x] `[TASK-0.1]` Analyze SEP490 curriculum materials & student guides (`SEP490-Materials/`)
- [x] `[TASK-0.2]` Finalize `docs/PRD.md` v1.1 (100% Academic Compliance & 10 Milestones Checklist)
- [x] `[TASK-0.3]` Initialize Git repository, `.gitignore` & documentation tree
- [x] `[TASK-0.4]` Finalize `docs/ARCHITECTURE.md` v1.0 (Spring Boot 3.3 + Next.js 14 + Postgres 16 + VPS spec)
- [x] `[TASK-0.5]` Finalize `docs/DESIGN.md` v1.2 (Icon-First UI/UX, `Be Vietnam Pro`, 17-table ERD & REST APIs)
- [x] `[TASK-0.6]` Finalize `docs/RULES.md` v1.1 (AI Coding Rules, Atomic Commits, Anti-IDOR, Hibernate 6.4+ rules)
- [x] `[TASK-0.7]` Break down granular backlog in `docs/TASKS.md` with atomic `[TASK-X.Y]` codes
- [x] `[TASK-0.8]` Setup `docker-compose.yml` & `.env.example` (PostgreSQL 16, named volumes `postgres_data` & `uploads_data`)
- [x] `[TASK-0.9]` Initialize `backend/` Spring Boot 3.3 project skeleton (`pom.xml`, `application.yml` with `ddl-auto=validate`)
- [x] `[TASK-0.10]` Implement backend core foundation (`ApiResponse<T>`, `AppException`, `ErrorCode`, `GlobalExceptionHandler`, `BaseEntity` with `@SQLRestriction("is_deleted = false")`)
- [x] `[TASK-0.11]` Create Flyway migration `V1__init_schema.sql` (17 core PostgreSQL tables & Partial Unique Indexes `WHERE is_deleted = FALSE`)
- [x] `[TASK-0.12]` Initialize `frontend/` Next.js 14 App Router skeleton (TypeScript strict, TailwindCSS, `Be Vietnam Pro` & `JetBrains Mono` fonts, `lucide-react`)
- [x] `[TASK-0.13]` Setup frontend core utilities (`src/lib/api-client.ts` with Dual Auth Interceptor, TanStack Query Provider, `Sonner` toast, base Shadcn/UI primitives)

---

## Sprint 1: Authentication, Project Setup, Milestones & Planning (Module 1 & Module 2)

### 1.1 Backend (`backend/`)
- [x] `[TASK-1.1]` Implement `User` entity, repository & JWT security foundation (`JwtTokenProvider`, `JwtAuthFilter`, `SecurityConfig`)
- [x] `[TASK-1.2]` Implement Auth REST APIs (`POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`) — *FR-01*
- [ ] `[TASK-1.3]` Implement `Project`, `ProjectRepository`, `ProjectMember`, `Milestone`, `MilestoneDeliverable` entities & repositories with Project-Scope isolation helper
- [ ] `[TASK-1.4]` Implement Project creation & auto-seeding service (auto-seed 10 SEP490 Milestones with OGA weights, Cumulative Checklists & 5 Quality Objectives) — *FR-02, FR-03*
- [ ] `[TASK-1.5]` Implement Project Member management & Supervisor Magic Link rotation APIs (`POST /api/projects/{id}/members`, `POST /api/projects/{id}/magic-link/rotate`) — *FR-02, FR-18*
- [ ] `[TASK-1.6]` Implement Milestone Deliverables upload & secure download APIs (`POST /deliverables/{delivId}/upload`, `GET /files/download/{fileId}` with `50MB` limit, extension whitelist & anti-Path Traversal) — *FR-04*
- [ ] `[TASK-1.7]` Implement `DocumentChangeLog` entity & Spring AOP `@AuditLog` aspect for automatic `Record of Changes` (`A/M/D`) tracking — *FR-17*
- [ ] `[TASK-1.8]` Implement `Feature` CRUD APIs (`GET/POST/PUT/DELETE /api/projects/{id}/features`) — *FR-05*
- [ ] `[TASK-1.9]` Implement `WbsTask` CRUD APIs with `work_category` (`REQUIREMENT`/`DESIGN`/`CODING`/`TESTING`/`PM`), `effort-distribution` aggregation API, `github_prs JSONB`, Auto-Complexity Estimator ($\le 7 / \le 15 / > 15$ fields & $\le 3 / \le 7 / > 7$ transactions) & total `man-days` aggregation — *FR-05*
- [ ] `[TASK-1.10]` Implement `RaciAssignment` matrix APIs (`GET/PUT /api/projects/{id}/raci`) — *FR-06*
- [ ] `[TASK-1.11]` Implement `ProjectRisk` & `QualityObjective` CRUD APIs (`/api/projects/{id}/risks`, `/api/projects/{id}/quality-objectives`) — *FR-07*
- [ ] `[TASK-1.17]` Implement Multi-Repo GitHub Management APIs (`GET/POST/DELETE /api/projects/{id}/repositories` with auto-generated `webhook_secret`) — *FR-14*

### 1.2 Frontend (`frontend/`)
- [ ] `[TASK-1.12]` Build Auth pages (`/login`, `/register`) & `authStore` (Zustand) with React Hook Form + Zod
- [ ] `[TASK-1.13]` Build Student Workspace App Shell (`Left Sidebar` 240px/64px icon-only, `Top Header` with Week indicator, Project Switcher & Project Creation Modal)
- [ ] `[TASK-1.14]` Build `Overview & Milestones` page (OGA weight cards, 10-Milestone Accordion, Cumulative Checklist Dropzone upload & Magic Link manager)
- [ ] `[TASK-1.15]` Build `Planning (Report 2)` — Features & WBS Table with `Work Category` selector, Allocated Effort Distribution summary & realtime `Complexity Auto-Calculator Widget`
- [ ] `[TASK-1.16]` Build `Planning (Report 2)` — Interactive `RaciMatrixTable` (sticky column + Click-to-Select/Cycle `D/R/S/I`), Risk Register & Quality Objectives tabs

---

## Sprint 2: SRS Specification Hub, Traceability & Quality Assurance (Module 3 & Module 4)

### 2.1 Backend (`backend/`)
- [ ] `[TASK-2.1]` Implement `SystemActor` & `UseCase` CRUD APIs (`/api/projects/{id}/actors`, `/api/projects/{id}/use-cases`) — *FR-08*
- [ ] `[TASK-2.2]` Implement `ScreenFunction` CRUD & JSONB 8-level `ScreenAuthMatrix` toggle API (`PATCH /api/projects/{id}/screens/{screenId}/auth-matrix`) — *FR-08*
- [ ] `[TASK-2.3]` Implement SRS Artifacts CRUD & Diagram upload APIs (`BusinessRule`, `AppMessage`, `DiagramAttachment` at `/api/projects/{id}/srs-artifacts/{type}`) — *FR-08, FR-09*
- [ ] `[TASK-2.4]` Implement End-to-End Traceability Matrix query API (`GET /api/projects/{id}/traceability` with `JOIN FETCH` optimization: `FE <-> UC <-> Screen <-> Task <-> TC <-> Defect`) — *FR-10*
- [ ] `[TASK-2.5]` Implement `TestCase` CRUD, `actual_result`, auto-populate `executed_by`/`executed_at` on `PASS`/`FAIL`, & Test Evidence image APIs (`POST`/`DELETE`/`GET download`, max 2 images, `5MB`/image, `.png`/`.jpg`/`.webp`) — *FR-11*
- [ ] `[TASK-2.6]` Implement Quality Metrics calculation API (`GET /api/projects/{id}/quality-metrics` comparing Actual vs. Target Test Coverage & Defect Rate across 5 stages) — *FR-12*

### 2.2 Frontend (`frontend/`)
- [ ] `[TASK-2.7]` Build `SRS & Traceability (Report 3)` — Actors, Use Cases & Screens management with Complexity estimator
- [ ] `[TASK-2.8]` Build Interactive `ScreenAuthMatrix` Grid (sticky column, Click-to-Toggle `✓` across 8 FPTU permission levels with Optimistic UI)
- [ ] `[TASK-2.9]` Build SRS Business Rules, Application Messages (`INLINE`, `TOAST`, `FIELD_ERROR`) & Diagram Attachments gallery
- [ ] `[TASK-2.10]` Build `Testing & QA (Report 5)` page (Test Case table by `UT/IT/ST/AT`, `Actual Result` + 2-image Evidence uploader/preview, Pass/Fail toggle & Target vs. Actual Quality Recharts dashboard)
- [ ] `[TASK-2.11]` Build interactive `Traceability Matrix` view & global `Traceability Pill Links` Slide-over Drawer (`Sheet`)

---

## Sprint 3: Kanban, GitHub Webhook, Tracking Registers, Analytics & Smart Exporters (Module 5 & Module 6)

### 3.1 Backend (`backend/`)
- [ ] `[TASK-3.1]` Implement Kanban status update API (`PATCH /api/projects/{id}/wbs-tasks/{taskId}/status`) & Iteration filtering (`ITER_1`..`ITER_4`) — *FR-13*
- [ ] `[TASK-3.2]` Implement Multi-Repo GitHub Webhook receiver (`POST /api/webhooks/github/{repoId}` with `X-Hub-Signature-256` HMAC-SHA256 verification, `[TASK-XX]` PR parser, `github_prs JSONB` upsert & multi-PR state aggregation) — *FR-14*
- [ ] `[TASK-3.3]` Implement unified `TrackingLog` CRUD APIs (`/api/projects/{id}/tracking/{logType}` for `ISSUE`, `DEFECT`, `QA`, `WEEKLY_MATTER`) — *FR-15*
- [ ] `[TASK-3.4]` Implement 3-criteria `PeerReview` & Contribution Analytics APIs (`/api/projects/{id}/peer-reviews`, `/api/projects/{id}/analytics/contribution` counting merged PRs across `github_prs` & free-rider detection) — *FR-16*
- [ ] `[TASK-3.5]` Implement `MagicLinkFilter` (`X-Supervisor-Token`) & Supervisor Portal APIs (`GET /api/supervisor/{magicToken}/overview`, `POST /api/supervisor/{magicToken}/qa/{qaId}/answer`) — *FR-18*
- [ ] `[TASK-3.6]` Implement Markdown SRS & `Record of Changes` Exporter (`GET /api/projects/{id}/export/srs-md`) — *FR-17, FR-19*
- [ ] `[TASK-3.7]` Implement Apache POI 5-Sheet Project Tracking `.xlsx` Exporter (`GET /api/projects/{id}/export/tracking-xlsx`: `WBS`, `Issues`, `Defects`, `Q&A`, `Test Cases` incl. `Actual Result` & `Executed At` with `Calibri` Unicode font) — *FR-19*
- [ ] `[TASK-3.8]` Implement Apache POI Weekly Status Report `.xlsx` Exporter (`GET /api/projects/{id}/export/weekly-xlsx` matching 4 sections of `Project Weekly Report_GroupName.xlsx`) — *FR-19*

### 3.2 Frontend (`frontend/`)
- [ ] `[TASK-3.9]` Build `Execution & Kanban` Board (Iteration 1–4 filter, drag-and-drop columns, compact Task Cards with `[TASK-XX]`, Multi-Repo GitHub PR chips & Multi-Repo Webhook setup modal)
- [ ] `[TASK-3.10]` Build `Tracking Registers` page (`Issues`, `Defects`, `Supervisor Q&A`, `Weekly Matters`) & Top Header `+ Quick Log` modal
- [ ] `[TASK-3.11]` Build `Analytics & Export` page (Recharts Contribution Chart, Anonymous 3-Criteria `Peer-Review Matrix`, `Record of Changes` log & `Export Center` `.xlsx`/`.md` downloads)
- [ ] `[TASK-3.12]` Build `Supervisor Portal` (`/supervisor/[token]` Read-only Top Nav layout with Milestones, Contribution, Tracking & Direct Q&A Reply)
