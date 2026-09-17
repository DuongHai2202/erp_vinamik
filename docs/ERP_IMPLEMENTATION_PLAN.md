# Kế hoạch triển khai website ERP Vinamilk

## 1. Mục tiêu và nguyên tắc kiến trúc

Xây dựng một website ERP phục vụ đồ án với năm phân hệ nghiệp vụ:

1. Quản lý nhân sự.
2. Quản lý kho.
3. Quản lý sản xuất.
4. Quản lý chất lượng và giá thành.
5. Quản lý dữ liệu và báo cáo.

Hệ thống được triển khai theo modular monolith: một dự án nguồn, một giao diện web, một ứng dụng Spring Boot có thể chạy độc lập như một đơn vị triển khai, và một PostgreSQL trung tâm. Các phân hệ được tách theo trách nhiệm nghiệp vụ trong cả backend và frontend. Không tách thành năm ứng dụng/microservice ở giai đoạn đồ án.

Monorepo nói về cách tổ chức mã nguồn; modular monolith nói về cách tổ chức module và triển khai. Có thể dùng một monorepo chứa ERP_Backend và ERP_Client, sau đó đóng gói một backend và một giao diện web dùng chung.

Ưu tiên ranh giới rõ, dễ chạy và dễ kiểm thử hơn mức phân mảnh build. Bắt đầu bằng package-by-feature và kiểm thử cấu trúc phụ thuộc. Chỉ chuyển backend sang Maven multi-module nếu nhóm cần ranh giới biên dịch chặt hơn hoặc có nhiều nhóm phát triển độc lập; cách triển khai cuối vẫn có thể là một ứng dụng Spring Boot. Không dùng microservices, message broker, Kubernetes hay nhiều database độc lập trừ khi yêu cầu môn học thay đổi rõ rệt.

## 2. Hiện trạng và đầu việc trước khi phát triển

- ERP_Backend hiện đã có nền tảng Spring Boot/Java 21, Flyway, Spring Data JPA, identity/session/security và các vertical slice HR, inventory, production theo backlog; các luồng còn thiếu vẫn phải giữ trong task tương ứng và kiểm thử với PostgreSQL thật.
- ERP_Client đã có app shell React JavaScript, auth/private route, lazy route và identity admin page; các màn hình nghiệp vụ còn lại làm theo hợp đồng API.
- Tài liệu docs chứa yêu cầu HR, kho, chất lượng/giá thành, báo cáo và ERP tổng quan. Cần dùng chúng làm nguồn tham khảo, sau đó hợp nhất thành một bộ thuật ngữ và quy tắc nhất quán.
- Ảnh UI_Sample có một số nội dung theo ngữ cảnh bệnh viện. Không sao chép các nhãn đó vào website ERP Vinamilk.
- Git root hiện được phát hiện ở D:/WorkSpace/envCode, phía trên thư mục ERP_Vinamik và chứa các thư mục dự án khác. Trước khi tạo branch/worktree hoặc stage/commit, cần tách ranh giới Git cho dự án hoặc để người dùng xử lý. Trong thời gian đó, chỉ làm việc trong thư mục ERP_Vinamik và không chạy lệnh thay đổi Git.

### Đầu ra cần có trước khi code nghiệp vụ

- Sơ đồ kiến trúc và sơ đồ module.
- Danh sách chủ sở hữu dữ liệu theo module.
- ERD cấp logic, danh sách trạng thái và quy tắc chuyển trạng thái.
- Ma trận quyền role-permission.
- Một luồng demo xuyên module được xác nhận.
- Danh sách giả định cho các nghiệp vụ chưa được đặc tả, đặc biệt là tính lương và chi phí sản xuất.

Không dành quá nhiều thời gian làm tài liệu hình thức. Các đầu ra trên phải trực tiếp giúp code và kiểm thử.

## 3. Kiến trúc triển khai

### 3.1. Môi trường đồ án

