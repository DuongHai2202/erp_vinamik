# PROD-06 — Kiểm tra nhu cầu nguyên vật liệu theo lệnh
- Nhóm chức năng: 2/5 — Quản lý nguyên vật liệu
- Nguồn: ERP_ht use case Kiểm tra nhu cầu nguyên vật liệu
- Trạng thái: IN_PROGRESS
- Phụ thuộc: PROD-05; production-order API; Inventory stock query

## Mục tiêu
Tính số vật tư cần cho lệnh và chỉ ra lượng thiếu so với tồn khả dụng.

## Phạm vi
Đọc BOM hiệu lực của sản phẩm trong lệnh; tính required quantity theo số lượng sản xuất; truy vấn tồn qua API Kho; trả số lượng cần, tồn khả dụng và thiếu hụt.

## Tiêu chí nghiệm thu
- Không có BOM thì trả lỗi nghiệp vụ bằng tiếng Anh, không tạo kết quả giả.
- Chỉ đọc tồn qua Inventory public contract.
- Kiểm tra nhu cầu không ghi stock ledger.

## Kiểm tra
Test BOM nhiều dòng, tồn đủ/thiếu và không có BOM.

## Ngoài phạm vi
Không lập kế hoạch nhiều kỳ, đề xuất nhà cung cấp hoặc đơn mua hàng.

