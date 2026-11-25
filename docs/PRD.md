# PRODUCT REQUIREMENTS DOCUMENT (PRD)
**Product Name:** AI Study Companion (`SRS-ASC-G6-001` — Updated v1.1)  
**Target Domain:** Hỗ trợ học sinh (tự chọn khối lớp `1–12`, trọng tâm THCS) tự học theo thời khóa biểu & giám sát bởi Phụ huynh (`Guardian`)  
**Document Version:** 1.1 (Dynamic Student Grade, `v1..v3` Cap, Guardian Timetable Edit, KNTT + Wikipedia Grounding, AI 3-Retry Fallback)

---

## 1. Product Overview & Vision
### 1.1 Problem Statement
Học sinh thường gặp 4 khó khăn trong quá trình tự học hằng ngày:
1. Dễ quên kiến thức buổi học trước, khó kết nối mạch kiến thức liên môn.
2. Ít chuẩn bị/đọc trước bài mới trước khi lên lớp.
3. Có thể hiểu sai hoặc thiếu ý trọng tâm nhưng không tự phát hiện ra.
4. Người giám sát (`Guardian`) khó nắm được thực chất học sinh đã tự viết/tự hiểu những gì thay vì chỉ nhìn điểm số bề mặt.

### 1.2 Product Vision
Xây dựng nền tảng web responsive **AI Study Companion** với định hướng cốt lõi: **Student tự học và tự trình bày; AI kiểm chứng từ SGK Kết nối tri thức với cuộc sống + Wikipedia và phản hồi ngắn gọn; Guardian theo dõi minh bạch trên Dashboard**.  
Ứng dụng **không** làm bài thay học sinh, **không** chấm điểm tổng kết hay xếp hạng thay nhà trường, và **không** tạo tính năng chấm/duyệt thủ công riêng khi AI lỗi.

---

## 2. Target Users & Roles (RBAC)
1. **Student (Học sinh):**
   - **Tự thiết lập khối lớp của mình (`1–12`)** khi đăng ký hoặc cập nhật hồ sơ học tập (không mặc định cứng khối 6).
   - Nhập và cập nhật **Thời khóa biểu tuần** theo danh mục môn phù hợp khối lớp (`Toán`, `Ngữ văn`, `Tiếng Anh`, `Khoa học tự nhiên`, `Lịch sử – Địa lý`, và các môn THCS/THPT `Vật lí`, `Hóa học`, `Sinh học`, `Lịch sử`, `Địa lí`, `Tin học`, `GDCD`).
   - Chuẩn bị bài vào **ngày hôm trước** của ngày có lịch học môn (theo múi giờ `Asia/Ho_Chi_Minh`: ví dụ thứ Hai có môn Toán $\rightarrow$ Chủ nhật làm bài ôn/soạn môn Toán).
   - Thực hiện tuần tự cặp nhiệm vụ **Review (Ôn bài cũ)** trước $\rightarrow$ **Preview (Soạn bài mới)** sau, tự viết kiến thức từ `850` đến `3.000` ký tự (kèm lưu nháp cục bộ trên trình duyệt).
   - Xem kết quả AI đánh giá (`Accuracy`, `Completeness`, `overall_status`, nhận xét `<= 600` ký tự, nguồn tham khảo) và có thể nộp tối đa **3 phiên bản tổng cộng (`v1`, `v2`, `v3`)** cho mỗi nhiệm vụ.
2. **Guardian (Người giám sát / Phụ huynh):**
   - Liên kết với hồ sơ Student thông qua **mã liên kết dùng 1 lần (8 ký tự)** (`O-01`).
   - Có quyền **xem và cập nhật Thời khóa biểu tuần** cho Student đã liên kết (lưu dưới dạng phiên bản mới, giữ nguyên lịch sử cũ).
   - Xem **Guardian Dashboard** ở chế độ chỉ đọc bài làm: tiến độ theo ngày/tuần, lịch sử từng phiên bản bài viết gốc bất biến của Student (`v1..v3`), đánh giá chi tiết của AI (`incorrect_points`, `missing_points`, URL nguồn & thời điểm truy xuất). Khi AI lỗi sau 3 lần thử tự động, bài làm vẫn được chuyển sang Guardian kèm thông báo lỗi hệ thống để Phụ huynh tự kiểm tra trực tiếp bài làm của con (không làm tính năng check thủ công riêng).
   - **Không** nộp bài thay Student và **không** xem thông số kỹ thuật/chi phí AI (`AIUsageLog`).

---

## 3. Core Functional Requirements (MVP Scope)

### Module 1: Authentication, Grade Setup & Student–Guardian Linking (`FR-AUTH`)
- **FR-AUTH-01..05:** Đăng ký (`STUDENT` chọn khối lớp `1–12` của mình; `GUARDIAN`), đăng nhập, đăng xuất và quản lý phiên JWT. Chấp nhận mọi địa chỉ email hợp lệ bất kỳ. Student có thể cập nhật lại khối lớp qua `PUT /api/student/profile`.
- **FR-AUTH-06 (Student–Guardian 1-Time Link Code — `O-01`):** Tạo mã liên kết 8 ký tự dùng 1 lần trên giao diện (hạn 24h, không Email/SMS).

