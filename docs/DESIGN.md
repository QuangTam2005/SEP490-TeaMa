# SYSTEM DESIGN: UI/UX, DATABASE SCHEMA (ERD) & REST API SPECIFICATIONS
**Product Name:** SEP490 Capstone Copilot (`SEP490-TeaMa`)  
**Design Philosophy:** Academic Precision meets Modern SaaS (Linear / Notion / Jira hybrid) — **Icon-First, Minimal Text**  
**Database Engine:** PostgreSQL 16 (Managed via Flyway Migrations)  
**Document Version:** 1.3 (Full UI/UX + Multi-Repo Database + API Specification)  

---

# PART I: UI/UX DESIGN SYSTEM

## 1. Visual Identity & Color Palette
Hệ thống mặc định sử dụng **Light Mode** độ tương phản cao (tối ưu cho việc đọc tài liệu đặc tả dài, bảng biểu dày đặc và trình chiếu trước Giảng viên hướng dẫn), hỗ trợ chuyển đổi sang **Dark Mode** thông qua `next-themes`.

### 1.1 Brand & Neutral Colors (Tailwind / CSS Variables)
- **Primary Brand (FPT Academic Orange):** `#F27024` (Hover: `#D95D16`, Soft Tint: `#FFF4ED`) — Dùng cho nút hành động chính (Primary CTA), thanh tiến độ Milestone hiện tại và badge vai trò `LEADER`.
- **Secondary Accent (Tech Slate Blue):** `#0F172A` (Slate-900) / `#334155` (Slate-700) — Dùng cho Sidebar, tiêu đề bảng và các thành phần điều hướng.
- **Backgrounds (Light Mode):**
  - App Canvas: `#F8FAFC` (Slate-50)
  - Card / Surface: `#FFFFFF` (Pure White) với viền mỏng `1px solid #E2E8F0` (Slate-200)
- **Text Hierarchy:**
  - Primary Text: `#0F172A` (Slate-900)
  - Secondary / Muted Text: `#64748B` (Slate-500)

### 1.2 Semantic & Domain Status Colors
Sử dụng nhất quán trên toàn bộ bảng biểu, badge và biểu đồ:
- **Success / Passed / Approved / Done:** Emerald `#10B981` (Bg: `#ECFDF5`, Text: `#047857`)
- **Warning / In Review / Medium Risk / Submitted:** Amber `#F59E0B` (Bg: `#FFFBEB`, Text: `#B45309`)
- **Danger / Failed / Out-of-Sync / High Risk / Blocked:** Rose `#EF4444` (Bg: `#FEF2F2`, Text: `#B91C1C`)
- **Info / In Progress / RACI `D` (Do):** Blue `#3B82F6` (Bg: `#EFF6FF`, Text: `#1D4ED8`)
- **RACI Badges:**
  - `D` (Do): Blue (`#DBEAFE` / `#1E40AF`)
  - `R` (Review): Purple (`#F3E8FF` / `#6B21A8`)
  - `S` (Support): Teal (`#CCFBF1` / `#115E59`)
  - `I` (Informed): Gray (`#F1F5F9` / `#475569`)

---

## 2. Typography, Iconography & Minimal-Text Rules

### 2.1 Typography (Vietnamese-Friendly Standard)
- **Primary Font (UI & Prose):** `Be Vietnam Pro` (fallback `Inter`), nạp qua `next/font/google` với `subsets: ['vietnamese', 'latin']` — Tối ưu độ sắc nét và cân đối cho dấu thanh tiếng Việt trên các bảng biểu mật độ cao.
- **Monospace Font (Codes & IDs):** `JetBrains Mono` — Bắt buộc dùng cho toàn bộ mã định danh (`FE-01`, `UC-01`, `TASK-12`, `BR-01`, `MSG01`, `TC-01`), Git PR numbers và Magic Link tokens.
- **Excel Export Font (Apache POI):** `Calibri` hoặc `Arial` chuẩn Unicode (UTF-8) cho các file `.xlsx` xuất ra.
- **Type Scale:**
  - Page Title: `24px` (`text-2xl font-bold tracking-tight`)
  - Section Header: `18px` (`text-lg font-semibold`)
  - Table Header: `12px` (`text-xs font-semibold uppercase tracking-wider text-slate-500`)
  - Table Cell / Body: `14px` (`text-sm`)
