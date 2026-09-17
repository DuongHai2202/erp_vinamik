# FND-15 — Ghi audit cho thao tác nhạy cảm
- Nhóm: Foundation
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `04-database-migrations.md`, `08-auth-schema.md`
## Mục tiêu
Có thể biết ai đã thực hiện thao tác quản trị hoặc thay đổi trạng thái quan trọng.
## Phạm vi
Bảng/sự kiện audit append-only cho login quan trọng, user/role, duyệt, posting kho, payroll lock và release lệnh sản xuất.
## Tiêu chí nghiệm thu
- Sự kiện có actor, thời điểm, loại đối tượng, hành động và tham chiếu đối tượng.
- Không ghi password, session secret hoặc dữ liệu nhạy cảm dư thừa.
- User nghiệp vụ không thể sửa/xóa audit qua API thường.
- Có API tra cứu audit phân trang với permission `identity_audit_read`; audit log được khóa UPDATE/DELETE bằng trigger PostgreSQL `v015`.
## Kiểm tra
Thực hiện một thao tác mẫu và truy vấn audit bằng quyền quản trị phù hợp.
## Ngoài phạm vi
Không xây module báo cáo/audit analytics trong task này.

