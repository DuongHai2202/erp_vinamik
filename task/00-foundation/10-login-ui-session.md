# FND-10 — Màn hình đăng nhập và quản lý phiên frontend
- Nhóm: Foundation
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `06-frontend-shell.md`, `09-login-api.md`
## Mục tiêu
Người dùng đăng nhập vào cùng website và thấy trạng thái phiên hiện hành.
## Phạm vi
Form username/password, trạng thái đang gửi/lỗi, lưu phiên theo cơ chế backend chọn, lấy user hiện tại khi tải lại và nút đăng xuất.
## Tiêu chí nghiệm thu
- Route riêng tư chuyển về đăng nhập khi chưa có phiên.
- Đăng xuất làm phiên mất hiệu lực ở backend.
- Không lưu password hoặc token nhạy cảm trong localStorage.
## Kiểm tra
Thử đăng nhập đúng/sai, tải lại trang, đăng xuất và mở route cần đăng nhập.
## Ngoài phạm vi
Không tự cấp quyền ghi cho user mới.

