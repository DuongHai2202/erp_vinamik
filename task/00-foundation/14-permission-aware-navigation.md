# FND-14 — Ẩn menu và khóa thao tác theo permission
- Nhóm: Foundation
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `10-login-ui-session.md`, `11-permission-catalog.md`, `12-backend-rbac-enforcement.md`
## Mục tiêu
Giao diện phản ánh quyền đã cấp để user biết họ có thể làm gì.
## Phạm vi
Ẩn module/menu không được đọc; ẩn hoặc disable nút ghi/duyệt khi thiếu quyền; vẫn hiển thị trang chi tiết ở chế độ chỉ xem nếu có READ.
## Tiêu chí nghiệm thu
- User chỉ xem thấy dữ liệu nhưng không thấy thao tác ghi.
- User không có READ không truy cập được route qua URL.
- Backend tiếp tục chặn thao tác dù frontend bị bỏ qua.
## Kiểm tra
Đăng nhập bằng hai role khác nhau và so sánh menu, trang và phản hồi API.
## Ngoài phạm vi
Không coi route guard frontend là biện pháp bảo mật duy nhất.

