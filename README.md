# ERP Vinamik

ERP Vinamik là hệ thống ERP modular monolith cho Vinamik. Backend Spring Boot và frontend React dùng chung một PostgreSQL trung tâm; mọi máy trạm truy cập cùng API và cùng dữ liệu.

Repository: https://github.com/DuongHai2202/erp_vinamik

## Đọc trước khi thay đổi code

Agent hoặc developer mới cần đọc theo thứ tự:

1. `AGENTS.md` — bất biến kiến trúc, quy ước tên, ngôn ngữ và yêu cầu bảo toàn dữ liệu.
2. `task/README.md` — thứ tự ưu tiên, phạm vi module và quy tắc xử lý task.
3. `docs/database_design.md` — schema, chủ sở hữu bảng và quan hệ dữ liệu.
4. `docs/backend_api_contract.md` — hợp đồng API và mã lỗi.
5. `docs/development_setup.md` — thiết lập PostgreSQL, backend, frontend và triển khai.
6. Task cụ thể trong `task/` cùng các task phụ thuộc trước khi sửa.

Nội dung trong `docs/` là tài liệu nghiệp vụ/tham khảo. Khi có mâu thuẫn, yêu cầu người dùng và `AGENTS.md` được ưu tiên; không tự mở rộng phạm vi chức năng.

## Bản đồ repository

~~~text
ERP_Backend/   Spring Boot, Java, API, service, repository và Flyway migration
ERP_Client/    React.js, JavaScript, Vite, Ant Design và route giao diện
data/          Fixture CSV và script tạo/nạp dữ liệu local/test
docs/          Thiết kế database, API, nghiệp vụ và hướng dẫn vận hành
task/          Backlog, phạm vi module và tiêu chí nghiệm thu
UI_Sample/     Mẫu giao diện/tham chiếu trực quan
AGENTS.md      Chỉ dẫn bắt buộc cho agent
~~~

Mỗi module sở hữu nghiệp vụ, API và migration của mình. Module khác chỉ gọi hợp đồng công khai/service API; không đọc hoặc ghi trực tiếp bảng nội bộ của module khác. Client chỉ gọi API, không kết nối PostgreSQL.

## Phạm vi module hiện tại

- **Nền tảng dùng chung:** đăng nhập, session, user, role, permission và audit.
- **Quản lý nhân sự:** hồ sơ nhân viên, hợp đồng, nghỉ phép/vắng mặt, khen thưởng/kỷ luật và tính lương.
- **Kho và nguyên vật liệu:** danh mục vật tư, phiếu nhập, phiếu xuất, kiểm kê và điều chuyển kho.
- **Quản lý sản xuất:** kế hoạch, định mức/BOM, lệnh sản xuất, phân công nhân sự và sản lượng.
- **Quản lý chất lượng và giá thành:** MVP năm chức năng đã được ưu tiên triển khai trong code hiện tại; đọc task và API contract trước khi mở rộng.
- **Dữ liệu và báo cáo:** vẫn là backlog/deferred cho đến khi có quyết định mới.

Kho là chủ sở hữu số dư và sổ giao dịch. Sản xuất gửi yêu cầu qua hợp đồng kho, không tự cập nhật tồn kho.

## Yêu cầu môi trường

- Java 21 hoặc JDK tương thích.
- Node.js 20 trở lên và npm.
- PostgreSQL 15 trở lên.
- Một database PostgreSQL trung tâm tên `erp_vinamik` (hoặc tên được cấu hình trong `ERP_DB_URL`).

Không commit mật khẩu, token, file `.env`, log runtime, `target/`, `node_modules/` hoặc `dist/`.

## Chạy local

### 1. Cấu hình backend

Sao chép `ERP_Backend/.env.example` hoặc `.env.example` thành cấu hình local rồi export biến môi trường vào process/IDE. Spring Boot không tự đọc file `.env`.

Ví dụ PowerShell (thay giá trị bằng secret local của bạn):

