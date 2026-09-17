# FND-04 — Thiết lập quy ước migration database
- Nhóm: Foundation
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `03-backend-database-config.md`
## Mục tiêu
Thay đổi schema có version, có thể tái lập và không ghi đè dữ liệu khi cập nhật.
## Phạm vi
Cấu hình Flyway hoặc công cụ migration tương thích với backend hiện tại; tạo schema identity, hr, inventory, production theo docs/database_design.md và giữ một lịch sử migration có thứ tự.

Tên migration dùng dạng v001__create_identity_schema.sql, tiếng Anh chữ thường snake_case; cấu hình migration prefix của Flyway là v. Tên bảng, cột, constraint và index theo cùng quy tắc.
## Tiêu chí nghiệm thu
- Migration đầu tiên chạy được trên database rỗng.
- Chạy lại backend không áp dụng cùng migration lần hai.
- Mỗi module sở hữu migration của mình; không dùng migration để sửa tay dữ liệu thật.
- Không tạo schema hoặc bảng cho quality_cost và reporting trong giai đoạn ưu tiên.
- Migration tuân theo thứ tự identity → hr → inventory → production và không tạo khóa ngoại tới bảng nội bộ module khác.
## Kiểm tra
Khởi tạo DB thử nghiệm mới, chạy migration và xác nhận lịch sử migration.
## Ngoài phạm vi
Không xóa database hoặc reset database nhóm đang sử dụng.


## Quan hệ với JPA

Flyway là nguồn sự thật cho cấu trúc PostgreSQL. Entity và `JpaRepository` chỉ ánh xạ/thao tác trên bảng đã có; Hibernate chạy `ddl-auto=validate` để phát hiện mapping sai khi backend khởi động, không thay thế migration và không tự sửa schema.

Mọi thao tác dữ liệu chạy qua JPA repository. Khi JPQL không đáp ứng được yêu cầu PostgreSQL, repository có thể dùng native query với bind parameters qua Spring Data JPA hoặc `EntityManager`; không dùng Spring JDBC/JdbcTemplate.


