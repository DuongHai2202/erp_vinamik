# FND-06 — Tạo khung frontend và ranh giới module
- Nhóm: Foundation
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `01-architecture-decisions.md`, `00-repo-inventory.md`
## Mục tiêu
Tạo một web client có layout nhất quán và vùng code riêng cho từng module.
## Phạm vi
Tạo platform/common, modules/human-resources, modules/inventory, modules/production; router, navigation, trang lỗi và cấu hình gọi cùng backend.
## Tiêu chí nghiệm thu
- Tất cả máy truy cập cùng bản frontend trên máy chủ ứng dụng.
- Mỗi module chỉ đặt route/component nghiệp vụ của mình trong thư mục riêng.
- Không lưu dữ liệu nghiệp vụ duy nhất trên localStorage của một máy.
## Kiểm tra
Build frontend và mở các route placeholder từ một trình duyệt.
## Ngoài phạm vi
Không xây màn hình nghiệp vụ chi tiết trong task khung.

