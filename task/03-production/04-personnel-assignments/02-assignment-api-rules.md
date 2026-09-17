# PROD-15 — API phân công và kiểm tra lịch trùng
- Nhóm chức năng: 4/5 — Quản lý phân công nhân sự
- Nguồn: ERP_ht FR26–FR33
- Trạng thái: IN_PROGRESS
- Phụ thuộc: PROD-14; HR employee lookup contract; order API

## Mục tiêu
Tạo, xem, cập nhật và hủy phân công theo quy tắc lịch làm việc.

## Phạm vi
API danh sách/lọc theo nhân viên, lệnh, thời gian, trạng thái; create/update/cancel; kiểm tra employee tồn tại và lịch trùng.

## Tiêu chí nghiệm thu
- Không lưu hai phân công chồng lịch của cùng nhân viên theo quy tắc nguồn.
- Không hủy assignment của công việc đã hoàn thành.
- Thiếu quyền mutate bị chặn ở backend.

## Kiểm tra
Test overlap, employee không tồn tại, công việc hoàn thành và role chỉ xem.

## Ngoài phạm vi
Không đồng bộ dữ liệu chấm công thực tế.

