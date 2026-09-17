# FND-17 — Kiểm tra đồng bộ từ năm máy
- Nhóm: Foundation
- Trạng thái: TODO
- Phụ thuộc: `16-central-deployment-runbook.md`, `12-backend-rbac-enforcement.md`
## Mục tiêu
Chứng minh năm máy trạm thấy cùng dữ liệu từ cùng một backend và PostgreSQL.
## Phạm vi
Kịch bản kiểm tra: tạo dữ liệu trên máy A, đọc trên máy B, cập nhật trên máy C, xác nhận trên D/E; kiểm tra role chỉ xem.
## Tiêu chí nghiệm thu
- Cả năm máy dùng cùng base URL và nhìn cùng bản ghi sau refresh.
- Máy không có quyền ghi nhận lỗi 403 ở API.
- Không cần đồng bộ file hoặc sao chép database giữa các PC.
## Kiểm tra
Ghi phiên bản ứng dụng, URL nội bộ đã che thông tin nhạy cảm, user role và kết quả từng bước.
## Ngoài phạm vi
Không kiểm thử tải cao hoặc clustering nhiều backend.