~~~powershell
$env:ERP_DB_URL = "jdbc:postgresql://127.0.0.1:5432/erp_vinamik"
$env:ERP_DB_USERNAME = "erp_vinamik"
$env:ERP_DB_PASSWORD = "<local-secret>"
$env:ERP_FLYWAY_ENABLED = "true"
$env:ERP_AUTH_SECURE_COOKIE = "false"
$env:ERP_BOOTSTRAP_ADMIN_USERNAME = "admin"
$env:ERP_BOOTSTRAP_ADMIN_PASSWORD = "<long-local-secret>"
~~~

Chạy backend:

~~~powershell
cd ERP_Backend
.\mvnw.cmd spring-boot:run
~~~

Backend mặc định chạy tại `http://localhost:8080`. Health check: `http://localhost:8080/actuator/health`.

Flyway áp dụng migration version trước khi Hibernate validate schema. Không dùng `ddl-auto=update` và không sửa dữ liệu production bằng SQL thủ công.

### 2. Chạy frontend

Mở terminal khác:

~~~powershell
cd ERP_Client
npm ci
npm run dev
~~~

Frontend mặc định chạy tại `http://localhost:5173`. Vite proxy các request `/api` tới backend `http://localhost:8080`.

### 3. Fixture local/test

Fixture chỉ phục vụ local/test, không phải dữ liệu production. Chỉ bật khi cần:

~~~powershell
$env:ERP_FIXTURE_CONTRACT_ENABLED = "true"
$env:ERP_FIXTURE_STOCKTAKE_ENABLED = "true"
~~~

Các fixture phải chạy qua service nghiệp vụ để giữ validation và audit. Tắt hai cờ này ở môi trường dùng chung/production.

## Kiểm tra trước khi commit

Backend:

~~~powershell
cd ERP_Backend
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
~~~

Frontend:

~~~powershell
cd ERP_Client
npm run lint
npm run build
~~~

Kiểm tra thay đổi và file nhạy cảm:

~~~powershell
cd ..
git status --short
git diff --check
git diff --stat
~~~

Nếu task sửa API hoặc schema, cần kiểm tra cả migration, permission backend, response lỗi và luồng frontend tương ứng. Không đánh dấu task hoàn tất khi chưa chạy các kiểm tra phù hợp và chưa nêu giới hạn còn lại.

## Quy tắc làm việc cho agent

- Trước khi sửa, đọc task cụ thể và các task phụ thuộc.
- Giữ code/identifier/comment bằng tiếng Anh; nội dung giao diện bằng tiếng Việt; thông báo success/error trả client bằng tiếng Anh theo quy ước dự án.
- Dùng `lower_snake_case` cho tên mới của API, database, migration, permission và file nghiệp vụ.
- Backend luôn kiểm tra quyền; 401 là chưa xác thực, 403 là đã đăng nhập nhưng thiếu quyền.
- Dùng migration Flyway có version cho thay đổi schema.
- Dùng JPA/JPQL/native query qua lớp repository JPA; không dùng JdbcTemplate hoặc JDBC API trực tiếp.
- Không nhân bản dữ liệu chủ giữa module. Cần dữ liệu HR/kho/sản xuất thì dùng hợp đồng công khai.
- Không đưa secret vào source, fixture, README, log hoặc commit.
- Giữ thay đổi nhỏ, kiểm tra `git diff` trước commit và ghi rõ test đã chạy.

## Quy trình Git đề xuất

~~~powershell
git fetch origin
git status --short
git switch -c codex/<short-task-name>
# sửa code và chạy kiểm tra
git diff --check
git add <files>
git commit -m "<scope>: <short description>"
git push -u origin codex/<short-task-name>
~~~

Nhánh `main` đang theo dõi remote `origin`. Chỉ push trực tiếp vào `main` khi người dùng yêu cầu rõ ràng; khi làm task độc lập, ưu tiên nhánh `codex/` để dễ review.

## Tài liệu tham chiếu nhanh

- `docs/database_design.md` — thiết kế schema và quan hệ.
- `docs/backend_api_contract.md` — API và response/error contract.
- `docs/development_setup.md` — setup và triển khai.
- `docs/identity_registration_flow.md` — đăng ký, đăng nhập và phân quyền.
- `docs/payroll_policy.md` — chính sách và công thức tính lương.
- `docs/client_ui_ux_upgrade_plan.md` — roadmap UI/UX.
- `task/README.md` — backlog và thứ tự triển khai.
