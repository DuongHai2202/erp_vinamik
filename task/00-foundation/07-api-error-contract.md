# FND-07 — Chuẩn hóa lỗi và validation API
- Nhóm: Foundation
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `05-backend-module-seams.md`
## Mục tiêu
Các module trả lỗi nhất quán để frontend hiển thị và người dùng sửa dữ liệu.
## Phạm vi
Quy ước status code, mã lỗi ổn định, lỗi validation theo field, correlation/request id và thông báo không lộ stack trace.
## Tiêu chí nghiệm thu
- Validation không hợp lệ trả lỗi theo trường.
- Lỗi quyền thống nhất 401/403; tài nguyên không tồn tại trả 404.
- Frontend có thể đọc cùng một cấu trúc lỗi cho cả ba module ưu tiên.
## Kiểm tra
Kiểm thử ít nhất một lỗi validation, một lỗi chưa đăng nhập và một lỗi thiếu quyền.
## Ngoài phạm vi
Không thiết kế hệ thống thông báo hoặc xử lý sự cố phân tán.

