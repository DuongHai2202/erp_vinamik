# PROD-08 — API theo dõi tiến độ
- Chức năng: 4/5 — Theo dõi tiến độ
- Trạng thái: TODO
- Phụ thuộc: `06-execution-api.md`
## Mục tiêu
Tổng hợp planned vs actual từ lệnh và event thực thi.
## Phạm vi
Read model/filter theo ngày, trạng thái, sản phẩm; tính tỷ lệ hoàn thành từ số lượng kế hoạch và số lượng đã ghi.
## Tiêu chí nghiệm thu
- Số tổng hợp khớp execution log.
- Lệnh hủy/đóng được thể hiện với trạng thái riêng, không biến mất khó hiểu.
- API chỉ đọc, không tạo tác động phụ.
## Kiểm tra
So sánh API với bộ lệnh có số lượng planned/actual đã biết.
## Ngoài phạm vi
Không tính OEE hoặc năng suất máy nếu chưa được yêu cầu.

