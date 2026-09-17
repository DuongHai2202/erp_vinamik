# Tiến độ triển khai nền tảng — ERP Vinamik

Tài liệu này ghi lại phần đã hiện thực và giới hạn kiểm tra, để task tiếp theo không lặp lại hoặc đi lệch kiến trúc.

## Đã hiện thực

- Spring Boot backend dùng PostgreSQL qua biến môi trường; `spring-boot-starter-flyway` và adapter PostgreSQL chạy migration theo thư mục sở hữu, sau đó Spring Data JPA kiểm tra mapping bằng ddl-auto=validate.
- Schema/migration identity, HR, inventory, production; seed role/permission idempotent trong `identity/v005__seed_identity_permissions.sql`; migration `v012` thêm quyền quản trị system_admin; `v013` và `v017` bảo vệ duy nhất một super admin cùng role `system_admin`; `v014` khóa ledger Kho append-only; `v015` khóa audit append-only; `v016` seed quyền đọc audit; `v018` thêm quyền master data. Các migration HR `v019`–`v022` hoàn thiện lifecycle/snapshot payroll, trigger chống sửa hoặc chuyển dữ liệu đã khóa và partial/covering index cho truy vấn input lương; `v023` bổ sung ngày sinh nhân viên và ràng buộc ngày sinh không sau ngày vào; `v024` khóa định dạng phone canonical ở database.
- Login/logout/current-user bằng session opaque; chỉ lưu hash session trong DB, gửi session qua cookie `HttpOnly`, CSRF có handler tương thích SPA (raw token từ cookie), khóa tạm khi đăng nhập sai nhiều lần. Bootstrap super admin khóa advisory theo transaction để nhiều instance khởi động đồng thời không tạo trùng tài khoản bảo vệ.
- API error response dùng mã ổn định, correlation ID, lỗi validation và status 401/403/409/500; log vận hành viết tiếng Việt; audit metadata được serialize bằng Jackson để giữ JSONB hợp lệ và lọc đệ quy khóa password/secret/token/cookie trước khi ghi. Có API tra cứu audit phân trang tại `/api/v1/identity/audit_events`, chỉ yêu cầu `identity_audit_read`; login thành công/thất bại và logout được ghi audit.
- Actuator chỉ expose health; readiness/liveness trả trạng thái tổng quát cho reverse proxy, API nghiệp vụ vẫn cần authentication, graceful shutdown bật để rollout không cắt request đang chạy.
- React JavaScript app shell, login route, private route, menu theo permission và cấu hình theme Việt hóa theo chuẩn UI/UX; identity admin page đã gọi API quản lý user/role.
- Đã có backend vertical slice cho hồ sơ nhân viên, hợp đồng, nghỉ phép, khen thưởng/kỷ luật, payroll, phiếu nhập/phiếu xuất, điều chuyển, kiểm kê, số dư kho, BOM, kế hoạch, lệnh, kiểm tra nhu cầu, phân công, tiêu hao, sản lượng tốt/lỗi và tiến độ lệnh. Payroll dùng công thức version `monthly_mon_sat_v1`, snapshot isolation `REPEATABLE_READ`, snapshot input, lifecycle calculate/approve/reject/lock, audit và trigger PostgreSQL bảo vệ kỳ đã khóa. Mọi tương tác dữ liệu ứng dụng dùng Spring Data JPA hoặc native query tham số hóa qua EntityManager gateway; không còn JdbcTemplate, Spring JDBC hoặc service tự giữ gateway truy vấn. Các truy vấn nằm trong repository sở hữu feature/module, gồm ledger cần khóa dòng, advisory lock, idempotency và ON CONFLICT. Production chỉ import public contract của HR/Inventory. Master API của HR/Kho có soft deactivate, phân trang, RBAC và audit. Phân trang dùng guard chung, giới hạn page hợp lý và tính offset bằng long để tránh overflow.
- Hợp đồng bàn giao frontend nằm tại `docs/antigravity_frontend_handoff.md`; API v1 và lifecycle nằm tại `docs/backend_api_contract.md`.
- Fixture tải lớn nằm trong `data/`: 600 nhân viên có số điện thoại chuẩn hóa, ngày sinh và cột trình bày ngày `dd/mm/yyyy`; 520 mặt hàng, 600 lệnh sản xuất cùng các file quan hệ cho hợp đồng, nghỉ, thưởng/kỷ luật, lô, ledger/số dư Kho, BOM, nhu cầu, phân công, tiêu hao và sản lượng. Generator dùng seed cố định; validator kiểm tra số lượng, mã tham chiếu, định dạng phone/ngày và số dư trước khi bàn giao.
- Route màn hình nghiệp vụ lazy-load; client không lưu password hoặc session token vào localStorage.