Một máy chủ trong mạng trường/lab chạy PostgreSQL, backend và giao diện web. Năm máy người dùng dùng trình duyệt và truy cập cùng một địa chỉ, ví dụ http://erp-server:8080. Website gọi cùng API và backend kết nối cùng database. Trình duyệt không kết nối trực tiếp tới PostgreSQL.

Đối với môi trường demo:

- Một host chạy Docker Compose gồm PostgreSQL, backend và frontend/static web server.
- PostgreSQL lưu dữ liệu qua persistent volume. Restart container không được xóa dữ liệu.
- Chỉ mở cổng website/API cho các máy client. Không mở cổng PostgreSQL cho người dùng cuối.
- Tài khoản database và mật khẩu đặt trong biến môi trường hoặc file .env bị loại khỏi Git; không ghi bí mật vào application.properties đã commit.
- Backend dùng một URL kết nối trung tâm. Với Docker Compose, host database là tên service trong mạng Compose, không phải localhost của container backend.
- Có lệnh migration và dữ liệu demo xác định trước để dựng môi trường giống nhau.

Máy lập trình viên có thể dùng PostgreSQL cục bộ hoặc Docker trong lúc phát triển. Cơ sở dữ liệu cục bộ của mỗi lập trình viên chỉ là môi trường dev, không phải dữ liệu demo chung. Khi demo năm máy, tất cả phải truy cập cùng server và database trung tâm.

### 3.2. PostgreSQL và quản lý schema

Tạo một database erp_vinamik; tổ chức schema theo identity, hr, inventory, production. Tất cả schema nằm trong cùng một PostgreSQL database và cùng một dịch vụ backend truy cập. Hai schema quality_cost và reporting chỉ tạo khi hai module được ưu tiên triển khai. Schema giúp phân vùng quyền sở hữu dữ liệu nhưng không thay thế kiểm tra quyền ở backend.

Thiết kế chi tiết về bảng, trường, khóa, kiểu dữ liệu, hợp đồng tham chiếu và migration nằm tại docs/database_design.md. Không dùng phương án tên bảng có tiền tố thay thế các schema đã chốt, để tránh có hai cách đặt tên song song.

Dùng migration có phiên bản (ưu tiên Flyway nếu tương thích với Spring Boot hiện có) để tạo schema, bảng, index, constraint và seed dữ liệu ban đầu. Không dùng Hibernate auto-update/create cho database dùng chung. Ở môi trường có migration, ORM chỉ validate schema. Có thể dùng một role DB cho migration và role runtime ít quyền hơn nếu cấu hình không làm chậm đồ án.

Mọi bảng nghiệp vụ phải có khóa chính, khóa ngoại cần thiết, unique constraint cho mã nghiệp vụ, cột thời gian tạo/cập nhật, trạng thái khi vòng đời yêu cầu, và transaction cho nghiệp vụ thay đổi nhiều bản ghi. Không xóa cứng hồ sơ đã được chứng từ khác tham chiếu; dùng vô hiệu hóa hoặc trạng thái phù hợp.

### 3.3. Cấu trúc repository đề xuất

Giữ cấu trúc hiện có thay vì đổi tên hàng loạt:

~~~text
ERP_Vinamik/
  ERP_Backend/
    src/main/java/vn/vinamik/erp_backend/
      identity/
      hr/
      inventory/
      production/
      qualitycost/
      reporting/
      shared/
    src/main/resources/
      application.properties
      db/migration/
  ERP_Client/
    src/
      app/
      shared/
      modules/
        hr/
        inventory/
        production/
        quality-cost/
        reporting/
  docs/
  compose.yaml
  .env.example
  .agents/rules/erp-vinamik-scope.md
~~~

