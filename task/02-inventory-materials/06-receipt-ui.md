# INV-06 — Giao diện nhập kho
- Chức năng: 2/5 — Quản lý nhập kho
- Trạng thái: TODO
- Phụ thuộc: `05-receipt-posting-api.md`
## Mục tiêu
Người có quyền lập và theo dõi phiếu nhập.
## Phạm vi
Danh sách phiếu, form nhiều dòng vật tư, tổng lượng, draft/post và trang chi tiết.
## Tiêu chí nghiệm thu
- Hiển thị trạng thái chưa post/đã post.
- Chặn thao tác post nếu không có permission; backend cũng chặn.
- Refresh trang không nhân đôi phiếu.
## Kiểm tra
Thử nhập một phiếu nhiều dòng, user chỉ xem và retry sau lỗi mạng.
## Ngoài phạm vi
Không tạo form kiểm định chất lượng.

