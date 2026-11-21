# TASK BACKLOG & WORK BREAKDOWN (TASKS.md)
**Project:** SEP490 Capstone Copilot (`SEP490-TeaMa`)  
**Rule:** Every single `[TASK-X.Y]` corresponds to **1 atomic commit** (`feat`/`fix`/`docs`/`chore`) and maps to GitHub PR tracking.  
**Version:** 1.5 *(UI-First Restructuring: Phase A UI/UX Validation $\rightarrow$ Phase B Backend $\rightarrow$ Phase C API Wiring)*

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

## Phase A: Full UI/UX & Interactive Prototype (`frontend/` — Contract-First Mock Services)

- [x] `[TASK-UI.0]` Build Contract-First Mock Architecture (`src/types/*` strict DTOs, `src/services/` Port Layer, `MockRuntime` real `ApiClientError` & Role simulator, `QueryStateBoundary`, `ErrorStateCard`, `EmptyState`, `applyApiFieldErrors`, `UiReviewToolbar`, global `error.tsx`/`not-found.tsx` & fix `/invite` public guard in `api-client.ts`)
- [ ] `[TASK-UI.1]` Upgrade Design Tokens (`#E85D04` Burnt Orange, Light/Dark Top Bar switcher, `1px hairline` Card/Button micro-motion), shared `DataTable` / `StatusBadge` / `InspectorDrawer`, and **Landing Page `/`** (`app-frame` interactive preview per `taste-skill`)
- [ ] `[TASK-UI.2]` Build Auth & Email Invitation Flow (`/login`, `/register`, `/invite?token=...` covering 4 token states: `valid-new`, `valid-existing`, `mismatch`, `expired`) + Project Directory & Create Project Wizard (`/projects`)
- [ ] `[TASK-UI.3]` Build Student Workspace `AppShell` (Sidebar `260px`/`64px` + Top Header Light/Dark + Week Indicator + Project Switcher) + `Overview & 10 Milestones OGA` (Accordion, 50MB Dropzone UI) + `Settings` (Email Invite Sender, Pending Invitations & Magic Link manager)
- [ ] `[TASK-UI.4]` Build `Planning (Report 2)` — Features & WBS Table (`Work Category`, Allocated Effort Distribution, realtime `Complexity Auto-Calculator Widget`), interactive `RaciMatrixTable` (sticky column + Click-to-Cycle `D/R/S/I`), Risk Register & 5 Quality Objectives tabs
- [ ] `[TASK-UI.5]` Build `SRS & Traceability Hub (Report 3)` — Actors, Use Cases, Screens & interactive `ScreenAuthMatrix` (8 FPTU permission levels click-to-toggle), Business Rules, Application Messages, Diagram Attachments gallery & Traceability Slide-over Drawer (`Sheet`)
- [ ] `[TASK-UI.6]` Build `Execution Kanban`, `Multi-Repo GitHub` & `Testing QA (Report 5)` — Iteration 1–4 Kanban Board with `[TASK-XX]` & Multi-Repo PR chips, Repositories Webhook modal, Test Case table (`UT/IT/ST/AT`) with `Actual Result`, 2-image Evidence uploader/preview & Target vs. Actual Quality dashboard
- [ ] `[TASK-UI.7]` Build `Tracking Registers`, `Analytics`, `Peer-Review` & `Export Center` — Issues / Defects / Supervisor Q&A / Weekly Matters + Top Header `+ Quick Log` modal, Contribution Matrix, Anonymous 3-Criteria `Peer-Review Matrix`, `Record of Changes` log & `.xlsx`/`.md` Export Center UI
- [ ] `[TASK-UI.8]` Build `Supervisor Read-Only Portal` (`/supervisor/[token]` 4-tab layout: Milestones, Contribution, Tracking & Direct Q&A Reply, Traceability) + Full Responsive & `taste-skill` QA (`390px`, `768px`, `1024px`, `1440px`, Light/Dark)

---

## Phase B: Backend Implementation (`backend/` — Spring Boot 3.4 + Flyway + PostgreSQL 16)

### B.1 Sprint 1 Backend (Auth, Project Setup, Milestones & Planning)
- [x] `[TASK-1.1]` Implement `User` entity, repository & JWT security foundation (`JwtTokenProvider`, `JwtAuthFilter`, `SecurityConfig`)
- [x] `[TASK-1.2]` Implement Auth REST APIs (`POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`) — *FR-01*
- [x] `[TASK-1.3]` Implement `Project`, `ProjectRepository`, `ProjectMember`, `Milestone`, `MilestoneDeliverable` entities & repositories with Project-Scope isolation helper
- [ ] `[TASK-1.4]` Implement Project creation & auto-seeding service (auto-seed 10 SEP490 Milestones with OGA weights, Cumulative Checklists & 5 Quality Objectives) — *FR-02, FR-03*
- [ ] `[TASK-1.5]` Implement Flyway `V4__add_project_invitations.sql`, Email Invitation + `MailSender` workflow (`/api/projects/{id}/invitations/**`, `/api/invitations/verify|accept`), Member Role/Removal & Supervisor Magic Link rotation APIs — *FR-02, FR-18*
- [ ] `[TASK-1.6]` Implement Milestone Deliverables upload & secure download APIs (`POST /deliverables/{delivId}/upload`, `GET /files/download/{fileId}` with `50MB` limit, extension whitelist & anti-Path Traversal) — *FR-04*
- [ ] `[TASK-1.7]` Implement `DocumentChangeLog` entity & Spring AOP `@AuditLog` aspect for automatic `Record of Changes` (`A/M/D`) tracking — *FR-17*
- [ ] `[TASK-1.8]` Implement `Feature` CRUD APIs (`GET/POST/PUT/DELETE /api/projects/{id}/features`) — *FR-05*
- [ ] `[TASK-1.9]` Implement `WbsTask` CRUD APIs with `work_category` (`REQUIREMENT`/`DESIGN`/`CODING`/`TESTING`/`PM`), `effort-distribution` aggregation API, `github_prs JSONB`, Auto-Complexity Estimator ($\le 7 / \le 15 / > 15$ fields & $\le 3 / \le 7 / > 7$ transactions) & total `man-days` aggregation — *FR-05*
- [ ] `[TASK-1.10]` Implement `RaciAssignment` matrix APIs (`GET/PUT /api/projects/{id}/raci`) — *FR-06*
- [ ] `[TASK-1.11]` Implement `ProjectRisk` & `QualityObjective` CRUD APIs (`/api/projects/{id}/risks`, `/api/projects/{id}/quality-objectives`) — *FR-07*
- [ ] `[TASK-1.17]` Implement Multi-Repo GitHub Management APIs (`GET/POST/DELETE /api/projects/{id}/repositories` with auto-generated `webhook_secret`) — *FR-14*

