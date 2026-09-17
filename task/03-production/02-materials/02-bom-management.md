# PROD-05 — Định mức/BOM sản xuất
- Nhóm chức năng: 2/5 — Quản lý nguyên vật liệu
- Nguồn: ERP_ht FR20–FR23
- Trạng thái: IN_PROGRESS
- Phụ thuộc: PROD-04; Foundation migrations

## Mục tiêu
Quản lý lượng vật tư định mức cho một đơn vị thành phẩm.

## Phạm vi
Tạo production.bom và production.bom_line theo docs/database_design.md. Header tham chiếu phiên bản stock item thành phẩm qua Inventory contract, version, ngày hiệu lực và lượng cơ sở; line tham chiếu từng stock item nguyên liệu, đơn vị snapshot, quantity định mức và hao hụt nếu nguồn yêu cầu xác nhận. BOM thuộc Sản xuất, không tạo BOM trong schema Kho.

## Tiêu chí nghiệm thu
- Quantity định mức dương; vật tư phải tồn tại trong danh mục Kho.
- Không xóa BOM đang được lệnh tham chiếu.
- Thay đổi BOM không sửa ngược định mức của lệnh đã phát hành.
- Không tạo FK trực tiếp tới bảng nội bộ inventory.stock_item; xác thực mã/ID qua Kho public contract.
- Một BOM đã được phát hành có version bất biến; sửa định mức tạo version mới.

## Kiểm tra
Test validation, tham chiếu vật tư và phiên bản BOM được chọn cho lệnh.

## Ngoài phạm vi
BOM là use case con của nguyên vật liệu, không đặt tên thành module MRP.

