# PROJECT PROGRESS LOG (PROGRESS.md)
**Project:** SEP490 Capstone Copilot (`SEP490-TeaMa`)  
**Current Phase:** Phase A — UI-First Prototype & Validation (`[TASK-UI.0]`–`[TASK-UI.8]`)  
**Last Updated:** 2026-11-21  

---

## 1. Document Single Source of Truth Status

| Document | Version | Status | Summary |
| :--- | :---: | :---: | :--- |
| [`docs/PRD.md`](./PRD.md) | `v1.5` | ✅ Finalized | Student self-management & GVHD reporting scope (no school grading/1–10 peer-scoring), Supervisor Milestone Review (`Approved`/`Needs Revision`), Email Invitation + `MailSender`, 4-sheet Tracking + Test Report separation. |
| [`docs/ARCHITECTURE.md`](./ARCHITECTURE.md) | `v1.2` | ✅ Finalized | Modular Monolith (Java 21 / Spring Boot 3.4+) + Next.js 14 App Router + PostgreSQL 16 + Multi-Repo Webhook & Objective Contribution Architecture. |
| [`docs/DESIGN.md`](./DESIGN.md) | `v1.6` | ✅ Finalized | `taste-skill` + `#E85D04` + Zero-Violet RACI + Supervisor Milestone Review (`NEEDS_REVISION`/`review_note`) + Email Invitation + 4-Sheet Tracking & Report 5 Export Specs. |
| [`docs/RULES.md`](./RULES.md) | `v1.3` | ✅ Finalized | AI Coding Rules, Mandatory `docs/*.md` Context-First (`AGENTS.md`), Atomic Commits, `ddl-auto=validate`, `@SQLRestriction`, IDOR, Upload Safety & Open Email Policy. |
| [`docs/TASKS.md`](./TASKS.md) | `v1.6` | ✅ Finalized | UI-First execution (`[TASK-UI.0]`–`[TASK-UI.8]` $\rightarrow$ Phase B Backend $\rightarrow$ Phase C Wiring) synced with Supervisor Review, no 1–10 Peer-Review scoring & 4-sheet FPTU Excel template. |

---

## 2. Detailed Chronological Log

### 2026-11-15 — Sprint 0: Foundation & Specification
- **[DONE] Curriculum & Material Analysis:**
  - Inspected all 8 official files in `SEP490-Materials/` (*StudentGuide Fall 2023*, *Report 1*, *Report 2*, *Report 3*, *Project Tracking.xlsx*, *Project Weekly Report.xlsx*, *Sample Schedule.pdf/.pod*).
  - Extracted exact OGA weights (4%, 8%, 16%, 18%, 18%, 4%, 32%), WBS complexity thresholds ($\le 7 / \le 15 / > 15$ fields & $\le 3 / \le 7 / > 7$ transactions), 8-level Screen Authorization matrix, and 4 tracking register structures.
- **[DONE] Repository Initialization (`e5d0aa6`, `29df2bb`):**
  - Initialized Git repository and connected remote `origin` (`https://github.com/QuangTam2005/SEP490-TeaMa.git`).
  - Configured `.gitignore` to exclude heavy binary materials (`SEP490-Materials/`), build artifacts, and secrets.
  - Published `docs/PRD.md` v1.1 as the Single Source of Truth.
- **[DONE] System Architecture Specification (`bfb7ee1`, `b970b9c`):**
  - Finalized `docs/ARCHITECTURE.md` v1.0 with C4 Container Mermaid diagram, Monorepo folder tree, Magic Link security flow (`X-Supervisor-Token`), Spring AOP automatic `Record of Changes`, secure file download endpoint (`max-file-size=50MB`), and nightly VPS backup strategy.
  - Added Vietnamese-friendly typography requirements across `PRD.md`, `ARCHITECTURE.md`, and `DESIGN.md`.
- **[DONE] UI/UX Design System & Database/API Specification (`e5d25e9`, `70c1ba5`):**
  - Finalized `docs/DESIGN.md` v1.1 (UI/UX): FPT Academic Orange (`#F27024`), `Be Vietnam Pro` + `JetBrains Mono`, Icon-First principle using `lucide-react` + `Shadcn Tooltip`, Student Workspace vs. Supervisor Portal layouts, and 7 core component specs (including Kanban Card & Peer-Review Matrix).
  - Upgraded to `docs/DESIGN.md` v1.2 (Full DB & API): Integrated 17 PostgreSQL tables with Partial Unique Indexes (`WHERE is_deleted = FALSE`), `JSONB` screen authorization matrix, `system_actors`, 3-criteria `peer_reviews`, complete `tracking_logs` columns, and core REST API endpoints.