- **Spacing & Radius:**
  - Border Radius chuẩn: `8px` (`rounded-lg`) cho Cards/Modals; `6px` (`rounded-md`) cho Buttons/Inputs.
  - Mật độ hiển thị (Density): **Compact-Comfortable** (padding ô bảng `py-2 px-3`) để hiển thị được nhiều dòng Use Case / WBS trên một màn hình mà không cần cuộn ngang quá nhiều.

### 2.2 Iconography & Nguyên tắc "Hạn chế dùng chữ" (Icon-First UI)
- **Thư viện Icon đồng bộ duy nhất:** Toàn bộ hệ thống chỉ sử dụng **`lucide-react`** (với `strokeWidth={1.75}` đồng nhất; size `16px` cho nút/bảng/badge và `20px` cho Sidebar/Header). Tuyệt đối không trộn lẫn thư viện icon khác hoặc dùng emoji thô trong giao diện chức năng.
- **Icon + Tooltip thay thế văn bản lặp lại:**
  - Các nút thao tác trong bảng (Sửa, Xóa, Tải xuống, Sao chép link, Xem liên kết chéo, Mở PR GitHub) chỉ hiển thị **Icon** (`icon-only button`), kèm `Shadcn Tooltip` giải thích ngắn khi hover.
  - Các nhãn trạng thái (`Pending`, `In Progress`, `In Review`, `Done`, `Pass`, `Fail`) ưu tiên dùng **Icon kết hợp màu sắc** (VD: `CheckCircle2`, `Clock`, `GitPullRequest`, `XCircle`) hoặc badge ngắn 1 từ, tránh viết câu mô tả dài trong ô bảng.
  - Thanh công cụ và bộ lọc ưu tiên biểu tượng trực quan (VD: Icon bộ lọc Iteration, Icon Export `.xlsx` / `.md`, Icon Quick Log) để giữ giao diện thoáng, tập trung vào dữ liệu nghiệp vụ.

---

## 3. Application Shell & Navigation Architecture

### 3.1 Student Workspace Layout (`(dashboard)/projects/[id]/...`)
- **Left Sidebar (Fixed `240px`, collapsible xuống `64px` ở chế độ Icon-only):**
  - Project Switcher ở trên cùng (hiển thị `Project Code` + `Group Name`).
  - Nhóm menu theo đúng vòng đời SEP490 (mỗi mục đi kèm 1 icon `lucide-react` đặc trưng):
    1. `Overview & Milestones` *(LayoutDashboard)* — Dashboard 15 tuần + OGA Weight + Deliverables Checklist
    2. `Planning (Report 2)` *(CalendarRange)* — WBS & Complexity, RACI Matrix, Risk Register, Quality Targets
    3. `SRS & Traceability (Report 3)` *(FileCode2)* — Features, Actors, Use Cases, Screens & Auth Matrix, Rules, Messages, Diagrams
    4. `Execution & Kanban` *(Kanban)* — Iteration 1–4 Board, GitHub Webhook Sync
    5. `Testing & QA (Report 5)` *(FlaskConical)* — Test Cases, Coverage vs Target
    6. `Tracking Registers` *(ClipboardList)* — Issues, Defects, Supervisor Q&A, Weekly Matters
    7. `Analytics & Export` *(BarChart3)* — Contribution Chart, Peer-Review, Record of Changes, Export `.xlsx`/`.md`
- **Top Header (`56px`):**
  - Breadcrumb điều hướng + Badge hiển thị **W5/15** và **Countdown Milestone** tiếp theo.
  - Nút thao tác nhanh dạng Icon/Compact: `+ Quick Log` (Issue/Defect/Q&A) và `Export Center`.

### 3.2 Supervisor Portal Layout (`supervisor/[token]/...`)
- **Không có Sidebar phức tạp**, sử dụng **Top Navigation Bar tinh gọn** với badge/banner ngắn gọn: *"Supervisor Read-Only • [Group Name]"*.
- Tập trung 4 Tab chính cho GVHD:
  1. `Milestones` (Tiến độ 10 cột mốc + tải file báo cáo nhóm đã nộp)
  2. `Contribution` (Biểu đồ Task/PR/Man-days từng sinh viên + kết quả Peer-Review)
  3. `Tracking & Q&A` (Xem Issues/Defects và khung trả lời trực tiếp câu hỏi Q&A của nhóm)
  4. `Traceability` (Kiểm tra nhanh độ phủ Use Case – Code – Test)

