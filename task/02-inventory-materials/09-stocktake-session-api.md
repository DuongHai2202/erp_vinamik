# INV-09 — API phiên kiểm kê và số lượng đếm
- Chức năng: 4/5 — Kiểm kê tồn kho
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `04-stock-ledger-and-balance-query.md`
## Mục tiêu
Tạo phiên kiểm kê với số hệ thống và số đếm thực tế.
## Phạm vi
Tạo inventory.stocktake header theo kho/phạm vi đếm và inventory.stocktake_line theo từng mặt hàng–vị trí–lô; snapshot số hệ thống tại lúc bắt đầu, nhập số đếm thực tế và tính chênh lệch.
## Tiêu chí nghiệm thu
- Chỉ một phiên active theo phạm vi kho nếu quy tắc dự án chọn như vậy.
- Lưu cả system quantity lẫn counted quantity.
- Phiên chưa post không đổi ledger/số dư.
## Kiểm tra
Test snapshot, thiếu dòng đếm, chênh lệch dương/âm.
## Ngoài phạm vi
Không post điều chỉnh tồn trong API tạo phiên.

