# PROD-01 — Mô hình kế hoạch sản xuất
- Nhóm chức năng: 1/5 — Quản lý kế hoạch sản xuất
- Nguồn: ERP_ht FR3–FR8
- Trạng thái: IN_PROGRESS
- Phụ thuộc: 00-foundation/04-database-migrations.md

## Mục tiêu
Lưu kế hoạch và các mốc thời gian làm căn cứ phát hành lệnh sản xuất.

## Phạm vi
Tạo production.production_plan và production.production_plan_line theo docs/database_design.md. Header có mã kế hoạch duy nhất, tên, ngày lập, ngày dự kiến, ghi chú, trạng thái và metadata; mỗi line tham chiếu stock item qua Inventory contract và giữ số lượng mục tiêu/mốc hoàn thành.

## Tiêu chí nghiệm thu
- Mã kế hoạch duy nhất; ngày kết thúc không trước ngày bắt đầu.
- Lệnh có thể tham chiếu kế hoạch/dòng kế hoạch ổn định.
- Một kế hoạch có thể chứa nhiều sản phẩm qua các dòng riêng; không lưu một tên sản phẩm tự do trên header.
- Migration chạy được trên PostgreSQL thử nghiệm và không xóa dữ liệu.

## Kiểm tra
Test unique code, ngày hợp lệ và migration.

## Ngoài phạm vi
Không thêm dự báo nhu cầu, MRP nhiều kỳ hoặc tối ưu lịch sản xuất.

