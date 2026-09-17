# FND-05 — Tạo ranh giới module backend
- Nhóm: Foundation
- Trạng thái: TODO
- Phụ thuộc: `01-architecture-decisions.md`, `04-database-migrations.md`
## Mục tiêu
Tạo cấu trúc backend để module không phụ thuộc implementation nội bộ của module khác.
## Phạm vi
Đặt package platform, humanresources, inventory, production và các module deferred riêng; mỗi module có API/service công khai và vùng internal riêng.
## Tiêu chí nghiệm thu
- Có quy tắc đặt package và hướng phụ thuộc.
- Module khác không import repository/entity nội bộ.
- Hợp đồng tích hợp Kho–Sản xuất được gọi qua service/API công khai.
## Kiểm tra
Chạy build; thêm hoặc cấu hình kiểm tra phụ thuộc module nếu framework hiện tại hỗ trợ.
## Ngoài phạm vi
Không tách thành nhiều ứng dụng hoặc copy tiện ích chung sang từng module.