- **[DONE] AI Coding Rules & Constraints (`cbcdbe0`, `9c4b408`, `38e64f2`):**
  - Finalized `docs/RULES.md` v1.0 enforcing Context-First Execution, Minimal Diff, Auto-Update Progress, Atomic Commits (1 feature/fix = 1 commit), `ddl-auto=validate` with Flyway, `@SQLRestriction("is_deleted = false")`, Project-Scope Isolation (anti-IDOR), and Next.js App Router client/server boundaries.
  - Upgraded `docs/RULES.md` to v1.1 adding `@Transactional` / `@EntityGraph` (`JOIN FETCH`) N+1 query optimization and File Upload Safety (`50MB` limit, extension whitelist, anti-Path Traversal).
- **[DONE] `[TASK-0.7]` Granular Task Backlog Breakdown (`b38cd95`):**
  - Upgraded `docs/TASKS.md` to v1.1 with 52 atomic tasks (`[TASK-0.1]` to `[TASK-3.12]`) across Sprint 0–3, mapping 1:1 with `PRD.md` (FR-01..FR-19), `DESIGN.md` (17 DB tables, REST APIs, UI screens), and GitHub Webhook PR tracking.
- **[DONE] `[TASK-0.8]` Docker Compose & Environment Template (`3202785`):**
  - Created `docker-compose.yml` (`postgres:16-alpine`, `backend`, `frontend` services, healthcheck, named volumes `sep490_postgres_data` & `sep490_uploads_data`) and `.env.example` without hardcoded secrets. Validated configuration via `docker compose --env-file .env.example config`.