Trong backend, mỗi module tự tổ chức api, application, domain và infrastructure nếu khối lượng code cần đến; không tạo nhiều lớp rỗng chỉ để đủ tên thư mục. Controller/DTo, use case, entity/repository và migration phải thuộc module sở hữu nghiệp vụ. shared chỉ chứa hạ tầng thực sự dùng chung như lỗi API, audit interface, đồng hồ hệ thống và tiện ích nhỏ; không để shared trở thành nơi chứa toàn bộ entity hoặc nghiệp vụ.

## 4. Ranh giới sở hữu dữ liệu và giao tiếp module

### Identity

Sở hữu user_account, role, permission, user_role, role_permission, audit_log. Tài khoản website không phải tài khoản PostgreSQL.

### Nhân sự

Sở hữu phòng ban, chức danh, hồ sơ nhân viên, hợp đồng, ca, chấm công, đơn nghỉ, điều chỉnh thưởng/phạt và bảng lương. Các phân hệ khác lưu employee_id khi cần truy nguyên người lập, người thực hiện hoặc giờ lao động; không tạo hồ sơ nhân viên bản sao.

### Kho

Sở hữu nguyên vật liệu, sản phẩm/thành phẩm theo danh mục kho, đơn vị tính, kho/vị trí, chứng từ nhập/xuất/chuyển/kiểm kê, dòng chứng từ, sổ giao dịch tồn kho và số dư suy ra/cache nếu cần. Sổ giao dịch là nguồn truy vết; không sửa số lượng tồn trực tiếp mà bỏ qua giao dịch.

### Sản xuất

Sở hữu định mức/BOM, kế hoạch, lệnh sản xuất, phân công, nguyên liệu dự kiến, vật liệu thực dùng, tiến độ và sản lượng thực tế. Khi cần giữ/xuất vật tư, gọi public application service của Kho thay vì lấy repository Kho.

### Chất lượng và giá thành

Sở hữu tiêu chí/phiếu kiểm tra, kết quả, lỗi, quyết định đạt/không đạt, cách ly và các lần tính giá thành. Kết quả kiểm tra gửi quyết định cho Kho cập nhật trạng thái/tồn thành phẩm thông qua API nội bộ công khai. Giá thành dựa trên tiêu hao vật tư thực tế, sản lượng đạt, lao động và overhead đã ghi nhận/nhập theo quy tắc được công bố.

### Báo cáo

Sở hữu định nghĩa báo cáo, điều kiện lọc, lịch sử xuất file và mô hình đọc/tổng hợp nếu cần. Đây là module read-only đối với dữ liệu nghiệp vụ. Nó không cập nhật nhân viên, tồn kho, lệnh sản xuất hoặc kết quả kiểm tra.

### Nguyên tắc tích hợp

- Module chỉ được phụ thuộc vào API công khai/DTO/event đã thống nhất của module khác.
- Không import entity, repository hoặc lớp internal của module khác.
- Không để hai module cùng sở hữu cùng một bảng.
- Không tạo chu kỳ phụ thuộc. Ví dụ Production có thể gọi Inventory API; Inventory không gọi ngược vào Production để hoàn tất cùng nghiệp vụ.
- Dùng method/service call đồng bộ trước vì đây là một ứng dụng và một database. Chỉ dùng application event nội bộ khi có lợi rõ ràng như thông báo hoặc cập nhật projection. Không thêm broker ở MVP.
- Dùng transaction cho nhập/xuất kho, xác nhận tiêu hao, hoàn thành lệnh và nhận thành phẩm để không lưu trạng thái nửa chừng.
- Module reporting chỉ dùng query service/view đọc, không chỉnh sửa dữ liệu nguồn.

## 5. Quyền truy cập và dùng chung

Quyền được mô hình hóa theo module và hành động, không chỉ theo một nhãn role rộng:

- read/view
- create
- update
- delete/deactivate
- approve/reject
- export

Ví dụ permission code: hr_employee_read, hr_employee_update, inventory_receipt_create, inventory_receipt_post, production_order_update. Một tài khoản có thể có nhiều role. Role mẫu: system_admin, hr_staff, hr_manager, inventory_staff, production_staff, read_only.

