# PROD-07 — Giao diện thực hiện sản xuất
- Chức năng: 3/5 — Thực hiện sản xuất
- Trạng thái: TODO
- Phụ thuộc: `06-execution-api.md`
## Mục tiêu
Nhân sự được cấp quyền cập nhật hoạt động của lệnh sản xuất.
## Phạm vi
Danh sách lệnh được release, xem thông tin sản phẩm/số lượng và ghi nhận start/pause/resume/complete theo trạng thái cho phép.
## Tiêu chí nghiệm thu
- Action chỉ xuất hiện theo permission và trạng thái lệnh.
- Gửi lại thao tác không tạo log trùng.
- Có xác nhận trước thao tác hoàn tất.
## Kiểm tra
Thử luồng từ released đến completed bằng role sản xuất và chỉ xem.
## Ngoài phạm vi
Không chỉnh định mức BOM trên màn hình thực hiện.

