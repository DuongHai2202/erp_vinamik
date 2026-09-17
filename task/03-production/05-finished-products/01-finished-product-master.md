# PROD-18 — Danh mục thành phẩm
- Nhóm chức năng: 5/5 — Quản lý thành phẩm
- Nguồn: ERP_ht FR34–FR38
- Trạng thái: IN_PROGRESS
- Phụ thuộc: Foundation migrations

## Mục tiêu
Đảm bảo Sản xuất chọn đúng thành phẩm stockable mà Kho sở hữu và lưu được đầu ra theo lệnh.

## Phạm vi
Không tạo bảng master thành phẩm thứ hai trong production. Tra cứu/chọn inventory.stock_item có item_type=finished_product qua public contract của Kho; phần Sản xuất quản lý kết quả theo lệnh và tham chiếu inventory item/lot theo contract.

## Tiêu chí nghiệm thu
- Không xóa cứng sản phẩm đã có lô hoặc giao dịch tham chiếu.
- Số lượng tồn kho không là trường sửa tay; đọc qua Kho.
- Không tồn tại product code/name master trùng trong schema production.
- Giá bán/giá thành thuộc phạm vi Quality/Cost đang hoãn, không tự tính tại đây.

## Kiểm tra
Test contract tra cứu mã sản phẩm, item_type, trường bắt buộc và giới hạn deactivate.

## Ngoài phạm vi
Không triển khai bảng giá hoặc công thức giá thành.