Nếu người dùng có read nhưng không có create/update/delete/approve thì họ chỉ đọc. Frontend ẩn/khóa nút để dễ dùng; backend bắt buộc kiểm tra quyền ở endpoint và service, trả HTTP 403 khi bị từ chối. Không coi kiểm tra ở giao diện là bảo mật. Ghi user_id, thời điểm và hành động quan trọng vào audit log; không lưu mật khẩu dạng rõ.

Frontend dùng một app shell chung: đăng nhập, menu module theo quyền, header/user menu, API client, trang lỗi và component UI dùng chung. Nghiệp vụ, form, bảng và validation theo module nằm trong thư mục module tương ứng. Mọi dữ liệu bền vững được lấy/lưu qua REST API; localStorage không phải nguồn dữ liệu nghiệp vụ.

## 6. Luồng nghiệp vụ tích hợp làm chuẩn đồ án

1. Quản trị tạo tài khoản, gán role và danh mục dùng chung.
2. Nhân sự tạo nhân viên, phòng ban, chức danh, hợp đồng, ca và ghi nhận giờ lao động.
3. Sản xuất lập kế hoạch/lệnh sản xuất dựa trên BOM và nhu cầu nhân lực/ca.
4. Kho kiểm tra tồn, giữ/xuất vật liệu cho lệnh. Cấm xuất vượt tồn trừ khi có quy trình cho phép rõ ràng.
5. Sản xuất ghi nhận tiêu hao và sản lượng thực tế.
6. Chất lượng ghi phiếu kiểm tra. Lô đạt được chuyển sang nhập kho thành phẩm; lô lỗi bị cách ly/đánh dấu xử lý, không cộng nhầm vào tồn khả dụng.
7. Giá thành tổng hợp vật tư thực tế, lao động và overhead đã ghi, chia theo sản lượng đạt theo quy tắc đã thống nhất.
8. Báo cáo đọc số liệu nguồn: tồn, sản lượng kế hoạch/thực tế, tỷ lệ đạt/lỗi, chi phí, nhân sự và giờ công.

Luồng này phải chạy end-to-end trước khi mở rộng dashboard hoặc thêm hiệu ứng giao diện.

## 7. Kế hoạch triển khai theo giai đoạn

### Giai đoạn 0 — Khóa phạm vi và hợp đồng module

- Đọc các tài liệu hiện có và lập danh mục requirement theo năm module.
- Chốt glossary: nhân viên, nguyên liệu, sản phẩm, lô, lệnh sản xuất, phiếu nhập/xuất, kết quả chất lượng, giá thành.
- Vẽ ERD logic và ma trận sở hữu bảng.
- Viết role-permission matrix và vòng đời trạng thái chứng từ.
- Chốt giả định dễ gây sai: cách tính nghỉ/lương; ngày công chuẩn; overtime; thuế/bảo hiểm có nằm ngoài MVP không; cách tính overhead; xử lý phiếu đã duyệt; đơn vị tính.
- Chốt giao diện tiếng Việt và bối cảnh nhà máy sữa; loại bỏ nội dung bệnh viện trong hình mẫu.
- Tạo backlog và tiêu chí nghiệm thu cho mỗi use case.

Điều kiện hoàn thành: không còn hai module cùng sở hữu cùng dữ liệu; mọi giả định chưa có trong tài liệu đều được ghi lại, không để agent tự bịa thành luật.

### Giai đoạn 1 — Cô lập repo và dựng nền tảng chạy

