# TASK BACKLOG & WORK BREAKDOWN (TASKS.md)
**Project:** AI Study Companion – Grade 6 (`SRS-ASC-G6-001`)  
**Rule:**
- Mỗi **Task cha** chứa **tối đa 3 subtasks**.
- Mỗi **subtask** chỉ có độ phức tạp **`2 SP`**, **`3 SP`**, hoặc **`5 SP`** (Fibonacci Agile, không vượt quá `5 SP`).
- `1 subtask = 1 atomic commit`, bắt buộc pass `typecheck`/`lint`/`build` (FE) hoặc `./mvnw.cmd test` (BE) trước khi commit.  
**Version:** 2.2 *(Max 3 Subtasks / Task · 2 / 3 / 5 Story Points)*

---

## Phase 0: Specification & Documentation Reset (`docs/`)

### Task 0 — Reset tài liệu hệ thống *(2 subtasks)*
- [x] `[TASK-DOC.1]` *(3 SP)* Rewrite `docs/PRD.md` & `docs/ARCHITECTURE.md` aligned 100% with `SRS-ASC-G6-001` v1.0 (`5` subjects, `REVIEW` $\rightarrow$ `PREVIEW`, max `3` post-evaluation revisions, Brave + MiMo/CKEY, 1-time 8-char Guardian link code)
- [x] `[TASK-DOC.2]` *(3 SP)* Rewrite `docs/DESIGN.md` (14-table ERD + REST APIs + Student/Guardian UI), `docs/RULES.md` (`BR-01..BR-24`), `docs/TASKS.md`, `docs/PROGRESS.md`, and `README.md`

---

## Phase 1: Legacy Domain Pruning & Package Rename (`com.asc.studycompanion`)

### Task 1 — Dọn mã nguồn cũ & đổi package sang `com.asc.studycompanion` *(3 subtasks)*
- [x] `[TASK-CLEAN.1]` *(2 SP)* Update `docker-compose.yml`, `.env.example`, and `application.yml` for `AI Study Companion` (`Asia/Ho_Chi_Minh`, Brave Search & CKEY/MiMo config; remove SEP490 storage/SMTP config)
- [x] `[TASK-CLEAN.2]` *(3 SP)* Rename Java package `com.fptu.sep490` $\rightarrow$ `com.asc.studycompanion` (`StudyCompanionApplication.java`, `pom.xml`), remove `module/project/**` & legacy SEP490 `.gitkeep` folders, update `ErrorCode.java`, and verify `./mvnw.cmd test` passes
- [x] `[TASK-CLEAN.3]` *(3 SP)* Remove legacy SEP490 frontend routes/components (`app/invite`, `app/(dashboard)/projects/**`, `components/projects/**`, `components/workspace/**`, SEP490 types & mocks), adapt `api-client.ts`, and verify `typecheck/lint/build`

---

## Phase 2: Frontend UI-First Prototype (`frontend/` — Contract-First Mock Services)

### Task 2 — Nền tảng UI Ocean Blue + White & Luồng Auth/Link *(2 subtasks)*
- [x] `[TASK-UI.1]` *(3 SP)* Build Ocean Blue (`#0284C7`/`#0369A1`) + White (`#FFFFFF`) theme tokens, strict Study Companion DTOs, `study-utils.ts` (`BR-12` char counter `850–2,000`, `Asia/Ho_Chi_Minh` next-day helper), `StatusBadge`, and `MockRuntime` (`EVALUATING`/`EVALUATION_FAILED`/`INSUFFICIENT_EVIDENCE`)
- [x] `[TASK-UI.2]` *(3 SP)* Build Auth & 1-Time Link Code Flow (`/login`, `/register` with `STUDENT`/`GUARDIAN` role selector, and `/link-student` 8-char code generator/acceptor per `SRS O-01`)

