# HR-02 — API hồ sơ nhân viên
- Chức năng: 1/5 — Hồ sơ nhân viên
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `01-employee-schema.md`, `../../00-foundation/12-backend-rbac-enforcement.md`
## Mục tiêu
Cho phép người có quyền tra cứu và cập nhật hồ sơ nhân viên.
## Phạm vi
API phân trang/tìm kiếm/chi tiết/tạo/cập nhật/vô hiệu hóa; kiểm tra trường bắt buộc và trùng mã.
## Tiêu chí nghiệm thu
- Tra cứu hỗ trợ mã/tên và lọc trạng thái.
- Không xóa cứng nhân viên đã được tham chiếu bởi hợp đồng/giao dịch.
- Quyền READ và quyền ghi được kiểm tra tại backend.
## Kiểm tra
Test CRUD, lọc, trùng mã, user chỉ xem và user thiếu READ.
## Ngoài phạm vi
Không thêm endpoint cho module khác truy cập bảng HR trực tiếp.

