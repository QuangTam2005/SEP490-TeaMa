# VPS NGINX INTEGRATION — Smart Otter (`domain.xyz`)

Hướng dẫn tích hợp **Smart Otter** vào Nginx đang chạy trên Ubuntu host mà **không ghi đè cấu hình hay ảnh hưởng dự án cũ (`skillswap-backend`)**.

---

## 1. Nguyên tắc an toàn

- **KHÔNG** chỉnh sửa file site cấu hình của dự án cũ trong `/etc/nginx/sites-enabled/`.
- **KHÔNG** sửa hay restart Nginx khi chưa chạy `sudo nginx -t` pass 100%.
- **KHÔNG** mở thêm port công khai mới ngoài `80` / `443` của Nginx.

---

## 2. Cấu hình mẫu (Phương án 1 — Khuyên dùng: 1 Domain duy nhất)

Ở Phương án 1:
- `asc-frontend` chỉ lắng nghe trên **`127.0.0.1:3100`**.
- `asc-backend` **không mở port ra host**; toàn bộ `/api/*` đi qua Next.js rewrite nội bộ sang `http://backend:8080`.

Tạo **1 file cấu hình mới hoàn toàn riêng biệt** `/etc/nginx/sites-available/smart-otter.conf` (thay `domain.xyz` bằng domain thật của bạn):

```nginx
server {
    listen 80;
    listen [::]:80;
    server_name domain.xyz;

    # Tăng giới hạn body phòng trường hợp payload lớn trong tương lai
    client_max_body_size 10M;

    location / {
        proxy_pass http://127.0.0.1:3100;
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # Phù hợp proxyTimeout của Next.js (AI evaluation & polling)
        proxy_connect_timeout 30s;
        proxy_send_timeout 120s;
        proxy_read_timeout 120s;
    }
}
```

### Kích hoạt & Kiểm tra an toàn
```bash
# Tạo symlink sang sites-enabled
sudo ln -s /etc/nginx/sites-available/smart-otter.conf /etc/nginx/sites-enabled/smart-otter.conf

# BẮT BUỘC kiểm tra cú pháp toàn bộ Nginx trước khi reload
sudo nginx -t

# Chỉ reload (không drop kết nối đang chạy của dự án cũ)
sudo systemctl reload nginx
```

---

## 3. Kích hoạt HTTPS (SSL/TLS) sau khi đã trỏ DNS domain thật

Sau khi domain đã trỏ bản ghi DNS `A` về IP của VPS:
```bash
sudo certbot --nginx -d domain.xyz
```
*(Backend đã bật `server.forward-headers-strategy: framework` trong `application.yml`, nên khi đứng sau HTTPS reverse proxy, Spring nhận chính xác `X-Forwarded-Proto: https`.)*

---

## 4. Ghi chú: Nếu sau này muốn tách `api.domain.xyz` (Phương án 2 — Tuỳ chọn)

Nếu về sau bạn muốn tách `api.domain.xyz` gọi thẳng vào Backend:
1. Mở thêm binding **chỉ loopback** cho service `backend` với port khác `8080` (vì `skillswap-backend` đã chiếm `127.0.0.1:8080`), ví dụ `127.0.0.1:8081:8080`.
2. Build lại `frontend` với `NEXT_PUBLIC_API_URL=https://api.domain.xyz` và cập nhật `.env` `CORS_ALLOWED_ORIGINS=https://domain.xyz`.
3. Thêm block `server_name api.domain.xyz` trỏ `proxy_pass http://127.0.0.1:8081;`.
*(Khi chạy Phương án 1, bạn không cần làm bước này.)*