### 2026-11-16 — Sprint 0: Backend Boilerplate
- **[DONE] `[TASK-0.9]` Backend Spring Boot 3.4 Skeleton (`backend/` - `94d4edd`):**
  - Initialized Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/`), `pom.xml` (Java 21, Spring Boot `3.4.13`, Spring Web, Data JPA, Security, Validation, AOP, Flyway, PostgreSQL, `jjwt` `0.12.6`, Apache POI `5.5.1`, Lombok, MapStruct `1.6.3`).
  - Configured `application.yml` with `ddl-auto: validate`, Flyway (`db/migration`), multipart max file size `50MB`, and environment-variable placeholders for secrets.
  - Created `Dockerfile` (multi-stage build), `Sep490Application.java`, `Sep490ApplicationTests.java`, and Modular Monolith package folders (`config`, `common`, `security`, `aspect`, `module/*`).
  - Verified build & tests via `.\mvnw.cmd test` (`Tests run: 1, Failures: 0` — `BUILD SUCCESS`).
- **[DONE] `[TASK-0.10]` Backend Core Foundation (`common/` & `config/` - `9822e69`):**
  - Locked UTC JDBC timezone (`hibernate.jdbc.time_zone: UTC`) and ISO-8601 Jackson date serialization (`write-dates-as-timestamps: false`) in `application.yml`.
  - Created `JpaAuditingConfig` (`@EnableJpaAuditing`) and `SecurityAuditorHelper` for safe user ID extraction.
  - Separated `BaseTimestampEntity` (`createdAt`, `updatedAt`) from `BaseEntity` (`createdBy`, `updatedBy`, `isDeleted` with `@SuperBuilder` + `@Builder.Default`), keeping `@SQLRestriction("is_deleted = false")` off `@MappedSuperclass` and preserving `updatedBy` on background/webhook updates.
  - Implemented `ApiResponse<T>` (`success`, `message`, `data`, `errorCode`, `timestamp`), domain-prefixed `ErrorCode` enum, `AppException`, and `GlobalExceptionHandler` handling 10 exception types.
  - Verified with 10 passing unit & MockMvc tests (`.\mvnw.cmd test` — `Tests run: 10, Failures: 0`).
- **[DONE] Workspace Agent Rule (`AGENTS.md` & `RULES.md` v1.2 - `7c422fb`):**
  - Added root `AGENTS.md` (always-on workspace rule) and updated `docs/RULES.md` §1.1 to enforce reading all `docs/*.md` files before starting any task.
- **[DONE] `[TASK-0.11]` Flyway Initial Schema Migration (`V1__init_schema.sql` - `3a75eae`):**
  - Created `backend/src/main/resources/db/migration/V1__init_schema.sql` defining all 20 physical PostgreSQL tables (covering all 17 ERD groups in `DESIGN.md` v1.2), foreign keys, `CHECK` constraints, `JSONB` `auth_matrix_json`, and 9 Partial Unique Indexes (`WHERE is_deleted = FALSE`).
  - Verified project build & unit tests (`.\mvnw.cmd test` — `Tests run: 10, Failures: 0`); live PostgreSQL execution pending Docker daemon startup.
- **[DONE] Multi-Repo GitHub Upgrade (`3b66418`, `3c5014a`):**
  - Upgraded `PRD.md` (`v1.2`), `ARCHITECTURE.md` (`v1.1`), `DESIGN.md` (`v1.3`), and `TASKS.md` (`v1.2`, added `[TASK-1.17]`) for `1-to-N` `project_repositories`, `/api/webhooks/github/{repoId}`, `wbs_tasks.github_prs JSONB`, multi-PR state aggregation (`OPEN -> IN_REVIEW`; `>=1 MERGED & 0 OPEN -> DONE`), and multi-PR Kanban card chips.
  - Created `backend/src/main/resources/db/migration/V2__support_multi_repo_github.sql` and added `PROJ_006` (`REPOSITORY_NOT_FOUND`) & `PROJ_007` (`DUPLICATE_REPOSITORY`) to `ErrorCode.java`.
  - Verified with `.\mvnw.cmd test` (`Tests run: 10, Failures: 0` — `BUILD SUCCESS`); live PostgreSQL execution pending Docker daemon startup.
- **[DONE] Test Case Evidence & WBS Work Category Upgrade (`V3` - `df012e9`, `a266d27`):**
  - Synchronized `PRD.md` (`v1.3`), `DESIGN.md` (`v1.4`), `RULES.md` (added `.webp` & 5MB/2-image evidence safety rules), and `TASKS.md` (`v1.3`).
  - Created `backend/src/main/resources/db/migration/V3__add_test_evidence_and_wbs_category.sql` adding `actual_result`, `executed_at`, `evidence_attachments JSONB` on `test_cases`, and `work_category` with `CHECK` constraint & partial index on `wbs_tasks`.
  - Added `TEST_002` (`MAX_EVIDENCE_EXCEEDED`) to `ErrorCode.java` and verified with `.\mvnw.cmd test` (`Tests run: 10, Failures: 0` — `BUILD SUCCESS`).
- **[DONE] `[TASK-0.12]` Frontend Next.js 14.2 App Router Skeleton (`frontend/` - `9c44d50`):**
  - Initialized `frontend/` with Next.js `14.2.35` (React `18.3.1`), strict TypeScript (`strict: true`, `noImplicitAny: true`), and ESLint 8 (`@typescript-eslint` `^7.18.0` banning `any` & `@ts-ignore`).
  - Configured TailwindCSS + `DESIGN.md` tokens (`#F27024` FPT Academic Orange, Light Mode default + `next-themes` `ThemeProvider` with `suppressHydrationWarning`), Google Fonts (`Be Vietnam Pro` + `JetBrains Mono`), `lucide-react`, and `src/lib/utils.ts` (`cn`).
  - Configured `next.config.mjs` (`output: 'standalone'`, `experimental.proxyTimeout: 120000`, `/api/*` rewrite), multi-stage `frontend/Dockerfile`, and directory placeholders matching `ARCHITECTURE.md` §3.
  - Verified cleanly via `npm install`, `npm run typecheck`, `npm run lint`, and `npm run build` (0 errors, 0 warnings).
- **[DONE] `[TASK-0.13]` Frontend Core Utilities & Base Shadcn/UI Primitives (`b5890f4` — Sprint 0 Complete):**
  - Added strict API contracts (`src/types/api.ts`) matching `ApiResponse<T>` & `ErrorCode` 1-to-1.
  - Implemented `src/lib/auth-token.ts` and `src/lib/api-client.ts` with Dual Auth Interceptor (`Authorization: Bearer` vs `X-Supervisor-Token`), multipart `FormData` `Content-Type` safety, `apiUpload` / `apiDownload`, null-safe `ApiResponse` unwrapping, and `ApiClientError`.
  - Configured `QueryProvider` (TanStack Query v5), `AppToaster` (`Sonner` bottom-right), `TooltipProvider`, and 8 base Shadcn/UI primitives (`button`, `badge`, `card`, `input`, `label`, `tooltip`, `dialog`, `sheet` with `tailwindcss-animate`).
  - Verified cleanly via `npm install`, `npm run typecheck`, `npm run lint`, and `npm run build`.
- **[DONE] `[TASK-1.1]` User Entity, Repository & JWT Security Foundation (`9725ae7`):**
  - Created `User` entity extending `BaseTimestampEntity` (matching `users` table without `is_deleted`) and `UserRepository` (`findByEmailIgnoreCase`, `existsByEmailIgnoreCase`).
  - Implemented `JwtProperties`, `UserPrincipal` (`getUsername()` returns `String.valueOf(id)` for `SecurityAuditorHelper`), `CustomUserDetailsService`, `JwtTokenProvider` (UTF-8 default secret with explicit `base64:` prefix support & $\ge 32$-byte check), `JwtAuthFilter` (safe `401` JSON delegation on expired/missing-user tokens, skips `X-Supervisor-Token`), `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`, and `SecurityConfig` (stateless JWT, `BCryptPasswordEncoder`, comma-split CORS + `Content-Disposition` exposure).
  - Verified with 20 passing unit & security filter tests (`.\mvnw.cmd test` — `Tests run: 20, Failures: 0`).
- **[DONE] Email Policy Rule (`docs/RULES.md` v1.3 - `4a072f1`):**
  - Added `Email Validation Policy` forbidding `@fpt.edu.vn` domain restrictions, validation messages, or UI placeholders (accept any valid email).
- **[DONE] `[TASK-1.2]` Auth REST APIs (`POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`):**
  - Created `RegisterRequest`, `LoginRequest` (with compact constructors trimming whitespace before Jakarta `@Email` validation, accepting any valid email domain), `UserResponse`, and `AuthResponse`.
  - Implemented MapStruct `UserMapper`, `AuthService` (single-query `BCrypt` login throwing `AUTH_002`, `saveAndFlush` catching `DataIntegrityViolationException` for concurrent duplicate emails as `409 AUTH_005`), and `AuthController`.
  - Added `AuthServiceTest` (6 tests) & `AuthControllerTest` (4 standalone MockMvc tests using `AuthenticationPrincipalArgumentResolver`); verified `.\mvnw.cmd test` (`Tests run: 30, Failures: 0` — `BUILD SUCCESS`).
- **[DONE] `[TASK-1.3]` Project, Milestone, Member, GitRepo Entities, Repositories & `ProjectAccessGuard`:**
  - Created domain enums (`ProjectRole`, `MilestoneStatus`, `DeliverableStatus`) aligned with `V1` DB check constraints.
  - Implemented `Project` (`projects`) and `ProjectGitRepository` (`project_repositories`, avoiding collision with `ProjectRepository`) extending `BaseEntity` with `@SQLRestriction("is_deleted = false")` and `V2` column alignment (`github_repo_url`/`webhook_secret` removed from `Project`).
  - Implemented `ProjectMember` (`project_members`), `Milestone` (`milestones`), and `MilestoneDeliverable` (`milestone_deliverables`) matching `V1` columns without `BaseEntity` or `@SQLRestriction`.
  - Created `ProjectRepository`, `ProjectGitRepositoryRepository`, `ProjectMemberRepository`, `MilestoneRepository`, and `MilestoneDeliverableRepository` with `@EntityGraph`/`JOIN FETCH` N+1 prevention and `findByIdAndProjectId` project-scope isolation queries.
  - Implemented `ProjectAccessGuard` (`requireProjectExists` `PROJ_001`, `requireMember` `PROJ_002`, `requireLeader` `PROJ_003`, `requireProjectByMagicToken` `AUTH_006`).
  - Added `ProjectEntityTest` (4 tests) and `ProjectAccessGuardTest` (11 tests); verified `.\mvnw.cmd test` (`Tests run: 45, Failures: 0` — `BUILD SUCCESS`).

### 2026-11-21 — UI/UX Master Plan & Email Invitation Spec Sync
- **[DONE] Local `taste-skill` (`design-taste-frontend` 2.5.1) Setup & `.gitignore` (`f5f3821`):**
  - Installed `Leonxlnx/taste-skill` locally into `.agents/skills/design-taste-frontend` and added `.agents/skills/design-taste-frontend/`, `.taste-skill/`, and `skills-lock.json` to `.gitignore`.
- **[DONE] Specification Sync (`PRD.md` v1.4, `DESIGN.md` v1.5, `TASKS.md` v1.4):**
  - Locked UI/UX Master Plan v2: `#E85D04` Burnt Orange accent, Light/Dark switcher in Top Bar (`next-themes`), and Landing `/` upgrade with interactive `app-frame` preview.
  - Updated `FR-02`, ERD (`project_invitations` table #22 for `V4`), API contracts (`/api/projects/{id}/invitations/**`, `/api/invitations/verify|accept`), and Sprint 1 tasks (`[TASK-1.5]`, `[TASK-1.12]`–`[TASK-1.14]`) for Owner Email Invitation + `MailSender` + Sign Up/Sign In join flow.
- **[DONE] UI-First Roadmap Restructuring (`docs/TASKS.md` v1.5 - `9966d8e`):**
  - Reorganized backlog into **Phase A (UI-First Prototype `[TASK-UI.0]`–`[TASK-UI.8]`) $\rightarrow$ Phase B (Backend `[TASK-1.4]`–`[TASK-3.8]`) $\rightarrow$ Phase C (API Wiring `[TASK-INT.1]`–`[TASK-INT.2]`)** while preserving completed `[TASK-1.1]`–`[TASK-1.3]`.
- **[DONE] `[TASK-UI.0]` Contract-First Mock Architecture, Error State Boundaries & UI Review Simulator:**
  - Added `INV_001`–`INV_004` to `src/types/api.ts` and fixed `api-client.ts` 401 redirect guard to exclude `/` and `/invite`.
  - Created strict TypeScript DTO contracts (`src/types/{auth,project,planning,srs,testing,tracking,index}.ts`) matching `DESIGN.md` v1.5 with zero `any`.
  - Built `src/mocks/mock-runtime.ts` (latency, role enforcement, real `ApiClientError` & `VAL_001` `fieldErrors` simulator) + `src/mocks/fixtures/seed-data.ts` (10 Milestones, 5 Quality Objectives, 4 invite token scenarios `valid-new`/`valid-existing`/`mismatch`/`expired`, Multi-Repo GitHub).
  - Implemented `src/services/` Port Layer (`authService`, `projectService`) switching transparently between `mock/` and `http/` via `NEXT_PUBLIC_DATA_MODE`.
  - Created standardized state/error primitives (`QueryStateBoundary`, `ErrorStateCard`, `EmptyState`, `form-error-utils.ts`, dev-only `UiReviewToolbar`, global `app/error.tsx` & `app/not-found.tsx`).
  - Verified `npm run typecheck`, `npm run lint` (`0` warnings/errors), and `npm run build` (`Compiled successfully`).
- **[DONE] `[TASK-UI.1]` Design Tokens (`#E85D04`), Shared Primitives & Landing Page `/` (`taste-skill`):**
  - Upgraded `tailwind.config.ts` & `globals.css` to `#E85D04` Burnt Orange (`hover #C84E03`, adaptive `brand-soft`), Light (`#F8F9FA` / `#FFFFFF` / `#1A1D23`) & Dark (`#0B0F19` / `#111827`) hairline palettes, `.skip-link`, and `prefers-reduced-motion` guard; updated `Button` (micro-press `active:scale-[0.985]`) & `Card` (flat `1px hairline`).
  - Built shared primitives: `ThemeToggle`, domain-typed `StatusBadge`, generic `DataTable<T>`, and `InspectorDrawer`.
  - Built `taste-skill` Landing Page `/` (`LandingHeader` with Light/Dark switcher & mobile `Sheet`, live HTML interactive `HeroAppFrame` with 3 tabs: 10 Milestones OGA / WBS & Complexity / GitHub Multi-Repo, OGA weight strip, 3 live-UI feature cards, Email Invitation vs Supervisor Magic Link split, and closing footer).
  - Verified `npm run typecheck`, `npm run lint` (`0` warnings/errors), and `npm run build` (`Compiled successfully`).
- **[DONE] `[TASK-UI.2]` Auth (`/login`, `/register`), Email Invitation Gate (`/invite`) & Project Directory (`/projects`):**
  - Installed `react-hook-form`, `@hookform/resolvers`, `zod` (`^3.24.2`), and `zustand`; created `src/store/auth-store.ts`.
  - Built 2-column `(auth)/layout.tsx`, `/login` & `/register` with React Hook Form + Zod (accepting any valid email domain, mapping `VAL_001` via `applyApiFieldErrors`, auto-accepting `?inviteToken=...` after login/signup).
  - Built public `/invite?token=...` gate (`InviteAcceptView`) handling all 4 scenarios (`valid-new`, `valid-existing`, `mismatch` `INV_003`, `expired` `INV_001`) plus mock quick-test token switcher.
  - Built `/projects` Directory (`ProjectsDirectoryView` with search, `QueryStateBoundary` 3-card skeleton/empty/error states, linked `1px hairline` project cards) and `CreateProjectDialog` wizard (auto-seeding 10 Milestones & 5 Quality Objectives).
  - Verified `npm run typecheck`, `npm run lint` (`0` warnings/errors), and `npm run build` (`Compiled successfully`, `8/8` static pages).
- **[DONE] `[TASK-UI.3]` Student Workspace `AppShell`, `Overview & 10 Milestones OGA`, & `Settings` (Email Invite + Magic Link):**
  - Built `WorkspaceShell` (collapsible `260px`/`64px` Left Sidebar with tooltips, mobile `Sheet` drawer, Project Switcher, 8 nav items in 3 groups, sticky Top Header with Breadcrumb, `W0X/15` Week Indicator & Light/Dark `ThemeToggle`).
  - Built `/projects/[projectId]/overview` (`OverviewMilestonesView` + `DeliverableUploadRow`: 4 `tabular-nums` OGA metric tiles, filterable 10-Milestone Accordion, drag-and-drop 50MB file uploader with progress bar & `FILE_413`/`FILE_001` validation, `SUPERVISOR` read-only lock).
  - Built `/projects/[projectId]/settings` (`ProjectSettingsView`: `LEADER`-only Email Invitation sender with Zod, Active Members `DataTable` with inline role update & remove confirmation, Invitations `DataTable` with Copy/Resend/Revoke, and Supervisor Magic Link Copy & Rotate confirmation with `MEMBER` masking).
  - Verified `npm run typecheck`, `npm run lint` (`0` warnings/errors), and `npm run build` (`Compiled successfully`).

- **[DONE] Materials & Scope Alignment Audit (`PRD.md` v1.5, `ARCHITECTURE.md` v1.2, `DESIGN.md` v1.6, `TASKS.md` v1.6):**
  - Removed school-grading / 1–10 `Peer-Review` scoring (`peer_reviews` excluded from MVP; replaced with objective `Contribution Analytics` measuring WBS tasks, actual man-days, merged PRs & executed Test Cases).
  - Retained Supervisor Milestone Review (`Approve` / `Needs Revision` with feedback note, zero numerical grading) on `/supervisor/[token]`.
  - Reframed `%` milestone weights strictly as Syllabus Reference Weights (`Tỷ trọng Report tham chiếu`), aligned `Report3_Project Tracking.xlsx` to official **4 sheets** (`WBS`, `Issues`, `Defects`, `Q&A`) + separate `Test Report` (`Report 5`) with English export headers, and replaced RACI `R` Purple badge with Sky (`#E0F2FE` / `#0369A1`).

---

## 3. Next Steps Queue (Phase A — UI-First)
1. `[TASK-UI.4]` Build `Planning (Report 2)` — Features & WBS Table (`Work Category`, Allocated Effort Distribution, realtime `Complexity Auto-Calculator Widget`), interactive `RaciMatrixTable` (sticky column + Click-to-Cycle `D/R/S/I`), Risk Register & 5 Quality Objectives tabs.
2. `[TASK-UI.5]` Build `SRS & Traceability Hub (Report 3)` — Actors, Use Cases, Screens & interactive `ScreenAuthMatrix` (8 FPTU permission levels click-to-toggle), Business Rules, Messages, Diagrams & Traceability Slide-over Drawer.
