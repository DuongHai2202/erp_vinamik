# INV-05 — API lập và post phiếu nhập
- Chức năng: 2/5 — Quản lý nhập kho
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `04-stock-ledger-and-balance-query.md`, `02-material-api.md`
## Mục tiêu
Ghi nhận hàng nhập và cập nhật ledger/số dư đúng một lần.
## Phạm vi
Tạo header inventory.receipt ở trạng thái draft, thêm một hoặc nhiều inventory.receipt_line, validate mặt hàng/kho/vị trí/lô/quantity và post trong transaction cùng movement ledger; lưu supplier/reference nguồn nếu có.
## Tiêu chí nghiệm thu
- Post phiếu tạo ledger và đổi số dư trong cùng transaction.
- Không post hai lần khi client retry.
- API trả mã phiếu và trạng thái rõ ràng.
## Kiểm tra
Test receipt hợp lệ, vật tư không tồn tại, quantity <= 0, retry và rollback.
## Ngoài phạm vi
Không triển khai kiểm tra chất lượng nguyên liệu.

