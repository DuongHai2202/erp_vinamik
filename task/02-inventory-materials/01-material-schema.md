# INV-01 — Mô hình danh mục nguyên vật liệu
- Chức năng: 1/5 — Danh mục nguyên vật liệu
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `../../00-foundation/04-database-migrations.md`
## Mục tiêu
Lưu mã hàng tồn kho chuẩn, gồm nguyên vật liệu và loại hàng stockable được contract Kho hỗ trợ.
## Phạm vi
Tạo inventory.unit_of_measure, inventory.item_category và inventory.stock_item theo docs/database_design.md. stock_item.item_type phân biệt raw_material và finished_product; Kho sở hữu mã, tên, nhóm, đơn vị cơ sở, trạng thái và cờ theo dõi lô. Màn hình danh mục chức năng 1 chỉ mở phần nguyên vật liệu; sản phẩm/thành phẩm được cấp qua contract Kho.
## Tiêu chí nghiệm thu
- Mã vật tư duy nhất và đơn vị tính bắt buộc.
- Record đã có giao dịch không bị xóa cứng.
- Số dư không được lưu/sửa trong stock_item.
- Tên schema/bảng/cột/index và migration là tiếng Anh chữ thường snake_case.
- Trường số lượng dùng kiểu decimal phù hợp đơn vị.
## Kiểm tra
Test unique code, required unit và migration.
## Ngoài phạm vi
Không quản lý nhà cung cấp, kiểm định chất lượng hoặc BOM ở task này.

