# PROD-12 — Theo dõi tiến độ bên trong lệnh sản xuất
- Nhóm chức năng: 3/5 — Use case con của Quản lý lệnh sản xuất
- Nguồn: ERP_ht use case Theo dõi tiến độ sản xuất
- Trạng thái: IN_PROGRESS
- Phụ thuộc: PROD-10

## Mục tiêu
Ghi nhận trạng thái và số lượng thực tế để so sánh kế hoạch với kết quả.

## Phạm vi
API xem timeline trạng thái từ production.production_order_event; sản lượng tốt/lỗi đọc từ production.production_output. Lưu actor, thời điểm và ghi chú; tổng hợp planned, actual và remaining từ bản ghi nguồn, không cập nhật nhiều cột tổng độc lập.

## Tiêu chí nghiệm thu
- Event tiến độ chỉ hợp lệ với trạng thái lệnh cho phép.
- Số liệu tổng hợp truy ngược được về event gốc.
- Đây là phần của Quản lý lệnh sản xuất, không phải nhóm chức năng độc lập.

## Kiểm tra
Test chuyển trạng thái, tổng planned/actual và lịch sử event.

## Ngoài phạm vi
Không tính OEE hoặc phân tích hiệu suất máy.