---

## 4. Core Component Specifications (Shadcn/UI Standards)

1. **Interactive Matrix Grids (`RaciMatrixTable` & `ScreenAuthMatrix`):**
   - Cố định cột đầu tiên (`sticky left-0 bg-white z-10`).
   - **`ScreenAuthMatrix`:** Sử dụng **Click-to-Toggle** trực tiếp trên từng ô để bật/tắt dấu `✓` (`Check` icon) cho 8 mức quyền chuẩn FPTU, gọi `PATCH` ngầm kèm Optimistic UI.
   - **`RaciMatrixTable`:** Sử dụng **Click-to-Select Popover** (hoặc Click-to-Cycle qua 5 trạng thái: `D → R → S → I → Empty`) hiển thị badge màu chữ cái gọn nhẹ.
2. **Complexity Auto-Calculator Widget (Trong Modal tạo/sửa Screen & WBS):**
   - Nhập số `Fields` và `Transactions` -> tự động hiển thị Badge tính toán realtime (`Simple` / `Medium` / `Complex`) kèm Tooltip hiển thị quy chuẩn bộ môn (`<=7 fields & <=3 trans = Simple`, ...).
3. **Milestone Accordion & Checklist Card:**
   - 10 cột mốc hiển thị dạng Accordion dọc. Mỗi mốc hiển thị badge trọng số OGA (VD: `16%`), hạn nộp, danh sách checklist lũy tiến kèm Dropzone kéo-thả upload file và icon trạng thái duyệt.
4. **Traceability Pill Links:**
   - Mọi mã `UC-01`, `TASK-15`, `TC-04`, `BUG-02` khi xuất hiện trong bảng đều là **Clickable Badge** (font `JetBrains Mono`) mở ra Slide-over Drawer (Sheet bên phải) hiển thị chi tiết thực thể đó mà không làm mất trang hiện tại.
5. **Iteration Kanban Board & Task Card:**
   - Thanh lọc nhanh 4 đợt Iteration (`Iter 1` – `Iter 4`) và thành viên.
   - Mỗi Task Card tối giản chữ, hiển thị: Mã `[TASK-15]` (`JetBrains Mono`), tiêu đề ngắn (tối đa 2 dòng), badge Iteration, **Multi-Repo GitHub PR chips** (VD: `BE #12`, `FE #08` kèm icon trạng thái `OPEN`/`MERGED`/`CLOSED` và click mở link PR), số `man-days` (icon đồng hồ) và Avatar người phụ trách.
6. **Anonymous Peer-Review Matrix:**
   - Giao diện đánh giá chéo cuối mỗi Iteration dạng bảng lưới nhỏ gọn: Hàng là tên thành viên, Cột là 3 tiêu chí (*Thái độ*, *Đúng hạn*, *Chất lượng*) chấm bằng **Icon Star Rating / Thanh chọn điểm 1–10**, tự động gắn icon cảnh báo (`AlertTriangle`) khi điểm trung bình dưới ngưỡng an toàn (nguy cơ *free-rider*).
7. **Feedback & Empty States:**
   - Mọi thao tác lưu/xóa/export đều phản hồi bằng `Sonner` Toast ở góc dưới bên phải.
   - Bảng trống hiển thị **Empty State** gồm Icon minh họa lớn + 1 dòng hướng dẫn ngắn + 1 nút CTA chính (VD: `+ Thêm Use Case`).

---

# PART II: DATABASE SCHEMA (ERD)

## 5. Database Design Conventions
- **Primary Keys:** `BIGSERIAL` (auto-increment `BIGINT`) cho toàn bộ các bảng nội bộ để tối ưu hiệu năng index và join; riêng `supervisor_magic_token` dùng `UUID` v4 chống đoán mò.
- **Audit & Soft-Delete Columns:** Tất cả các bảng nghiệp vụ chính (trừ bảng log bất biến `document_change_logs`) đều có các cột:
  - `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
  - `updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
  - `created_by BIGINT REFERENCES users(id)`
  - `updated_by BIGINT REFERENCES users(id)`
  - `is_deleted BOOLEAN NOT NULL DEFAULT FALSE`
