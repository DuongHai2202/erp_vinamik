# FND-12 — Chặn thao tác không đủ quyền ở backend
- Nhóm: Foundation
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `11-permission-catalog.md`, `07-api-error-contract.md`
## Mục tiêu
Không thể vượt quyền bằng cách gọi trực tiếp API.
## Phạm vi
Áp dụng permission ở endpoint/service cho READ, CREATE, UPDATE, APPROVE và các hành động nhạy cảm của từng module.
## Tiêu chí nghiệm thu
- Chưa đăng nhập nhận 401; đã đăng nhập nhưng thiếu quyền nhận 403.
- Role chỉ xem gọi API ghi bị từ chối, kể cả khi tự gửi request.
- Kiểm tra quyền được áp dụng nhất quán ngoài giao diện.
## Kiểm tra
Đã có test HTTP `identity_rbac_http_tests` cho 401 khi chưa xác thực ở inventory/audit và 403 khi role chỉ xem đọc audit hoặc gọi POST nhập kho; cần mở rộng ma trận read/write cho mọi module.
## Ngoài phạm vi
Ẩn nút trên frontend không được tính là kiểm soát quyền hoàn tất.

