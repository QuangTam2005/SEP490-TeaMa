# AI CODING RULES & CONSTRAINTS (RULES.md)
**Project:** SEP490 Capstone Copilot (`SEP490-TeaMa`)  
**Purpose:** Quy tắc bắt buộc tuân thủ cho mọi AI Agent và thành viên khi sinh code hoặc chỉnh sửa repository.

---

## 1. General & Workflow Rules
1. **Context-First Execution:** Trước khi bắt đầu bất kỳ tác vụ nào (lập kế hoạch, viết code, sửa cấu hình), AI bắt buộc phải đọc lại toàn bộ các file `.md` trong `docs/*.md` (`PRD.md`, `ARCHITECTURE.md`, `DESIGN.md`, `RULES.md`, `TASKS.md`, `PROGRESS.md` và mọi file tài liệu bổ sung nếu có) để nắm chính xác ngữ cảnh, ràng buộc kỹ thuật và trạng thái hiện tại.
2. **Minimal Diff Principle:** Chỉ sửa đổi hoặc thêm mới các file liên quan trực tiếp đến task đang thực hiện. Tuyệt đối không tự ý format lại toàn bộ file cũ, không đổi tên biến/hàm đang chạy ổn định ở các module khác.
3. **Auto-Update Progress:** Ngay sau khi hoàn thành và kiểm thử xong một task trong `TASKS.md`, AI phải đánh dấu `[x]` vào `TASKS.md` và cập nhật trạng thái vào `docs/PROGRESS.md`.
4. **Language Convention:**
   - Code, tên biến, tên hàm, tên bảng DB, comment kỹ thuật, Git commit message: **100% Tiếng Anh**.
   - Nhãn giao diện (UI Labels), thông báo lỗi hiển thị cho người dùng, tài liệu hướng dẫn: **Tiếng Việt** (kết hợp đúng thuật ngữ chuyên ngành SEP490 như *Use Case, WBS, RACI, Sprint, Iteration, Milestone*).

---

## 2. Backend Rules (`backend/` - Java 21 / Spring Boot 3.3+)
1. **Database Schema & Migration (Lằn ranh đỏ):**
   - Cấm tuyệt đối cấu hình `spring.jpa.hibernate.ddl-auto=create` hoặc `update`. Bắt buộc dùng `validate`.
   - Mọi thay đổi về bảng, cột, index đều phải viết thông qua file SQL migration mới trong `src/main/resources/db/migration/` theo chuẩn đặt tên Flyway (`V{version}__{description}.sql`).
   - Mọi cột `code` trong các bảng có Soft-Delete (`features`, `use_cases`, `wbs_tasks`, `screens_functions`, `business_rules`, `app_messages`, `test_cases`, `tracking_logs`) bắt buộc dùng **Partial Unique Index**: `CREATE UNIQUE INDEX ... ON <table> (project_id, code) WHERE is_deleted = FALSE`.
2. **Architecture, Layering & Project-Scope Isolation (Chống IDOR):**
   - Tuân thủ nghiêm ngặt luồng: `Controller` $\rightarrow$ `Service` $\rightarrow$ `Repository`. Không viết business logic trong Controller; không trả trực tiếp JPA `@Entity` ra ngoài API (bắt buộc map sang `Request/Response DTO`).
   - **Project-Scope Isolation:** Mọi thao tác đọc/sửa/xóa thực thể con (`taskId`, `useCaseId`, `screenId`, `testCaseId`...) bắt buộc phải truy vấn kèm `projectId` (`findByIdAndProjectId(id, projectId)`) sau khi đã xác thực user thuộc `project_members` (hoặc có `X-Supervisor-Token` hợp lệ của chính project đó).
   - Mọi REST API trả về JSON đều phải bọc trong class chuẩn `ApiResponse<T>`.
   - Mọi Exception nghiệp vụ phải ném ra `AppException` (kèm `ErrorCode` enum) và được bắt tập trung tại `GlobalExceptionHandler`.
   - **Transaction & Query Optimization:** Mọi phương thức Service ghi dữ liệu (Create/Update/Soft-Delete) bắt buộc gắn `@Transactional`; các phương thức chỉ đọc gắn `@Transactional(readOnly = true)`. Tránh lỗi N+1 query bằng `@EntityGraph` hoặc `JOIN FETCH` khi tải danh sách có quan hệ cha-con (như Traceability Matrix).