### Task 3 — Không gian Học sinh & Cổng Phụ huynh chỉ xem *(3 subtasks)*
- [x] `[TASK-UI.3]` *(3 SP)* Build Student Workspace Shell & Weekly Timetable (`/student/layout.tsx`, `/student/timetable` 7-day × 5-subject grid with history preservation & Minimal-Text UI)
- [x] `[TASK-UI.4]` *(5 SP)* Build Next-Day Plan & Review/Preview Submission Workspace (`/student/today`, `/student/tasks/[taskId]` — `REVIEW` locks `PREVIEW`, realtime `850–2,000` counter, async `EVALUATING` state, `<= 600` char AI feedback + sources, retry & max-3-revision flow)
- [x] `[TASK-UI.5]` *(3 SP)* Build Guardian Read-Only Dashboard (`/guardian/dashboard` daily/subject filters) & Immutable Submission Inspector (`/guardian/submissions/[submissionId]` version history, Accuracy/Completeness, incorrect/missing points, sources — no AI cost leaks) + Responsive/Light-Dark QA

---

## Phase 3: Database Baseline & Auth/Link Backend (`backend/`)

### Task 4 — Khởi tạo Database Schema 14 bảng (Flyway) *(2 subtasks)*
- [x] `[TASK-BE.1a]` *(3 SP)* Tạo Flyway `V1__init_auth_and_curriculum_schema.sql` (`users`, `student_profiles`, `guardian_links`, `subjects` + seed 5 môn lớp 6, `timetables`, `timetable_entries`) & kiểm tra PostgreSQL migration
- [x] `[TASK-BE.1b]` *(3 SP)* Tạo Flyway `V2__init_study_and_ai_schema.sql` (`study_plans`, `study_tasks`, `study_submissions`, `ai_evaluations`, `source_references`, `ai_usage_logs`, `knowledge_cache`, `evaluation_jobs`) & xác nhận JPA `ddl-auto=validate`

### Task 5 — Nâng cấp Auth, Mã liên kết 8 ký tự & Anti-IDOR Guard *(3 subtasks)*
- [x] `[TASK-BE.2a]` *(3 SP)* Nâng cấp module `auth` (`STUDENT`/`GUARDIAN`, tự tạo `StudentProfile` khi đăng ký Học sinh, kiểm tra email mở, `GET /api/auth/me` + trạng thái `GuardianLink`) & unit tests
- [x] `[TASK-BE.2b]` *(3 SP)* Xây dựng API mã liên kết 8 ký tự dùng 1 lần (`POST /api/links/code`, `POST /api/links/accept`, `GET /api/links/current`, hạn 24h, `AUTH_007`/`AUTH_008`) & unit tests
- [x] `[TASK-BE.2c]` *(2 SP)* Xây dựng `StudyAccessGuard` (chống IDOR cho Student owner & `GuardianLink` active) + `GET /api/subjects` & security tests

---

## Phase 4: Timetable, Study Plan & Submission Backend (`backend/`)

### Task 6 — Thời khóa biểu phiên bản & Kế hoạch ngày kế tiếp *(2 subtasks)*
- [x] `[TASK-BE.3]` *(3 SP)* Xây dựng module `timetable` (`Timetable`, `TimetableEntry`, `GET/PUT /api/student/timetable` cho phép cả `STUDENT` owner & `GUARDIAN` linked cập nhật phiên bản mới, lưu trữ bản cũ `BR-20`) & `PUT /api/student/profile` (chọn/đổi khối lớp)
- [x] `[TASK-BE.4a]` *(3 SP)* Xây dựng phần đọc kế hoạch học (`StudyPlan`, `StudyTask`, `GET /api/student/plans/next-day` theo `Asia/Ho_Chi_Minh`: hôm nay chuẩn bị cho lịch học ngày mai, mỗi môn 1 cặp `REVIEW` + `PREVIEW` & xử lý ngày trống `BR-04..BR-07`) & tests

