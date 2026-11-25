# BACKUP & RESTORE GUIDE — PostgreSQL (`asc-postgres`)

Hướng dẫn sao lưu và khôi phục dữ liệu độc lập cho Smart Otter (`asc_postgres_data`) mà **không ảnh hưởng đến `skillswap-postgres`**.

---

## 1. Quy tắc an toàn dữ liệu

- **KHÔNG BAO GIỜ** chạy `docker compose -p smart-otter down -v` vì cờ `-v` sẽ xóa sạch volume `asc_postgres_data`.
- Không chỉnh sửa hay xóa các file Flyway migration đã áp dụng (`V1`, `V2`).
- Tất cả bảng (`users`, `student_profiles`, `guardian_links`, `timetables`, `study_plans`, `study_tasks`, `study_submissions`, `ai_evaluations`, `source_references`, `evaluation_jobs`, `knowledge_cache`, `ai_usage_logs`) đều nằm gọn trong database `study_companion` thuộc container `asc-postgres`.

---

## 2. Backup thủ công (Online Backup không cần tắt DB)

Tạo thư mục lưu backup trên VPS:
```bash
mkdir -p ~/backups/smart-otter
```

### Backup định dạng custom `.dump` (khuyên dùng — nén nhỏ, restore linh hoạt)
```bash
docker exec -t asc-postgres pg_dump \
  -U asc_user \
  -d study_companion \
  -Fc \
  > ~/backups/smart-otter/study_companion_$(date +%Y%m%d_%H%M%S).dump
```

### Kiểm tra file backup vừa tạo
```bash
ls -lh ~/backups/smart-otter/
```

---

## 3. Tự động Backup hằng ngày bằng Cron (Khuyến nghị)

Mở crontab trên Ubuntu:
```bash
crontab -e
```
Thêm dòng sau để backup lúc **02:30 sáng hằng ngày** (giờ VPS) và tự xóa file cũ quá 14 ngày:
```cron
30 2 * * * docker exec asc-postgres pg_dump -U asc_user -d study_companion -Fc > /root/backups/smart-otter/study_companion_$(date +\%Y\%m\%d_\%H\%M\%S).dump && find /root/backups/smart-otter -type f -name "*.dump" -mtime +14 -delete
```

---

## 4. Restore dữ liệu từ file `.dump`

1. Tạm dừng `backend` và `frontend` để tránh kết nối ghi đè trong lúc restore (giữ `postgres` chạy):
   ```bash
   docker compose -p smart-otter stop backend frontend
   ```
2. Restore đè sạch schema `public` vào database `study_companion`:
   ```bash
   cat ~/backups/smart-otter/study_companion_YYYYMMDD_HHMMSS.dump | \
   docker exec -i asc-postgres pg_restore \
     -U asc_user \
     -d study_companion \
     --clean \
     --if-exists \
     --no-owner
   ```
3. Bật lại `backend` & `frontend` và kiểm tra:
   ```bash
   docker compose -p smart-otter start backend frontend
   docker compose -p smart-otter ps
   ```

---

## 5. Lưu ý nếu từng tạo volume `asc_postgres_data` ngoài project `smart-otter`

- Khi chạy lần đầu trên VPS bằng lệnh chuẩn `docker compose -p smart-otter up -d`, Docker Compose tự gán nhãn volume `asc_postgres_data` cho project `smart-otter`.
- Nếu trước đó từng lỡ tạo volume `asc_postgres_data` dưới tên project khác và muốn tái sử dụng lại nguyên vẹn mà không mất dữ liệu: đổi phần khai báo cuối `docker-compose.yml` thành `external: true` (hoặc backup `.dump` trước, tạo stack `-p smart-otter` sạch, rồi restore lại).
