# PROJECT PROGRESS LOG (PROGRESS.md)
**Project:** SEP490 Capstone Copilot (`SEP490-TeaMa`)  
**Current Phase:** Sprint 0 — Requirements, Architecture, UI/UX & Monorepo Boilerplate  
**Last Updated:** 2026-11-16  

---

## 1. Document Single Source of Truth Status

| Document | Version | Status | Summary |
| :--- | :---: | :---: | :--- |
| [`docs/PRD.md`](./PRD.md) | `v1.1` | ✅ Finalized | 100% SEP490 Academic Compliance, 19 Functional Requirements, 10 Milestones Checklist, Vietnamese Typography NFR. |
| [`docs/ARCHITECTURE.md`](./ARCHITECTURE.md) | `v1.0` | ✅ Finalized | Modular Monolith (Java 21 / Spring Boot 3.3+) + Next.js 14 App Router + PostgreSQL 16 + Docker VPS (8 vCPU / 16GB RAM / 180GB SSD). |
| [`docs/DESIGN.md`](./DESIGN.md) | `v1.2` | ✅ Finalized | Part I: Icon-First UI/UX (`lucide-react`, `Be Vietnam Pro` + `JetBrains Mono`); Part II: 17-table PostgreSQL ERD; Part III: REST API Specs. |
| [`docs/RULES.md`](./RULES.md) | `v1.1` | ✅ Finalized | AI Coding Rules, Atomic Commits, `ddl-auto=validate`, `@SQLRestriction`, IDOR protection, `@Transactional`/N+1 prevention, File Upload Safety. |
| [`docs/TASKS.md`](./TASKS.md) | `v1.1` | ✅ Finalized | 52 granular atomic tasks (`[TASK-0.1]` to `[TASK-3.12]`) mapped 1:1 to FR-01..FR-19, 17 DB tables, REST APIs & UI specs. |

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
- **[DONE] `[TASK-0.11]` Flyway Initial Schema Migration (`V1__init_schema.sql`):**
  - Created `backend/src/main/resources/db/migration/V1__init_schema.sql` defining all 20 physical PostgreSQL tables (covering all 17 ERD groups in `DESIGN.md` v1.2), foreign keys, `CHECK` constraints, `JSONB` `auth_matrix_json`, and 9 Partial Unique Indexes (`WHERE is_deleted = FALSE`).
  - Verified project build & unit tests (`.\mvnw.cmd test` — `Tests run: 10, Failures: 0`); live PostgreSQL execution pending Docker daemon startup.

---

## 3. Next Steps Queue (Remaining Sprint 0 Boilerplate Tasks)
1. `[TASK-0.12]` Initialize `frontend/` Next.js 14 App Router skeleton (TypeScript strict, TailwindCSS, `Be Vietnam Pro` & `JetBrains Mono` fonts, `lucide-react`).
2. `[TASK-0.13]` Setup frontend core utilities (`src/lib/api-client.ts` with Dual Auth Interceptor, TanStack Query Provider, `Sonner` toast, base Shadcn/UI primitives).
