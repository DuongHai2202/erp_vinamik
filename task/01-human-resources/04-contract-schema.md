# HR-04 — Mô hình hợp đồng lao động
- Chức năng: 2/5 — Hợp đồng và cảnh báo hết hạn
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `01-employee-schema.md`
## Mục tiêu
Lưu hợp đồng gắn với nhân viên, thời hạn và mức lương cơ bản.
## Phạm vi
Migration/entity cho số hợp đồng duy nhất, employee ID, loại, ngày hiệu lực, ngày hết hạn, mức lương và trạng thái.
## Tiêu chí nghiệm thu
- Hợp đồng tham chiếu được nhân viên hợp lệ.
- Ngày kết thúc không trước ngày bắt đầu.
- Dữ liệu lương dùng kiểu số chính xác và có đơn vị tiền tệ rõ.
## Kiểm tra
Test constraint thời hạn, unique contract number và quan hệ nhân viên.
## Ngoài phạm vi
Không quản lý chữ ký số hoặc tệp scan nếu chưa được yêu cầu.

