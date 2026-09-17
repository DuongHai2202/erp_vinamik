# PROD-19 — Mã lô và hạn sử dụng thành phẩm
- Nhóm chức năng: 5/5 — Quản lý thành phẩm
- Nguồn: ERP_ht FR39–FR40
- Trạng thái: IN_PROGRESS
- Phụ thuộc: PROD-18

## Mục tiêu
Truy nguyên sản lượng theo lô và hạn sử dụng.

## Phạm vi
Khi ghi nhận đầu ra, gửi mã lô/ngày sản xuất/hạn dùng tới Inventory receipt contract; Inventory tạo định danh stock_lot canonical. Production lưu inventory_stock_lot_id opaque cùng quan hệ lệnh và kết quả sản xuất, không tự tạo danh mục lô tồn kho thứ hai.

## Tiêu chí nghiệm thu
- Inventory contract trả về stock_lot ID/mã lô canonical duy nhất theo mặt hàng và áp dụng date rules.
- Hạn dùng không trước ngày sản xuất.
- Truy ngược được lô về lệnh và sản phẩm.
- Lô được xác thực/tạo qua Kho contract và không thể có số dư nếu thiếu movement Kho đã post.

## Kiểm tra
Test contract tạo/tra cứu lot, date rules và truy vấn liên hệ production output–order–product.

## Ngoài phạm vi
Không xây kiểm nghiệm chất lượng hoặc thu hồi sản phẩm.

