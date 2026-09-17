# Hướng dẫn tác nhân cho ERP Vinamik

## Mục tiêu hiện tại

Xây dựng website ERP cho nhóm, ưu tiên theo backlog `task/README.md`: nền tảng dùng chung → Quản lý nhân sự → Quản lý kho và nguyên vật liệu → Quản lý sản xuất. Quản lý chất lượng & giá thành và Quản lý dữ liệu & báo cáo đang được hoãn.

## Bất biến kiến trúc

- Dùng modular monolith: một ứng dụng được triển khai và một PostgreSQL trung tâm; tách rõ module backend và module giao diện trong cùng repository. Năm máy truy cập cùng backend/database, không dựng database riêng theo máy.
- Mỗi nghiệp vụ chỉ có một chủ sở hữu module. Module không truy cập bảng/implementation nội bộ module khác; dùng hợp đồng công khai. Shared code đặt tại platform/common và chỉ chứa năng lực thực sự dùng chung.
- Login, user, role, permission, session/token và audit là platform dùng chung, không tính vào năm chức năng nghiệp vụ mỗi module.
- Backend phải kiểm tra quyền cho mọi thao tác; tài khoản không có quyền ghi chỉ được xem. Trả 401 khi chưa xác thực, 403 khi đã đăng nhập nhưng thiếu quyền.
- Sản xuất gửi yêu cầu xuất/nhập qua hợp đồng của Kho. Chỉ Kho được ghi sổ giao dịch và tính số dư tồn.
- Không đưa module deferred vào code/luồng ưu tiên chỉ vì thấy trong tài liệu. Chỉ lập backlog cho hai module này cho đến khi người dùng yêu cầu chuyển sang triển khai.

## Quy tắc thay đổi

- Trước khi sửa, đọc task tương ứng và các task phụ thuộc; chỉ chạm phạm vi của task.
- Không đổi framework, kiến trúc, công nghệ hoặc thứ tự ưu tiên nếu chưa có lý do kỹ thuật rõ ràng và ghi lại quyết định.
- Không tạo bảng dùng chung tùy tiện, không nhân bản dữ liệu chủ để tránh gọi module sở hữu dữ liệu.
- Bảo toàn dữ liệu: dùng migration có version, không xóa/sửa dữ liệu production bằng thao tác thủ công. Không commit secrets.
- Giao diện đồng nhất ở mọi máy vì truy cập cùng bản triển khai; không lưu dữ liệu nghiệp vụ chỉ ở localStorage/session của một máy.
- Trước khi kết thúc task, chạy kiểm tra phù hợp, ghi kết quả và nêu rõ giới hạn/chưa làm. Không đánh dấu task hoàn thành nếu chưa thỏa tiêu chí nghiệm thu.

## Yêu cầu từ tài liệu môn học

Tài liệu trong `docs/` là nguồn tham khảo để xác định nghiệp vụ. Giữ đúng phạm vi năm nhóm chức năng mỗi module đã chốt trong backlog; khi nội dung tài liệu không rõ hoặc mâu thuẫn, không tự suy diễn thành tính năng mới.

## Quy ước ngôn ngữ

- Code dùng tiếng Anh: tên file, class, method, biến, API, database object, permission code và comment trong source code.
- Log phục vụ vận hành/điều tra được viết bằng tiếng Việt, có ngữ cảnh và không chứa mật khẩu, token hoặc dữ liệu nhạy cảm.
- Toàn bộ nội dung giao diện người dùng viết bằng tiếng Việt: menu, nhãn, nút, tiêu đề, hướng dẫn và trạng thái.
- Thông báo lỗi/thành công hiển thị cho người dùng dùng tiếng Anh, gồm toast, banner, thông báo validation và nội dung lỗi trả từ API.
- Không trộn hai ngôn ngữ trong cùng một câu thông báo. Tách log nội bộ tiếng Việt khỏi error/success message tiếng Anh trả cho client.
- Dùng message constant/catalog tập trung khi phù hợp; không hard-code các thông báo rải rác nếu làm chúng khó thống nhất.

## Quy ước đặt tên

- Tên mới do dự án tự đặt dùng tiếng Anh ASCII chữ thường; nhiều từ nối bằng dấu gạch dưới theo `lower_snake_case`. Áp dụng cho tên thư mục/file mã nguồn, package segment, class/record/service, method, biến, API resource, schema, bảng, cột, constraint, index, sequence, migration và permission code.
- Không dùng tên có dấu, chữ hoa hoặc identifier SQL đặt trong dấu ngoặc kép. Giữ nguyên tên API/framework bên ngoài khi gọi chúng; không đổi tên file đặc biệt như `AGENTS.md`, `README.md` hoặc tên cấu hình bắt buộc khiến công cụ không nhận diện được.
- Tên migration Flyway dùng prefix chữ thường `v` và mô tả `lower_snake_case`.
- Mã nghiệp vụ/permission/API dùng `lower_snake_case`; nhãn hiển thị cho người dùng vẫn viết tiếng Việt, không đưa identifier kỹ thuật lên giao diện.
- Trước khi thêm thư viện hoặc convention bắt buộc tên riêng, ưu tiên quy ước của người dùng; nếu công cụ yêu cầu cách viết khác, chỉ dùng ngoại lệ tối thiểu cần để build/runtime hoạt động và ghi rõ lý do.
- React JSX yêu cầu tên symbol component viết hoa để nhận diện component và React Hooks yêu cầu tên hàm hook theo useXxx; đây là ngoại lệ cú pháp ở symbol. Tên file, thư mục, route và export alias nghiệp vụ vẫn dùng lower_snake_case khi không bị framework bắt buộc.

## Stack công nghệ đã thống nhất

- Backend: Spring Boot trên Java.
- Frontend: React.js dùng JavaScript.
- Database: PostgreSQL trung tâm dùng chung.
- Không tự thay framework/database hoặc chuyển JavaScript sang TypeScript nếu chưa có quyết định mới của người dùng.
- Cấu hình kết nối database nằm ở backend qua environment variables; trình duyệt React chỉ gọi API, không kết nối PostgreSQL trực tiếp.
- PostgreSQL được tạo và nâng cấp bằng Flyway migration; Spring Data JPA/JPA là lớp truy cập dữ liệu duy nhất của backend với ddl-auto=validate. Kể cả ledger, số dư, khóa dòng và truy vấn PostgreSQL đặc thù đều phải nằm trong repository JPA (JPQL hoặc native query qua Spring Data JPA/EntityManager); không dùng JdbcTemplate, Spring JDBC hoặc API JDBC trực tiếp trong code ứng dụng. PostgreSQL JDBC driver chỉ là driver kết nối bên dưới Hibernate/JPA.


## Chuẩn triển khai

- Backend tuân theo task/00-foundation/19-backend-coding-standards.md.
- Frontend UI/UX tuân theo task/00-foundation/20-frontend-ui-ux-standard.md; hiệu suất/dữ liệu lớn tuân theo task/00-foundation/21-frontend-performance-standard.md.
- Phạm vi nghiệp vụ và thứ tự ưu tiên lấy từ task/README.md cùng task cụ thể; tài liệu môn học là nguồn nghiệp vụ để đối chiếu.
- Thiết kế CSDL chuẩn lấy từ docs/database_design.md; task schema/API phải theo đúng chủ sở hữu bảng, tên và hợp đồng tham chiếu trong tài liệu này.

