# UI/UX DESIGN SYSTEM (DESIGN.md)
**Product Name:** SEP490 Capstone Copilot (`SEP490-TeaMa`)  
**Design Philosophy:** Academic Precision meets Modern SaaS (Linear / Notion / Jira hybrid) — **Icon-First, Minimal Text**  
**Document Version:** 1.1  

---

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
   - Mỗi Task Card tối giản chữ, hiển thị: Mã `[TASK-15]` (`JetBrains Mono`), tiêu đề ngắn (tối đa 2 dòng), badge Iteration, **Icon GitHub PR `#42`** (click mở link PR), số `man-days` (icon đồng hồ) và Avatar người phụ trách.
6. **Anonymous Peer-Review Matrix:**
   - Giao diện đánh giá chéo cuối mỗi Iteration dạng bảng lưới nhỏ gọn: Hàng là tên thành viên, Cột là 3 tiêu chí (*Thái độ*, *Đúng hạn*, *Chất lượng*) chấm bằng **Icon Star Rating / Thanh chọn điểm 1–10**, tự động gắn icon cảnh báo (`AlertTriangle`) khi điểm trung bình dưới ngưỡng an toàn (nguy cơ *free-rider*).
7. **Feedback & Empty States:**
   - Mọi thao tác lưu/xóa/export đều phản hồi bằng `Sonner` Toast ở góc dưới bên phải.
   - Bảng trống hiển thị **Empty State** gồm Icon minh họa lớn + 1 dòng hướng dẫn ngắn + 1 nút CTA chính (VD: `+ Thêm Use Case`).
