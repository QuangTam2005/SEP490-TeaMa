# DEPLOYMENT GUIDE — Smart Otter (`smart-otter`) on Ubuntu VPS

Hướng dẫn triển khai độc lập **Smart Otter – AI Study Companion** trên Ubuntu VPS đang chạy song song dự án `skillswap-*`, đảm bảo **không chạm tới container, port, volume hay cấu hình Nginx của dự án cũ**.

---

## 1. Chuẩn bị Ubuntu & Kiểm tra trước khi deploy

### 1.1. Kiểm tra Docker & Docker Compose v2
```bash
docker --version
docker compose version
```
*(Bắt buộc dùng Docker Compose **v2** `docker compose` — không dùng `docker-compose` v1 vì `deploy.resources.limits` có thể bị bỏ qua nếu thiếu flag `--compatibility`.)*

Nếu VPS chưa có Docker Compose plugin:
```bash
sudo apt-get update && sudo apt-get install -y docker-compose-plugin
```

### 1.2. Kiểm tra xung đột Port & Tên tài nguyên
```bash
# Kiểm tra port Frontend dự kiến (mặc định 3100) và các port phổ biến
ss -tulpn | grep -E '3100|3000|8080|5432|80|443'

# Kiểm tra không trùng tên container / network / volume với dự án khác
docker ps -a --format "table {{.Names}}\t{{.Ports}}"
docker network ls
docker volume ls
```
> Trên VPS đã ghi nhận `skillswap-backend` dùng `127.0.0.1:8080` và `skillswap-postgres` dùng `127.0.0.1:5444`. Smart Otter **không publish** port `5432` hay `8080` ra host, và `asc-frontend` chỉ bind vào `127.0.0.1:3100`.

---

## 2. Cấu hình môi trường (`.env`)

Tại thư mục repo Smart Otter trên VPS:
```bash
cp .env.example .env
chmod 600 .env
nano .env
```
Điền tối thiểu các biến bắt buộc: `POSTGRES_PASSWORD`, `JWT_SECRET` (>= 32 ký tự), `CKEY_API_BASE_URL`, `CKEY_API_KEY`, `MIMO_MODEL_ID`, `BRAVE_API_KEY`, `CORS_ALLOWED_ORIGINS` (ví dụ `https://domain.xyz`), và giữ `NEXT_PUBLIC_API_URL=` (rỗng) để đi qua Next.js proxy.

Xem chi tiết tại [`ENVIRONMENT_SETUP.md`](./ENVIRONMENT_SETUP.md).

---

## 3. Build Image & Deploy lần đầu

```bash
# Kiểm tra cú pháp và biến thay thế trước khi chạy
docker compose -p smart-otter config

# Build image (không ảnh hưởng bất kỳ container nào của dự án khác)
docker compose -p smart-otter build

# Khởi chạy tách nền theo thứ tự healthy: postgres -> backend -> frontend
docker compose -p smart-otter up -d
```

> **Lưu ý về Next.js build & Google Fonts:** Frontend sử dụng `next/font/google` nên ở bước `docker compose build`, container build cần kết nối Internet ra `fonts.googleapis.com` để tải font vào bundle `.next/static`. Khi đã build xong, container chạy runtime hoàn toàn tự phục vụ font nội bộ.

---

## 4. Kiểm tra Health, Logs & Tài nguyên

```bash
# 1. Trạng thái & health của 3 container
docker compose -p smart-otter ps

# 2. Kiểm tra endpoint health nội bộ Frontend & Proxy API
curl -i http://127.0.0.1:3100/login
curl -i http://127.0.0.1:3100/api/healthcheck

# 3. Xem logs riêng từng service (không trộn lẫn dự án khác)
docker compose -p smart-otter logs -f backend
docker compose -p smart-otter logs -f frontend
docker compose -p smart-otter logs -f postgres

# 4. Theo dõi CPU / RAM thực tế
docker compose -p smart-otter stats --no-stream
```

---

## 5. Vận hành hàng ngày (Restart / Stop / Update / Rollback)

### Restart riêng từng service
```bash
docker compose -p smart-otter restart backend
docker compose -p smart-otter restart frontend
```

### Tạm dừng Smart Otter (giữ nguyên dữ liệu PostgreSQL)
```bash
docker compose -p smart-otter stop
# Hoặc gỡ container nhưng GIỮ volume (TUYỆT ĐỐI KHÔNG thêm cờ -v)
docker compose -p smart-otter down
```

### Update phiên bản mới (Zero impact tới project cũ)
1. Backup DB trước khi update (xem [`BACKUP_RESTORE.md`](./BACKUP_RESTORE.md)).
2. Ghi lại tag/digest image hiện tại để phòng rollback:
   ```bash
   docker tag smart-otter-backend:latest smart-otter-backend:rollback
   docker tag smart-otter-frontend:latest smart-otter-frontend:rollback
   ```
3. Kéo source code mới (`git pull origin main` — giữ nguyên `.env` trên VPS).
4. Rebuild và rollout đúng stack `smart-otter`:
   ```bash
   docker compose -p smart-otter up -d --build
   docker compose -p smart-otter ps
   ```

### Rollback nhanh khi bản mới gặp sự cố
```bash
docker compose -p smart-otter stop backend frontend
docker tag smart-otter-backend:rollback smart-otter-backend:latest
docker tag smart-otter-frontend:rollback smart-otter-frontend:latest
docker compose -p smart-otter up -d --no-build backend frontend
```
*(Nếu bản update có thêm migration DB mới, hãy restore lại file backup PostgreSQL trước khi chạy bản code cũ.)*
