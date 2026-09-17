# FND-08 — Tạo dữ liệu tài khoản và role
- Nhóm: Foundation
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `04-database-migrations.md`
## Mục tiêu
Lưu user, role và quan hệ user-role trong vùng platform.
## Phạm vi
Migration cho identity.user_account, identity.role, identity.permission, identity.user_role, identity.role_permission, identity.user_session và identity.audit_log theo docs/database_design.md.

user_account lưu username duy nhất sau chuẩn hóa, password hash, trạng thái khóa/kích hoạt và employee ID tùy chọn qua HR contract; không chứa hồ sơ nhân viên, role dạng text hoặc cờ is_admin/is_manager. user_session chỉ lưu hash token và thời hạn/thu hồi; audit_log append-only và không giữ secret.
## Tiêu chí nghiệm thu
- Mật khẩu dạng rõ không bao giờ được lưu.
- Tài khoản bị khóa không thể đăng nhập.
- Xóa/vô hiệu hóa user không làm mất lịch sử nghiệp vụ đã ghi.
- Mỗi tài khoản được gán nhiều role và mỗi role được gán nhiều permission bằng bảng liên kết có khóa chính ghép.
- Permission code và role code duy nhất, viết tiếng Anh chữ thường snake_case.
- Tham chiếu employee ID được kiểm tra qua HR contract, không nhân bản employee table hoặc tạo FK tới bảng nội bộ HR.
- Chỉ có một `super_admin`; tài khoản này luôn active, giữ `system_admin`, nhận mọi permission active và không có đường API hoặc trigger database cho phép xóa/hạ quyền.
## Kiểm tra
Migration trên DB thử nghiệm; test unique username và trạng thái disabled. Schema integration test đã kiểm tra unique index, trạng thái active, role system_admin và trigger bảo vệ super_admin.
## Ngoài phạm vi
Không lưu bản sao user hoặc password trong từng module.