## Chưa coi là hoàn tất

- Đã chạy 24 migration trên PostgreSQL 18 cục bộ và JPA schema validation khởi động thành công; chưa chạy trên staging/CI có replication hoặc failover.
- Backend ba module ưu tiên đã có API/lifecycle để frontend tích hợp. Phần còn lại của giai đoạn hiện tại là các UI nghiệp vụ, smoke test năm máy và đóng gói triển khai.
- Integration test JPA/PostgreSQL opt-in kiểm tra migration/schema/constraint/trigger; sổ Kho kiểm tra nhập → xuất → điều chuyển → kiểm kê, idempotency và tồn không âm; Sản xuất kiểm tra kế hoạch → BOM → lệnh → nhu cầu → tiêu hao → sản lượng → nhập thành phẩm; HR kiểm tra hồ sơ → hợp đồng → nghỉ không lương/thưởng/phạt → tính lại → duyệt → khóa payroll, bỏ input pending/rejected và bảo vệ snapshot.
- Chưa có stress/load test dài hạn, failover PostgreSQL hoặc ma trận HTTP cho từng endpoint đơn lẻ. Kiểm thử HTTP hiện bao phủ 401 và 403 cho các nhóm mutate nhạy cảm của HR, payroll, master data, nhập/xuất/điều chuyển/kiểm kê Kho, lệnh và nhập thành phẩm Sản xuất.
- Đã viết `docs/central_deployment_runbook.md` cho mô hình một backend/PostgreSQL trung tâm, reverse proxy/static frontend và kiểm tra năm máy; chưa có người khác chạy xác nhận trên môi trường sạch.
- Chưa đóng gói frontend static vào backend hoặc compose triển khai; đây là đầu việc deployment sau khi API nghiệp vụ ổn định.

## Kiểm tra gần nhất

- Lần chạy toàn bộ gần nhất với `ERP_RUN_INTEGRATION_TESTS=true`: 92 test thuộc 28 test suite, 0 failure, 0 error, 0 skipped trên PostgreSQL 18 cục bộ. Cùng lượt chạy này, Flyway xác nhận 24 migration, JPA validation thành công; schema, RBAC, HR/payroll, master data, ledger Kho và luồng Sản xuất–Kho đều chạy xuyên suốt.
- Validator fixture cuối cùng trả `valid: true`, không lỗi tham chiếu hoặc số dư âm; chi tiết số dòng nằm trong `data/manifest.json`.
- Frontend: `npm run lint` và `npm run build` chạy thành công với Vite; trang hợp đồng được lazy-load. Build thành công không thay thế kiểm thử gọi API trên database dùng chung.
- Không task nào được đánh dấu `DONE` chỉ dựa trên hai lệnh build/test trên; cần hoàn thành tiêu chí nghiệm thu và kiểm thử DB trước.

## Quy ước tên

File/thư mục/module route và identifier do dự án đặt dùng tiếng Anh chữ thường, nhiều từ nối bằng `_`. Nhãn UI dùng tiếng Việt; error/success trả client dùng tiếng Anh; log vận hành dùng tiếng Việt. Symbol component React viết hoa chỉ vì JSX bắt buộc.