- **Partial Unique Index cho Soft-Delete:** Để tránh lỗi trùng mã (`FE-01`, `UC-01`, `TASK-01`, `TC-01`...) sau khi sinh viên xóa mềm và tạo lại mã cũ trong cùng dự án, toàn bộ ràng buộc duy nhất theo mã đều sử dụng **PostgreSQL Partial Unique Index**:
  ```sql
  CREATE UNIQUE INDEX uq_<table>_project_code ON <table> (project_id, code) WHERE is_deleted = FALSE;
  ```
- **Naming Convention:** `snake_case` số nhiều cho tên bảng (`users`, `wbs_tasks`), `snake_case` cho tên cột.

---

## 6. Detailed PostgreSQL Schema (21 Core Tables / 18 ERD Groups)

### 6.1 Identity, Project & Repositories Core
1. **`users`**
   - `id BIGSERIAL PRIMARY KEY`
   - `email VARCHAR(255) UNIQUE NOT NULL`
   - `password_hash VARCHAR(255) NOT NULL`
   - `full_name VARCHAR(150) NOT NULL`
   - `student_code VARCHAR(20)` (VD: `SE171234`)
   - `phone VARCHAR(20)`
   - `avatar_url VARCHAR(500)`
   - `created_at`, `updated_at`

2. **`projects`**
   - `id BIGSERIAL PRIMARY KEY`
   - `project_code VARCHAR(30) NOT NULL` (VD: `COS`)
   - `project_name VARCHAR(255) NOT NULL`
   - `group_name VARCHAR(50) NOT NULL` (VD: `SEP490-G4`)
   - `software_type VARCHAR(100)` (Web App, Mobile App...)
   - `start_date DATE NOT NULL` (Mốc tính Tuần 1 -> Tuần 15)
   - `supervisor_magic_token UUID UNIQUE NOT NULL`
   - `supervisor_name VARCHAR(150)`
   - `supervisor_email VARCHAR(255)`
   - + Audit & Soft-Delete columns

2.1. **`project_repositories`** *(Hỗ trợ Multi-Repo: Backend, Frontend, Mobile...)*
   - `id BIGSERIAL PRIMARY KEY`
   - `project_id BIGINT NOT NULL REFERENCES projects(id)`
   - `repo_label VARCHAR(50) NOT NULL` (VD: `Backend`, `Frontend`, `Mobile`)
   - `repo_full_name VARCHAR(255) NOT NULL` (VD: `owner/repo-name`)
   - `github_repo_url VARCHAR(500) NOT NULL`
   - `webhook_secret VARCHAR(255) NOT NULL`
   - + Audit & Soft-Delete columns
   - Partial Unique Index: `CREATE UNIQUE INDEX uq_project_repositories_active ON project_repositories (project_id, repo_full_name) WHERE is_deleted = FALSE;`

