# PROD-09 — Màn hình theo dõi tiến độ
- Chức năng: 4/5 — Theo dõi tiến độ
- Trạng thái: TODO
- Phụ thuộc: `08-progress-read-api.md`
## Mục tiêu
Người quản lý nhanh chóng biết lệnh nào chưa bắt đầu, đang làm hoặc chậm.
## Phạm vi
Bảng/dashboard theo trạng thái, ngày và sản phẩm; planned/actual/remaining; liên kết tới lệnh.
## Tiêu chí nghiệm thu
- Tổng quan khớp dữ liệu API sau refresh.
- Có lọc ngày/trạng thái.
- User chỉ xem sử dụng được đầy đủ trang theo quyền READ.
## Kiểm tra
Kiểm tra empty state, filter và số liệu trên bộ dữ liệu mẫu.
## Ngoài phạm vi
Không xây phân tích thời gian thực hoặc biểu đồ OEE.

