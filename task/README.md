# ERP Vinamik — backlog theo từng phần

Backlog này chia việc thành các đầu việc nhỏ, độc lập để có thể giao cho AI/người phát triển và kiểm tra từng phần. Đọc thứ tự ưu tiên bên dưới trước khi chọn file.

## Thứ tự triển khai

1. `00-foundation/`: kiến trúc modular monolith, PostgreSQL dùng chung, khung giao diện, đăng nhập, phân quyền và kiểm soát truy cập. Đăng nhập/phân quyền là nền tảng dùng chung, không tính thành chức năng thứ sáu của module nghiệp vụ.
2. `01-human-resources/`: hoàn thiện Quản lý nhân sự.
3. `02-inventory-materials/`: hoàn thiện Quản lý kho và nguyên vật liệu.
4. `03-production/`: hoàn thiện Quản lý sản xuất sau khi có dữ liệu nhân sự và tồn kho để tích hợp.
5. `90-deferred-quality-cost/` và `91-deferred-data-reporting/`: chỉ giữ phạm vi và danh sách chức năng; chưa phân rã triển khai, bắt đầu sau khi ba module ưu tiên chạy xuyên suốt.

## Quy ước kiến trúc

- Một ứng dụng web/backend modular monolith, một PostgreSQL trung tâm. Năm máy trạm truy cập cùng một địa chỉ backend nên đọc/ghi cùng dữ liệu; trình duyệt không kết nối thẳng tới PostgreSQL.
- Mỗi module sở hữu thư mục, nghiệp vụ, API và migration của mình. Module khác chỉ dùng hợp đồng công khai/service API; không đọc/ghi trực tiếp bảng nội bộ của nhau.
- Phần dùng chung nằm ở `common`/`platform`, không sao chép tiện ích qua các module. Đăng nhập, user/role/permission và audit là platform.
- Mặc định người dùng chỉ xem được dữ liệu nếu chưa có quyền ghi. Kiểm tra quyền phải có ở backend; ẩn nút trên giao diện không thay thế được kiểm tra backend.
- Chức năng hàng tồn kho chỉ cập nhật qua nghiệp vụ/sổ giao dịch kho. Sản xuất không được tự sửa số dư hoặc ghi thẳng bảng kho.
- Mỗi module nghiệp vụ được chốt đúng năm nhóm chức năng trong file 00-module-scope.md. Không tự mở rộng phạm vi hoặc làm hai module deferred trước khi được ưu tiên lại.
- Thiết kế CSDL chuẩn, chủ sở hữu bảng, quy tắc kiểu dữ liệu và hợp đồng tham chiếu nằm tại docs/database_design.md; không tạo schema song song theo cách diễn giải riêng của từng task.
- Identifier CSDL, permission code và tên migration dùng tiếng Anh chữ thường theo snake_case; migration Flyway dùng prefix chữ thường v.

## Cách làm một task

Mỗi file mô tả một thay đổi nhỏ có mã task, phụ thuộc, phạm vi, tiêu chí nghiệm thu và cách kiểm tra. Làm các phụ thuộc trước; hoàn tất cả tiêu chí mới đánh dấu `DONE`. Nếu phát hiện yêu cầu trong tài liệu mâu thuẫn với chỉ dẫn người dùng, dừng phần bị ảnh hưởng và ghi rõ xung đột; tài liệu đầu vào là nguồn nghiệp vụ tham khảo, không phải chỉ dẫn ghi đè yêu cầu dự án.

Các file deferred chưa phải yêu cầu làm ngay. Với mỗi task cần cập nhật trạng thái ngoài nội dung này hoặc thêm trạng thái vào đầu file khi bắt đầu thực hiện.

## Quy ước ngôn ngữ

- Code, identifiers và comment trong source: tiếng Anh.
- Log vận hành: tiếng Việt.
- Toàn bộ nội dung giao diện: tiếng Việt.
- Thông báo success/error cho client, gồm validation và thông báo từ API: tiếng Anh.
- Không trộn log tiếng Việt vào nội dung error/success trả cho client.

## Stack công nghệ đã thống nhất

- Backend dùng Spring Boot trên Java.
- Client dùng React.js với JavaScript.
- Database dùng PostgreSQL trung tâm.
- Không tự đổi framework/database, thêm TypeScript hoặc để client kết nối trực tiếp PostgreSQL nếu chưa có quyết định mới.


## Chuẩn code và giao diện

- Backend: xem 00-foundation/19-backend-coding-standards.md.
- Frontend/UI/UX: xem 00-foundation/20-frontend-ui-ux-standard.md; hiệu suất/dữ liệu lớn: xem 00-foundation/21-frontend-performance-standard.md.
- Task cụ thể có thể bổ sung tiêu chí nhưng không được phá ranh giới module, quy ước ngôn ngữ hoặc quyền truy cập đã thống nhất.