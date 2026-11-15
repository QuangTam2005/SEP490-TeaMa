# SEP490 Capstone Copilot (TeaMa)

> Nền tảng quản trị dự án chuyên biệt chuẩn hóa theo quy trình, biểu mẫu và quy chế đồ án tốt nghiệp **SEP490** - Đại học FPT.

---

## 📌 Single Source of Truth (Tài liệu dự án)
Tất cả tài liệu kỹ thuật và thiết kế được quản lý tập trung trong thư mục [`docs/`](./docs/):

- 📋 [**PRD.md**](./docs/PRD.md): Product Requirements Document (Phiên bản 1.1 - Chuẩn hóa học thuật 100%).
- 🏗️ [**ARCHITECTURE.md**](./docs/ARCHITECTURE.md): Kiến trúc hệ thống, C4 model và luồng xử lý.
- 📐 [**DESIGN.md**](./docs/DESIGN.md): Thiết kế CSDL (ERD), API endpoints và chi tiết giao diện.
- 📏 [**RULES.md**](./docs/RULES.md): Quy chuẩn lập trình, Git branching và quy trình commit/PR.
- 📝 [**TASKS.md**](./docs/TASKS.md): Danh sách backlog và kế hoạch sprint chi tiết.
- 📈 [**PROGRESS.md**](./docs/PROGRESS.md): Nhật ký tiến độ thực hiện dự án.

---

## 🚀 Các tính năng cốt lõi (MVP)
1. **15-Week Milestone Engine:** Tự động tạo 10 cột mốc và checklist tài liệu lũy tiến bắt buộc theo chuẩn SEP490.
2. **Auto-Complexity Estimator & WBS:** Phân loại độ phức tạp Simple / Medium / Complex theo số lượng Field và Transaction.
3. **Traceability Matrix:** Kết nối xuyên suốt `Feature` $\leftrightarrow$ `Use Case` $\leftrightarrow$ `Screen/Function` $\leftrightarrow$ `Task` $\leftrightarrow$ `Test Case` $\leftrightarrow$ `Defect`.
4. **GitHub Webhook Sync:** Tự động cập nhật tiến độ Kanban board theo Pull Request.
5. **Quality Objectives & Peer-Review:** Đo lường chỉ tiêu chất lượng và đánh giá chéo chống free-rider.
6. **Smart Exporters:** Xuất báo cáo đặc tả ra Markdown và bộ 4 sổ theo dõi + Báo cáo tuần ra file Excel `.xlsx` chuẩn mẫu FPTU.
