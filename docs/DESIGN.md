# SYSTEM DESIGN: UI/UX, DATABASE SCHEMA (ERD) & REST API SPECIFICATIONS
**Product Name:** AI Study Companion – Grade 6 (`SRS-ASC-G6-001`)  
**Design Philosophy:** Minimal-Text, Child-Friendly Clarity (`Be Vietnam Pro` + `JetBrains Mono`, `#E85D04` Burnt Orange, `1px hairline` Light/Dark)  
**Database Engine:** PostgreSQL 16 (Managed via Flyway Migrations)  
**Document Version:** 1.0 (Baseline — 14 Tables + REST APIs + Student/Guardian UI)  

---

# PART I: UI/UX DESIGN SYSTEM

## 1. Visual Identity & Color Palette
- **Entry Route (`/`):** Redirect trực tiếp sang `/login` (không có Landing Page marketing).
- **Minimal-Text Discipline:** Cấm viết các đoạn văn giải thích dài dòng dưới tiêu đề trang, thẻ thống kê hoặc header form. Ngôn ngữ thân thiện, rõ ràng, **không mang tính trách phạt** cho học sinh lớp 6 (`NFR-ACC-01`).
- **Light / Dark Mode:** Mặc định Light Mode (`#F8F9FA` canvas, `#FFFFFF` card, viền `1px #E5E7EB`, chữ chính `#1A1D23`), hỗ trợ nút chuyển Light/Dark trên Top Bar (`next-themes`).
- **Single Brand Accent (`< 5%`):** `#E85D04` (`hover #C84E03`, soft tint `#FFF4ED`), cấm gradient tím/lavender.
- **Semantic Status Colors (Accuracy / Completeness / Overall / Task Status):**
  - `GOOD` / `CORRECT` / `COMPLETE` / `EVALUATED`: Emerald (`#047857` trên `#ECFDF5`)
  - `NEEDS_IMPROVEMENT` / `PARTIALLY_CORRECT` / `INCOMPLETE` / `EVALUATING`: Amber (`#B45309` trên `#FFFBEB`)
  - `INCORRECT` / `EVALUATION_FAILED`: Rose (`#B91C1C` trên `#FEF2F2`)
  - `SUBMITTED` / `IN_PROGRESS`: Blue (`#1D4ED8` trên `#EFF6FF`)
  - `NOT_STARTED` / `BLOCKED` / `INSUFFICIENT_EVIDENCE` / `UNVERIFIABLE` / `UNDETERMINED`: Slate (`#475569` trên `#F1F5F9`)

## 2. Typography & Iconography
- **Primary Font:** `Be Vietnam Pro` (`subsets: ['vietnamese', 'latin']`).
- **Monospace Font:** `JetBrains Mono` (`tabular-nums`) cho bộ đếm ký tự (`850 / 2,000`), mã môn (`MATH`, `LIT`, `ENG`, `SCI`, `HISGEO`), phiên bản (`v1`..`v4`), mã liên kết (`8` ký tự) và ngày mục tiêu.
- **Icons:** Duy nhất `lucide-react` (`strokeWidth={1.75}`), kèm `Shadcn Tooltip`.

## 3. Application Shell & Screens (`SRS §8.1`)
1. **Auth & Account Linking:**
   - `/login`, `/register` (chọn vai trò `STUDENT` hoặc `GUARDIAN`, email bất kỳ hợp lệ).
   - `/link-student`: Tạo / nhập **mã liên kết 8 ký tự dùng 1 lần** để gắn `Guardian` với `StudentProfile`.
2. **Student Workspace (`/student/*`):**
   - Header cố định gọn: Brand, 2 tab điều hướng (`Hôm nay & Ngày mai`, `Thời khóa biểu`), trạng thái liên kết Guardian, `<ThemeToggle />`, `Đăng xuất`.
   - `/student/today`: Ngày mục tiêu (`Asia/Ho_Chi_Minh`), danh sách môn theo lịch (hoặc Empty State *"Không có môn học theo thời khóa biểu"*), mỗi môn gồm 2 nhiệm vụ tuần tự **`Ôn bài cũ (Review)`** và **`Soạn bài mới (Preview)`**.
   - `/student/timetable`: Lưới **7 ngày (`T2`–`CN`) × 5 môn cố định**, lưu phiên bản lịch hiện hành.
   - `/student/tasks/[taskId]`:
     - Nhập **Tựa bài** + **Nội dung** kèm bộ đếm ký tự realtime `850–2,000` (chuẩn hóa newline + trim), chặn Submit ngoài biên, không gọi AI khi đang gõ.
     - Trạng thái bất đồng bộ: `Đang AI đánh giá`, `Chưa thể đánh giá – có thể thử lại`, `Đã có kết quả`.
     - Hiển thị kết quả AI (`Accuracy`, `Completeness`, `overall_status`, nhận xét ngắn `<= 600` ký tự, nguồn URL) và nút **`Viết bản mới (Revision)`** (hiển thị bộ đếm `Đã sửa X / 3 lần`).