3. **`project_members`**
   - `id BIGSERIAL PRIMARY KEY`
   - `project_id BIGINT NOT NULL REFERENCES projects(id)`
   - `user_id BIGINT NOT NULL REFERENCES users(id)`
   - `role VARCHAR(20) NOT NULL` (`LEADER` | `MEMBER`)
   - `joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
   - `UNIQUE (project_id, user_id)`

### 6.2 Milestones & Deliverables (SEP490 15-Week Track)
4. **`milestones`**
   - `id BIGSERIAL PRIMARY KEY`
   - `project_id BIGINT NOT NULL REFERENCES projects(id)`
   - `milestone_code VARCHAR(10) NOT NULL` (`M1`..`M10`)
   - `name VARCHAR(255) NOT NULL`
   - `week_number INT NOT NULL` (`1`..`15`)
   - `due_date DATE`
   - `oga_weight_percent DECIMAL(5,2) NOT NULL DEFAULT 0` (R1: 4%, R2: 8%, R3: 16%, R4: 18%, R5: 18%, R6: 4%, R7: 32%)
   - `status VARCHAR(20) NOT NULL DEFAULT 'PENDING'` (`PENDING` | `SUBMITTED` | `APPROVED`)

5. **`milestone_deliverables`**
   - `id BIGSERIAL PRIMARY KEY`
   - `milestone_id BIGINT NOT NULL REFERENCES milestones(id)`
   - `item_name VARCHAR(255) NOT NULL` (VD: `Report 3`, `Updated Report 1`, `Demo Code Package`)
   - `is_required BOOLEAN NOT NULL DEFAULT TRUE`
   - `file_path VARCHAR(500)`
   - `file_name VARCHAR(255)`
   - `file_size BIGINT`
   - `uploaded_by BIGINT REFERENCES users(id)`
   - `uploaded_at TIMESTAMPTZ`
   - `status VARCHAR(20) NOT NULL DEFAULT 'PENDING'` (`PENDING` | `SUBMITTED` | `APPROVED`)

### 6.3 Planning & Report 2 Entities
6. **`features`**
   - `id BIGSERIAL PRIMARY KEY`
   - `project_id BIGINT NOT NULL REFERENCES projects(id)`
   - `code VARCHAR(20) NOT NULL` (`FE-01`)
   - `name VARCHAR(255) NOT NULL`
   - `description TEXT`
   - + Audit & Soft-Delete columns

7. **`wbs_tasks`**
   - `id BIGSERIAL PRIMARY KEY`
   - `project_id BIGINT NOT NULL REFERENCES projects(id)`
   - `feature_id BIGINT REFERENCES features(id)`
   - `use_case_id BIGINT REFERENCES use_cases(id)`
   - `screen_function_id BIGINT REFERENCES screens_functions(id)` *(Nullable — liên kết truy vết trực tiếp tới Screen/Function)*
   - `task_code VARCHAR(20) NOT NULL` (`TASK-01`)
   - `title VARCHAR(255) NOT NULL`
   - `field_count INT NOT NULL DEFAULT 0` *(Hỗ trợ tính Complexity ngay từ giai đoạn lập WBS)*
   - `transaction_count INT NOT NULL DEFAULT 0`
   - `complexity VARCHAR(20) NOT NULL DEFAULT 'SIMPLE'` (`SIMPLE` | `MEDIUM` | `COMPLEX`)
   - `est_mandays DECIMAL(5,2) NOT NULL DEFAULT 1.0`
   - `actual_mandays DECIMAL(5,2) NOT NULL DEFAULT 0.0` *(Phục vụ thống kê đóng góp thực tế FR-16)*
   - `iteration VARCHAR(20) NOT NULL DEFAULT 'ITER_1'` (`ITER_1` | `ITER_2` | `ITER_3` | `ITER_4`)
   - `status VARCHAR(20) NOT NULL DEFAULT 'TODO'` (`TODO` | `IN_PROGRESS` | `IN_REVIEW` | `DONE`)
   - `assignee_id BIGINT REFERENCES users(id)`
   - `github_prs JSONB NOT NULL DEFAULT '[]'::jsonb` *(Danh sách PR liên kết từ nhiều repository)*
     > **JSONB Schema Example:**
     > ```json
     > [
     >   {
     >     "repoId": 1,
     >     "repoLabel": "Backend",
     >     "prNumber": 12,
     >     "prUrl": "https://github.com/org/repo-be/pull/12",
     >     "branch": "feat/TASK-01-auth",
     >     "status": "MERGED",
     >     "author": "username",
     >     "updatedAt": "2026-11-16T02:00:00Z"
     >   }
     > ]
     > ```
   - + Audit & Soft-Delete columns

8. **`raci_assignments`**
   - `id BIGSERIAL PRIMARY KEY`
   - `project_id BIGINT NOT NULL REFERENCES projects(id)`
   - `deliverable_or_task_name VARCHAR(255) NOT NULL`
   - `member_id BIGINT NOT NULL REFERENCES project_members(id)`
   - `raci_role VARCHAR(5)` (`D` | `R` | `S` | `I` | NULL)
   - `UNIQUE (project_id, deliverable_or_task_name, member_id)`

9. **`project_risks`**
   - `id BIGSERIAL PRIMARY KEY`
   - `project_id BIGINT NOT NULL REFERENCES projects(id)`
   - `risk_description TEXT NOT NULL`
   - `impact VARCHAR(20) NOT NULL` (`HIGH` | `MEDIUM` | `LOW`)
   - `possibility VARCHAR(20) NOT NULL` (`HIGH` | `MEDIUM` | `LOW`)
   - `response_plan TEXT NOT NULL`
   - + Audit & Soft-Delete columns

10. **`quality_objectives`**
    - `id BIGSERIAL PRIMARY KEY`
    - `project_id BIGINT NOT NULL REFERENCES projects(id)`
    - `testing_stage VARCHAR(30) NOT NULL` (`REVIEWING` | `UNIT_TEST` | `INTEGRATION_TEST` | `SYSTEM_TEST` | `ACCEPTANCE_TEST`)
    - `target_coverage_percent DECIMAL(5,2) NOT NULL DEFAULT 80.00`
    - `target_defect_rate_percent DECIMAL(5,2) NOT NULL DEFAULT 5.00`

### 6.4 SRS & Report 3 Entities
11. **`system_actors`** *(Phục vụ mục 2.1 System Actors của Report 3 và làm cột Role động cho `ScreenAuthMatrix`)*
    - `id BIGSERIAL PRIMARY KEY`
    - `project_id BIGINT NOT NULL REFERENCES projects(id)`
    - `name VARCHAR(100) NOT NULL` (VD: `Administrator`, `Menu Manager`, `Patron`)
    - `description TEXT`
    - + Audit & Soft-Delete columns

12. **`use_cases`**
    - `id BIGSERIAL PRIMARY KEY`
    - `project_id BIGINT NOT NULL REFERENCES projects(id)`
    - `feature_id BIGINT NOT NULL REFERENCES features(id)`
    - `code VARCHAR(20) NOT NULL` (`UC-01`)
    - `name VARCHAR(255) NOT NULL`
    - `actors VARCHAR(255) NOT NULL`
    - `description TEXT`
    - + Audit & Soft-Delete columns

13. **`screens_functions`**
    - `id BIGSERIAL PRIMARY KEY`
    - `project_id BIGINT NOT NULL REFERENCES projects(id)`
    - `use_case_id BIGINT REFERENCES use_cases(id)`
    - `code VARCHAR(20) NOT NULL` (`SCR-01` / `FN-01`)
    - `name VARCHAR(255) NOT NULL`
    - `type VARCHAR(20) NOT NULL` (`SCREEN` | `NON_SCREEN`)
    - `description TEXT`
    - `field_count INT NOT NULL DEFAULT 0`
    - `transaction_count INT NOT NULL DEFAULT 0`
    - `auth_matrix_json JSONB NOT NULL DEFAULT '{}'::jsonb`
      > **JSONB Schema Example:** `{"Guest": {"queryAll": false, "add": false}, "Admin": {"queryAll": true, "queryOwn": true, "queryManaged": true, "add": true, "updateAll": true, "updateOwn": true, "updateManaged": true, "delete": true}}`
    - + Audit & Soft-Delete columns

14. **`business_rules`**, **`app_messages`**, **`diagram_attachments`** (3 bảng SRS Artifacts)
    - `business_rules`: `id`, `project_id`, `code` (`BR-01`), `definition`, + Audit/Soft-Delete.
    - `app_messages`: `id`, `project_id`, `code` (`MSG01`), `message_type` (`INLINE` | `TOAST` | `FIELD_ERROR`), `context`, `content`, + Audit/Soft-Delete *(Khớp 3 loại chuẩn Report 3: In line, Toast message, In red under text box)*.
    - `diagram_attachments`: `id`, `project_id`, `diagram_type` (`CONTEXT` | `USE_CASE` | `SCREEN_FLOW` | `ERD` | `ARCHITECTURE`), `title`, `file_path`, `file_name`, + Audit/Soft-Delete.

### 6.5 Testing, Tracking, Peer-Review & Audit Logs
15. **`test_cases`**
    - `id BIGSERIAL PRIMARY KEY`
    - `project_id BIGINT NOT NULL REFERENCES projects(id)`
    - `use_case_id BIGINT NOT NULL REFERENCES use_cases(id)`
    - `code VARCHAR(20) NOT NULL` (`TC-01`)
    - `test_level VARCHAR(10) NOT NULL` (`UT` | `IT` | `ST` | `AT`)
    - `description TEXT NOT NULL`
    - `expected_result TEXT NOT NULL`
    - `status VARCHAR(20) NOT NULL DEFAULT 'UNTESTED'` (`UNTESTED` | `PASS` | `FAIL`)
    - `executed_by BIGINT REFERENCES users(id)`
    - + Audit & Soft-Delete columns

16. **`tracking_logs`** (Gộp 4 sổ theo dõi vận hành chuẩn mẫu Excel FPTU)
    - `id BIGSERIAL PRIMARY KEY`
    - `project_id BIGINT NOT NULL REFERENCES projects(id)`
    - `log_type VARCHAR(20) NOT NULL` (`ISSUE` | `DEFECT` | `QA` | `WEEKLY_MATTER`)
    - `code VARCHAR(20) NOT NULL` (`ISS-01`, `BUG-01`, `QA-01`, `MAT-01`)
    - `title VARCHAR(255) NOT NULL`
    - `description TEXT`
    - `priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'` (`HIGH` | `MEDIUM` | `LOW`) *(Cột bắt buộc của sheet Issues & Q&A)*
    - `potential_impact TEXT` *(Cột bắt buộc của sheet Issues)*
    - `screen_function_id BIGINT REFERENCES screens_functions(id)` *(Để Defect truy ngược ra Feature & Function/Screen ngay cả khi không gắn qua Test Case)*
    - `related_test_case_id BIGINT REFERENCES test_cases(id)`
    - `raised_by VARCHAR(150)`
    - `assignee_id BIGINT REFERENCES users(id)`
    - `status VARCHAR(20) NOT NULL DEFAULT 'OPEN'` (`OPEN` | `IN_PROGRESS` | `RESOLVED` | `CLOSED` | `PENDING`)
    - `closed_at TIMESTAMPTZ` *(Close Date cho sheet Issues; Open Date lấy từ `created_at`)*
    - `answer_or_resolution TEXT` *(GVHD hoặc Leader nhập câu trả lời/cách xử lý)*
    - + Audit & Soft-Delete columns

