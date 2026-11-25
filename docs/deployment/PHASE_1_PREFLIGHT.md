# PHASE 1: VPS Pre-Flight Audit & Cô Lập Môi Trường (5-Phase Deployment)

Tài liệu này hướng dẫn **Phase 1** trong hành trình **5 Phase** đưa **Smart Otter – AI Study Companion** lên Ubuntu VPS đang chạy song song dự án `skillswap-*`, bảo đảm **tuyệt đối không chạm, không restart, không ảnh hưởng tới `skillswap-backend`, `skillswap-postgres`, `skillswap-rabbitmq` hay Nginx vhost cũ**.

---

## Tổng quan hành trình 5 Phase

1. **Phase 1 (Tài liệu này):** Kiểm tra tài nguyên/port/Docker/Nginx (read-only), tạo thư mục độc lập `/opt/smart-otter`, clone code và chuẩn bị `.env` an toàn (**chưa bật container**).
2. **Phase 2:** Hoàn thiện `.env` (API keys thật), chạy `docker compose -p smart-otter build && docker compose -p smart-otter up -d`, xác nhận Flyway `V1–V3` & healthcheck nội bộ `127.0.0.1:3100/api/healthcheck` (có thể test trước qua SSH tunnel khi chưa mua domain).
3. **Phase 3:** Tạo file Nginx riêng `/etc/nginx/sites-available/smart-otter.conf` (trỏ `127.0.0.1:3100`), chạy `sudo nginx -t && sudo systemctl reload nginx`, trỏ DNS domain thật và bật HTTPS (`certbot`).
4. **Phase 4:** Kiểm thử E2E thực tế trên domain thật và xác nhận các container `skillswap-*` giữ nguyên uptime.
5. **Phase 5:** Bật cron backup PostgreSQL (`BACKUP_RESTORE.md`), giám sát RAM/Disk, và áp dụng quy trình update/rollback an toàn.

---

## Quy tắc an toàn bắt buộc trước khi gõ lệnh trên VPS

> [!CAUTION]
> Khi thao tác SSH trên VPS:
> - **KHÔNG** chạy `docker compose down`, `docker restart`, `docker network prune`, `docker volume prune` hay `docker system prune` không có `-p smart-otter`.
> - **KHÔNG** đụng tới thư mục dự án cũ (`fu-skillswap-be`) hoặc bất kỳ file nào trong `/etc/nginx/sites-enabled/` hiện tại.
> - Luôn gắn cờ **`-p smart-otter`** cho mọi lệnh `docker compose`.

---

## Các bước thực hiện Phase 1 trên Ubuntu VPS

### Bước 1.1: Kiểm tra phiên bản Docker, tài nguyên & xác nhận `skillswap-*` ổn định (Read-Only)
SSH vào VPS và chạy các lệnh chỉ đọc sau:

```bash
# 1. Kiểm tra Docker Engine & Docker Compose v2 (yêu cầu >= v2.x)
docker --version
docker compose version

# 2. Kiểm tra RAM trống (>= 4 GB) và Disk trống (>= 10 GB)
free -h
df -h /

# 3. Lưu lại trạng thái ban đầu của stack cũ để đối chiếu
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

Nếu chưa có `docker compose` plugin (v2):
```bash
sudo apt-get update && sudo apt-get install -y docker-compose-plugin
```

---

### Bước 1.2: Kiểm tra xung đột Port, Network, Volume & Nginx (Read-Only)

```bash
# Xác nhận port 3100 ĐANG TRỐNG (không dùng 8080 hay 5444 của skillswap)
ss -tulpn | grep -E '3100|3000|8080|5432|5444|80|443'

# Xác nhận chưa tồn tại asc-postgres / asc-backend / asc-frontend / asc_network
docker ps -a --format "table {{.Names}}\t{{.Ports}}"
docker network ls
docker volume ls

