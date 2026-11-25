# PRODUCT REQUIREMENTS DOCUMENT (PRD)
**Product Name:** AI Study Companion – Grade 6 (`SRS-ASC-G6-001`)  
**Target Domain:** Hỗ trợ học sinh lớp 6 tự học theo thời khóa biểu & giám sát tiến độ bởi Phụ huynh/Người giám sát (`Guardian`)  
**Document Version:** 1.0 (Baseline — Synced with `SRS-ASC-G6-001` & MVP Decisions)  

---

## 1. Product Overview & Vision
### 1.1 Problem Statement
Học sinh lớp 6 thường gặp 4 khó khăn trong quá trình tự học hằng ngày:
1. Dễ quên kiến thức buổi học trước, khó kết nối mạch kiến thức liên môn.
2. Ít chuẩn bị/đọc trước bài mới trước khi lên lớp.
3. Có thể hiểu sai hoặc thiếu ý trọng tâm nhưng không tự phát hiện ra.
4. Người giám sát (`Guardian`) khó nắm được thực chất học sinh đã tự viết/tự hiểu những gì thay vì chỉ nhìn điểm số bề mặt.

### 1.2 Product Vision
Xây dựng nền tảng web responsive **AI Study Companion – Grade 6** với định hướng cốt lõi: **Student tự học và tự trình bày; AI kiểm chứng từ nguồn Internet và phản hồi ngắn gọn; Guardian theo dõi minh bạch trên Dashboard**.  
Ứng dụng **không** làm bài thay học sinh, **không** chấm điểm tổng kết hay xếp hạng thay nhà trường, và **không** mặc định mọi kết quả Internet là đúng khi thiếu căn cứ.

---

## 2. Target Users & Roles (RBAC)
1. **Student (Học sinh lớp 6):**
   - Nhập và cập nhật **Thời khóa biểu tuần** gồm **5 môn cố định** (`Toán`, `Ngữ văn`, `Tiếng Anh`, `Khoa học tự nhiên`, `Lịch sử – Địa lý`).
   - Xem kế hoạch học tập cho **ngày lịch kế tiếp** theo múi giờ `Asia/Ho_Chi_Minh`.
   - Thực hiện tuần tự cặp nhiệm vụ **Review (Ôn bài cũ)** trước $\rightarrow$ **Preview (Soạn bài mới)** sau, tự viết kiến thức từ `850` đến `2.000` ký tự.
   - Xem kết quả AI đánh giá (`Accuracy`, `Completeness`, `overall_status`, nhận xét `<= 600` ký tự, nguồn tham khảo) và có thể **cập nhật bản sửa (Revision) tối đa 3 lần** sau khi AI đánh giá xong.
2. **Guardian (Người giám sát / Phụ huynh):**
   - Liên kết với hồ sơ Student thông qua **mã liên kết dùng 1 lần (8 ký tự)** (`O-01`).
   - Xem **Guardian Dashboard** ở chế độ chỉ đọc: tiến độ theo ngày/tuần, lịch sử từng phiên bản bài viết gốc bất biến của Student, đánh giá chi tiết của AI (`incorrect_points`, `missing_points`, URL nguồn & thời điểm truy xuất).
   - **Không** sửa thời khóa biểu thay Student, **không** nộp bài thay Student, và **không** xem thông số kỹ thuật/chi phí AI (`AIUsageLog`) ở Dashboard thông thường.

---

## 3. Core Functional Requirements (MVP Scope)

### Module 1: Authentication, Profiles & Student–Guardian Linking (`FR-AUTH`)
- **FR-AUTH-01..05:** Đăng ký, đăng nhập, đăng xuất và quản lý phiên JWT với tối thiểu 2 vai trò `STUDENT` và `GUARDIAN`. Chấp nhận mọi địa chỉ email hợp lệ (không ràng buộc domain riêng).
- **FR-AUTH-06 (Student–Guardian 1-Time Link Code — `O-01`):** Hỗ trợ liên kết tài khoản Student–Guardian bằng mã liên kết 8 ký tự dùng 1 lần tạo trực tiếp trên giao diện (không gửi Email/SMS). Guardian chỉ truy cập được dữ liệu của Student đã liên kết hợp lệ.

### Module 2: Weekly Timetable & Next-Day Study Plan (`FR-SCH`, `FR-PLAN`)
- **FR-SCH-01..07 (Weekly Timetable):** Student chọn các ngày trong tuần (`MON`–`SUN`) và gán một hoặc nhiều môn trong **5 môn cố định** (`MATH`, `LIT`, `ENG`, `SCI`, `HISGEO`), kèm ghi chú/tiết học tùy chọn. Lưu thời điểm & người cập nhật dưới dạng phiên bản lịch mới; chỉnh sửa lịch **không xóa hay thay đổi** các `StudyPlan`, `StudySubmission` và `AIEvaluation` lịch sử.
- **FR-PLAN-01..05 (Next-Day Study Plan):** Xác định ngày mục tiêu mặc định là **ngày lịch kế tiếp** theo múi giờ `Asia/Ho_Chi_Minh`. Mỗi môn trong ngày tạo đúng **1 cặp nhiệm vụ (`REVIEW` + `PREVIEW`)** kể cả khi môn xuất hiện nhiều tiết trong ngày. Nếu ngày kế tiếp không có môn, hiển thị trạng thái rỗng *"Không có môn học theo thời khóa biểu"* (không tự suy đoán ngày học bù).