3. **Audit & Soft-Delete (Chuẩn Hibernate 6.4+):**
   - Không dùng `repository.delete()` (Hard Delete) cho các thực thể nghiệp vụ. Bắt buộc cập nhật `is_deleted = true`.
   - Sử dụng **`@SQLRestriction("is_deleted = false")`** (chuẩn Hibernate 6.3+, không dùng `@Where` đã bị deprecated) trên các Entity có Soft-Delete, và bắt buộc thêm điều kiện `is_deleted = FALSE` trong mọi câu native query thống kê (`Test Coverage`, `Total Man-days`, `Contribution`).
   - Mọi thao tác Thêm (`A`), Sửa (`M`), Xóa (`D`) trên dữ liệu báo cáo phải kích hoạt event/AOP ghi vào bảng `document_change_logs`.
4. **Dependency Lock:** Không tự ý thêm dependency mới vào `pom.xml` nếu không được sự đồng ý của chủ dự án.

---

## 3. Frontend Rules (`frontend/` - Next.js 14 / TypeScript)
1. **TypeScript Strictness (Lằn ranh đỏ):**
   - Cấm tuyệt đối sử dụng kiểu `any` hoặc `@ts-ignore`. Mọi API payload và response đều phải định nghĩa `interface` hoặc `type` rõ ràng trong thư mục `src/types/`.
2. **UI, Icon-First & Typography Compliance:**
   - Sử dụng font `Be Vietnam Pro` (subset `vietnamese`) làm font chính và `JetBrains Mono` cho các mã định danh (`FE-01`, `UC-01`, `TASK-01`, `BR-01`).
   - Tuân thủ nguyên tắc **Icon-First**: sử dụng duy nhất thư viện `lucide-react` (`strokeWidth={1.75}`, size `16px`/`20px`), kết hợp `Shadcn Tooltip` cho các nút thao tác bảng; không dùng emoji thô.
   - Ưu tiên tái sử dụng component từ `src/components/ui/` (Shadcn/UI) và tuân thủ bảng màu trong `docs/DESIGN.md` (v1.2). Không viết inline CSS style bừa bãi.
3. **App Router Boundary, Data Fetching & Forms:**
   - Giữ `layout.tsx` và `page.tsx` làm Server Component mỏng (để cấu hình `metadata` và `next/font`); đặt `'use client'` ở các Interactive Feature Components.
   - Mọi lời gọi API phía client đều phải đi qua Axios instance chuẩn (`src/lib/api-client.ts`) có interceptor tự động đính kèm `Authorization: Bearer <jwt>` (cho Student Workspace) hoặc `X-Supervisor-Token: <token>` (cho Supervisor Portal) và quản lý cache bằng **TanStack Query**.
   - Mọi Form nhập liệu đều phải validate bằng **React Hook Form + Zod**.

---

## 4. Git & Security Constraints
- **Secrets Protection:** Tuyệt đối không hardcode mật khẩu DB, JWT Secret, hoặc Webhook Secret vào source code. Luôn đọc từ biến môi trường (`application.yml` / `.env`).
- **File Upload Safety:** Mọi endpoint nhận file upload bắt buộc kiểm tra đồng thời kích thước tối đa (`50MB` cho Deliverables; `5MB`/ảnh và tối đa `2` ảnh cho Test Case Evidence), whitelist phần mở rộng (`.docx`, `.pdf`, `.xlsx`, `.zip`, `.png`, `.jpg`, `.webp` — riêng Test Evidence chỉ cho phép ảnh `.png`, `.jpg`, `.webp`) và chống lỗ hổng Path Traversal khi lưu file xuống ổ đĩa.
- **Commit Convention:** Tuân thủ Conventional Commits gắn mã Task (VD: `feat(wbs): [TASK-3.1] implement complexity auto-calculator`).
- **Atomic Commits (Lằn ranh đỏ):**
  - Mỗi tính năng mới (`feat`), mỗi bản sửa lỗi (`fix`), hoặc mỗi file migration/cấu hình độc lập bắt buộc phải được tách thành **1 commit riêng biệt**.
  - Tuyệt đối cấm gộp nhiều tính năng hoặc dồn hàng ngàn dòng code vào một commit duy nhất (tránh "mega-commit"), đảm bảo lịch sử Git minh bạch, dễ review và dễ `git revert` khi cần.
