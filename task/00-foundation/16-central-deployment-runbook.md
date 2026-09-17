# FND-16 — Viết hướng dẫn chạy website tập trung
- Nhóm: Foundation
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `03-backend-database-config.md`, `06-frontend-shell.md`
## Mục tiêu
Một máy chủ ứng dụng chạy backend/web; các máy còn lại truy cập bằng trình duyệt qua cùng URL.
## Phạm vi
Viết cấu hình môi trường, lệnh build/start, địa chỉ truy cập LAN, quy tắc firewall cho web, và cách kết nối backend tới PostgreSQL trung tâm.
## Tiêu chí nghiệm thu
- Hướng dẫn phân biệt máy chủ ứng dụng với máy trạm.
- Chỉ backend cần credential DB; không yêu cầu cài app/database ở từng PC.
- Có hướng dẫn đổi host DB mà không sửa code.
## Kiểm tra
Một người khác làm theo tài liệu trên môi trường sạch và truy cập được trang đăng nhập.
## Ngoài phạm vi
Không công khai website ra Internet hoặc đặt secret thật vào tài liệu.