- Giữ ERP_Backend và ERP_Client; không tạo năm ứng dụng Spring Boot.
- Xác nhận thư mục project là phạm vi chỉnh sửa. Git root hiện ở thư mục cha; không dùng worktree hoặc stage/commit từ thư mục cha cho tới khi ranh giới repo được xử lý.
- Kiểm tra Java 21, Maven wrapper, Node/npm và PostgreSQL/Docker trên máy chạy demo.
- Thêm datasource, ORM cần thiết, connection pool mặc định, profile local/demo, Flyway migration, health check.
- Thêm compose.yaml cho database bền vững, backend và web; khai báo .env.example, loại .env thật khỏi Git.
- Tạo React/Vite/JavaScript trong ERP_Client; cấu hình API base URL bằng môi trường.
- Tạo request/response API envelope thống nhất nếu thật sự giúp frontend; thống nhất lỗi validation, 401, 403, 404 và 409.

Điều kiện hoàn thành: app chạy rỗng được, database giữ dữ liệu sau restart, migration dựng DB mới tự động, frontend gọi được backend, không có mật khẩu trong mã nguồn.

### Giai đoạn 2 — Identity, phân quyền và audit

- Thiết kế user, role, permission và các liên kết; tạo account demo với mật khẩu hash.
- Implement login/logout, current-user endpoint, session/token strategy duy nhất.
- Implement endpoint/method authorization cho read/create/update/delete/approve/export.
- Tạo seeded roles và permission matrix.
- Thêm trang đăng nhập, route guard và menu dựa trên permission.
- Ghi audit cho đăng nhập cần theo dõi và các nghiệp vụ quan trọng; không ghi bí mật vào log.
- Viết tests: anonymous bị 401; read-only đọc được nhưng mutation nhận 403; role đúng thực hiện được thao tác được cấp.

Điều kiện hoàn thành: quyền được chứng minh tại backend, không chỉ bằng cách giấu nút.

### Giai đoạn 3 — Khung giao diện và danh mục nền

- Tạo app shell, menu năm module, header, profile/logout, breadcrumb, loading/error/empty state, bảng có tìm kiếm/phân trang.
- Tạo danh mục phòng ban/chức danh ở Nhân sự; kho, đơn vị tính, nguyên liệu, sản phẩm và danh mục liên quan ở Kho.
- Tạo mã nghiệp vụ duy nhất và validation dùng chung theo hợp đồng API.
- Tạo dữ liệu demo nhất quán để trình diễn.

Điều kiện hoàn thành: trang hoạt động theo permission; refresh trang không mất dữ liệu đã lưu; không chứa dữ liệu bệnh viện.

### Giai đoạn 4 — Nhân sự

- CRUD/tra cứu nhân viên, soft-deactivate và lọc theo phòng ban/trạng thái.
- Hợp đồng: loại, ngày bắt đầu/kết thúc, lương cơ bản, trạng thái, cảnh báo trong 15 ngày.
- Ca/chấm công đơn giản và đơn nghỉ có trạng thái draft/pending/approved/rejected/cancelled theo yêu cầu cuối cùng.
- Thưởng/phạt và lịch sử thay đổi.
- Bảng lương theo kỳ: lưu kỳ, nhân viên, snapshot lương, ngày công, các dòng cộng/trừ và trạng thái draft/approved/paid.
- Không tự tính thuế/bảo hiểm hay diễn giải pháp luật nếu ngoài phạm vi.
- Test công thức, hợp đồng sắp hết hạn, duplicate/overlap leave, quyền xem lương.

Điều kiện hoàn thành: mỗi nhân viên/hợp đồng/lương có thể truy nguyên người tạo và dữ liệu nguồn.

### Giai đoạn 5 — Kho

- Danh mục nguyên liệu/thành phẩm, kho/vị trí, đơn vị tính.
- Chứng từ nhập, xuất, chuyển kho, kiểm kê, trạng thái lưu nháp/chờ duyệt/đã duyệt/hủy theo mức cần thiết.
- Lưu stock movement bất biến sau khi duyệt; tính số dư từ giao dịch hoặc cập nhật số dư bằng transaction có audit.
- Validation số lượng > 0, tham chiếu tồn tại, đơn vị hợp lệ; chống xuất âm; kiểm tra double submit.
- Test luồng nhập làm tăng, xuất làm giảm, chuyển kho cân bằng, hủy/điều chỉnh đúng quy tắc.

