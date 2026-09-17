# PROD-20 — Ghi nhận thành phẩm đạt và lỗi tại sản xuất
- Nhóm chức năng: 5/5 — Quản lý thành phẩm
- Nguồn: ERP_ht FR41–FR42
- Trạng thái: IN_PROGRESS
- Phụ thuộc: PROD-09, PROD-19

## Mục tiêu
Lưu số lượng thành phẩm đạt và lỗi theo lệnh/lô.

## Phạm vi
Tạo production.production_output ghi số lượng đạt/lỗi tách riêng theo lệnh và ID lô được Inventory receipt contract trả về, actor/time; tổng hợp với quantity mục tiêu và timeline lệnh.

## Tiêu chí nghiệm thu
- Quantity không âm; đạt và lỗi được lưu tách biệt.
- Không ghi sản lượng vào lệnh không tồn tại hoặc trạng thái không hợp lệ.
- Ghi nhận đạt/lỗi tại công đoạn không thay thế quyết định QC chính thức.
- Chỉ quantity đạt mới được gửi qua Inventory receipt contract; output record và inventory movement có khóa tham chiếu idempotent để không nhập kho trùng.

## Kiểm tra
Test quantity, trạng thái lệnh và số liệu theo lô/lệnh.

## Ngoài phạm vi
Không quyết định chất lượng theo tiêu chuẩn QC đang deferred.

