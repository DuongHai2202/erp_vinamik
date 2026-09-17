# PROD-14 — Kịch bản demo xuyên ba module ưu tiên
- Nhóm: HR + Kho + Sản xuất
- Trạng thái: TODO
- Phụ thuộc: `../01-human-resources/17-hr-integration-tests.md`, `../02-inventory-materials/14-inventory-tests.md`, `13-production-integration-tests.md`, `../../00-foundation/17-five-client-consistency-smoke-test.md`
## Mục tiêu
Có một kịch bản trình bày được end-to-end và chứng minh dữ liệu dùng chung.
## Phạm vi
Tạo nhân viên/role, tạo vật tư và tồn, tạo BOM/MRP/kế hoạch/lệnh, xuất vật tư, ghi sản xuất, nhập thành phẩm, kiểm tra role chỉ xem từ máy khác.
## Tiêu chí nghiệm thu
- Kịch bản có dữ liệu mẫu, user roles và bước thao tác có thể lặp.
- Cùng kết quả nhìn được từ ít nhất hai máy; runbook mô tả mở rộng kiểm tra đủ năm máy.
- Nêu rõ giới hạn QC/Cost và Reporting chưa nằm trong demo.
## Kiểm tra
Chạy kịch bản trên deployment tập trung theo `FND-16`.
## Ngoài phạm vi
Không mô phỏng module chất lượng hoặc báo cáo nâng cao.

