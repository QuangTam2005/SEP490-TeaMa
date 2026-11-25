# ENVIRONMENT SETUP — Smart Otter (`smart-otter`)

Tài liệu phân rõ các biến môi trường **Build-time vs Runtime**, quy tắc bảo mật secret và thông số chuẩn cho VPS.

---

## 1. Bảng biến môi trường (`.env`)

| Biến | Bắt buộc | Thời điểm đọc | Giá trị khuyên dùng (VPS Production) | Ghi chú |
| :--- | :---: | :--- | :--- | :--- |
| `POSTGRES_DB` | Tùy chọn | Runtime | `study_companion` | Tên DB trong `asc-postgres` |
| `POSTGRES_USER` | Tùy chọn | Runtime | `asc_user` | User sở hữu schema Flyway |
| `POSTGRES_PASSWORD` | **Có** | Runtime | *(mật khẩu mạnh >= 16 ký tự)* | Không dùng mật khẩu mẫu |
| `SPRING_PROFILES_ACTIVE` | Tùy chọn | Runtime | `prod` | Profile Spring Boot |
| `HIBERNATE_FORMAT_SQL` | Tùy chọn | Runtime | `false` | Tắt format SQL ở prod để giữ log gọn |
| `JWT_SECRET` | **Có** | Runtime | *(chuỗi ngẫu nhiên >= 32 ký tự)* | Ký HS256 JWT (`openssl rand -hex 32`) |
| `JWT_EXPIRATION_MS` | Tùy chọn | Runtime | `86400000` | 24 giờ |
| `CORS_ALLOWED_ORIGINS` | **Có** | Runtime | `https://domain.xyz` | Không dùng `*`; khớp origin domain thật |
| `APP_TIMEZONE` | Tùy chọn | Runtime | `Asia/Ho_Chi_Minh` | Múi giờ lập kế hoạch học ngày mai |
| `JAVA_TOOL_OPTIONS` | Tùy chọn | Runtime | `-XX:+UseContainerSupport -XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=70.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom` | Tối ưu JVM trong container `1536M` (VPS `Swap: 0B`) |
| `CKEY_API_BASE_URL` | **Có** | Runtime | `https://api.xah.io/v1` | Endpoint OpenAI-compatible của CKEY |
| `CKEY_API_KEY` | **Có** | Runtime | *(key thật từ `ckey.vn`)* | Chỉ nằm ở Backend |
| `MIMO_MODEL_ID` | **Có** | Runtime | `xztimee/laguna-xs-2.1` | Tên model LLM cấu hình trên CKEY |
| `BRAVE_API_KEY` | **Có** | Runtime | *(key thật từ Brave Search)* | Chỉ nằm ở Backend |
| `AI_MAX_SOURCES` | Tùy chọn | Runtime | `3` | Cáp cứng tối đa 3 nguồn / 3 request Brave |
| `AI_MAX_CONTEXT_TOKENS` | Tùy chọn | Runtime | `2048` | Ngân sách token prompt |
| `FRONTEND_HOST_PORT` | Tùy chọn | Compose Bind | `3100` | Port localhost cho Nginx trỏ tới |
| `NEXT_PUBLIC_API_URL` | Tùy chọn | **Build-time** | *(để trống `""`)* | **Quan trọng:** rỗng để browser gọi `/api/*` qua Next.js proxy |
| `INTERNAL_API_URL` | Tùy chọn | **Build-time** | `http://backend:8080` | Đích của Next.js rewrite `/api/:path*` |
| `NEXT_PUBLIC_DATA_MODE` | Tùy chọn | **Build-time** | `api` | Dùng API thật |

---

## 2. Phân biệt Build-time vs Runtime ở Next.js (Cực kỳ quan trọng)

1. **`NEXT_PUBLIC_API_URL`**, **`INTERNAL_API_URL`** và **`NEXT_PUBLIC_DATA_MODE`** được Next.js đọc lúc **`npm run build`** bên trong `frontend/Dockerfile`:
   - `NEXT_PUBLIC_API_URL` được nhúng trực tiếp vào JS bundle phía trình duyệt. Nếu điền `http://localhost:8080`, người dùng truy cập từ Internet sẽ bị lỗi vì trình duyệt gọi về máy cá nhân của họ! Vì vậy **bắt buộc để rỗng `NEXT_PUBLIC_API_URL=`** ở production (PA1).
   - `INTERNAL_API_URL` được ghi vào manifest `rewrites()` lúc `next build`. Vì Docker network đặt tên service là `backend`, giá trị chuẩn là `http://backend:8080`.
2. Nếu thay đổi 3 biến trên sau này, **bắt buộc build lại Frontend**:
   ```bash
   docker compose -p smart-otter up -d --build frontend
   ```

---

## 3. Bảo mật & Không commit Secret

- Tạo `JWT_SECRET` an toàn trên Ubuntu:
  ```bash
  openssl rand -base64 48 | tr -d '\n'
  ```
- `.gitignore` đã chặn `.env`, `.env.local`, `.env.*.local`, `*.pem`.
- `.dockerignore` ở cả `backend/` và `frontend/` chặn `.env*` không cho chép vào Docker image layer.
- Tuyệt đối không đặt key AI/Search/JWT vào biến tiền tố `NEXT_PUBLIC_*`.