### Module 3: Review & Preview Submissions (`FR-REV`, `FR-PRE`, `O-04`)
- **FR-REV-01..07 (Review — Ôn bài cũ):** Nhập **Tựa bài cũ** (bắt buộc) và **Nội dung kiến thức chủ đạo** có độ dài `850–2.000` ký tự (tính theo quy tắc chuẩn hóa newline + trim hai đầu, bao gồm khoảng trắng và dấu câu nội bộ). UI hiển thị bộ đếm ký tự realtime và chặn Submit ngoài biên (`849` hoặc `2.001`). Backend lưu bản ghi bất biến trước khi tạo `EvaluationJob`.
- **FR-PRE-01..07 (Preview — Soạn bài mới):** Chỉ mở khóa sau khi nhiệm vụ `REVIEW` cùng môn/ngày đã được **Submit và lưu thành công** (không bắt buộc chờ AI chạy xong mới được mở Preview). Áp dụng cùng quy tắc tựa bài và `850–2.000` ký tự.
- **Revision Policy (`O-04`):** Sau khi nhận kết quả AI (`EVALUATED`), Student được phép nộp bản cập nhật (**Revision**), **tối đa 3 lần cập nhật** cho mỗi nhiệm vụ (tối đa 4 phiên bản bất biến: `v1` gốc + `v2..v4` sửa; khóa cập nhật trong lúc đang `EVALUATING` và không ghi đè bản cũ).

### Module 4: AI Verification Pipeline & Cost Control (`FR-AI`, `FR-OPS`)
- **FR-AI-01..15 (Brave Search + MiMo v2.6 Flash via CKEY):**
  - Chỉ kích hoạt khi Student bấm Submit hợp lệ (tuyệt đối **không** gọi AI khi đang gõ; **không** gửi lịch sử chat).
  - Kiểm tra `KnowledgeCache` (chỉ lưu nội dung khi điều khoản API cho phép) $\rightarrow$ miss thì gọi **Brave Search API** với truy vấn `Lớp 6 + môn + tựa bài` (+ cụm từ khóa rút gọn), lấy tối đa **3 nguồn**, giới hạn ngân sách ngữ cảnh khoảng **2.048 token**.
  - Gọi **MiMo v2.6 Flash via CKEY** 1 lượt với prompt cố định, **tuyệt đối không gửi tên thật hay thông tin định danh học sinh** (`BR-23`).
  - Đánh giá độc lập 2 trục:
    - `accuracy`: `CORRECT` | `PARTIALLY_CORRECT` | `INCORRECT` | `UNVERIFIABLE`
    - `completeness`: `COMPLETE` | `INCOMPLETE` | `UNDETERMINED`
    - `overall_status`: `GOOD` | `NEEDS_IMPROVEMENT` | `INCORRECT` | `INSUFFICIENT_EVIDENCE`
    - `confidence`: `HIGH` | `MEDIUM` | `LOW`
  - Nhận xét Tiếng Việt thân thiện trình độ lớp 6, tổng phần hiển thị cho Student **không vượt quá 600 ký tự**; kèm `incorrect_points`, `missing_points` và mảng `sources` (`title`, `url`, `retrieved_at`). Thiếu căn cứ trả `UNVERIFIABLE` / `UNDETERMINED` / `INSUFFICIENT_EVIDENCE`, không bịa nguồn.
- **FR-OPS-01..07:** Xử lý bất đồng bộ qua `EvaluationJob` queue; hỗ trợ `Idempotency-Key` chống trùng job khi bấm Submit liên tục; retry có exponential backoff với `timeout`/`429`/`5xx`; lưu `AIUsageLog` (tokens, search count, latency, estimated cost) mà không ghi lộ API secret.

### Module 5: Guardian Dashboard (`FR-GRD`)
- **FR-GRD-01..08:** Xem tổng quan tiến độ hoàn thành theo ngày/tuần, lọc theo ngày và 5 môn học; xem nguyên văn bất biến mọi phiên bản bài viết của Student, trạng thái `EVALUATING`/`EVALUATION_FAILED`/`EVALUATED`, đầy đủ `Accuracy`, `Completeness`, `overall_status`, `feedback`, `incorrect_points`, `missing_points` và URL nguồn tham khảo.

---

## 4. Non-Functional Requirements & Out of Scope
- **Usability & Accessibility:** Giao diện Minimal-Text, chữ đọc rõ ràng cho học sinh lớp 6, tiếng Việt mặc định (`Be Vietnam Pro` + `JetBrains Mono`), responsive trên mobile/tablet/desktop; phản hồi tích cực không mang tính trách phạt (`NFR-ACC-01`).
- **Security & Privacy:** HTTPS/TLS; secret `CKEY_API_KEY` và `BRAVE_API_KEY` chỉ lưu tại Backend; kiểm tra chéo quyền `User`/`GuardianLink` tại server; không gửi dữ liệu định danh trẻ vị thành niên sang AI/Search (`NFR-PRIV-01..02`).
- **Out of Scope (MVP 1.0):** Tự tạo đề thi/ngân hàng câu hỏi; chatbot hội thoại tự do; OCR/đọc ảnh chụp sách vở/thời khóa biểu; đồng bộ lịch từ trường; native mobile app; chấm điểm tổng kết/xếp hạng; Email/SMS/Push notification; mở rộng khối lớp 7–9; thanh toán/subscription.
