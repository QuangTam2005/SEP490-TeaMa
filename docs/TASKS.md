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
- [x] `[TASK-FEAT.2c]` *(3 SP)* Xây dựng `/student/history`, hiển thị Streak trên `/student/today`, và lưu nháp tự động `localStorage` trong `TaskWorkspaceView`

### Task 13 — Guardian Weekly Overview, Excel Export & Parent Note *(3 subtasks)*
- [x] `[TASK-FEAT.3a]` *(3 SP)* Xây dựng Backend `POST /api/guardian/tasks/{taskId}/acknowledge`, `GET /api/guardian/weekly` & `GET /api/guardian/export` (Apache POI `.xlsx`) + unit tests
- [x] `[TASK-FEAT.3b]` *(3 SP)* Xây dựng UI Phụ huynh bấm "Đã xem" + lời nhắn động viên, chọn chuyển giữa các con, Tổng quan tuần & tải Excel trên `/guardian/dashboard` + `/guardian/timetable`
- [x] `[TASK-FEAT.3c]` *(3 SP)* Đồng bộ toàn bộ `MockServices` (Account, Multi-Child, History, Streak, Acknowledge, Weekly) tương thích `1:1` với API thật

### Task 14 — Rate Limiting, Ops Telemetry & Full Verification *(2 subtasks)*
- [x] `[TASK-FEAT.4a]` *(3 SP)* Xây dựng `RateLimitFilter` (`/api/auth/login`, `/api/auth/register`, submissions -> `429 SYS_429`) & `GET /api/ops/usage` bảo vệ bằng `X-Ops-Secret` + tests
- [x] `[TASK-FEAT.4b]` *(3 SP)* Kiểm thử toàn bộ Backend (`.\mvnw.cmd clean test`), Frontend (`tsc`, `lint`, `build`), Docker Compose build & cập nhật `docs/PROGRESS.md`

