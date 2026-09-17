# INV-11 — Giao diện kiểm kê
- Chức năng: 4/5 — Kiểm kê tồn kho
- Trạng thái: TODO
- Phụ thuộc: `09-stocktake-session-api.md`, `10-stocktake-adjustment.md`
## Mục tiêu
Nhân viên kho nhập số đếm và approver xem/chốt chênh lệch.
## Phạm vi
Tạo phiên, nhập số đếm, xem system/count/difference và màn hình review/post.
## Tiêu chí nghiệm thu
- Số hệ thống lúc bắt đầu và số đếm phân biệt rõ.
- Chỉ người đủ quyền được chốt.
- Phiên đã post mở ở chế độ chỉ đọc.
## Kiểm tra
Thử số đếm chênh lệch, role nhập liệu, approver và chỉ xem.
## Ngoài phạm vi
Không cho phép chỉnh số dư trực tiếp từ màn hình tổng quan.

