# FND-13 — Quản trị user và gán role
- Nhóm: Foundation
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `09-login-api.md`, `11-permission-catalog.md`, `12-backend-rbac-enforcement.md`
## Mục tiêu
Quản trị viên cấp tài khoản và role mà không sửa trực tiếp database.
## Phạm vi
API/UI danh sách user, tạo hoặc khóa user, reset mật khẩu theo luồng an toàn và gán/bỏ role; ghi audit sự kiện nhạy cảm.
## Tiêu chí nghiệm thu
- User bị khóa mất quyền đăng nhập ở lần gọi kế tiếp.
- Chỉ role được phép quản trị mới sửa user/role.
- Có thể tạo tài khoản chỉ xem mà không cấp quyền ghi.
## Kiểm tra
Tạo user chỉ xem, đăng nhập user đó và xác nhận các API ghi bị từ chối.
## Ngoài phạm vi
Không có chức năng xem lại mật khẩu hoặc tự nâng quyền.