### Task 15 — Editor Integrity, Widescreen 24-inch, Streak, Subject, AI Policy & Stability Polish *(39 subtasks)*
- [x] `[TASK-FEAT.5a]` *(2 SP)* Xây dựng hệ thống chống copy/dán vào ô nội dung bài làm (`anti-paste.ts`, `TaskWorkspaceView`: chặn `paste`/`drop`/`contextmenu`/`Ctrl+V`/`beforeinput` & bulk-insert `delta > 30` giữ nguyên bộ gõ Telex/VNI) & đồng bộ `docs/`
- [x] `[TASK-FEAT.5b]` *(2 SP)* Tối ưu layout responsive màn hình rộng 24 inch (`max-w-[1600px]` cho `StudentShell`/`GuardianShell`/`AuthLayout`/`LinkStudentView`, `lg:` nav breakpoint chống dồn header, `2xl:grid-cols-2`/`8:4` cho `NextDayPlanView`/`StudyHistoryView`/`TaskWorkspaceView`/`SubmissionDetailView`/`ProfileView`)
- [x] `[TASK-FEAT.5c]` *(2 SP)* Cập nhật logic tính chuỗi học (`StudyPlanService.getStreak`) bỏ qua các ngày nghỉ không có môn trên thời khóa biểu `ACTIVE` (ví dụ T7, CN trống lịch) mà vẫn ngắt chuỗi khi bỏ lỡ ngày có lịch học & bổ sung unit tests (`StudyPlanServiceTest`)
- [x] `[TASK-FEAT.5d]` *(1 SP)* Bổ sung môn `Công nghệ` (`SubjectCode.TECH`, Khối `3–12`) qua Flyway `V4__add_tech_subject.sql`, `ALL_SUBJECTS`, test harness & đồng bộ `docs/`
- [x] `[TASK-FEAT.5e]` *(3 SP)* Nâng cấp Back-end AI Policy, System Prompt & phân hóa Rubric theo nhóm môn (`EvaluationPromptBuilder`: chống prompt injection bằng thẻ XML + anti-gaming + ma trận `REVIEW`/`PREVIEW` + giọng văn theo cấp học + rubric 13 môn; `CkeyMimoAdapter`: `max_tokens=950`, kiểm tra nhất quán enum chéo & giới hạn mảng điểm yếu; `BraveSearchAdapter`: tinh chỉnh query theo môn + unit tests)
- [x] `[TASK-FEAT.5f]` *(2 SP)* Sửa lỗi `500 / SYS_500` ở `GET /api/student/plans/next-day` (`StudyPlanService`: đối chiếu task tại chỗ + `flush()` trước khi thêm mới để tránh đụng `uq_study_tasks_plan_subject_type`, tách `REQUIRES_NEW` khi tạo mới đồng thời chống nhiễm độc transaction PostgreSQL & bổ sung unit test `StudyPlanServiceTest`)
- [x] `[TASK-FEAT.5g]` *(3 SP)* Hạ ngưỡng ký tự tối thiểu xuống `400` (`400–3.000` ký tự qua Flyway `V5`, `StudyTextUtils`, `ErrorCode`, `study-utils.ts`, `TaskWorkspaceView`) và bổ sung tính năng Phụ huynh đánh dấu **Bỏ qua / Khôi phục** (`SKIPPED`) ở từng bài Ôn/Soạn (`POST /api/guardian/tasks/{taskId}/skip`, tự mở khóa `PREVIEW` khi bỏ qua `REVIEW`, giữ Streak khi cả ngày `SKIPPED`, trừ khỏi mẫu số tuần/dashboard & đồng bộ `GuardianDashboardView`/`NextDayPlanView`/`TaskWorkspaceView`/MockServices/docs)
- [x] `[TASK-FEAT.5h]` *(3 SP)* Rà soát & vá toàn bộ lỗi phát sinh BE + FE (`CkeyMimoAdapter` giới hạn cứng `<=600` ký tự & `NullNode`; `OpsUsageController` nhận `"SUCCEEDED"`; `StudySubmissionService` chặn `retryEvaluation` sau fallback + kiểm tra độ dài header `Idempotency-Key <= 64`; `EvaluationJobWorker` tách `REQUIRES_NEW` từng job & upsert `AIEvaluation`; `StudyPlanService` hỗ trợ lọc lịch sử 1 đầu ngày; FE `useAuthStore.isHydrated` chống `403` & flash shell khi reload, empty-state Guardian chưa liên kết con, `queryClient.clear()` + reset `useGuardianStore` khi logout/login/unlink, vá `draftKey` race + revision kick-out trong `TaskWorkspaceView`, lọc `REVOKED`/hết hạn ở `LinkStudentView`, hiện badge `Đã xem` mobile & đồng bộ MockServices/error codes)
- [x] `[TASK-FEAT.5i]` *(1 SP)* Sửa lỗi tràn khung giao diện khi học sinh nhập chuỗi ký tự liên tiếp không khoảng trắng (`.......`) trên `SubmissionDetailView`, `TaskWorkspaceView` và `GuardianDashboardView` bằng `min-w-0`, `overflow-hidden`, `break-words [overflow-wrap:anywhere]`
- [x] `[TASK-FEAT.5j]` *(1 SP)* Tinh chỉnh badge trạng thái (`StatusBadge`: `SKIPPED` $\rightarrow$ `Bỏ qua`, `BLOCKED` $\rightarrow$ `Chưa mở`, `whitespace-nowrap`) và loại bỏ icon ở các badge kết quả AI & badge `Đã xem` trên `GuardianDashboardView`
- [x] `[TASK-FEAT.5k]` *(1 SP)* Tinh chỉnh prompt đánh giá AI (`EvaluationPromptBuilder`): cấm bắt lỗi vặt hình thức ghi chép (chữ hoa đầu dòng như `Cave`, dấu ngăn cách ghi chú `:`, `;`), buộc kiểm tra đúng nguyên văn trước khi chỉ lỗi để không gán nhầm sang từ khác (như `forest`), và không bác bỏ từ vựng/ý đúng thuộc phần khác trong cùng bài/Unit SGK (`EvaluationPromptBuilderTest`)
- [x] `[TASK-FEAT.5l]` *(1 SP)* Đổi tiêu đề cột `"Kết quả AI"` thành `"AI Đánh giá"` trên `GuardianDashboardView`
- [x] `[TASK-FEAT.5m]` *(1 SP)* Loại bỏ toàn bộ icon khỏi `StatusBadge` (mọi trạng thái, vai trò & đánh giá) và khỏi cột `Môn học` (`Ôn bài cũ`/`Soạn bài mới`) + nút thao tác (`Xem bài gốc`, `Bỏ qua`, `Khôi phục`) trên `GuardianDashboardView`
- [x] `[TASK-FEAT.5n]` *(5 SP)* Nâng cấp AI Mentor (`<= +10%` ngân sách token, Option B không gọi ngoài Wikipedia API): định danh `textbookSeries`/`textbookEdition` (`Global Success` / `Kết nối tri thức với cuộc sống` / `GDPT 2018`) trong `SearchQuery`, `AiEvaluationInput` & `KnowledgeCache v3`; bật `extra_snippets=true`, định tuyến nguồn theo môn (`ENG` → `Global Success`, `LIT` → Ngữ văn, lọc nguồn mạng xã hội/rác); nâng `maxContextTokens=2250` (`+9.86%`) & `max_tokens=1045` (`+10%`), `feedback VARCHAR(1000)` qua Flyway `V6` (`600–1.000` ký tự gồm `Nhận xét tổng quan` + `Cần khắc phục`), `StudyTextUtils`/`study-utils.ts` + `whitespace-pre-wrap` hiển thị FE & đồng bộ unit/integration tests (`99/99` BE, `14` FE routes)
- [x] `[TASK-FEAT.5o]` *(1 SP)* Bỏ ngưỡng ký tự tối thiểu cho nhận xét AI (`StudyTextUtils` / `study-utils.ts`: bỏ `MIN_FEEDBACK_CHARS`, `CkeyMimoAdapter`: bỏ nối câu đệm giữ nguyên nhận xét súc tích và chỉ giới hạn trần `<= 1.000` ký tự, cập nhật `EvaluationPromptBuilder` & `docs/` / tests)
- [x] `[TASK-FEAT.5p]` *(1 SP)* Chống AI bịa chi tiết/thuật ngữ ảo (`CkeyMimoAdapter`: hạ `temperature` `0.2 -> 0.0`; `EvaluationPromptBuilder`: quy tắc mọi trích dẫn/thuật ngữ khen chê phải tồn tại nguyên văn trong `<student_submission>` và không lặp máy móc cụm từ từ tiêu chí trọng tâm + `EvaluationPromptBuilderTest`)
- [x] `[TASK-FEAT.5q]` *(1 SP)* Cô đọng `buildSystemPrompt()` trong `EvaluationPromptBuilder` theo mẫu Mentor 5 mục (`5.781 -> 4.176` ký tự, tiết kiệm `~535` token/lượt gọi AI mà giữ nguyên đủ luật chống bịa, bảo mật, tiêu chí chấm & JSON <= 1.000 ký tự) và đồng bộ tests (`99/99` BE)
- [x] `[TASK-FEAT.5r]` *(1 SP)* Thu gọn tối đa **2 nguồn** tham khảo và tăng độ sâu mỗi nguồn lên **`1.000` ký tự**, nâng trần `<reference_sources>` `MAX_SOURCES_CHARS = 2.500` ký tự (tỉa đều cuối từ để không rớt nguồn thứ 2), nâng `KnowledgeCache` `v4` (`AiProperties`, `application.yml`, `BraveSearchAdapter`, `EvaluationPromptBuilder`, tests & docs)
- [x] `[TASK-FEAT.5s]` *(2 SP)* Kiểm định thực tế khả năng crawl của Brave trên 5 trang chính thống & các cổng K12: loại bỏ `hanhtrangso.nxbgd.vn` (SPA rỗng), `hoc10.vn` (SPA), `moet.gov.vn` (tin hành chính), bỏ ưu tiên `nxbgd.vn`/`gdpt.vn` & các trang chặn bot Cloudflare (`olm.vn`, `hoc247.net`, `haylamdo.com`), bổ sung & nâng điểm các cổng K12 HTML server-render giàu nội dung (`vietjack.com`, `khoahoc.vietjack.com`, `loigiaihay.com`, `vndoc.com`, `tech12h.com`, `vuihoc.vn`, `doctailieu.com`, `vi.wikipedia.org`, `tailieumoi.vn`, `quantrimang.com`, `toanmath.com`, `monkey.edu.vn`, `colearn.vn`, `hocgioi.vn`, `vi.wikibooks.org`, `vi.wiktionary.org`, `taphuan.nxbgd.vn`) trong `BraveSearchAdapter` + tests
- [x] `[TASK-FEAT.5t]` *(1 SP)* Loại bỏ hoàn toàn các trang chặn Cloudflare/anti-bot/timeout (`olm.vn`, `hoc247.net`, `haylamdo.com`, `thuvienhoclieu.com`, `hocmai.vn`, `baitap123.com`) khỏi kết quả chọn nguồn của `BraveSearchAdapter` + tests (`99/99` BE)
- [x] `[TASK-FEAT.5u]` *(1 SP)* Nâng giới hạn trích nguồn lên **`2.000` ký tự/nguồn** (`BraveSearchAdapter.MAX_SNIPPET_CHARS = 2000`), nâng tổng vùng nguồn **`MAX_SOURCES_CHARS = 4.500`** ký tự, nâng **`maxContextTokens = 2.700`** (`8.100` ký tự để không cắt bài làm `3.000` ký tự), nâng `KnowledgeCache` `v5`, đồng bộ docs (`RULES.md`, `PRD.md`, `ARCHITECTURE.md`) & tests (`99/99` BE)
- [x] `[TASK-FEAT.5v]` *(1 SP)* Rà soát cuối cùng toàn web: vá lỗi `UriComponentsBuilder` trong `BraveSearchAdapter` khi tên bài chứa ký tự `{...}` (`queryParam("q", "{q}").encode().buildAndExpand(searchText)`), kiểm chứng `100/100` BE tests & `typecheck`/`lint`/`build` Frontend (`14` routes)
- [x] `[TASK-FEAT.5w]` *(2 SP)* Tối ưu trải nghiệm di động (`360–640px`): chống iOS zoom `text-base md:text-sm` + `h-10`/`h-11` cho `Input`/textarea/nút phiên bản, hiện tên/lớp trong drawer mobile (`StudentShell`/`GuardianShell`), ưu tiên hiển thị `Đánh giá AI` lên trên khi xem kết quả trên mobile (`TaskWorkspaceView`/`SubmissionDetailView`), thanh lọc môn cuộn ngang gọn trên `GuardianDashboardView` & tối ưu chiều cao editor khi bật bàn phím ảo (`tsc`, `lint`, `next build` `14/14` routes)
- [x] `[TASK-FEAT.5x]` *(1 SP)* Vá phản hồi AI (`EvaluationPromptBuilder`: giữ đúng xưng hô `"con"`/`"em"`, cấm gọi bài làm là `"Đề bài"`, cấm viết `[]`/`null` trong `feedback`, chấm công tâm hơn với câu trả lời `PREVIEW` còn sơ sài hoặc thiếu từ làm hụt nghĩa; `CkeyMimoAdapter.sanitizeMentorFeedback`: tự động dọn `Cần khắc phục: []` & bổ sung đầu mục `"Nhận xét tổng quan:"` nếu thiếu) + tests (`100/100` BE)
- [x] `[TASK-FEAT.5y]` *(1 SP)* Nâng chất lượng nhận xét AI chuẩn mentor cụ thể (`EvaluationPromptBuilder`: cấm văn mẫu sáo rỗng, bắt buộc bóc tách từng ý/từng câu đối chiếu trực tiếp với `<student_submission>` và `<reference_sources>`, bổ sung mẫu văn phong 350–750 ký tự; `CkeyMimoAdapter`: nâng `temperature` từ `0.0` lên `0.15` để giữ chặt kiểm chứng chống bịa nhưng diễn đạt tự nhiên hơn) + `EvaluationPromptBuilderTest` (`100/100` BE)
- [x] `[TASK-FEAT.5z]` *(2 SP)* Bổ sung kiểm tra **độ phủ nội dung chính của bài học**: cập nhật `BraveSearchAdapter` (truy vấn trọng tâm `"nội dung chính kiến thức trọng tâm"` / `"câu hỏi đọc hiểu"`, bỏ tiền tố `"Văn bản một/hai/ba..."`, ưu tiên trang tóm tắt nội dung chính thay vì chỉ tiểu sử `tác giả tác phẩm`), nâng `KnowledgeCache` lên `v6`, mở rộng `EvaluationPromptBuilder` với mục bắt buộc **`Mức độ đủ nội dung chính:`** (đối chiếu bài làm với toàn bộ ý trọng tâm của bài từ `<reference_sources>` chứ không chỉ chấm đúng/sai phần đã viết) & đồng bộ `CkeyMimoAdapter` (`missingPoints` non-empty tự động hạ `INCOMPLETE`/`NEEDS_IMPROVEMENT`), `docs/`, tests (`101/101` BE)
- [x] `[TASK-FEAT.5aa]` *(2 SP)* Sửa lỗi Guardian Dashboard sang ngày mới không hiển thị các môn cần soạn trước khi Student mở/bắt đầu làm bài: tách `StudyPlanService.ensurePlanForStudentAndDate(...)` dùng chung và tự động khởi tạo/đồng bộ kế hoạch chưa bắt đầu cho ngày hiện tại & ngày mai trong `GuardianService` (`getDashboard`, `getWeeklySummary`, `exportExcelReport`) + đồng bộ `InMemoryStudyHarness` & `GuardianServiceTest` (`102/102` BE)
- [x] `[TASK-FEAT.6a]` *(3 SP)* Phát triển tính năng Student gửi yêu cầu **Xin bỏ qua (`REVIEW`/`PREVIEW`)** kèm lý do (`5–300` ký tự) để Phụ huynh xem xét phê duyệt: Flyway `V7__student_skip_requests.sql`, `SkipRequestStatus` (`PENDING`/`APPROVED`/`REJECTED`), `POST/DELETE /api/student/tasks/{taskId}/skip-request`, `POST /api/guardian/tasks/{taskId}/skip-request/review`, tự động hủy đơn `PENDING` khi Student nộp bài, đồng bộ DTO/HTTP & Mock Services, `SkipRequestDialog` (chọn nhanh lý do phổ biến + nhập tự do), `NextDayPlanView`/`TaskWorkspaceView` & banner/duyệt nhanh trên `GuardianDashboardView` (`105/105` BE tests, `tsc`/`lint`/`build` `14/14` FE routes)
- [x] `[TASK-FEAT.6b]` *(5 SP)* Bổ sung tính năng **Guardian giao nhiệm vụ thay thế kèm thư mục Google Drive**: Flyway `V8__guardian_custom_tasks_and_drive.sql`, `TaskAssignmentMode` (`STANDARD_AI`/`CUSTOM_DRIVE`), lưu `default_drive_folder_url` theo liên kết `guardian_links`; `POST/DELETE /api/guardian/tasks/{taskId}/custom-task` & `POST /verify` (`EVALUATED`/`IN_PROGRESS`), `POST /api/student/tasks/{taskId}/custom-submission` (miễn `400` ký tự & `$0` AI, tự mở khóa `PREVIEW`), đồng bộ Excel report, `AssignCustomTaskDialog`, `GuardianDashboardView`, `NextDayPlanView`, `TaskWorkspaceView` & Mock/HTTP services (`107/107` BE tests, `tsc`/`lint`/`build` `14/14` FE routes)
- [x] `[TASK-FEAT.6c]` *(2 SP)* Gia cố toàn bộ điểm biên sau kiểm thử 2 agent BE + FE: `GuardianService.assertTaskHasNoSubmission` chặn thao tác ghi đè/bỏ qua khi đã có `customSubmittedAt != null`, `StudyTaskService.normalizeOptionalHttpUrl` kiểm tra chuẩn `https://` + `<= 500` ký tự, dọn `BraveSearchAdapter.executeBraveRequest`, mở rộng `FlywayMigrationSchemaTest` (`V7`–`V8`) & Anti-IDOR/`VAL_001` tests; FE đồng bộ invalidation (`guardian-dashboard`, `guardian-weekly`, `student-history`, `student-task`), prefill ghi chú/link Drive khi làm lại bài, sửa `<select>` `text-base md:text-sm` + nút `h-11` mobile & parity Mock Services (`111/111` BE tests, `tsc`/`lint`/`build` `14/14` FE routes)
- [x] `[TASK-FEAT.6d]` *(1 SP)* Tự host bộ font `Be Vietnam Pro` và `JetBrains Mono` (`@fontsource` `.woff2` `vietnamese` + `latin`, weights `400, 500, 600, 700` kèm `unicode-range` trong `frontend/src/app/fonts/fonts.css`) thay cho `next/font/google` trong `frontend/src/app/layout.tsx` + `frontend/src/app/globals.css`, giúp `next build` trong Docker VPS chạy offline 100% không cần truy cập `fonts.googleapis.com` (`typecheck`, `lint`, `build` `14/14` routes)
- [x] `[TASK-FEAT.6e]` *(1 SP)* Tinh gọn nội dung giải thích/ghi chú thừa chuẩn Minimal-Text trên `AssignCustomTaskDialog`, `SkipRequestDialog`, `GuardianDashboardView` và `TaskWorkspaceView` (`CUSTOM_DRIVE`), thu gọn chip chọn nhanh thành pill 1 dòng và bỏ card hướng dẫn 3 bước dư thừa (`typecheck`, `lint`, `build` `14/14` routes)
- [x] `[TASK-FEAT.6f]` *(1 SP)* Đổi trạng thái `Nộp qua Drive (không AI)` ở cột `AI Đánh giá` trên `GuardianDashboardView` thành badge `Manual`, đồng thời gộp các nút cột `Thao tác` lên 1 hàng ngang gọn (`flex-nowrap`, `h-8 px-2.5 text-xs`, bỏ icon thừa) (`typecheck`, `lint`, `build` `14/14` routes)
- [x] `[TASK-FEAT.6g]` *(1 SP)* Đổi nhãn nút xin bỏ qua trên card nhiệm vụ `NextDayPlanView` từ `Xin phụ huynh bỏ qua...` thành `Xin PH bỏ qua phần này...` (`typecheck`, `lint`, `build` `14/14` routes)
- [x] `[TASK-FEAT.6h]` *(2 SP)* Chuyển link thư mục Google Drive khi Guardian giao nhiệm vụ thay thế sang **tùy chọn** (`GuardianAssignCustomTaskRequest` bỏ `@NotBlank`, `GuardianService.normalizeOptionalDriveFolderUrl` cho phép `null`/trống và không ghi đè `defaultDriveFolderUrl` khi rỗng; FE thêm gợi ý `"Xem video bài học"`, ẩn ô lưu mặc định khi bỏ trống link, điều chỉnh nhãn `PH giao` / `"Xác nhận đã hoàn thành"` trên `GuardianDashboardView`, `NextDayPlanView`, `TaskWorkspaceView` & `guardian.mock-service.ts`) (`111/111` BE tests, `tsc`/`lint`/`build` `14/14` FE routes)
- [x] `[TASK-FEAT.6i]` *(1 SP)* Vá lỗi kẹt luồng khi bài thay thế đã nộp từng có `customSubmittedAt != null` và bị Guardian `"Nhắc làm lại"` (`IN_PROGRESS`): bổ sung kiểm tra `task.getCustomSubmittedAt() != null` trong `StudyTaskService.requestSkip` + unit test trong `StudyTaskServiceTest`, chặn `!task.customSubmittedAt` ở `NextDayPlanView.canRequestSkip`, `!row.currentSubmissionId && !row.customSubmittedAt` ở `GuardianDashboardView.canToggleSkip`, và đồng bộ `study.mock-service.ts` (`111/111` BE tests, `tsc`/`lint`/`build` `14/14` FE routes)
- [x] `[TASK-FEAT.6j]` *(1 SP)* Đồng bộ toàn bộ trải nghiệm chi tiết cho bài thay thế & xin bỏ qua: tái sử dụng `skipStatusBanners` (`PENDING`/`REJECTED`) và badge `Chờ PH duyệt bỏ qua` trong nhánh `isCustomDrive` của `TaskWorkspaceView`, bổ sung `customSubmissionUrl` (`Link bài`) vào card mobile trên `GuardianDashboardView`, chuẩn hóa `isValidHttpsUrl` (`study-utils.ts`) cho `AssignCustomTaskDialog` & `TaskWorkspaceView`, và tách nhãn thông báo/Excel (`GuardianService.exportExcelReport`) theo việc bài có gắn Drive URL hay làm thủ công (`111/111` BE tests, `tsc`/`lint`/`build` `14/14` FE routes)
- [x] `[TASK-FEAT.6k]` *(2 SP)* Chống lỗi `5xx` khi thao tác song song (race condition): khóa hàng `PESSIMISTIC_WRITE` trên `StudentProfile` trong `StudyPlanService.ensurePlanForStudentAndDate` & `TimetableService.updateTimetable`, khóa `StudyTask` + `refresh(task)` trong `StudySubmissionService.submitTask`, đồng thời bổ sung handler `DataIntegrityViolationException`/`ConcurrencyFailureException` (`409 SYS_409`), `HttpRequestMethodNotSupportedException` (`405 SYS_405`), thiếu param/header (`400 VAL_001`) vào `GlobalExceptionHandler` + `ErrorCode` + `api.ts` (`112/112` BE tests, `tsc`/`lint`/`build` `14/14` FE routes)
- [x] `[TASK-FEAT.6l]` *(3 SP)* Đơn giản hóa nộp bài thay thế (`CUSTOM_DRIVE`) thành xác nhận 1 chạm ngay trên `NextDayPlanView` (bỏ bắt buộc ghi chú/link trên BE `StudentCustomSubmitRequest` + `StudyTaskController` + `StudyTaskService`, `study.mock-service.ts`, tối giản `CustomTaskWorkspace`) và tách module cho `GuardianDashboardView` (`guardian-task-actions.tsx`, `guardian-task-table.tsx`, `guardian-weekly-overview.tsx`, `guardian-note-dialogs.tsx`) & `TaskWorkspaceView` (`ai-evaluation-panel.tsx`, `task-skip-banners.tsx`, `custom-task-workspace.tsx`, `study-essay-editor.tsx`) (`112/112` BE tests, `tsc`/`lint`/`build` `14/14` FE routes)

