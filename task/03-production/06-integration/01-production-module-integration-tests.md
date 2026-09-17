# PROD-23 — Kiểm thử tích hợp năm nhóm Sản xuất
- Nhóm: Integration
- Trạng thái: IN_PROGRESS
- Phụ thuộc: PROD-03, PROD-08, PROD-13, PROD-17, PROD-22

## Mục tiêu
Kiểm chứng năm use case Sản xuất liên kết đúng kế hoạch, vật tư, lệnh, phân công và thành phẩm.

## Phạm vi
Kịch bản kế hoạch → lệnh → kiểm tra BOM/tồn → phân công → ghi tiến độ/tiêu hao → ghi sản lượng đạt/lỗi → nhập thành phẩm qua Kho.

## Tiêu chí nghiệm thu
- Thiếu quyền Sản xuất thì API ghi bị chặn; role chỉ xem vẫn đọc được.
- Không module nào ghi trực tiếp bảng nội bộ module khác. (Đã kiểm tra luồng Production gọi public contract của Inventory.)
- Tồn cuối đối soát từ movement do Kho tạo. (Đã kiểm tra tiêu hao và nhập thành phẩm trong integration test.)

## Kiểm tra
Chạy `$env:ERP_RUN_INTEGRATION_TESTS='true'; .\mvnw.cmd test -q -Dtest=production_module_integration_tests` trên PostgreSQL thử nghiệm; kiểm tra kế hoạch, BOM, lệnh, nhu cầu, tiêu hao, sản lượng, lot và retry nhập thành phẩm. Còn thiếu kiểm thử HTTP quyền chỉ đọc, lịch phân công chồng và lỗi thiếu tồn trong cùng kịch bản.

## Ngoài phạm vi
Không test QC hoặc tính giá thành.

