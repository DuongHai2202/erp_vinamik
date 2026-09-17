# PROD-07 — Cấp vật tư và ghi nhận tiêu hao thực tế
- Nhóm chức năng: 2/5 — Quản lý nguyên vật liệu
- Nguồn: ERP_ht FR24–FR25 và use case Theo dõi tiêu hao
- Trạng thái: IN_PROGRESS
- Phụ thuộc: PROD-06; Inventory issue/return contract

## Mục tiêu
Gắn lượng vật tư cấp và lượng thực dùng vào đúng lệnh sản xuất.

## Phạm vi
Tạo yêu cầu xuất Kho theo lệnh; lưu phiếu Kho trả về; ghi actual consumption và chênh lệch định mức.

## Tiêu chí nghiệm thu
- Tồn chỉ đổi qua Inventory contract; Production không cập nhật stock.
- Actual quantity không âm và không vượt lượng đã cấp nếu chưa có luồng bổ sung được duyệt.
- Không trừ tồn lần hai khi ghi actual; vật tư thừa/hoàn trả đi qua API Kho.
- Chỉ cảnh báo vượt ngưỡng khi ngưỡng đã được nhóm xác nhận/cấu hình.

## Kiểm tra
Test thiếu tồn, retry, actual vượt lượng cấp, hoàn trả và rollback.

## Ngoài phạm vi
Không tự đặt ngưỡng tiêu hao hoặc tính giá thành.


