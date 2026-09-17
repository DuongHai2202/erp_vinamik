# PROD-06 — API thực hiện lệnh sản xuất
- Chức năng: 3/5 — Thực hiện sản xuất
- Trạng thái: TODO
- Phụ thuộc: `04-plan-model-api.md`
## Mục tiêu
Ghi nhận bắt đầu, tạm dừng/tiếp tục và hoàn tất lệnh ở cấp độ môn học.
## Phạm vi
Lệnh được release, trạng thái thực thi và log số lượng đã hoàn thành theo lần ghi nhận.
## Tiêu chí nghiệm thu
- Không ghi tiến độ cho lệnh chưa release hoặc đã đóng.
- Số lượng ghi nhận không âm và có actor/time.
- Chuyển trạng thái chỉ theo luồng hợp lệ.
## Kiểm tra
Test transitions, quantities, duplicate requests và quyền thao tác.
## Ngoài phạm vi
Không điều khiển máy móc hoặc thu thập tín hiệu IoT.

