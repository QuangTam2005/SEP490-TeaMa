# PRODUCT REQUIREMENTS DOCUMENT (PRD)
**Product Name:** SEP490 Capstone Copilot  
**Target Domain:** FPT University Software Engineering Capstone Project (`SEP490`)  
**Document Version:** 1.4 (MVP - Full Academic Compliance, Multi-Repo GitHub & Email Invitation)  

---

## 1. Product Overview & Vision
### 1.1 Problem Statement
Sinh viên ngành Kỹ thuật Phần mềm (SE) ĐH FPTU thực hiện đồ án tốt nghiệp `SEP490` trong 15 tuần với 10 cột mốc (Milestones), 7 gói tài liệu báo cáo (Report 1–7), báo cáo tiến độ hàng tuần (`Project Weekly Report`) và 4 sổ theo dõi vận hành (`WBS`, `Issues`, `Defects`, `Q&A`). Việc sử dụng các công cụ rời rạc khiến nhóm dễ bị lệch pha giữa đặc tả (SRS), tiến độ code và kiểm thử, đồng thời Giảng viên hướng dẫn (GVHD) thiếu dữ liệu định lượng để đánh giá đóng góp cá nhân và chống tình trạng *free-rider*.

### 1.2 Product Vision
Xây dựng một nền tảng quản trị dự án tập trung, tuân thủ 100% biểu mẫu và quy chế của `SEP490`. Hệ thống số hóa toàn bộ cấu trúc WBS, Use Case, Test Case, Tracking Logs, tự động đồng bộ tiến độ thực tế từ GitHub, kiểm soát checklist nộp bài lũy tiến qua 10 cột mốc và cung cấp góc nhìn minh bạch cho GVHD.

---

## 2. Target Users & Roles (RBAC)
1. **Team Leader (Sinh viên nhóm trưởng):**
   - Khởi tạo dự án, cấu hình thông tin nhóm, thiết lập WBS/RACI/Quality Targets và mời thành viên.
   - Quản lý 10 cột mốc (Milestones), duyệt Peer-Review, tạo/thu hồi Magic Link cho GVHD và xuất dữ liệu báo cáo.
2. **Team Member (Thành viên nhóm - 4 đến 5 SV/nhóm):**
   - Cập nhật đặc tả (Features, Use Cases, Screens, Business Rules, Messages), quản lý Task theo Iteration, viết Test Case, log Defect/Issue.
   - Thực hiện đánh giá chéo (Peer-Review) cuối mỗi Iteration và upload các file báo cáo lên checklist của từng Milestone.
3. **Supervisor / Lecturer (Giảng viên hướng dẫn):**
   - Truy cập không cần đăng nhập thông qua **Magic Link** riêng biệt của nhóm.
   - Xem tổng quan tiến độ 15 tuần, biểu đồ đóng góp từng sinh viên, kiểm tra các sổ Tracking và trả lời trực tiếp vào mục **Q&A**.

---

## 3. Core Functional Requirements (MVP Scope)

### Module 1: Authentication, Project Setup & Cumulative Milestones
- **FR-01 (Auth):** Đăng ký, đăng nhập và quản lý phiên làm việc bằng **Email & Password** truyền thống (JWT Authentication).
- **FR-02 (Project Initialization & Email Invitation):** Cho phép Leader (Owner) tạo dự án với các trường chuẩn của Report 1 (`Project Name`, `Project Code`, `Group Name`, `Software Type`) và mời thành viên qua **Email** (`Leader` hoặc `Member`). Hệ thống gửi email chứa đường link mời (`MailSender` + token bảo mật); người được mời truy cập link, đăng ký (`Sign Up`) hoặc đăng nhập (`Sign In`) bằng **đúng email đó** để gia nhập dự án tự động.
- **FR-03 (15-Week Milestone & Cumulative Deliverables Checklist):** Khi tạo dự án, hệ thống tự động khởi tạo **10 cột mốc chuẩn của SEP490** (Tuần 1 đến Tuần 15) kèm trọng số điểm **On-Going Assessment (OGA - 50%)** (R1: 4%, R2: 8%, R3: 16%, R4: 18%, R5: 18%, R6: 4%, R7: 32%). Mỗi Milestone hiển thị sẵn **Checklist các đầu mục lũy tiến bắt buộc phải nộp** (Ví dụ: M3 nộp Report 3 + Updated Report 1, 2 + Tracking; M4 nộp Report 4 + khung Report 5 + Demo Code Package + Updated Report 1, 2, 3...).
- **FR-04 (Deliverable File Upload):** Tại từng hạng mục trong Checklist của mỗi Milestone, thành viên có thể upload file báo cáo/source package tương ứng (`.docx`, `.pdf`, `.xlsx`, `.zip`) và cập nhật trạng thái (`Pending`, `Submitted`, `Approved`).