17. **`peer_reviews`** & **`document_change_logs`**
    - `peer_reviews`:
      - `id BIGSERIAL PRIMARY KEY`
      - `project_id BIGINT NOT NULL REFERENCES projects(id)`
      - `iteration VARCHAR(20) NOT NULL` (`ITER_1`..`ITER_4`)
      - `reviewer_id BIGINT NOT NULL REFERENCES users(id)`
      - `reviewee_id BIGINT NOT NULL REFERENCES users(id)`
      - `attitude_score INT NOT NULL CHECK (attitude_score BETWEEN 1 AND 10)` *(Tiêu chí 1: Thái độ)*
      - `timeliness_score INT NOT NULL CHECK (timeliness_score BETWEEN 1 AND 10)` *(Tiêu chí 2: Đúng hạn)*
      - `quality_score INT NOT NULL CHECK (quality_score BETWEEN 1 AND 10)` *(Tiêu chí 3: Chất lượng công việc)*
      - `comment TEXT`
      - `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
      - `UNIQUE (project_id, iteration, reviewer_id, reviewee_id)`
    - `document_change_logs`:
      - `id BIGSERIAL PRIMARY KEY`
      - `project_id BIGINT NOT NULL REFERENCES projects(id)`
      - `report_target VARCHAR(10) NOT NULL` (`R1`..`R7`)
      - `change_type VARCHAR(5) NOT NULL` (`A` | `M` | `D`)
      - `in_charge_user_id BIGINT REFERENCES users(id)`
      - `change_description TEXT NOT NULL`
      - `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`

---

# PART III: REST API SPECIFICATIONS (CORE ENDPOINTS)

Tất cả endpoint dưới `/api/projects/{projectId}/**` đều yêu cầu header `Authorization: Bearer <jwt>` và được kiểm tra quyền thành viên nhóm qua AOP.

### 7.1 Auth & Project Setup
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Đăng ký tài khoản sinh viên mới | Public |
| `POST` | `/api/auth/login` | Đăng nhập, trả về JWT Token & User Info | Public |
| `GET` | `/api/projects` | Lấy danh sách dự án của user hiện tại | Authenticated |
| `POST` | `/api/projects` | Tạo dự án mới + Auto-seed 10 Milestones & QualityTargets | Authenticated |
| `POST` | `/api/projects/{id}/members` | Thêm thành viên vào nhóm bằng Email | `LEADER` |
| `POST` | `/api/projects/{id}/magic-link/rotate` | Tạo mới / thu hồi Magic Link cho GVHD | `LEADER` |
| `GET` | `/api/projects/{id}/repositories` | Lấy danh sách GitHub repositories của dự án | Member |
| `POST` | `/api/projects/{id}/repositories` | Thêm repository mới (tự sinh `webhook_secret`, trả về Webhook URL `/api/webhooks/github/{repoId}`) | `LEADER` |
| `DELETE` | `/api/projects/{id}/repositories/{repoId}` | Gỡ (soft-delete) repository khỏi dự án | `LEADER` |

### 7.2 Milestones & File Storage
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/projects/{id}/milestones` | Lấy 10 cột mốc kèm danh sách Deliverables lũy tiến | Member |
| `POST` | `/api/projects/{id}/deliverables/{delivId}/upload` | Upload file báo cáo (`multipart/form-data`, max 50MB) | Member |
| `GET` | `/api/projects/{id}/files/download/{fileId}` | Tải file bảo mật (kiểm tra quyền thành viên/GVHD) | Member / Supervisor |

### 7.3 Planning (Report 2) & SRS (Report 3)
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET / POST` | `/api/projects/{id}/features` | Quản lý danh sách Features (`FE-xx`) | Member |
| `GET / POST` | `/api/projects/{id}/wbs-tasks` | Quản lý WBS & Kanban Tasks (tự tính Complexity) | Member |
| `PATCH` | `/api/projects/{id}/wbs-tasks/{taskId}/status` | Kéo thả trạng thái Kanban (`TODO` -> `DONE`) | Member |
| `GET / PUT` | `/api/projects/{id}/raci` | Lấy & cập nhật ma trận RACI (`D/R/S/I`) | Member |
| `GET / POST` | `/api/projects/{id}/actors` | Quản lý System Actors/Roles (phục vụ Report 3 & cột Auth Matrix) | Member |
| `GET / POST` | `/api/projects/{id}/use-cases` | Quản lý Use Cases (`UC-xx`) | Member |
| `GET / POST` | `/api/projects/{id}/screens` | Quản lý Screens/Functions & ma trận 8 quyền JSONB | Member |
| `PATCH` | `/api/projects/{id}/screens/{screenId}/auth-matrix` | Click-to-toggle ô phân quyền trên ma trận | Member |
| `GET / POST` | `/api/projects/{id}/srs-artifacts/{type}` | Quản lý `business-rules`, `messages`, `diagrams` | Member |
| `GET` | `/api/projects/{id}/traceability` | Xuất ma trận liên kết `FE -> UC -> Screen -> Task -> TC -> Bug` | Member |

### 7.4 Testing, Tracking, Analytics & Export
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET / POST` | `/api/projects/{id}/test-cases` | Quản lý Test Cases & cập nhật trạng thái `PASS/FAIL` | Member |
| `GET` | `/api/projects/{id}/quality-metrics` | Tính `Actual vs Target` Test Coverage & Defect Rate | Member |
| `GET / POST` | `/api/projects/{id}/tracking/{logType}` | Quản lý `ISSUE`, `DEFECT`, `QA`, `WEEKLY_MATTER` | Member |
| `GET / POST` | `/api/projects/{id}/peer-reviews` | Nộp & xem kết quả đánh giá chéo 3 tiêu chí theo Iteration | Member |
| `GET` | `/api/projects/{id}/analytics/contribution` | Thống kê Task, Man-days, PR merged & cảnh báo Free-rider | Member |
| `GET` | `/api/projects/{id}/export/tracking-xlsx` | Tải file Excel 5 sheets (`WBS`, `Issues`, `Defects`, `Q&A`, `TC`) | Member |
| `GET` | `/api/projects/{id}/export/weekly-xlsx` | Tải file Excel Báo cáo tuần chuẩn FPTU | Member |
| `GET` | `/api/projects/{id}/export/srs-md` | Tải file Markdown bảng biểu SRS & `Record of Changes` | Member |

### 7.5 Webhooks & Supervisor Magic Link
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/webhooks/github/{repoId}` | Nhận PR event từ GitHub theo từng repository (`X-Hub-Signature-256`), cập nhật `github_prs JSONB` & trạng thái Task | GitHub Webhook |
| `GET` | `/api/supervisor/{magicToken}/overview` | Lấy dữ liệu Read-only toàn dự án cho GVHD | Magic Token |
| `POST` | `/api/supervisor/{magicToken}/qa/{qaId}/answer` | GVHD gửi câu trả lời trực tiếp vào mục Q&A | Magic Token |
