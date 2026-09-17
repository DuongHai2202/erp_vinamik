# Vinamik

Hệ thống ERP modular monolith cho Vinamik, dùng chung PostgreSQL trung tâm và phục vụ các module quản lý nhân sự, kho và nguyên vật liệu, sản xuất, quản lý chất lượng và giá thành, dữ liệu và báo cáo.

## Công nghệ

- Backend: Spring Boot, Java, Spring Security, Spring Data JPA
- Frontend: React.js, JavaScript, Vite, Ant Design
- Database: PostgreSQL, Flyway
- Kiến trúc: modular monolith, dùng chung một backend và một database trung tâm

## Phạm vi hiện tại

Ba module ưu tiên đang được triển khai:

- Quản lý nhân sự
- Quản lý kho và nguyên vật liệu
- Quản lý sản xuất

Quản lý chất lượng và giá thành cùng quản lý dữ liệu và báo cáo đang được giữ trong backlog theo tài liệu môn học.

## Chạy local

1. Tạo database PostgreSQL erp_vinamik.
2. Sao chép .env.example thành file .env và điền thông tin kết nối local.
3. Chạy backend:

~~~powershell
cd ERP_Backend
./mvnw spring-boot:run
~~~

4. Chạy frontend ở terminal khác:

~~~powershell
cd ERP_Client
npm ci
npm run dev
~~~

Frontend chỉ gọi API backend; trình duyệt không kết nối trực tiếp tới PostgreSQL.

## Dữ liệu kiểm thử

Thư mục data/ chứa fixture và công cụ nạp dữ liệu lớn cho HR, inventory và production. Dữ liệu dùng cho local/test, không thay thế dữ liệu production.

## Tài liệu

- docs/database_design.md: thiết kế database
- docs/backend_api_contract.md: hợp đồng API
- docs/development_setup.md: thiết lập môi trường
- docs/identity_registration_flow.md: đăng ký, đăng nhập và phân quyền
- docs/ui_redesign_direction.md: nguyên tắc UI/UX
- task/README.md: backlog và thứ tự triển khai
- AGENTS.md: quy tắc làm việc và bất biến kiến trúc
