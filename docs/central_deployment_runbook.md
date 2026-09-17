# Triển khai tập trung ERP Vinamik

## Mô hình mạng

Chỉ có một máy chủ chạy Spring Boot và một PostgreSQL trung tâm. Năm máy trạm mở cùng một địa chỉ web nội bộ, ví dụ `http://erp-server:8080` hoặc địa chỉ reverse proxy. Máy trạm không cài PostgreSQL, không giữ database riêng và không kết nối database trực tiếp từ React.

Luồng truy cập là:

```text
workstation_1..5 -> reverse_proxy/static_frontend -> spring_boot_api -> postgresql
```

Chỉ máy chủ ứng dụng cần `ERP_DB_URL`, `ERP_DB_USERNAME` và `ERP_DB_PASSWORD`. Không đưa các biến này vào frontend hoặc trình duyệt.

## Chuẩn bị PostgreSQL

Tạo database và role dùng cho ứng dụng bằng pgAdmin hoặc lệnh quản trị của PostgreSQL. Không ghi mật khẩu thật vào repository. Role ứng dụng cần quyền kết nối database và quyền tạo/cập nhật schema migration theo chính sách triển khai của nhóm.

## Cấu hình máy chủ backend

Dùng `.env.example` ở thư mục gốc làm mẫu cho file cấu hình ngoài source hoặc đặt biến môi trường của service:

```text
ERP_DB_URL=jdbc:postgresql://postgres-server:5432/erp_vinamik
ERP_DB_USERNAME=erp_vinamik
ERP_DB_PASSWORD=<secret>
ERP_DB_CONNECTION_TIMEOUT_MS=5000
ERP_DB_POOL_SIZE=10
ERP_FLYWAY_ENABLED=true
ERP_AUTH_SECURE_COOKIE=true
ERP_BOOTSTRAP_ADMIN_USERNAME=<only_for_first_bootstrap>
ERP_BOOTSTRAP_ADMIN_PASSWORD=<only_for_first_bootstrap>
```

`ERP_BOOTSTRAP_ADMIN_USERNAME` và `ERP_BOOTSTRAP_ADMIN_PASSWORD` chỉ cần đặt ở lần khởi động đầu tiên khi database chưa có user. Sau khi tài khoản super admin được tạo, bỏ hai biến bootstrap khỏi service environment và giữ secret trong secret store của máy chủ.

Build và chạy:

```powershell
cd ERP_Backend
.\mvnw.cmd clean package -DskipTests
java -jar target/erp_backend-0.0.1-SNAPSHOT.jar
```

Backend lắng nghe port `8080` theo mặc định. Mở firewall nội bộ cho port của reverse proxy; không mở PostgreSQL ra các máy trạm nếu không cần. Health check dùng `/actuator/health/readiness` và `/actuator/health/liveness`.

## Phục vụ frontend cho năm máy

Build frontend trên máy chủ phát hành:

```powershell
cd ERP_Client
npm ci
npm run lint
npm run build
```

Phục vụ thư mục `ERP_Client/dist` bằng reverse proxy nội bộ. Reverse proxy phải chuyển `/api/` tới `http://127.0.0.1:8080/api/` và fallback các route React về `index.html`. Vì client gọi API bằng đường dẫn tương đối `/api`, năm máy chỉ cần dùng cùng một base URL của reverse proxy.

Trong giai đoạn phát triển có thể chạy `npm run dev -- --host 0.0.0.0`, nhưng không dùng Vite dev server làm môi trường demo chính thức.

## Kiểm tra năm máy

1. Ghi lại phiên bản commit, địa chỉ base URL nội bộ đã che thông tin nhạy cảm và thời điểm triển khai.
2. Đăng nhập từ máy A, tạo một bản ghi master được phép tạo.
3. Refresh trên máy B và xác nhận cùng `id`, mã nghiệp vụ và thời điểm.
4. Cập nhật từ máy C, refresh trên máy D/E và xác nhận giá trị mới.
5. Đăng nhập user chỉ xem trên một máy, gọi thao tác ghi bằng giao diện và DevTools; API phải trả `403 ACCESS_DENIED`.
6. Kiểm tra health endpoint và log máy chủ; không sao chép file database hoặc dữ liệu nghiệp vụ giữa các máy.

Nếu đổi máy PostgreSQL, chỉ thay `ERP_DB_URL`, username/password ở máy chủ backend rồi restart service. Không sửa code React để đổi host database.