### B.2 Sprint 2 Backend (SRS Hub, Traceability & QA)
- [ ] `[TASK-2.1]` Implement `SystemActor` & `UseCase` CRUD APIs (`/api/projects/{id}/actors`, `/api/projects/{id}/use-cases`) — *FR-08*
- [ ] `[TASK-2.2]` Implement `ScreenFunction` CRUD & JSONB 8-level `ScreenAuthMatrix` toggle API (`PATCH /api/projects/{id}/screens/{screenId}/auth-matrix`) — *FR-08*
- [ ] `[TASK-2.3]` Implement SRS Artifacts CRUD & Diagram upload APIs (`BusinessRule`, `AppMessage`, `DiagramAttachment` at `/api/projects/{id}/srs-artifacts/{type}`) — *FR-08, FR-09*
- [ ] `[TASK-2.4]` Implement End-to-End Traceability Matrix query API (`GET /api/projects/{id}/traceability` with `JOIN FETCH` optimization: `FE <-> UC <-> Screen <-> Task <-> TC <-> Defect`) — *FR-10*
- [ ] `[TASK-2.5]` Implement `TestCase` CRUD, `actual_result`, auto-populate `executed_by`/`executed_at` on `PASS`/`FAIL`, & Test Evidence image APIs (`POST`/`DELETE`/`GET download`, max 2 images, `5MB`/image, `.png`/`.jpg`/`.webp`) — *FR-11*
- [ ] `[TASK-2.6]` Implement Quality Metrics calculation API (`GET /api/projects/{id}/quality-metrics` comparing Actual vs. Target Test Coverage & Defect Rate across 5 stages) — *FR-12*

### B.3 Sprint 3 Backend (Kanban, Multi-Repo Webhook, Tracking, Analytics & Exporters)
- [ ] `[TASK-3.1]` Implement Kanban status update API (`PATCH /api/projects/{id}/wbs-tasks/{taskId}/status`) & Iteration filtering (`ITER_1`..`ITER_4`) — *FR-13*
- [ ] `[TASK-3.2]` Implement Multi-Repo GitHub Webhook receiver (`POST /api/webhooks/github/{repoId}` with `X-Hub-Signature-256` HMAC-SHA256 verification, `[TASK-XX]` PR parser, `github_prs JSONB` upsert & multi-PR state aggregation) — *FR-14*
- [ ] `[TASK-3.3]` Implement unified `TrackingLog` CRUD APIs (`/api/projects/{id}/tracking/{logType}` for `ISSUE`, `DEFECT`, `QA`, `WEEKLY_MATTER`) — *FR-15*
- [ ] `[TASK-3.4]` Implement 3-criteria `PeerReview` & Contribution Analytics APIs (`/api/projects/{id}/peer-reviews`, `/api/projects/{id}/analytics/contribution` counting merged PRs across `github_prs` & free-rider detection) — *FR-16*
- [ ] `[TASK-3.5]` Implement `MagicLinkFilter` (`X-Supervisor-Token`) & Supervisor Portal APIs (`GET /api/supervisor/{magicToken}/overview`, `POST /api/supervisor/{magicToken}/qa/{qaId}/answer`) — *FR-18*
- [ ] `[TASK-3.6]` Implement Markdown SRS & `Record of Changes` Exporter (`GET /api/projects/{id}/export/srs-md`) — *FR-17, FR-19*
- [ ] `[TASK-3.7]` Implement Apache POI 5-Sheet Project Tracking `.xlsx` Exporter (`GET /api/projects/{id}/export/tracking-xlsx`: `WBS`, `Issues`, `Defects`, `Q&A`, `Test Cases` incl. `Actual Result` & `Executed At` with `Calibri` Unicode font) — *FR-19*
- [ ] `[TASK-3.8]` Implement Apache POI Weekly Status Report `.xlsx` Exporter (`GET /api/projects/{id}/export/weekly-xlsx` matching 4 sections of `Project Weekly Report_GroupName.xlsx`) — *FR-19*

---

## Phase C: API Integration & End-to-End Verification

- [ ] `[TASK-INT.1]` Implement real HTTP Services (`frontend/src/services/http/*`) wired to Spring Boot REST APIs & switch `NEXT_PUBLIC_DATA_MODE=api`
- [ ] `[TASK-INT.2]` Full End-to-End Verification (Auth, Email Invite + `MailSender`, 50MB Upload/Download, Multi-Repo Webhook, Excel/Markdown Export & Supervisor Magic Link)
