# HR-12 — Giao diện khen thưởng và kỷ luật
- Chức năng: 4/5 — Khen thưởng/kỷ luật
- Trạng thái: TODO
- Phụ thuộc: `11-reward-discipline-api.md`
## Mục tiêu
HR nhập và theo dõi quyết định, quản lý có quyền duyệt.
## Phạm vi
Danh sách, form tạo/sửa nháp, trang chi tiết và thao tác approve/reject theo permission.
## Tiêu chí nghiệm thu
- Người dùng thấy trạng thái và số tiền thưởng/phạt riêng.
- User không có quyền không thấy hoặc không dùng được nút duyệt.
- Record đã thuộc kỳ lương locked ở chế độ chỉ xem.
## Kiểm tra
Dùng role nhập, role approver và role chỉ xem.
## Ngoài phạm vi
Không xây dashboard KPI.

