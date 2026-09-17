# HR-07 — Mô hình nghỉ phép và vắng mặt
- Chức năng: 3/5 — Nghỉ phép/vắng mặt
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `01-employee-schema.md`, `04-database-migrations.md`
## Mục tiêu
Lưu yêu cầu nghỉ gắn với nhân viên, khoảng thời gian, loại nghỉ và trạng thái duyệt.
## Phạm vi
Loại nghỉ, ngày bắt đầu/kết thúc, lý do, trạng thái, người duyệt, thời điểm duyệt và cờ nghỉ có lương/không lương.
## Tiêu chí nghiệm thu
- Khoảng ngày và trạng thái có enum/quy ước thống nhất.
- Có thể xác định phần nghỉ không lương để payroll đọc.
- Thay đổi trạng thái duyệt có lưu actor/time.
## Kiểm tra
Test constraint ngày và quan hệ nhân viên/approver.
## Ngoài phạm vi
Không lấy dữ liệu chấm công từ máy ngoài.