### Task 7 — Nộp bài Review/Preview, Chuẩn độ dài & `v1..v3` *(3 subtasks)*
- [x] `[TASK-BE.4b]` *(2 SP)* Xây dựng `GET /api/student/tasks/{taskId}` + `StudyTextUtils` (`\r\n` $\rightarrow$ `\n`, `.trim()`, kiểm tra `850–2,000` ký tự `BR-12`) & unit tests
- [x] `[TASK-BE.4c]` *(5 SP)* Xây dựng `POST /api/student/tasks/{taskId}/submissions` (cổng `REVIEW` trước `PREVIEW` `BR-08`, tối đa 3 phiên bản `v1..v3` / `revisionCount <= 2`, khóa khi `EVALUATING`, chống trùng `Idempotency-Key` `BR-14`, lưu atomic `StudySubmission` + `PENDING` `EvaluationJob`) & tests
- [x] `[TASK-BE.4d]` *(2 SP)* Xây dựng `POST /api/student/submissions/{submissionId}/retry-evaluation` (hỗ trợ debug/re-trigger trước khi fallback hoàn tất) & tests

---

## Phase 5: Brave Search (KNTT + Wikipedia) + CKEY/MiMo AI Pipeline & Guardian Backend (`backend/`)

### Task 8 — Tích hợp Tìm kiếm KNTT/Wikipedia, Prompt không PII & MiMo/CKEY *(3 subtasks)*
- [x] `[TASK-BE.5a]` *(3 SP)* Xây dựng `SearchProvider` (`BraveSearchAdapter`: ưu tiên SGK **Kết nối tri thức với cuộc sống** đúng khối lớp của bé + **Wikipedia**, tối đa `2–3` nguồn tiết kiệm token, timeout/lỗi) & WireMock unit tests
- [x] `[TASK-BE.5b]` *(5 SP)* Xây dựng PII-free Prompt Builder & `AiEvaluationProvider` (`CkeyMimoAdapter`: ngân sách token gọn, strict JSON Schema parser + ràng buộc nhận xét `<= 600` ký tự) & unit tests
- [x] `[TASK-BE.6a]` *(2 SP)* Xây dựng `KnowledgeCache` service (tra cứu/lưu cache theo `grade` + `subjectCode` + `lessonTitle` đã chuẩn hóa với `cache_allowed` & TTL) & unit tests

### Task 9 — Async Worker (3-Retry Fallback) & Guardian Read-Only APIs *(2 subtasks)*
- [x] `[TASK-BE.6b]` *(5 SP)* Xây dựng Async `EvaluationJob` Worker (`@Scheduled`/bounded executor, exponential backoff tối đa `3` lần, quá 3 lần ghi `AIEvaluation` fallback báo lỗi hệ thống để phụ huynh tự kiểm tra và chuyển `EVALUATED` qua Guardian, lưu `AIUsageLog`) & integration tests
- [x] `[TASK-BE.6c]` *(3 SP)* Xây dựng module `guardian` Read-Only (`GET /api/guardian/dashboard` lọc ngày & môn, `GET /api/guardian/submissions/{submissionId}` lịch sử `v1..v3` + nguồn đối chiếu, không lộ `AIUsageLog`) & tests

---

## Phase 6: API Integration & Acceptance Verification (`AC-01`..`AC-20`)

### Task 10 — Tích hợp Full-Stack & Nghiệm thu 20 tiêu chí SRS *(3 subtasks)*
- [x] `[TASK-INT.1]` *(2 SP)* Chuyển `NEXT_PUBLIC_DATA_MODE=api`, rà soát CORS/JWT & khớp hợp đồng REST trên toàn bộ route Frontend
- [x] `[TASK-INT.2a]` *(2 SP)* Kiểm thử E2E luồng Auth, Link Code, Timetable Versioning và nộp bài `REVIEW` $\rightarrow$ `PREVIEW` (`AC-01`–`AC-10`)
- [x] `[TASK-INT.2b]` *(3 SP)* Chạy PoC kiểm định chất lượng đánh giá AI trên đủ 5 môn lớp 6, kiểm tra Guardian Read-Only & các tiêu chí còn lại (`AC-11`–`AC-20`) *(đã kiểm định đầy đủ bằng bộ test tự động MockMvc + MockRestServiceServer; staging thật cần bật Docker Desktop & `.env`)*

