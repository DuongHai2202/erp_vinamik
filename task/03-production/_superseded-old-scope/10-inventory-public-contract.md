# PROD-10 — Chốt hợp đồng public giữa Sản xuất và Kho
- Chức năng: 5/5 — Bàn giao vật tư/thành phẩm
- Trạng thái: TODO
- Phụ thuộc: `../02-inventory-materials/05-receipt-posting-api.md`, `../02-inventory-materials/07-issue-posting-api.md`
## Mục tiêu
Sản xuất yêu cầu Kho xuất nguyên liệu và nhận thành phẩm mà không phụ thuộc bảng nội bộ Kho.
## Phạm vi
DTO/service contract cho issue request, finished-goods receipt, result/reference ID, idempotency key và lỗi tồn không đủ.
## Tiêu chí nghiệm thu
- Contract không phơi entity/repository/schema nội bộ Kho.
- Kho là nơi duy nhất tạo movement và cập nhật số dư.
- Lỗi/từ chối có thể ánh xạ về lệnh và dòng vật tư.
## Kiểm tra
Compile test/module verification; mô phỏng thành công, insufficient stock và retry.
## Ngoài phạm vi
Không gọi thẳng database của Kho từ package Sản xuất.

