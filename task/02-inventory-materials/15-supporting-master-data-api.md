# Inventory-15 — API danh mục hỗ trợ Kho

- Trạng thái: DONE
- Phụ thuộc: `01-material-schema.md`, `02-material-api.md`, `05-receipt-posting-api.md`
- Phạm vi: đơn vị tính, nhóm mặt hàng, nhà cung cấp, kho và vị trí kho thuộc schema `inventory`.

## Đã làm

- Thêm endpoint lookup active và endpoint quản trị phân trang/detail/create/update/soft deactivate.
- Kiểm tra mã trùng, decimal places `0..6`, kho active khi tạo vị trí và giữ `warehouse_id` bất biến sau khi vị trí được tạo.
- Tạo quyền `inventory_master_read/create/update/deactivate`; role kho và read_only được seed theo action.
- Production và các màn hình chứng từ chỉ dùng lookup công khai; chỉ Inventory ghi sổ tồn.

## Tiêu chí nghiệm thu

- Mọi truy vấn dùng repository JPA/EntityManager bind parameter; không dùng JDBC trực tiếp.
- Chứng từ nhập/xuất/điều chuyển/kiểm kê có thể chọn dữ liệu danh mục do người dùng tạo qua API.
- Error/success response dùng tiếng Anh; audit/log vận hành dùng tiếng Việt.
- Integration test PostgreSQL tạo, lookup, phân trang và soft deactivate thành công.
