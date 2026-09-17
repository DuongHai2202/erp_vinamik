# INV-07 — API lập và post phiếu xuất
- Chức năng: 3/5 — Quản lý xuất kho
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `04-stock-ledger-and-balance-query.md`, `05-receipt-posting-api.md`
## Mục tiêu
Ghi xuất vật tư theo chứng từ và bảo vệ số lượng khả dụng.
## Phạm vi
Tạo header inventory.issue ở trạng thái draft, thêm một hoặc nhiều inventory.issue_line, validate mặt hàng/kho/vị trí/lô/quantity và nguồn yêu cầu; post ledger trong transaction.
## Tiêu chí nghiệm thu
- Không post xuất lớn hơn tồn khả dụng.
- Concurrent requests không làm tồn âm.
- Retry idempotent và movement truy được về chứng từ.
## Kiểm tra
Test tồn đủ/thiếu, hai request đồng thời và user thiếu quyền.
## Ngoài phạm vi
Không cho module Sản xuất ghi ledger trực tiếp.

