# PROD-09 — Mô hình lệnh sản xuất
- Nhóm chức năng: 3/5 — Quản lý lệnh sản xuất
- Nguồn: ERP_ht FR9–FR14
- Trạng thái: IN_PROGRESS
- Phụ thuộc: Production plan model, product reference

## Mục tiêu
Lưu lệnh phát sinh từ kế hoạch và dữ liệu thực hiện sản xuất.

## Phạm vi
Tạo production.production_order và production.production_order_material_requirement theo docs/database_design.md. Lệnh tham chiếu plan line, thành phẩm stock item qua contract Kho và một phiên bản BOM; các line nhu cầu lưu snapshot định mức/vật tư/đơn vị/quantity để lịch sử không đổi khi master BOM đổi.

## Tiêu chí nghiệm thu
- Mã duy nhất; quantity dương; ngày hợp lệ.
- Lệnh tham chiếu đúng kế hoạch/dòng kế hoạch và sản phẩm qua Inventory contract.
- Quantity đã cấp được truy qua Inventory issue contract; không tạo cột/tổng issued thủ công làm nguồn sự thật thứ hai.
- Trạng thái khởi tạo hiển thị là Chờ thực hiện.

## Kiểm tra
Test ràng buộc, reference và trạng thái ban đầu.

## Ngoài phạm vi
Không tạo work-in-process stock ledger hoặc nghiệp vụ mua hàng.

