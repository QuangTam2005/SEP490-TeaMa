# PROJECT PROGRESS LOG (PROGRESS.md)
**Project:** SEP490 Capstone Copilot (`SEP490-TeaMa`)  
**Current Phase:** Phase A — UI-First Prototype & Validation (`[TASK-UI.0]`–`[TASK-UI.8]`)  
**Last Updated:** 2026-11-21  

---

## 1. Document Single Source of Truth Status

| Document | Version | Status | Summary |
| :--- | :---: | :---: | :--- |
| [`docs/PRD.md`](./PRD.md) | `v1.4` | ✅ Finalized | 100% SEP490 Academic Compliance, 19 FRs (incl. Owner Email Invitation + `MailSender`, Multi-Repo GitHub, WBS Work Category & Test Evidence), 10 Milestones Checklist. |
| [`docs/ARCHITECTURE.md`](./ARCHITECTURE.md) | `v1.1` | ✅ Finalized | Modular Monolith (Java 21 / Spring Boot 3.4+) + Next.js 14 App Router + PostgreSQL 16 + Multi-Repo Webhook Architecture. |
| [`docs/DESIGN.md`](./DESIGN.md) | `v1.5` | ✅ Finalized | Part I: `taste-skill` + `#E85D04` Burnt Orange + Light/Dark Top Bar + Landing `app-frame`; Part II: 22-table PostgreSQL ERD (`project_invitations`); Part III: REST APIs. |
| [`docs/RULES.md`](./RULES.md) | `v1.3` | ✅ Finalized | AI Coding Rules, Mandatory `docs/*.md` Context-First (`AGENTS.md`), Atomic Commits, `ddl-auto=validate`, `@SQLRestriction`, IDOR, Upload Safety & Open Email Policy. |
| [`docs/TASKS.md`](./TASKS.md) | `v1.5` | ✅ Finalized | Restructured into UI-First execution: Phase A (`[TASK-UI.0]`–`[TASK-UI.8]`) $\rightarrow$ Phase B Backend (`[TASK-1.4]`–`[TASK-3.8]`) $\rightarrow$ Phase C Wiring (`[TASK-INT.1]`–`[TASK-INT.2]`). |

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
- **[DONE] UI-First Roadmap Restructuring (`docs/TASKS.md` v1.5):**
  - Reorganized backlog into **Phase A (UI-First Prototype `[TASK-UI.0]`–`[TASK-UI.8]`) $\rightarrow$ Phase B (Backend `[TASK-1.4]`–`[TASK-3.8]`) $\rightarrow$ Phase C (API Wiring `[TASK-INT.1]`–`[TASK-INT.2]`)** while preserving completed `[TASK-1.1]`–`[TASK-1.3]`.

---

## 3. Next Steps Queue (Phase A — UI-First)
1. `[TASK-UI.0]` Build Contract-First Mock Architecture (`src/types/*`, `src/services/`, `MockRuntime`, `QueryStateBoundary`, `ErrorStateCard`, `EmptyState`, `UiReviewToolbar` & `/invite` public guard).
2. `[TASK-UI.1]` Upgrade `#E85D04` tokens, Light/Dark Top Bar switcher, shared UI primitives & Landing Page `/` (`app-frame` preview).
