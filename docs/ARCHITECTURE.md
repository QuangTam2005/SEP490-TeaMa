# SYSTEM ARCHITECTURE DOCUMENT (ARCHITECTURE.md)
**Product Name:** SEP490 Capstone Copilot (`SEP490-TeaMa`)  
**Architecture Style:** Modular Monolith (Backend) + App Router SPA/SSR (Frontend) in a Monorepo  
**Document Version:** 1.2  

---

## 1. Technology Stack

### 1.1 Backend (`backend/`)
- **Language & Framework:** Java 21, Spring Boot 3.3+
- **Security:** Spring Security 6, Stateless JWT (Access Token), BCrypt Password Hashing, HMAC-SHA256 (GitHub Webhook Verification), UUID v4 Magic Link Filter
- **Database & Migration:** PostgreSQL 16, Spring Data JPA (Hibernate), Flyway (Schema Versioning)
- **Document & File Processing:** Apache POI (`poi-ooxml` cho xuất file `.xlsx` 4 sheets `Project Tracking` chuẩn FPTU, `Test Report`, và `Weekly Report`), Java NIO (`Path`/`Files` cho Local Storage)
- **Utilities:** Lombok, MapStruct (DTO Mapping), Jakarta Validation, Spring AOP (Auto Audit Logging)

### 1.2 Frontend (`frontend/`)
- **Framework:** Next.js 14+ (App Router), React 18, TypeScript (Strict Mode)
- **Styling & UI:** TailwindCSS, Shadcn/UI (Radix Primitives), Lucide React Icons
- **Typography:** Sử dụng font chữ hỗ trợ đầy đủ dấu tiếng Việt (`Be Vietnam Pro`, `Inter` hoặc `Plus Jakarta Sans` nạp qua `next/font/google` với `subsets: ['vietnamese', 'latin']`) và `JetBrains Mono` cho các mã định danh/code (`FE-01`, `UC-01`, `TASK-15`)
- **State & Data Fetching:** TanStack Query (React Query v5), Zustand (Client Global State), React Hook Form + Zod (Form Validation)
- **Visualization:** Recharts (Biểu đồ tiến độ 10 cột mốc, Target vs Actual Quality và thống kê đóng góp thành viên)

