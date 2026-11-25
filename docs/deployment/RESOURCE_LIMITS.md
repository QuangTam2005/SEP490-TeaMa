# RESOURCE LIMITS & CAPACITY PLANNING — Smart Otter (`smart-otter`)

## 1. Thông số VPS thực tế & Dung lượng trống

Đối chiếu từ kiểm tra VPS thực tế:
- **CPU:** 8 vCPU
- **RAM:** 15 GiB (`Swap: 0B`, đang available **~13 GiB** trước khi triển khai Smart Otter)
- **Disk:** 178 GiB (đang dùng 17 GiB / 10%, trống **152 GiB**)
- **Dự án cũ (`skillswap-*`):**
  - `skillswap-backend`: limit `3 GiB` (đang dùng ~749 MiB)
  - `skillswap-rabbitmq`: limit `1 GiB` (đang dùng ~161 MiB)
  - `skillswap-postgres`: limit `2 GiB` (đang dùng ~105 MiB)
  - **Tổng trần dự án cũ:** `6 GiB` RAM (thực tế dùng ~1 GiB)

---

## 2. Giới hạn tài nguyên Smart Otter (`docker-compose.yml`)

| Container | CPU Limit | Memory Limit | Memory Reservation | Ghi chú kỹ thuật |
| :--- | :---: | :---: | :---: | :--- |
| `asc-backend` | `1.0` | `1536 MB` | `512 MB` | JVM đặt `MaxRAMPercentage=70.0` (~1075 MB max heap; phần còn lại cho metaspace/thread/off-heap). |
| `asc-frontend` | `0.5` | `768 MB` | `256 MB` | Next.js standalone Node 20 runtime. |
| `asc-postgres` | `0.75` | `1536 MB` | `512 MB` | PostgreSQL 16 Alpine + Flyway 14 bảng + KnowledgeCache. |
| **Tổng Smart Otter** | **2.25 vCPU** | **3.75 GB (`3840 MB`)** | **1.25 GB (`1280 MB`)** | Giới hạn trần tối đa, không chiếm cứng RAM khi nhàn rỗi. |

---

## 3. Đánh giá độ an toàn khi chạy song song trên VPS 15 GiB (`Swap: 0B`)

- **Kịch bản bình thường:** `skillswap` (~1 GiB) + Smart Otter (~1–1.5 GiB) + OS/Nginx (~0.6 GiB) $\approx$ **3 GB / 15 GB** (còn dư ~12 GB).
- **Kịch bản xấu nhất (cả 2 project cùng chạm đỉnh limit đồng thời):** `6 GiB (skillswap)` + `3.75 GiB (Smart Otter)` + `1 GiB (OS/Nginx)` = **`10.75 GiB / 15 GiB`** $\rightarrow$ vẫn còn **~4.25 GiB dự phòng**, tránh OOM toàn VPS kể cả khi `Swap = 0B`.
- **Bảo vệ JVM (`JAVA_TOOL_OPTIONS`):**
  - `-XX:+UseContainerSupport` + `-XX:MaxRAMPercentage=70.0` đảm bảo Java không tự tính heap theo `15 GiB` của máy host mà giới hạn trong `1536 MB` của cgroup container.
  - `-XX:+ExitOnOutOfMemoryError` kết hợp `restart: unless-stopped` giúp container tự khởi động lại sạch sẽ nếu gặp đột biến bất thường.
- **HikariCP & PostgreSQL:** Giữ nguyên cấu hình pool mặc định của Spring Boot (tối đa 10 connections) — đủ cho workload web + `EvaluationJobWorker` 1 luồng mà không ngốn RAM của `asc-postgres`.
- **Disk Log Guard:** Giới hạn log JSON `max-size: 10m`, `max-file: 3` mỗi container (~90 MB tối đa cho cả stack), bảo vệ `152 GiB` ổ cứng còn trống.
