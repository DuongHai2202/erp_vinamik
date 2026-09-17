# FND-02 — Xác định PostgreSQL trung tâm
- Nhóm: Foundation
- Trạng thái: TODO
- Phụ thuộc: `01-architecture-decisions.md`
## Mục tiêu
Dùng PostgreSQL hiện có do nhóm sở hữu làm nguồn dữ liệu chung cho website.
## Phạm vi
Ghi host/port/database/schema dự kiến theo cấu hình môi trường; xác định máy chủ chạy backend và đường mạng từ 5 máy trạm tới backend. PostgreSQL chỉ nhận kết nối từ backend.
## Tiêu chí nghiệm thu
- Có sơ đồ: 5 trình duyệt → một backend → một PostgreSQL.
- Không yêu cầu cài PostgreSQL hoặc tạo bản sao dữ liệu trên từng máy trạm.
- Ghi checklist kiểm tra host reachability, firewall, backup và tài khoản DB tối thiểu quyền.
## Kiểm tra
Dùng một kết nối backend thử nghiệm đọc/ghi được; xác nhận máy trạm không cần thông tin đăng nhập DB.
## Ngoài phạm vi
Không tạo một cụm PostgreSQL mới, không mở cổng DB trực tiếp cho toàn bộ máy trạm, không lưu mật khẩu trong Git.

