# HR-18 — API danh mục tham chiếu

- Trạng thái: DONE
- Phụ thuộc: `01-employee-schema.md`, `02-employee-api.md`, `03-production/04-personnel-assignments`
- Phạm vi: phòng ban, chức danh và ca làm việc thuộc schema `hr`.

## Đã làm

- Thêm API phân trang, lọc `search/status`, detail, create, update và soft deactivate.
- Kiểm tra mã trùng, giới hạn chuỗi, trạng thái, parent department active, chống tự tham chiếu/vòng lặp phân cấp và giờ bắt đầu/kết thúc ca.
- Tạo quyền `hr_master_read/create/update/deactivate`; role HR và read_only được seed theo nguyên tắc least privilege.
- `GET /work_shifts/active` cho phép permission đọc/phân công của Production dùng lookup qua contract HTTP, không đọc bảng HR trực tiếp.

## Tiêu chí nghiệm thu

- Không hard delete bản ghi danh mục.
- Error/success response dùng tiếng Anh; audit/log vận hành dùng tiếng Việt.
- Truy vấn nằm trong repository JPA native query có bind parameter.
- Integration test PostgreSQL tạo, tìm kiếm, đọc và vô hiệu hóa dữ liệu thành công.