### Module 2: Planning, WBS, RACI & Quality Targets (Report 2)
- **FR-05 (Feature, WBS, Work Category & Auto-Complexity Estimator):**
  - Quản lý danh sách tính năng (`FE-01`, `FE-02`...) và phân rã thành các hạng mục WBS gắn phân loại công việc (`REQUIREMENT`, `DESIGN`, `CODING`, `TESTING`, `PM`) cùng ước lượng `man-days` (tự động tính tổng effort và bảng phân bổ % công sức *Allocated Effort Distribution* theo Report 2).
  - Tích hợp quy tắc tính độ phức tạp chuẩn của bộ môn (`Report3_Project Tracking`): cho phép nhập số `Fields` (input/cột DB) và số `Transactions` (nút bấm/xử lý nghiệp vụ) để tự động gợi ý mức độ:
    - `Simple`: $\le 7$ fields và $\le 3$ transactions.
    - `Medium`: $\le 15$ fields và $\le 7$ transactions.
    - `Complex`: $> 15$ fields hoặc $> 7$ transactions.
- **FR-06 (RACI Matrix):** Bảng phân công trách nhiệm cho từng hạng mục công việc/tài liệu theo chuẩn `D` (Do), `R` (Review), `S` (Support), `I` (Informed) cho từng thành viên.
- **FR-07 (Risk Register & Quality Objectives):**
  - Quản lý danh sách rủi ro dự án (`Risk Description`, `Impact`, `Possibility`, `Response Plans`).
  - Thiết lập bảng **Quality Objectives** cho 5 giai đoạn kiểm thử (`Reviewing`, `Unit Test`, `Integration Test`, `System Test`, `Acceptance Test`) với các chỉ số mục tiêu: `Target Test Coverage (%)` và `Target Defect Rate (%)`.

### Module 3: SRS Specification & Traceability Hub (Report 3)
- **FR-08 (Structured SRS Data):** Quản lý có cấu trúc các thực thể của Report 3:
  - `Actors` & `Use Cases` (`UC-01`, `UC-02`...).
  - `Screens` & `Non-Screen Functions` kèm ma trận phân quyền `Screen Authorization` 8 mức chuẩn FPTU (`Query All`, `Query Own`, `Query Managed`, `Add New`, `Update All`, `Update Own`, `Update Managed`, `Delete Data` theo từng Role).
  - `Business Rules` (`BR-01`...) và `Application Messages` (`MSG01` - Toast/Inline).
- **FR-09 (Diagram Attachments):** Cho phép upload hình ảnh biểu đồ (`.png`, `.jpg` xuất từ Draw.io/StarUML) gắn vào từng phần tương ứng (Context Diagram, Use Case Diagram, Screen Flow, ERD).
- **FR-10 (Traceability Matrix):** Liên kết dữ liệu xuyên suốt: `Feature` $\leftrightarrow$ `Use Case` $\leftrightarrow$ `Screen/Function` $\leftrightarrow$ `WBS Task` $\leftrightarrow$ `Test Case` $\leftrightarrow$ `Defect`.

### Module 4: Quality Assurance & Testing (Report 5)
- **FR-11 (Test Case Management & Execution Evidence):** Tạo và quản lý Test Case gắn với từng Use Case, phân loại theo cấp độ (`Unit Test`, `Integration Test`, `System Test`, `Acceptance Test`), trạng thái thực thi (`Untested`, `Pass`, `Fail`), kèm `Actual Result`, thời điểm thực thi (`Executed At`) và tối đa **2 ảnh chụp màn hình bằng chứng** ($\le 5\text{MB}$/ảnh, `.png`/`.jpg`/`.webp`).
- **FR-12 (Target vs. Actual Quality Metrics):** Tự động tính toán `Actual Test Coverage (%)` và `Actual Defect Rate (%)` từ dữ liệu Test Case & Defect Log, đối chiếu trực tiếp với bảng `Quality Objectives` đã thiết lập ở FR-07.

