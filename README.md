# ERP Vinamik

ERP Vinamik là website quản trị vận hành sản xuất cho Vinamilk. Hệ thống dùng một backend Spring Boot, giao diện React và cơ sở dữ liệu PostgreSQL dùng chung để mọi người truy cập cùng một dữ liệu.

Website hiện có các nhóm chức năng:

- Đăng nhập, tài khoản, phân quyền và nhật ký thao tác.
- Quản lý nhân sự: nhân viên, hợp đồng, nghỉ phép, khen thưởng/kỷ luật và kỳ lương.
- Quản lý kho và nguyên vật liệu: danh mục vật tư/thành phẩm, nhập kho, xuất kho, kiểm kê, điều chuyển và số dư tồn.
- Quản lý sản xuất: kế hoạch, định mức nguyên vật liệu (BOM), lệnh sản xuất, phân công nhân sự và ghi nhận sản lượng/tiêu hao.
- Quản lý chất lượng và giá thành ở mức dữ liệu phục vụ bản demo.

Thư mục `data/` chứa dữ liệu mẫu và công cụ nạp dữ liệu qua API để tạo một môi trường demo có thể kiểm thử xuyên suốt các module. Cấu hình `render.yaml` dùng để triển khai backend, frontend và PostgreSQL trên Render; sau khi triển khai, frontend truy cập API trung tâm nên dữ liệu vẫn còn khi tắt máy cá nhân.
