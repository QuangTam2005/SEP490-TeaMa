# DOCKER ARCHITECTURE — Smart Otter (`smart-otter`)

## 1. Tổng quan kiến trúc 3 Container

```text
Internet (80/443)
   │
   ▼
[Ubuntu Host Nginx]  ── proxy_pass http://127.0.0.1:3100 ──┐
                                                             │
┌── Docker Bridge Network: asc_network (Isolated) ───────────▼───────────────┐
│                                                                            │
│  ┌──────────────────────┐   INTERNAL_API_URL    ┌───────────────────────┐  │
│  │ asc-frontend         │ ────────────────────► │ asc-backend           │  │
│  │ Next.js 14 standalone│   http://backend:8080 │ Spring Boot 3.4 / J21 │  │
│  │ (user: nextjs)       │     (/api/* rewrite)  │ (user: spring)        │  │
│  │ host: 127.0.0.1:3100 │                       │ NO host ports!        │  │
│  └──────────────────────┘                       └──────────┬────────────┘  │
│                                                            │               │
│                                              jdbc:postgresql://postgres:5432
│                                                            │               │
│                                                 ┌──────────▼────────────┐  │
│                                                 │ asc-postgres          │  │
│                                                 │ postgres:16-alpine    │  │
│                                                 │ vol: asc_postgres_data│  │
│                                                 │ NO host ports!        │  │
│                                                 └───────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Chi tiết cô lập Network & Port

| Service | Container Name | Internal Port | Host Port Binding | Lý do thiết kế |
| :--- | :--- | :--- | :--- | :--- |
| `postgres` | `asc-postgres` | `5432` | **Không mở** | Tránh lộ DB ra Internet; chỉ `asc-backend` truy cập qua `postgres:5432` trên `asc_network`. |
| `backend` | `asc-backend` | `8080` | **Không mở** | Tránh đụng `127.0.0.1:8080` của `skillswap-backend` và không lộ API port ra ngoài. |
| `frontend` | `asc-frontend` | `3000` | `127.0.0.1:${FRONTEND_HOST_PORT:-3100}:3000` | Chỉ cho phép Nginx trên chính Ubuntu host kết nối qua loopback `127.0.0.1`. |

- **Network:** `asc_network` (`driver: bridge`), hoàn toàn tách rời `fu-skillswap-be_skillswap-network`. Không dùng `network_mode: host` và không gán IP tĩnh.
- **Volume:** `asc_postgres_data` lưu tại Docker volume riêng, tồn tại xuyên suốt các lần `docker compose down` / recreate container.

---

## 3. Hardening trong Dockerfile & Compose

1. **Multi-stage & Non-root Containers:**
   - `backend/Dockerfile`: Stage 1 (`eclipse-temurin:21-jdk-alpine`) build JAR bằng `./mvnw package -DskipTests`; Stage 2 (`eclipse-temurin:21-jre-alpine`) chỉ chứa `app.jar`, chạy dưới user **`spring:spring` (UID 1001)**.
   - `frontend/Dockerfile`: Stage `deps` cài gói $\rightarrow$ Stage `builder` build Next.js standalone $\rightarrow$ Stage `runner` (`node:20-alpine`) chỉ copy `.next/standalone`, `.next/static`, `public` và chạy dưới user **`nextjs:nodejs` (UID 1001)**.
2. **Bảo mật Runtime:**
   - `security_opt: ["no-new-privileges:true"]` cho cả 3 services.
   - Không chạy `privileged: true`, không mount `/var/run/docker.sock`.
   - `.dockerignore` ở cả `backend/` và `frontend/` loại bỏ `.env*`, `.git`, `target/`, `node_modules/`.
3. **Thứ tự khởi động & Healthcheck:**
   - `postgres`: `pg_isready` (interval 10s, timeout 5s, retries 5).
   - `backend`: đợi `postgres` **healthy** rồi mới start; tự kiểm tra `GET http://127.0.0.1:8080/api/healthcheck` (chạy `SELECT 1` vào PostgreSQL).
   - `frontend`: đợi `backend` **healthy** rồi mới start; tự kiểm tra `GET http://127.0.0.1:3000/login`.
4. **Graceful Shutdown & Log Rotation:**
   - `backend` bật `server.shutdown: graceful` (`30s`) + Compose `stop_grace_period: 35s` để xử lý nốt request & job AI đang chạy trước khi container dừng.
   - Cả 3 container dùng `logging: json-file` với `max-size: "10m"`, `max-file: "3"` (tối đa ~30 MB log/container).
