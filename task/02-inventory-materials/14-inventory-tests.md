# INV-14 — Kiểm thử tích hợp module Kho
- Chức năng: Tích hợp 1–5/5
- Trạng thái: IN_PROGRESS
- Phụ thuộc: INV-03, INV-06, INV-08, INV-11, INV-13
## Mục tiêu
Xác nhận các nghiệp vụ Kho cùng tạo số dư từ một ledger.
## Phạm vi
Kịch bản vật tư → nhập → xuất → kiểm kê điều chỉnh → điều chuyển → truy vấn số dư.
## Tiêu chí nghiệm thu
- Tồn cuối bằng tổng movement đã post. (Đã kiểm tra bằng integration test JPA/PostgreSQL opt-in.)
- Ledger `inventory.stock_movement` là append-only; sửa sai phải tạo movement đảo. (Đã thêm trigger PostgreSQL ở migration `v014__protect_stock_movement_append_only.sql`.)
- Không có đường API nào sửa số dư trực tiếp hoặc làm tồn âm. (Đã kiểm tra xuất vượt tồn bị từ chối.)
- Tài khoản chỉ xem không thể post bất kỳ phiếu nào. (Đã có test HTTP 403 cho POST nhập kho; còn mở rộng sang các loại phiếu còn lại.)
## Kiểm tra
Chạy `$env:ERP_RUN_INTEGRATION_TESTS='true'; .\mvnw.cmd test -q -Dtest=inventory_ledger_integration_tests` bằng DB thử nghiệm; kiểm tra số dư sau nhập, xuất, điều chuyển, kiểm kê và thử post lại.
## Ngoài phạm vi
Không test luồng kiểm định chất lượng hoặc kế toán giá thành.

