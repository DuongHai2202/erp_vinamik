# PROD-22 — Bàn giao thành phẩm cho Kho
- Nhóm chức năng: 5/5 — Quản lý thành phẩm
- Trạng thái: IN_PROGRESS
- Phụ thuộc: PROD-20; Inventory receipt public contract

## Mục tiêu
Nhập lượng thành phẩm đạt vào tồn kho qua module sở hữu sổ kho.

## Phạm vi
Production gửi product/lot/expiry/quantity/reference tới Inventory receipt service; lưu receipt reference trả về.

## Tiêu chí nghiệm thu
- Production không ghi stock ledger hoặc số dư.
- Số lượng lỗi không được gửi vào tồn thành phẩm đạt.
- Retry không tạo receipt trùng; lỗi để trạng thái có thể xử lý lại.
- Inventory API nhận được tham chiếu thành phẩm qua public contract; nếu cần thì mở rộng chức năng nhập kho hiện hữu, không tạo module tồn kho thứ hai.

## Kiểm tra
Test receipt, retry, lỗi giữa chừng và đối soát số dư qua Kho.

## Ngoài phạm vi
Không tích hợp QC. Cần xác nhận riêng nếu thành phẩm phải giữ trước khi vào tồn khả dụng.


