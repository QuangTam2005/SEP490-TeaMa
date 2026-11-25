# AI CODING RULES & CONSTRAINTS (RULES.md)
**Project:** AI Study Companion – Grade 6 (`SRS-ASC-G6-001`)  
**Purpose:** Quy tắc bắt buộc cho mọi AI Agent và thành viên khi thiết kế, viết code hoặc chỉnh sửa repository.

---

## 1. General & Workflow Rules
1. **Context-First Execution:** Trước khi bắt đầu bất kỳ tác vụ nào, AI bắt buộc đọc lại `docs/*.md` (`PRD.md`, `ARCHITECTURE.md`, `DESIGN.md`, `RULES.md`, `TASKS.md`, `PROGRESS.md`).
2. **Minimal Diff & Phased Anti-Hallucination:** Chỉ sửa đúng các file thuộc task hiện tại. Không gộp nhiều phase hoặc đoán tên bảng/API chưa định nghĩa.
3. **Auto-Update Progress & Atomic Commits:** Sau khi kiểm thử pass 100%, đánh dấu `[x]` vào `docs/TASKS.md`, cập nhật `docs/PROGRESS.md` và commit riêng 1 task = 1 atomic commit.
4. **Language Convention:**
   - Code, biến, tên hàm, tên bảng DB, comment kỹ thuật, commit message: **100% Tiếng Anh**.
   - Nhãn giao diện (UI Labels), thông báo lỗi, phản hồi học sinh: **Tiếng Việt** rõ ràng, tích cực, không mang tính trách phạt (`NFR-ACC-01`), tuân thủ nguyên tắc **Minimal-Text**.
5. **Open Email Validation Policy:** Chấp nhận mọi địa chỉ email hợp lệ bất kỳ (`@Email` / Zod `.email()`), không giới hạn bất kỳ domain trường học cụ thể nào.

---

## 2. Domain & AI Pipeline Red Lines (`SRS BR-01..BR-24`)
1. **Identical Character Counter (`BR-12`):** Cả Frontend và Backend phải dùng cùng quy tắc chuẩn hóa `\r\n` $\rightarrow$ `\n`, `.trim()` hai đầu, sau đó đếm toàn bộ ký tự (gồm khoảng trắng và dấu câu bên trong) và khóa chặt trong `[850, 2000]` cho cả `REVIEW` và `PREVIEW`.
2. **Strict Task Sequencing & Version Cap (`BR-08`, `v1..v3`):**
   - `PREVIEW` chỉ được mở khóa sau khi `REVIEW` cùng môn/ngày đã được Submit và lưu thành công.
   - Sau khi `EVALUATED`, Student được phép nộp bản cập nhật mới (**Revision**), **tối đa 3 phiên bản tổng cộng (`v1`, `v2`, `v3` $\rightarrow$ `revisionCount <= 2`)** cho mỗi nhiệm vụ; cấm sửa trong lúc đang `EVALUATING`; mọi phiên bản đã Submit đều bất biến (`BR-21`).
   - Student tự chọn khối lớp của mình (`1–12`), không hard-code khối 6; cả Student và Guardian đã liên kết đều có thể cập nhật Thời khóa biểu tuần.
3. **Save Before AI & No AI While Typing (`BR-13`, `BR-14`):** Tuyệt đối không gọi Brave Search hoặc MiMo khi đang nhập liệu. Backend bắt buộc lưu `StudySubmission` và `EvaluationJob` vào PostgreSQL trước khi gọi API ngoài; sử dụng `Idempotency-Key` chống trùng job.
4. **AI Privacy, KNTT + Wikipedia Grounding & 3-Retry Fallback (`BR-17`, `BR-18`, `BR-23`):**
   - Tuyệt đối không gửi tên thật, email, ID học sinh hoặc lịch sử chat sang Brave/MiMo; chỉ gửi `grade`, `subject`, `taskType`, `lessonTitle`, `studentText`.
   - Tìm kiếm đối chiếu từ **Wikipedia** (`vi.wikipedia.org`) và các website có nội dung về bài học đó (ưu tiên SGK **Kết nối tri thức với cuộc sống**), truy vấn theo tựa đề bài học và nội dung chính trích từ bài soạn/ôn; mỗi lần review bài chỉ gửi **tối đa 3 request vào Brave** và giữ tối đa **3 nguồn** để tiết kiệm token.
   - Nếu AI lỗi khi xử lý, **retry tối đa 3 lần**. Quá 3 lần vẫn lỗi thì **ngưng retry**, ghi nhận nhận xét fallback báo hệ thống AI lỗi để phụ huynh tự kiểm tra bài làm và chuyển trạng thái qua Guardian như đã review xong (không làm tính năng check thủ công riêng).
   - Phần nhận xét AI hiển thị cho Student phải có độ dài **tối thiểu 300 ký tự và tối đa 600 ký tự** (tính cả dấu câu và khoảng trắng) — không cắt ngang câu chữ ngẫu nhiên (`SRS §11`).
5. **Secret & Cost Protection (`NFR-SEC-02`, `§3.2`):** `CKEY_API_KEY`, `BRAVE_API_KEY`, `JWT_SECRET` chỉ nằm ở Backend; không bao giờ log secret hoặc trả `AIUsageLog` ra Guardian Dashboard thông thường.

---

## 3. Backend Rules (`backend/` - Java 21 / Spring Boot 3.4)
1. **Database Schema & Migration:** Bắt buộc `spring.jpa.hibernate.ddl-auto=validate`. Mọi thay đổi schema đi qua Flyway `V{version}__{description}.sql`.
2. **Layering & Anti-IDOR:** `Controller` $\rightarrow$ `Service` $\rightarrow$ `Repository` $\rightarrow$ `DTO`. Mọi thao tác đọc/ghi của `STUDENT` hoặc `GUARDIAN` phải xác thực quyền sở hữu (`StudentProfile`) hoặc liên kết hợp lệ (`GuardianLink`) tại server (`AC-17`).
3. **Transactions & Retry Safety:** Phương thức Service ghi dùng `@Transactional`; đọc dùng `@Transactional(readOnly = true)`. Worker AI retry có giới hạn (`timeout`/`429`/`5xx`) bằng exponential backoff + jitter; lỗi validation không retry AI.

---

## 4. Frontend Rules (`frontend/` - Next.js 14 / TypeScript)
1. **Strict TypeScript:** Cấm `any` hoặc `@ts-ignore`. Toàn bộ DTO khai báo trong `src/types/`.
2. **UI System:** Giữ `Be Vietnam Pro` + `JetBrains Mono`, duy nhất `lucide-react` (`strokeWidth={1.75}`), **Ocean Blue (`#0284C7` / `#0369A1`) + White (`#FFFFFF`)**, `1px hairline` Light/Dark, thao tác đơn giản cho học sinh THCS (`>= 44px` tap target) và Minimal-Text.
3. **App Router & Data Fetching:** Giữ `page.tsx`/`layout.tsx` Server Component mỏng khi không cần client state; mọi request đi qua `src/services/` $\rightarrow$ `src/lib/api-client.ts` + TanStack Query v5; mọi form validate bằng **React Hook Form + Zod**.
