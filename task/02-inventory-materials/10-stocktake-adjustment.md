# INV-10 — Post chênh lệch kiểm kê thành movement
- Chức năng: 4/5 — Kiểm kê tồn kho
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `09-stocktake-session-api.md`, `04-stock-ledger-and-balance-query.md`
## Mục tiêu
Chốt số đếm bằng giao dịch điều chỉnh có thể truy vết.
## Phạm vi
Luồng submit/review/post phiên kiểm kê; tạo movement chênh lệch một lần cho mỗi dòng được duyệt.
## Tiêu chí nghiệm thu
- Không sửa/xóa movement cũ để khớp kết quả đếm.
- Điều chỉnh có actor, thời điểm và lý do.
- Chỉ phiên đã duyệt mới ảnh hưởng số dư.
## Kiểm tra
Test chênh lệch âm/dương, retry, thiếu quyền duyệt và rollback.
## Ngoài phạm vi
Không hỗ trợ kiểm kê hàng loạt bằng thiết bị barcode ở task này.