### Module 2: Weekly Timetable & Preparation Study Plan (`FR-SCH`, `FR-PLAN`)
- **FR-SCH-01..07 (Weekly Timetable):** **Student hoặc Guardian đã liên kết** đều có thể chọn các ngày (`MON`–`SUN`) và gán môn theo khối lớp (`1–12`). Lưu thời điểm & người cập nhật (`updated_by_user_id`) dưới dạng phiên bản lịch mới; không xóa/sửa các `StudyPlan`, `StudySubmission` và `AIEvaluation` lịch sử.
- **FR-PLAN-01..05 (Next-Day Preparation Plan & Streak/History):** Ngày chuẩn bị là **hôm nay**, lập kế hoạch cho **ngày lịch kế tiếp** (ngày mai theo `Asia/Ho_Chi_Minh`). Ví dụ thứ Hai có môn Toán theo thời khóa biểu $\rightarrow$ Chủ nhật hệ thống hiển thị môn Toán để bé ôn/soạn. Mỗi môn trong ngày tạo đúng **1 cặp (`REVIEW` + `PREVIEW`)**. Hỗ trợ xem lại lịch sử bài đã làm theo ngày/môn và chuỗi ngày học (`Streak`).

### Module 3: Review & Preview Submissions (`FR-REV`, `FR-PRE`, `O-04`)
- **FR-REV-01..07 & FR-PRE-01..07:** Tựa bài (bắt buộc) + Nội dung **`850–3.000` ký tự** (chuẩn hóa `\r\n` $\rightarrow$ `\n`, `.trim()`), hỗ trợ lưu nháp tự động tại trình duyệt. Cổng tuần tự: `REVIEW` nộp xong mới mở `PREVIEW` cùng môn/ngày (`BR-08`).
- **Revision Policy (`v1..v3`):** Tối đa **3 phiên bản tổng cộng (`v1` gốc + tối đa 2 lần sửa `v2`, `v3`)** cho mỗi nhiệm vụ. Khóa nộp mới trong lúc đang `EVALUATING`; mọi phiên bản bất biến.

### Module 4: AI Verification Pipeline & Cost Control (`FR-AI`, `FR-OPS`)
- **FR-AI-01..15 (Brave Search + MiMo v2.6 Flash via CKEY):**
  - Chỉ kích hoạt khi Student bấm Submit hợp lệ (tuyệt đối **không** gọi AI khi đang gõ; **không** gửi PII/tên/email/ID).
  - Nguồn chuẩn hóa: tìm kiếm từ **Wikipedia** (`vi.wikipedia.org`) và các website học tập có nội dung về bài học đó (ưu tiên SGK **Kết nối tri thức với cuộc sống** đúng `grade` của Student) dựa theo tựa đề bài học & nội dung chính trích từ bài soạn/ôn; mỗi lần chấm gửi **tối đa 3 request vào Brave** và giữ tối đa **3 nguồn** (ngân sách ngữ cảnh ~`2.048` tokens).
  - Đánh giá độc lập `accuracy`, `completeness`, `overall_status`, `confidence`, nhận xét Tiếng Việt thân thiện **từ 300 đến 600 ký tự** (tính cả dấu câu và khoảng trắng), `incorrect_points`, `missing_points`, `sources`.
- **FR-OPS-01..07 (3-Attempt Retry & Fallback to Guardian):**
  - Xử lý bất đồng bộ qua `EvaluationJob` + `Idempotency-Key`.
  - Khi lỗi gọi/parse AI, Worker **retry tự động tối đa 3 lần** (exponential backoff).
  - **Nếu quá 3 lần vẫn lỗi:** ngưng retry, ghi nhận `AIEvaluation` fallback (`overall_status = INSUFFICIENT_EVIDENCE`, `accuracy = UNVERIFIABLE`, `completeness = UNDETERMINED`, nhận xét thông báo hệ thống AI đang gặp lỗi sau 3 lần thử, phụ huynh vui lòng tự kiểm tra bài làm của con) và chuyển trạng thái task sang `EVALUATED` để hiển thị cho Guardian như đã hoàn tất bước AI review (không tạo chức năng check thủ công riêng).

### Module 5: Guardian Dashboard, Multi-Child, Parent Note & Export (`FR-GRD`)
- **FR-GRD-01..10:** Xem tiến độ theo ngày/tuần, lọc theo ngày và môn, chuyển đổi giữa nhiều con đã liên kết hoặc hủy liên kết, chỉnh sửa Thời khóa biểu tuần cho con, bấm "Đã xem" + gửi lời nhắn động viên ngắn trên từng nhiệm vụ, xuất báo cáo tuần ra Excel (`.xlsx`), và kiểm tra nguyên văn bất biến mọi phiên bản (`v1..v3`) + đánh giá AI + nguồn đối chiếu (tuyệt đối không lộ `AIUsageLog`).

### Module 6: Account Security, Rate Limiting & Ops Cost Visibility (`FR-SEC`, `FR-OPS`)
- **FR-SEC-01..03:** Tự cập nhật họ tên, đổi mật khẩu, bảo vệ rate-limit cho đăng nhập/đăng ký/nộp bài (`429 SYS_429`), và endpoint nội bộ `GET /api/ops/usage` bảo vệ bằng `X-Ops-Secret` để chủ hệ thống kiểm tra chi phí/token AI mà không lộ ra UI Student/Guardian.
