# FND-11 — Khai báo permission và role khởi tạo
- Nhóm: Foundation
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `08-auth-schema.md`
## Mục tiêu
Mô tả quyền theo hành động và seed role tối thiểu phục vụ kiểm thử/demo.
## Phạm vi
Permission theo resource/action (READ, CREATE, UPDATE, APPROVE, EXPORT khi cần); role quản trị và role chỉ xem; seed idempotent.
## Tiêu chí nghiệm thu
- Role chỉ xem chỉ có quyền đọc, không có quyền ghi hoặc duyệt.
- Permission code ổn định, có module prefix.
- Seed chạy lặp lại không nhân bản role/permission.
## Kiểm tra
Đọc lại quyền của từng role từ DB sau ít nhất hai lần seed.
## Ngoài phạm vi
Không gán quyền mặc định rộng cho mọi user và không xem role name là kiểm tra bảo mật duy nhất.

