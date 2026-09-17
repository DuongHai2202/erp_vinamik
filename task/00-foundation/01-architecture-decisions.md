# FND-01 — Chốt quyết định kiến trúc và phạm vi
- Nhóm: Foundation
- Trạng thái: TODO
- Phụ thuộc: `00-repo-inventory.md`
## Mục tiêu
Ghi lại quyết định modular monolith, cách chia module và các giả định triển khai để mọi task đi cùng một hướng.
## Phạm vi
Một ứng dụng backend triển khai tập trung, một web client, một PostgreSQL trung tâm; module nghiệp vụ tách theo trách nhiệm; login/RBAC/audit là platform dùng chung.
## Tiêu chí nghiệm thu
- Quyết định ghi rõ một database dùng chung, không có database riêng trên từng máy người dùng.
- Chỉ rõ 5 module nghiệp vụ và hai module đang hoãn trong `task/README.md`.
- Ghi rõ API/backend chịu trách nhiệm kiểm tra quyền và module Kho sở hữu sổ tồn.
## Kiểm tra
Đối chiếu quyết định với `AGENTS.md`, task index và yêu cầu người dùng; không có mô tả mâu thuẫn.
## Ngoài phạm vi
Không biến modular monolith thành microservices, không thêm message broker hoặc hạ tầng chưa được yêu cầu.

## Stack bắt buộc

- Backend: Spring Boot trên Java.
- Frontend: React.js dùng JavaScript.
- Database: PostgreSQL trung tâm.
- React gọi API của Spring Boot; không kết nối trực tiếp tới PostgreSQL.
- Không thay framework hoặc đổi JavaScript sang TypeScript nếu chưa có quyết định mới của người dùng.