### Task 17 — Trắc nghiệm đầu ngày 2 lựa chọn (Bank, Cổng xoay vòng & Thống kê Phụ huynh) *(3 subtasks)*
- [x] `[TASK-FEAT.7a]` *(5 SP)* Xây dựng Backend module `quiz` + Flyway `V9__daily_guardian_quiz.sql` (`daily_quiz_settings`, `guardian_quiz_questions`, `daily_quiz_sessions`, `daily_quiz_session_items`), thuật toán xoay vòng tuần tự (`1–10 -> 11–20 -> 21–25 -> 1–10`) theo lịch thứ `T2–CN`, ẩn đáp án trước khi chọn, trả lời tuần tự từng câu, `PESSIMISTIC_WRITE` chống race condition, CRUD/Bulk-Add & Thống kê Guardian (`/api/guardian/quiz/**`, `/api/student/daily-quiz/**`) + unit/controller/schema tests (`120/120` BE tests)
- [x] `[TASK-FEAT.7b]` *(3 SP)* Xây dựng Frontend DTOs/Services (`quiz.service.ts` HTTP + Mock) và cổng trắc nghiệm đầu ngày bắt buộc cho Student (`DailyQuizGate` trong `StudentShell`: làm từng câu -> Đúng/Sai + giải thích ngay -> hoàn thành mở `/student/today`) (`tsc`/`lint`/`build` `14/14` FE routes)
- [ ] `[TASK-FEAT.7c]` *(5 SP)* Xây dựng cổng quản lý Bank câu hỏi, cài đặt hiển thị & trang thống kê `/guardian/quiz` (`GuardianQuizView`, `GuardianQuizSettingsPanel`, `GuardianQuizBankPanel` + Bulk Add, `GuardianQuizStatsPanel` + biểu đồ & chi tiết phiên làm bài)

---

## Phase 8: Production Ubuntu VPS Deployment (5-Phase Rollout, Zero Impact on `skillswap-*`)

### Task 16 — VPS Pre-Flight Audit & Preparation *(Phase 1)*
- [x] `[TASK-OPS.1]` *(2 SP)* Tạo kế hoạch 5 Phase & tài liệu thực thi chi tiết Phase 1 ([`PHASE_1_PREFLIGHT.md`](./deployment/PHASE_1_PREFLIGHT.md)) + read-only pre-flight script ([`phase1-preflight-check.sh`](./deployment/scripts/phase1-preflight-check.sh)) & `.gitattributes` (LF cho `.sh`)