3. **Guardian Portal (`/guardian/*`):**
   - Header chế độ giám sát chỉ đọc + `<ThemeToggle />`.
   - `/guardian/dashboard`: Tổng quan hoàn thành theo ngày/tuần, bộ lọc ngày & môn học, danh sách bài nộp Review/Preview của Student đã liên kết.
   - `/guardian/submissions/[submissionId]`: Xem nguyên văn bất biến từng phiên bản bài Student viết (`v1..v4`), `Accuracy`, `Completeness`, `overall_status`, `feedback`, `incorrect_points`, `missing_points`, URL nguồn + `retrieved_at`. Không hiển thị `AIUsageLog`.

---

# PART II: DATABASE SCHEMA (14 CORE TABLES — `SRS §7.1`)

1. **`users`**: `id`, `email UNIQUE`, `password_hash`, `full_name`, `role` (`STUDENT` | `GUARDIAN`), `status` (`ACTIVE` | `DISABLED`), `created_at`, `updated_at`.
2. **`student_profiles`**: `id`, `user_id UNIQUE REFERENCES users(id)`, `display_name VARCHAR(100)`, `grade INT NOT NULL DEFAULT 6`, `timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh'`, `created_at`, `updated_at`.
3. **`guardian_links`**: `id`, `guardian_user_id REFERENCES users(id)`, `student_id REFERENCES student_profiles(id)`, `link_code VARCHAR(16) UNIQUE`, `status` (`PENDING` | `ACTIVE` | `REVOKED`), `expires_at`, `linked_at`, `created_at`.
4. **`subjects`**: `id`, `code VARCHAR(20) UNIQUE` (`MATH`, `LIT`, `ENG`, `SCI`, `HISGEO`), `name VARCHAR(100)`, `is_active BOOLEAN DEFAULT TRUE`.
5. **`timetables`**: `id`, `student_id REFERENCES student_profiles(id)`, `effective_date DATE`, `version INT`, `status` (`ACTIVE` | `ARCHIVED`), `updated_by_user_id REFERENCES users(id)`, `created_at`, `updated_at`.
6. **`timetable_entries`**: `id`, `timetable_id REFERENCES timetables(id)`, `weekday INT` (`1..7`, Mon–Sun), `subject_id REFERENCES subjects(id)`, `period_note VARCHAR(120)`.
7. **`study_plans`**: `id`, `student_id REFERENCES student_profiles(id)`, `target_date DATE`, `timetable_id REFERENCES timetables(id)`, `created_at`, `UNIQUE (student_id, target_date)`.
8. **`study_tasks`**: `id`, `plan_id REFERENCES study_plans(id)`, `subject_id REFERENCES subjects(id)`, `task_type VARCHAR(20)` (`REVIEW` | `PREVIEW`), `status VARCHAR(30)` (`NOT_STARTED` | `IN_PROGRESS` | `SUBMITTED` | `EVALUATING` | `EVALUATED` | `EVALUATION_FAILED` | `BLOCKED`), `revision_count INT DEFAULT 0`, `created_at`, `updated_at`, `UNIQUE (plan_id, subject_id, task_type)`.
9. **`study_submissions`**: `id`, `task_id REFERENCES study_tasks(id)`, `version INT NOT NULL`, `is_current BOOLEAN NOT NULL DEFAULT TRUE`, `lesson_title VARCHAR(255) NOT NULL`, `content TEXT NOT NULL`, `char_count INT NOT NULL`, `idempotency_key VARCHAR(64) UNIQUE`, `submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`.
10. **`ai_evaluations`**: `id`, `submission_id UNIQUE REFERENCES study_submissions(id)`, `accuracy VARCHAR(30)`, `completeness VARCHAR(30)`, `overall_status VARCHAR(30)`, `feedback VARCHAR(600)`, `incorrect_points JSONB`, `missing_points JSONB`, `confidence VARCHAR(20)`, `raw_sanitized_response JSONB`, `evaluated_at TIMESTAMPTZ`.
11. **`source_references`**: `id`, `evaluation_id REFERENCES ai_evaluations(id)`, `title VARCHAR(255)`, `url VARCHAR(1000)`, `snippet TEXT`, `retrieved_at TIMESTAMPTZ`.
12. **`ai_usage_logs`**: `id`, `job_id`, `provider VARCHAR(50)`, `model VARCHAR(80)`, `input_tokens INT`, `output_tokens INT`, `search_count INT`, `cache_hit BOOLEAN`, `estimated_cost_usd NUMERIC(10,6)`, `latency_ms INT`, `status VARCHAR(30)`, `error_code VARCHAR(50)`, `created_at`.
13. **`knowledge_cache`**: `id`, `cache_key VARCHAR(255) UNIQUE`, `grade INT DEFAULT 6`, `subject_code VARCHAR(20)`, `normalized_lesson_title VARCHAR(255)`, `sources_json JSONB`, `cache_allowed BOOLEAN`, `policy_version VARCHAR(20)`, `created_at`, `expires_at TIMESTAMPTZ`.
14. **`evaluation_jobs`**: `id`, `submission_id UNIQUE REFERENCES study_submissions(id)`, `idempotency_key VARCHAR(64) UNIQUE`, `status VARCHAR(30)` (`PENDING` | `RUNNING` | `SUCCEEDED` | `FAILED`), `attempts INT DEFAULT 0`, `max_attempts INT DEFAULT 3`, `next_retry_at TIMESTAMPTZ`, `last_error TEXT`, `created_at`, `updated_at`.

