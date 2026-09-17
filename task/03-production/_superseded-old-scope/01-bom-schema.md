# PROD-01 — Mô hình định mức nguyên vật liệu (BOM)
- Chức năng: 1/5 — MRP
- Trạng thái: TODO
- Phụ thuộc: `../../00-foundation/04-database-migrations.md`, `../02-inventory-materials/01-material-schema.md`
## Mục tiêu
Mô tả vật tư và định lượng cần để sản xuất một đơn vị thành phẩm.
## Phạm vi
BOM header/line, mã thành phẩm, vật tư, lượng định mức, đơn vị và hiệu lực/active state tối thiểu.
## Tiêu chí nghiệm thu
- Quantity định mức dương.
- BOM không tham chiếu vật tư không tồn tại.
- Chỉ BOM hợp lệ/active được dùng để tính MRP.
## Kiểm tra
Test constraint, vật tư tham chiếu và version/hiệu lực nếu có.
## Ngoài phạm vi
Không triển khai quy trình phê duyệt kỹ thuật nhiều cấp.

