# FND-03 — Cấu hình backend kết nối PostgreSQL
- Nhóm: Foundation
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `02-central-postgresql-topology.md`
## Mục tiêu
Backend kết nối PostgreSQL trung tâm bằng cấu hình môi trường, dùng cùng một cấu hình logic ở mọi bản triển khai.
## Phạm vi
Khai báo ERP_DB_URL, ERP_DB_USERNAME, ERP_DB_PASSWORD, ERP_DB_POOL_SIZE và timeout qua environment variables; có .env.example không chứa secret. Mật khẩu bắt buộc, không có giá trị mặc định rỗng.
## Tiêu chí nghiệm thu
- Chạy được với PostgreSQL hiện có bằng cấu hình ngoài source.
- Thiếu hoặc sai cấu hình cho thông báo khởi động rõ ràng, không in password.
- Không có client code nào chứa thông tin kết nối PostgreSQL.
## Kiểm tra
Khởi động backend với cấu hình hợp lệ; thử cấu hình thiếu password; tìm secret trong diff.
## Ngoài phạm vi
Không tự thêm database theo từng developer và không thêm dịch vụ container bắt buộc nếu PostgreSQL hiện có đã dùng được.


## Quy ước với JPA

Database và schema không được tạo bởi Hibernate. Flyway tạo/cập nhật schema trước; Spring Data JPA dùng `ddl-auto=validate` để kiểm tra entity có khớp schema hay không. Không dùng `ddl-auto=update` hoặc `ddl-auto=create` trong môi trường dùng chung.

JPA/Spring Data JPA là API truy cập dữ liệu duy nhất của backend. Không dùng `JdbcTemplate`, Spring JDBC hoặc gọi JDBC API trực tiếp trong code ứng dụng. PostgreSQL JDBC driver chỉ được Hibernate/JPA dùng nội bộ để kết nối.