---

# PART III: REST API CONTRACTS

### 1. Auth & Linking
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Đăng ký tài khoản (`STUDENT` hoặc `GUARDIAN`) | Public |
| `POST` | `/api/auth/login` | Đăng nhập, trả JWT + User & Profile | Public |
| `GET` | `/api/auth/me` | Lấy thông tin người dùng hiện tại + trạng thái `GuardianLink` | Authenticated |
| `POST` | `/api/links/code` | Tạo mã liên kết 8 ký tự dùng 1 lần (hết hạn sau 24h) | Authenticated |
| `POST` | `/api/links/accept` | Nhập mã liên kết để ghép `Guardian` $\leftrightarrow$ `Student` | Authenticated |

### 2. Timetable & Next-Day Study Plan (Student)
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/subjects` | Lấy 5 môn học cố định lớp 6 | Authenticated |
| `GET / PUT` | `/api/student/timetable` | Lấy / lưu phiên bản thời khóa biểu tuần hiện hành | `STUDENT` |
| `GET` | `/api/student/plans/next-day` | Lấy (hoặc tự tạo) kế hoạch ngày kế tiếp theo `Asia/Ho_Chi_Minh` | `STUDENT` |
| `GET` | `/api/student/tasks/{taskId}` | Chi tiết nhiệm vụ, các phiên bản Submission & Evaluation hiện hành | `STUDENT` (Owner) |
| `POST` | `/api/student/tasks/{taskId}/submissions` | Submit bài (`850–2,000` chars, kiểm tra `REVIEW` trước `PREVIEW`, max 3 revisions sau đánh giá, `Idempotency-Key`) | `STUDENT` (Owner) |
| `POST` | `/api/student/submissions/{submissionId}/retry-evaluation` | Thử đánh giá lại khi job bị `EVALUATION_FAILED` | `STUDENT` (Owner) |

### 3. Guardian Read-Only Dashboard (`GUARDIAN`)
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/guardian/dashboard` | Tổng quan theo ngày/tuần, lọc theo `date` & `subjectCode` (chỉ Student đã liên kết) | `GUARDIAN` |
| `GET` | `/api/guardian/submissions/{submissionId}` | Chi tiết bài viết gốc bất biến, lịch sử phiên bản, kết quả AI & nguồn tham khảo | `GUARDIAN` (Linked) |