# Kiểm tra Nginx hiện tại (chỉ đọc, chưa sửa file nào)
ls -la /etc/nginx/sites-enabled/
sudo nginx -t
```

Kiểm tra Firewall (nếu dùng UFW) — bảo đảm **không** mở public port `3100`, `5432`, `8080`:
```bash
sudo ufw status verbose
```

---

### Bước 1.3: Tạo thư mục độc lập `/opt/smart-otter` & Clone Repository

> *(Nếu bạn muốn đặt trong thư mục home thay vì `/opt/smart-otter`, có thể dùng `mkdir -p ~/apps && cd ~/apps` thay thế.)*

```bash
sudo mkdir -p /opt/smart-otter
sudo chown -R "$USER":"$USER" /opt/smart-otter
cd /opt/smart-otter

git clone https://github.com/QuangTam2005/SEP490-TeaMa.git .
git status
```

---

### Bước 1.4: Chuẩn bị file `.env` Production (Chưa bật container)

Tạo `.env` với quyền đọc/ghi chỉ dành cho chủ sở hữu (`chmod 600`):
```bash
cd /opt/smart-otter
cp .env.example .env
chmod 600 .env
```

Sinh 3 chuỗi bí mật ngẫu nhiên để dán vào `.env`:
```bash
echo "POSTGRES_PASSWORD=$(openssl rand -hex 20)"
echo "JWT_SECRET=$(openssl rand -hex 32)"
echo "OPS_ADMIN_SECRET=$(openssl rand -hex 24)"
```

Mở `.env` (`nano .env`) và kiểm tra/điền tối thiểu các biến sau (giữ nguyên phần PA1):
- `SPRING_PROFILES_ACTIVE=prod`
- `POSTGRES_PASSWORD=` *(dán giá trị vừa sinh)*
- `JWT_SECRET=` *(dán chuỗi >= 32 ký tự vừa sinh)*
- `OPS_ADMIN_SECRET=` *(dán chuỗi vừa sinh — dùng cho `GET /api/ops/usage`)*
- `CKEY_API_BASE_URL=https://api.xah.io/v1`
- `CKEY_API_KEY=` *(key CKEY thật)*
- `MIMO_MODEL_ID=xztimee/laguna-xs-2.1`
- `BRAVE_API_KEY=` *(key Brave Search thật)*
- `CORS_ALLOWED_ORIGINS=http://localhost:3100` *(giữ `http://localhost:3100` nếu Phase 2 test qua SSH tunnel; đổi thành `https://domain.xyz` ở Phase 3 khi gắn domain thật)*
- `FRONTEND_HOST_PORT=3100`
- `NEXT_PUBLIC_API_URL=` *(BẮT BUỘC để trống)*
- `INTERNAL_API_URL=http://backend:8080`
- `NEXT_PUBLIC_DATA_MODE=api`

---

### Bước 1.5: Chạy Script Kiểm Định Phase 1 (Read-Only Gate)

Chạy script kiểm tra tự động (script **không** sửa hệ thống hay đụng container cũ):
```bash
cd /opt/smart-otter
bash docs/deployment/scripts/phase1-preflight-check.sh
```

**Tiêu chí hoàn tất Phase 1 (Exit Gate sang Phase 2):**
- Toàn bộ mục kiểm tra chính báo `[PASS]` và `FAIL=0`.
- File `.env` có quyền `600`, không thiếu key bắt buộc, `docker compose -p smart-otter config --quiet` hợp lệ.
- `docker ps` cho thấy 3 container `skillswap-backend`, `skillswap-postgres`, `skillswap-rabbitmq` không bị ảnh hưởng.

> **Sẵn sàng bước sang Phase 2:** Chạy `docker compose -p smart-otter build && docker compose -p smart-otter up -d` và test qua SSH tunnel (`ssh -L 3100:127.0.0.1:3100 user@vps-ip`) tại `http://localhost:3100`!
