# SYSTEM DESIGN & DATABASE SPECIFICATION
**Project:** SEP490 Capstone Copilot  
**Status:** In Progress / Pending Design  

---

## 1. Database Schema (ERD)
*Thiết kế CSDL chi tiết cho các thực thể:*
- `User`, `Project`, `ProjectMember`
- `Milestone`, `MilestoneDeliverable`, `DeliverableSubmission`
- `Feature`, `WbsItem`, `RaciMatrix`, `RiskRegister`, `QualityObjective`
- `Actor`, `UseCase`, `Screen`, `ScreenAuthorization`, `BusinessRule`, `AppMessage`
- `TestCase`, `TestExecution`, `Defect`
- `Task` (Kanban), `ProjectIssue`, `QaLog`, `PeerReview`
- `RecordOfChange` (Audit Log)

---

## 2. API Specifications
*(RESTful endpoints specification TBD)*

---

## 3. UI/UX Wireframes & Component Specs
### 3.1 Typography & Vietnamese Font Standard
- **Primary Sans-serif (UI & Content):** `Be Vietnam Pro` (hoặc `Inter`) cấu hình qua `next/font/google` với `subsets: ["vietnamese", "latin"]` để đảm bảo hiển thị dấu tiếng Việt cân đối, sắc nét trên cả tiêu đề lẫn nội dung bảng biểu mật độ cao.
- **Monospace (IDs, Codes & PR Links):** `JetBrains Mono` cho các mã `FE-01`, `UC-01`, `BR-01`, `TASK-15`.
- **Excel Export (Apache POI):** Sử dụng `Calibri` hoặc `Arial` chuẩn Unicode (UTF-8) trong toàn bộ các sheet xuất ra (`.xlsx`) để tương thích tốt khi mở trên Microsoft Excel / Google Sheets.

### 3.2 Wireframes & Components
*(TBD)*
