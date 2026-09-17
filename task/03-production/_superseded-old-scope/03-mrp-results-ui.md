# PROD-03 — Giao diện kết quả MRP
- Chức năng: 1/5 — MRP
- Trạng thái: TODO
- Phụ thuộc: `02-mrp-calculation-api.md`
## Mục tiêu
Planner chạy và xem vật tư thiếu/thừa theo nhu cầu.
## Phạm vi
Nhập nhu cầu, chạy MRP, xem kết quả theo thành phẩm/vật tư và mở lịch sử chạy.
## Tiêu chí nghiệm thu
- Gross need, available stock, net shortage hiển thị riêng.
- Hiển thị thời điểm chạy và nguồn nhu cầu.
- User chỉ xem không chạy hoặc lưu kế hoạch nếu thiếu quyền.
## Kiểm tra
Đối chiếu bảng UI với API cho trường hợp tồn đủ và thiếu.
## Ngoài phạm vi
Không hiển thị biểu đồ tối ưu năng lực máy.