---

## Phase 7: Production Feature Expansion & `850–3,000` Char Upgrade

### Task 11 — Specs, `850–3,000` Chars, Flyway `V3` & Account/Multi-Child APIs *(3 subtasks)*
- [x] `[TASK-FEAT.1a]` *(2 SP)* Đồng bộ đặc tả `docs/` và nâng chuẩn độ dài bài làm `[850, 3000]` ký tự (`StudyTextUtils`, `ErrorCode`, `EvaluationPromptBuilder`, `study-utils.ts`) & unit tests
- [x] `[TASK-FEAT.1b]` *(3 SP)* Tạo Flyway `V3__expand_features_and_subjects.sql` (`char_count BETWEEN 850 AND 3000`, mở rộng môn học theo Khối `1–12`, `study_tasks` parent note/ack), cập nhật entity/`SubjectController`/`TimetableView` & tests
- [x] `[TASK-FEAT.1c]` *(3 SP)* Xây dựng API tài khoản (`PUT /api/auth/profile`, `PUT /api/auth/password`), hủy liên kết (`POST /api/links/unlink`) & chuyển đổi nhiều con (`GET /api/links/children`, `?studentId=`) + tests

### Task 12 — Student History, Streak, Auto-Save Draft & Profile UI *(3 subtasks)*
- [x] `[TASK-FEAT.2a]` *(3 SP)* Xây dựng Backend `GET /api/student/plans/history` & `GET /api/student/streak` trong `StudyPlanService`/`StudyPlanController` & unit tests
- [x] `[TASK-FEAT.2b]` *(3 SP)* Xây dựng giao diện `/profile` (sửa họ tên, khối lớp, đổi mật khẩu, ngắt liên kết / danh sách liên kết) & gắn điều hướng trên `StudentShell`/`GuardianShell`
- [ ] `[TASK-FEAT.2c]` *(3 SP)* Xây dựng `/student/history`, hiển thị Streak trên `/student/today`, và lưu nháp tự động `localStorage` trong `TaskWorkspaceView`

### Task 13 — Guardian Weekly Overview, Excel Export & Parent Note *(3 subtasks)*
- [ ] `[TASK-FEAT.3a]` *(3 SP)* Xây dựng Backend `POST /api/guardian/tasks/{taskId}/acknowledge`, `GET /api/guardian/weekly` & `GET /api/guardian/export` (Apache POI `.xlsx`) + unit tests
- [ ] `[TASK-FEAT.3b]` *(3 SP)* Xây dựng UI Phụ huynh bấm "Đã xem" + lời nhắn động viên, chọn chuyển giữa các con, Tổng quan tuần & tải Excel trên `/guardian/dashboard` + `/guardian/timetable`
- [ ] `[TASK-FEAT.3c]` *(3 SP)* Đồng bộ toàn bộ `MockServices` (Account, Multi-Child, History, Streak, Acknowledge, Weekly) tương thích `1:1` với API thật

### Task 14 — Rate Limiting, Ops Telemetry & Full Verification *(2 subtasks)*
- [ ] `[TASK-FEAT.4a]` *(3 SP)* Xây dựng `RateLimitFilter` (`/api/auth/login`, `/api/auth/register`, submissions -> `429 SYS_429`) & `GET /api/ops/usage` bảo vệ bằng `X-Ops-Secret` + tests
- [ ] `[TASK-FEAT.4b]` *(3 SP)* Kiểm thử toàn bộ Backend (`.\mvnw.cmd clean test`), Frontend (`tsc`, `lint`, `build`), Docker Compose build & cập nhật `docs/PROGRESS.md`