### Module 5: Iteration Execution, Tracking Registers & GitHub Webhook
- **FR-13 (Iteration-based Kanban Board):** Quản lý công việc trên Kanban board (`TODO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`) có bộ lọc phân tách rõ ràng theo **4 đợt Iteration** (`Iteration 1` - W7, `Iteration 2` - W9, `Iteration 3` - W11, `Iteration 4 / System Test` - W13).
- **FR-14 (GitHub Webhook Integration - Multi-Repo):**
  - Hỗ trợ gắn nhiều Repository cho mỗi dự án (VD: `Backend`, `Frontend`, `Mobile`), cung cấp Webhook URL (`/api/webhooks/github/{repoId}`) và Secret Token riêng biệt cho từng repository.
  - Tự động parse mã Task (ví dụ `[TASK-15]`) từ tiêu đề Pull Request hoặc tên nhánh.
  - Cập nhật danh sách PR liên kết (`github_prs`) trên Task kèm `repoLabel`, `prNumber`, `prUrl`, `status` (`OPEN`, `MERGED`, `CLOSED`) và tác giả.
  - Tự động chuyển Task sang `IN_REVIEW` khi có PR `OPEN`, và chỉ chuyển sang `DONE` khi có ít nhất 1 PR `MERGED` và không còn PR nào đang `OPEN`.
- **FR-15 (3 Operational Tracking Registers & Weekly Report Data):** Số hóa 3 sổ theo dõi vận hành chuẩn `Report3_Project Tracking`:
  - `Project Issues`: Theo dõi các vấn đề phát sinh trong quá trình làm nhóm.
  - `Product Defects`: Theo dõi lỗi phần mềm (gắn với Test Case bị Fail).
  - `Q&A Log`: Nhật ký câu hỏi của nhóm gửi GVHD và câu trả lời phản hồi.
  - Cho phép ghi nhận nhanh các mục `Other Matters / Suggestions` để tổng hợp vào Báo cáo tuần.

### Module 6: Contribution Analytics, Audit & Data Export
- **FR-16 (Contribution & Peer-Review):**
  - Thống kê khách quan số Task hoàn thành, tổng `man-days` thực tế và số PR được merge của từng thành viên.
  - Tổ chức đánh giá chéo ẩn danh (**Peer-Review**) giữa các thành viên sau mỗi Iteration (chấm điểm thái độ, đúng hạn, chất lượng công việc) để phát hiện sớm rủi ro *free-rider*.
- **FR-17 (Automatic Record of Changes):** Tự động ghi nhận 100% lịch sử Thêm (`A`), Sửa (`M`), Xóa (`D`) trên các hạng mục SRS/WBS kèm tên người thực hiện và ngày giờ để xuất ra bảng `Record of Changes` ở đầu các Report.
- **FR-18 (Supervisor Magic Link):** Tạo token URL bảo mật (có thể thu hồi/tạo lại) cho phép GVHD truy cập chế độ Read-only (xem Dashboard, Milestone Checklist, Contribution, Tracking) và trả lời trực tiếp vào bảng `Q&A Log`.
- **FR-19 (Markdown & Excel Exporter):**
  - Xuất toàn bộ bảng biểu đặc tả (Use Cases, Screen Auth, Business Rules, Messages, Record of Changes) ra file **`.md` (Markdown)** sạch sẽ để sinh viên copy-paste nhanh vào file Word báo cáo.
  - Xuất bộ dữ liệu vận hành ra file **`.xlsx` (Excel)** chuẩn 4 sheets (`WBS`, `Issues`, `Defects`, `Q&A` + sheet `Test Cases`).
  - Xuất **Weekly Status Report (`.xlsx` / `.md`)** theo đúng 4 phần của mẫu `Project Weekly Report_GroupName.xlsx` (*Status Report*, *Project Issues*, *Next Week Plan*, *Other Matters/Suggestions*).

---

## 4. Non-Functional Requirements
- **Security:** Mật khẩu hash bằng BCrypt; phân quyền chặt chẽ theo `ProjectMember`; xác thực chữ ký HMAC-SHA256 cho GitHub Webhook; Magic Link cho GVHD sử dụng UUID v4 ngẫu nhiên.
- **Data Integrity:** Sử dụng Soft-Delete cho các thực thể quan trọng (`UseCase`, `WBS_Task`, `Defect`) để không làm gãy liên kết trong Ma trận truy xuất và giữ nguyên lịch sử `Record of Changes`.
- **Performance:** Thời gian phản hồi API dưới 300ms; tác vụ xuất file `.xlsx` và `.md` xử lý dưới 2 giây.
- **Usability & Typography:** Giao diện web sử dụng bộ font chữ tối ưu cho tiếng Việt (`Be Vietnam Pro`, `Inter` hoặc `Plus Jakarta Sans` hỗ trợ đầy đủ bảng mã Unicode dựng sẵn) nhằm tránh lỗi hiển thị dấu thanh khi ghi chú song ngữ Việt – Anh; các file `.xlsx` xuất ra sử dụng font `Calibri` hoặc `Arial` chuẩn Unicode để hiển thị chính xác trên mọi phiên bản Excel.