Điều kiện hoàn thành: số dư xem được từ chứng từ; không thể thay đổi tồn mà không có vết giao dịch.

### Giai đoạn 6 — Sản xuất

- Định mức/BOM gắn nguyên liệu và sản phẩm.
- Kế hoạch và lệnh sản xuất, số lượng mục tiêu, thời hạn, người/ca phụ trách.
- Kiểm tra hoặc giữ vật tư qua API Kho trước khi xác nhận lệnh.
- Ghi nhận tiêu hao thực tế, tiến độ, sản lượng đầu ra và phế phẩm.
- Dùng employee_id và ca từ Nhân sự, không sao chép hồ sơ nhân viên.
- Test thiếu tồn, cấp vật tư, hoàn thành lệnh, rollback khi một phần giao dịch lỗi.

Điều kiện hoàn thành: tồn kho và trạng thái lệnh nhất quán sau hoàn thành hoặc lỗi.

### Giai đoạn 7 — Chất lượng và giá thành

- Cấu hình tiêu chí kiểm tra theo nhóm sản phẩm/nguyên liệu trong phạm vi đồ án.
- Ghi inspection theo lô, kết quả, lỗi, người kiểm tra, quyết định đạt/không đạt/cách ly.
- Lô đạt mới được ghi nhận thành phẩm khả dụng; lô lỗi không xuất hiện như hàng đạt.
- Tính giá theo tiêu hao vật tư thực tế + giờ công/labor cost + overhead theo giả định được duyệt, chia cho sản lượng đạt.
- Lưu từng lần tính, chi tiết đầu vào và người xác nhận; không ghi đè kết quả lịch sử không có audit.
- Test tỷ lệ lỗi, cách ly, làm tròn và kiểm tra mẫu chi phí.

Điều kiện hoàn thành: mỗi kết quả giá thành truy ra lệnh sản xuất, vật tư, giờ công và phiên bản quy tắc dùng.

### Giai đoạn 8 — Dữ liệu và báo cáo

- Tra cứu/lọc/phân trang và quyền xem theo module.
- Báo cáo tối thiểu: tồn kho; nhập-xuất; kế hoạch so với thực tế; tỷ lệ chất lượng; giá thành; headcount/giờ công; bảng lương theo quyền.
- Dùng query service/view read-only; không tạo bản ghi nghiệp vụ khi chỉ xem báo cáo.
- Xuất CSV trước; thêm XLSX/PDF khi đã ổn định và còn thời gian.
- Test số liệu report khớp truy vấn nguồn với cùng bộ lọc và kỳ.

Điều kiện hoàn thành: người không có quyền lương không thể lấy dữ liệu lương qua API báo cáo hoặc export.

### Giai đoạn 9 — Tích hợp, kiểm thử và chạy năm máy

- Chạy luồng demo end-to-end từ nhập kho đến báo cáo giá thành.
- Chạy backend tests, frontend build/tests, migration mới từ database rỗng và migration nâng cấp.
- Kiểm thử 401/403, validation, duplicate submit, giao dịch tồn kho, quyền export và dữ liệu đồng thời.
- Cắm tối thiểu hai trình duyệt/máy vào cùng URL: ghi ở máy A, đọc lại ở máy B; mở rộng lên năm máy trong mạng lab.
- Restart backend/container và xác minh dữ liệu vẫn còn.
- Cung cấp backup/restore hướng dẫn, tài khoản demo/role matrix, quy trình dựng sạch, sơ đồ module và API.
- Kiểm thử trên các kích thước màn hình cơ bản; ưu tiên dùng được, không làm animation trước nghiệp vụ.