### 1.3 Infrastructure & Storage (`Docker` on VPS)
- **Target Server:** VPS (8 vCPU, 16GB RAM, 180GB SSD)
- **Containerization:** Docker & Docker Compose (`proxy`, `postgres`, `backend`, `frontend`)
- **Reverse Proxy & SSL:** Nginx hoặc Caddy (tự động cấp HTTPS/SSL Let's Encrypt phục vụ GitHub Webhook và Magic Link)
- **File Storage:** Local Filesystem gắn với Docker Named Volume (`uploads_data:/app/uploads`), phục vụ file qua endpoint bảo mật có kiểm tra quyền (không expose static public).

---

## 2. System Container Diagram

```mermaid
flowchart TB
    subgraph Users ["Actors & External Systems"]
        Leader["Sinh viên (Leader / Member)"]
        Supervisor["Giảng viên hướng dẫn (GVHD)"]
        GitHub["GitHub Webhook Service"]
    end

    subgraph Gateway ["Reverse Proxy & SSL (:80 / :443)"]
        Proxy["Nginx / Caddy (HTTPS + Routing)"]
    end

    subgraph ClientLayer ["Frontend (Next.js 14 App Router :3000)"]
        Dashboard["(dashboard)/* - JWT Auth"]
        SuperView["supervisor/[token] - Magic Link Auth"]
        Zustand["Zustand Store"]
        TanStack["TanStack Query (Cache & Sync)"]
    end

    subgraph BackendApp ["Backend Application (Spring Boot 3.3 - Java 21 :8080)"]
        Security["Spring Security 6 (JWT & MagicLink Filter)"]
        
        subgraph Modules ["Modular Monolith Modules"]
            AuthMod["auth"]
            ProjMod["project (Milestones & Deliverables)"]
            PlanMod["planning (WBS, RACI, Risks, Targets)"]
            SrsMod["srs (Actors, UC, Screens, Rules)"]
            TestMod["testing (TestCases & Metrics)"]
            ExecMod["execution (Kanban & Webhook)"]
            TrackMod["tracking (Issues, Defects, Q&A)"]
            AnalyticsMod["analytics (Objective Contribution)"]
            ExportMod["export (RecordOfChanges, .md & .xlsx)"]
        end

        AOP["AuditLog Aspect (Auto A/M/D Tracking)"]
        Flyway["Flyway Migration Engine"]
    end

    subgraph StorageLayer ["Data & File Storage"]
        Postgres[("PostgreSQL 16 DB (postgres_data)")]
        NamedVolume[("Docker Volume: uploads_data (/app/uploads)")]
    end

    %% Interactions
    Leader -->|HTTPS / JWT| Proxy
    Supervisor -->|HTTPS / Magic Link| Proxy
    GitHub -->|POST /api/webhooks/github/{repoId}| Proxy

    Proxy -->|/*| ClientLayer
    Proxy -->|/api/*| Security
    Dashboard --> TanStack
    SuperView --> TanStack
    TanStack -->|/api/*| Proxy

    Security --> Modules
    AOP -.->|Intercepts CUD ops| ExportMod
    
    Modules --> Postgres
    Flyway -->|Auto Migrate at Startup| Postgres
    Modules -->|NIO Stream Read/Write| NamedVolume
    ExportMod -->|Generates .xlsx & .md| TanStack
```

---

## 3. Monorepo Folder Structure

```text
SEP490-TeaMa/
├── docs/                           # Single Source of Truth cho AI & Team
│   ├── PRD.md
│   ├── ARCHITECTURE.md
│   ├── DESIGN.md
│   ├── RULES.md
│   ├── TASKS.md
│   └── PROGRESS.md
├── backend/                        # Spring Boot Application
│   ├── src/main/java/com/fptu/sep490/
│   │   ├── config/                 # Security, CORS, WebMvc, StorageConfig
│   │   ├── common/                 # ApiResponse<T>, GlobalExceptionHandler, BaseEntity (Audit)
│   │   ├── security/               # JwtTokenProvider, JwtAuthFilter, MagicLinkFilter
│   │   ├── aspect/                 # AuditLogAspect (Auto Record of Changes)
│   │   └── module/
│   │       ├── auth/               # Login, Register, User Profile
│   │       ├── project/            # Project, ProjectMember, ProjectRepository, Milestones (10 mốc), Deliverables
│   │       ├── planning/           # Features, WBS, Complexity Estimator, RACI, Risks, QualityTargets
│   │       ├── srs/                # Actors, UseCases, Screens, AuthMatrix, BusinessRules, AppMessages, Diagrams
│   │       ├── testing/            # TestCases, Coverage & Defect Metrics
│   │       ├── execution/          # Kanban Tasks, Iterations, GitHub Webhook Handler
│   │       ├── tracking/           # Issues, Defects, Q&A Log, Weekly Matters
│   │       ├── analytics/          # Contribution Stats (Tasks, Man-days, PRs, TCs)
│   │       └── export/             # Automatic RecordOfChanges, Markdown & Excel (POI) Generators
│   └── src/main/resources/
│       ├── db/migration/           # Flyway SQL scripts (V1__init_schema.sql, ...)
│       └── application.yml
├── frontend/                       # Next.js Application
│   ├── src/
│   │   ├── app/
│   │   │   ├── (auth)/             # /login, /register
│   │   │   ├── (dashboard)/        # /projects, /projects/[id]/overview, /milestones, /wbs, /srs, /kanban, /testing, /tracking, /analytics
│   │   │   └── supervisor/[token]/ # Giao diện Read-only + Q&A dành riêng cho GVHD (Magic Link)
│   │   ├── components/
│   │   │   ├── ui/                 # Shadcn/UI base components
│   │   │   └── shared/             # DataGrid, RaciMatrixTable, ScreenAuthMatrix, KanbanBoard
│   │   ├── lib/                    # Axios instance, API clients, Zod schemas, Utils
│   │   ├── store/                  # Zustand stores (authStore, activeProjectStore)
│   │   └── types/                  # TypeScript interfaces khớp 1-1 với Backend DTOs
├── docker-compose.yml              # Orchestration cho Proxy, Postgres, Backend, Frontend
└── .gitignore
```

---

## 4. Key Architectural Decisions & Security Mechanisms

### 4.1 Supervisor Magic Link Authentication (FR-18)
- Giảng viên hướng dẫn (GVHD) truy cập trực tiếp qua URL `https://<domain>/supervisor/[token]` (Token là UUID v4 ngẫu nhiên gắn với `project_id`, có thể thu hồi/tạo mới bởi Team Leader).
- Frontend đính kèm header `X-Supervisor-Token: <token>` trong mọi API request tại route `/supervisor/[token]`.
- `MagicLinkFilter` trong Spring Security xác thực token và cấp quyền:
  - `ROLE_SUPERVISOR_READONLY`: Được phép `GET` dữ liệu tổng quan, Milestone Checklist, Contribution, Tracking Logs.
  - `WRITE_QA_LOG`: Chỉ được phép `POST/PUT` phản hồi vào mục `Q&A Log`. Mọi thao tác chỉnh sửa WBS, SRS hoặc xóa dữ liệu khác đều bị chặn (`403 Forbidden`).

### 4.2 Automatic `Record of Changes` Engine (FR-17)
- Sử dụng **Spring AOP (`@Aspect`)** kết hợp với các annotation đánh dấu trên Service methods (hoặc JPA Entity Lifecycle Listeners) để tự động bắt 100% thao tác Thêm (`A`), Sửa (`M`), Xóa (`D`) trên các thực thể SRS và WBS.
- Ghi nhận tự động vào bảng `record_of_changes` gồm: `project_id`, `changed_date`, `change_type (A/M/D)`, `in_charge_user`, `change_description` để sẵn sàng xuất ra Markdown/Excel đúng chuẩn mục I của tất cả các Report SEP490.

### 4.3 Secure File Upload & Delivery (FR-04)
- Toàn bộ file báo cáo (`.docx`, `.pdf`, `.xlsx`), sơ đồ (`.png`, `.jpg`) và code packages (`.zip`) được lưu trữ trong Docker Named Volume `uploads_data` (`/app/uploads`).
- **Không** mở static file mapping công khai. Tất cả thao tác tải file đi qua endpoint bảo mật: `GET /api/v1/deliverables/{id}/download` có kiểm tra JWT (thành viên trong `ProjectMember`) hoặc `X-Supervisor-Token` hợp lệ.
- **Giới hạn kích thước:** Cấu hình giới hạn `max-file-size = 50MB` trên cả Reverse Proxy và Spring Boot (`spring.servlet.multipart.max-file-size=50MB`) để tránh quá tải khi sinh viên nén nhầm `node_modules/` hoặc `target/`.

### 4.4 GitHub Webhook Verification & Multi-PR Aggregation (FR-14)
- Hỗ trợ nhiều GitHub Repository cho mỗi dự án thông qua bảng `project_repositories` (VD: `Backend`, `Frontend`, `Mobile`).
- Endpoint `POST /api/webhooks/github/{repoId}` mở công khai trên Spring Security nhưng bắt buộc xác thực chữ ký HMAC-SHA256 từ header `X-Hub-Signature-256` đối chiếu với `project_repositories.webhook_secret` của repository tương ứng và kiểm tra khớp `payload.repository.full_name`.
- Tự động trích xuất mã task `[TASK-XX]` từ PR title hoặc branch name, upsert thông tin PR vào mảng `wbs_tasks.github_prs` (`JSONB`) theo khóa `(repoId, prNumber)`.
- **Quy tắc tổng hợp trạng thái Multi-PR:** Chuyển Task sang `IN_REVIEW` khi có ít nhất 1 PR `OPEN` (kể cả khi Task trước đó ở `TODO`, `IN_PROGRESS` hoặc `DONE`), và chỉ chuyển sang `DONE` khi có ít nhất 1 PR `MERGED` và không còn PR nào đang `OPEN` (bỏ qua các PR đóng nhầm `CLOSED` chưa merge). Đồng thời tự động ghi nhận `M` vào `document_change_logs` (`report_target = 'R2'`).

### 4.5 VPS Backup & High Availability
- Cấu hình Cronjob hàng đêm trên VPS chạy `pg_dump` xuất dữ liệu PostgreSQL kết hợp nén thư mục `uploads_data` lưu trữ snapshot (giữ tối thiểu 14 ngày gần nhất trên ổ SSD 180GB và đồng bộ thêm ra cloud storage nếu cần).