---

## 5. Out of Scope (Không làm trong MVP)
- Không tự động sinh file `.docx` hoàn chỉnh theo template Word (sinh viên tự viết trên Word/Google Docs và upload file hoàn chỉnh lên checklist của từng Milestone).
- Không tích hợp trình vẽ biểu đồ trực tiếp (như Mermaid/Draw.io editor) bên trong ứng dụng.
- Không tích hợp Google OAuth hoặc đồng bộ trực tiếp với hệ thống LMS/FAP của trường.
- Không hỗ trợ chế độ nghiên cứu khoa học `SET490` trong phiên bản này.

---

## 6. Seed Data Reference: 10 Milestones & Deliverables Checklist

| Milestone | Tuần | Tên mốc & Giai đoạn | Trọng số OGA | Checklist Deliverables bắt buộc |
| :--- | :---: | :--- | :---: | :--- |
| **M01** | W01 | Project Introduction *(Initiation)* | **4%** | ☐ `Report 1: Project Introduction Document` (.docx) |
| **M02** | W02 | Project Plan *(Plan & Req)* | **8%** | ☐ `Report 2: Project Management Plan (PMP)` (.docx)<br>☐ `Project Schedule` (.pdf / .pod) |
| **M03** | W03 | Overall Requirement *(Plan & Req)* | **16%** | ☐ `Report 3: SRS (Part I, II.1, II.2, II.3.1)` (.docx)<br>☐ `Updated Report 1` (.docx)<br>☐ `Updated Report 2` (.docx)<br>☐ `Project Tracking (WBS, Issues, Q&A)` (.xlsx) |
| **M04** | W05 | Software Design *(Design)* | **18%** | ☐ `Report 4: SDD (System & DB Design)` (.docx)<br>☐ `Report 5: STD (Khung ban đầu)` (.docx)<br>☐ `Demo Code Package` (Login, Register, CRUD cơ bản) (.zip)<br>☐ `Updated Reports 1, 2, 3` (.docx)<br>☐ `Updated Project Tracking` (.xlsx) |
| **M05** | W07 | Code Iteration 1 *(Implementation)* | — | ☐ `Software Package 1: Workable Code & DB Script` (.zip)<br>☐ `Updated Report 3` (Functional Req hoàn thiện cho Iter 1)<br>☐ `Updated Report 4` (Class & Sequence Diagrams cho Iter 1)<br>☐ `Updated Report 5` (UT, IT Test Cases & Defect Log)<br>☐ `Updated Reports 1, 2` & `Updated Tracking` (.xlsx)<br>☐ *Peer-Review Iteration 1* |
| **M06** | W09 | Code Iteration 2 *(Implementation)* | — | ☐ `Software Package 2: Code & DB Script` (.zip)<br>☐ `Updated Reports 1, 2, 3, 4, 5`<br>☐ `Updated Project Tracking` (.xlsx)<br>☐ *Peer-Review Iteration 2* |
| **M07** | W11 | Code Iteration 3 *(Implementation)* | — | ☐ `Software Package 3: Code & DB Script` (.zip)<br>☐ `Updated Reports 1, 2, 3, 4, 5`<br>☐ `Updated Project Tracking` (.xlsx)<br>☐ *Peer-Review Iteration 3* |
| **M08** | W13 | Full Package *(System Testing)* | — | ☐ `Full Software Package` (.zip)<br>☐ `Updated Report 5` (Full ST, IT, Defect List hoàn chỉnh)<br>☐ `Updated Reports 1, 2, 3, 4`<br>☐ `Updated Project Tracking` (.xlsx)<br>☐ *Peer-Review Iteration 4* |
| **M09** | W14 | User Guides *(Transition)* | **4%** | ☐ `Report 6: Software User Guides (SUG)` (.docx)<br>☐ `Updated Project Tracking` (.xlsx) |
| **M10** | W15 | Final Deliverables *(Transition & Defense)* | **32%** *(R7)* | ☐ `Report 7: Final Project Report` (.docx)<br>☐ `Final Source Code & DB Script Package` (.zip)<br>☐ `Final Test Documents & Defect List` (.xlsx / .docx)<br>☐ `Final Project Tracking` (.xlsx)<br>☐ `Defense Slides` (.pptx / .pdf) |