Điều kiện hoàn thành: năm máy dùng cùng URL thấy cùng dữ liệu; module báo lỗi không làm mất giao dịch đã commit; các bài test và lệnh build chính đều xanh.

## 8. Quy chuẩn API, giao diện và kiểm thử

- API phân vùng theo /api/v1/{module}; DTO không trả entity JPA trực tiếp.
- Dùng mã HTTP nhất quán: 200/201 thành công, 400 dữ liệu sai, 401 chưa đăng nhập, 403 thiếu quyền, 404 không tồn tại, 409 xung đột trạng thái/mã trùng.
- Dùng phân trang và sắp xếp server-side cho danh sách lớn; validate filter/date range.
- Frontend chỉ giữ session/token và trạng thái UI cần thiết; dữ liệu nghiệp vụ lấy lại từ server.
- Unit test cho công thức thuần; integration test cho API/database/permission; contract hoặc architecture test cho biên module; end-to-end test cho luồng demo.
- Mỗi module có ít nhất một test luồng CRUD, quyền read-only, validation và giao dịch đặc thù.
- Không chấp nhận test chỉ kiểm tra app khởi động khi có nghiệp vụ mới.

## 9. Definition of Done cho từng chức năng

Một chức năng chỉ được xem là hoàn thành khi:

1. Có owner module xác định và không truy cập nội bộ module khác.
2. Có migration/schema/constraint cần thiết, API, validation và UI.
3. Đã kiểm tra quyền backend cho cả phép được cấp lẫn phép bị từ chối.
4. Có trạng thái loading, empty, lỗi và thành công rõ ràng.
5. Có test phù hợp cho luật nghiệp vụ; không chỉ test render UI.
6. Dữ liệu lưu được qua restart và có thông tin audit cần thiết.
7. Đã chạy lệnh test/build liên quan và ghi kết quả vào báo cáo hoàn tất.
8. Tài liệu chạy local/demo và giả định nghiệp vụ được cập nhật.

## 10. Rủi ro và phương án kiểm soát

- Git root hiện ở thư mục cha: tránh stage, commit, clean, reset, checkout hoặc worktree từ phạm vi rộng; tách repo trước khi cần workflow Git.
- Đặc tả từ nhiều tài liệu không đồng nhất: duy trì glossary, ERD và decision log; không đoán các công thức ảnh hưởng tồn kho/lương/giá thành.
- Một backend lỗi có thể ảnh hưởng cả website: kiểm thử module, validate ranh giới, transaction và health checks; đây là đặc tính của một lần triển khai chung.
- Chia sẻ DB không tự bảo đảm dữ liệu luôn đúng: dùng transaction, khóa/constraint, API module và audit.
- Máy chủ tắt hoặc mất mạng thì năm máy không truy cập được: dùng host ổn định trong buổi demo và backup database.
- Thông tin lương và tài khoản nhạy cảm: phân quyền chặt, password hash, không đưa mật khẩu thật vào seed/commit/log.

## 11. Tiêu chí nghiệm thu cuối đồ án

- Một server có thể dựng hệ thống từ hướng dẫn; một database trung tâm giữ dữ liệu qua restart.
- Năm máy truy cập cùng URL, tạo dữ liệu ở máy này và xem ở máy khác.
- Một tài khoản có thể xem nhưng không sửa; backend chứng minh bằng HTTP 403 khi gọi mutation.
- Các module đúng phạm vi, menu hiện theo quyền và các API phân module.
- Luồng demo kho → sản xuất → chất lượng → thành phẩm → giá thành → báo cáo hoạt động.
- Nhân sự cung cấp hồ sơ/giờ công cho phân công và chi phí; báo cáo không ghi ngược vào dữ liệu nghiệp vụ.
- Không có dữ liệu bệnh viện trong giao diện ERP Vinamilk; tài liệu giả định/giới hạn được ghi rõ.
- Lệnh backend test, frontend build/test và hướng dẫn triển khai đều được xác minh.
