# AI Study Companion – Grade 6

> Nền tảng web hỗ trợ học sinh lớp 6 tự ôn bài cũ (**Review**), chuẩn bị bài mới (**Preview**) theo thời khóa biểu cá nhân, kiểm chứng kiến thức bằng **Brave Search + MiMo v2.6 Flash (via CKEY)**, và theo dõi tiến độ trên **Guardian Dashboard**.

---

## 📌 Tài liệu Đặc tả (`docs/`)
Tất cả yêu cầu nghiệp vụ, kiến trúc và quy tắc kỹ thuật được quản lý tập trung tại [`docs/`](./docs/):

- 📋 [**PRD.md**](./docs/PRD.md): Đặc tả yêu cầu sản phẩm (`SRS-ASC-G6-001` v1.0).
- 🏗️ [**ARCHITECTURE.md**](./docs/ARCHITECTURE.md): Kiến trúc Modular Monolith (`com.asc.studycompanion`) + Next.js 14 App Router + AI Orchestrator.
- 📐 [**DESIGN.md**](./docs/DESIGN.md): Thiết kế UI/UX (Minimal-Text, Light/Dark), 14 bảng PostgreSQL (ERD) & REST APIs.
- 📏 [**RULES.md**](./docs/RULES.md): Quy tắc nghiệp vụ (`BR-01..BR-24`) và chuẩn code bắt buộc.
- 📝 [**TASKS.md**](./docs/TASKS.md): Lộ trình triển khai chia theo 6 Phase nhỏ.
- 📈 [**PROGRESS.md**](./docs/PROGRESS.md): Nhật ký tiến độ dự án.

---

## 🚀 Tính năng cốt lõi (MVP 1.0)
1. **Thời khóa biểu & Kế hoạch ngày kế tiếp:** Hỗ trợ 5 môn cố định (`Toán`, `Ngữ văn`, `Tiếng Anh`, `KHTN`, `Lịch sử – Địa lý`) theo múi giờ `Asia/Ho_Chi_Minh`.
2. **Học chủ động `Review` $\rightarrow$ `Preview`:** Học sinh tự trình bày kiến thức (`850–2.000` ký tự), hoàn thành Review trước khi mở Preview; cho phép sửa tối đa 3 lần sau khi AI đánh giá.
3. **Kiểm chứng AI có nguồn Internet:** Tích hợp **Brave Search API** (tối đa 3 nguồn, cache theo điều khoản) và **MiMo v2.6 Flash via CKEY** trả phản hồi ngắn Tiếng Việt (`<= 600` ký tự) kèm `Accuracy`, `Completeness` và URL nguồn.
4. **Guardian Dashboard:** Phụ huynh liên kết bằng mã 8 ký tự dùng 1 lần để xem toàn bộ bài viết gốc bất biến, đánh giá AI và nguồn tham khảo.
