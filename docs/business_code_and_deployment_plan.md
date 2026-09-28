# Quy tắc mã tự động và triển khai

## Nguyên tắc

- Mã nghiệp vụ được cấp ở backend trong cùng transaction với bản ghi. Frontend chỉ hiển thị trạng thái “tự sinh khi lưu”, không tự quyết định số cuối cùng.
- Bộ đếm nằm tại `identity.business_code_sequence`, khóa theo `(code_type, period_key)` bằng `INSERT ... ON CONFLICT DO UPDATE`. Nhiều người dùng hoặc nhiều máy gửi đồng thời vẫn không nhận trùng số.
- Mã đã tồn tại trong dữ liệu cũ được giữ nguyên. Mã tự sinh dùng sáu chữ số để không va vào các fixture cũ có bốn hoặc năm chữ số.
- Sửa bản ghi không cấp mã mới; xóa bản nháp không tái sử dụng mã. Mã có thể có khoảng trống khi transaction bị hủy, đây là chủ ý để bảo toàn tính đồng thời.
- Mã kỹ thuật chống gửi trùng (`idempotency_key`) và mã lô được tạo tự động khi chúng phụ thuộc vào bản ghi cha; người dùng chỉ nhập khi tích hợp với chứng từ ngoài hệ thống.

Phạm vi đã tích hợp trong thay đổi này là luồng Quản lý sản xuất: kế hoạch, BOM, lệnh sản xuất, mã lô và khóa chống gửi trùng. Các mã danh mục Nhân sự và Kho được chốt quy tắc để triển khai ở migration/backfill riêng; chưa tự đổi mã cũ vì chúng đang là dữ liệu tham chiếu của nhiều module.

## Bảng quy tắc

| Trường | Quy tắc tự sinh | Nguồn kỳ | Dữ liệu tự lấy sau khi chọn | Trạng thái |
|---|---|---|---|
| `employee_code` | `vmk_YYYY_######` | ngày tạo | phòng ban, chức danh, quản lý theo nhân viên | Kế hoạch dùng chung |
| `plan_code` | `plan_YYYY_######` | ngày lập kế hoạch | dòng kế hoạch lấy thành phẩm, đơn vị, số lượng còn lại | Đã tích hợp |
| `bom_code` | `bom_<mã_thành_phẩm>_v<phiên_bản>_<######>` | phiên bản BOM | tên/đơn vị thành phẩm; dòng lấy tên, đơn vị nguyên vật liệu | Đã tích hợp |
| `order_code` | `production_order_YYYY_######` | ngày bắt đầu | sản phẩm, số lượng còn lại, BOM phù hợp, ngày theo kế hoạch | Đã tích hợp |
| `item_code` | `rm_YYYY_######` hoặc `fg_YYYY_######` | ngày tạo | nhóm vật tư, đơn vị và theo dõi lô | Kế hoạch dùng chung |
| `receipt_code` | `receipt_YYYY_######` | ngày lập | kho, vị trí, nhà cung cấp và đơn vị theo mặt hàng | Kế hoạch dùng chung |
| `issue_code` | `issue_YYYY_######` | ngày lập | kho, tồn khả dụng, vị trí và đơn vị | Kế hoạch dùng chung |
| `transfer_code` | `transfer_YYYY_######` | ngày lập | kho nguồn/đích, vị trí và tồn khả dụng | Kế hoạch dùng chung |
| `stocktake_code` | `stocktake_YYYYMM_######` | tháng kiểm kê | kho và các dòng tồn tại thời điểm mở phiên | Kế hoạch dùng chung |
| `lot_code` | `lot_<mã_lệnh>_YYYYMMDD_######` | ngày sản xuất | thành phẩm, đơn vị, hạn dùng mặc định theo mặt hàng | Đã tích hợp |
| `idempotency_key` | UUID hoặc `<mã_cha>-<loại>-<thời_gian>` | thời điểm gửi | chỉ dùng nội bộ để chống ghi trùng | Đã tích hợp cho sản xuất |

Các mã danh mục (đơn vị tính, nhóm vật tư, nhà cung cấp, kho, vị trí) nên sinh theo cùng bộ cấp mã sau khi chốt tiền tố nghiệp vụ; trước mắt vẫn chấp nhận mã cũ để không làm hỏng khóa tham chiếu hiện có.

## Luồng dữ liệu tự động

1. Tạo mặt hàng trước (loại `raw_material` hoặc `finished_product`), chọn đơn vị và nhóm vật tư. Mã mặt hàng vẫn tương thích với mã cũ; bộ sinh mã cho danh mục dùng chung nằm trong bước tiếp theo.
2. Tạo BOM cho thành phẩm; mã thành phẩm và đơn vị được đọc từ mặt hàng, các dòng BOM đọc tên/đơn vị nguyên vật liệu từ kho.
3. Tạo kế hoạch; chọn thành phẩm sẽ tự lấy mã, tên, đơn vị và số lượng còn lại.
4. Tạo lệnh từ dòng kế hoạch; hệ thống tự giới hạn sản lượng còn lại, ngày trong khoảng kế hoạch và BOM đang hiệu lực.
5. Ghi nhận sản lượng; lệnh tự cung cấp thành phẩm, đơn vị và sinh mã lô/idempotency; khi ghi sổ mới gửi yêu cầu nhập kho.
6. Ghi nhận tiêu hao; lệnh/BOM tự cung cấp nhu cầu vật tư, tồn khả dụng được đọc từ số dư Kho; chỉ thao tác ghi sổ của Kho mới trừ tồn.

## GitHub Actions

- `ci.yml` chạy lint/build frontend, unit test module sản xuất và package backend trên mỗi pull request/push.
- `deploy.yml` chỉ chạy thủ công hoặc khi tạo tag `v*`. Job build tạo artifact, job deploy dùng SSH secret để chép JAR và `dist` lên máy chủ; server chạy Flyway khi khởi động, không chạy SQL thủ công.
- GitHub Environment `production` chỉ cần `DEPLOY_HOST`, `DEPLOY_PORT`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`, `DEPLOY_APP_DIR` và `DEPLOY_FRONTEND_DIR`. Các biến `ERP_DB_URL`, `ERP_DB_USERNAME`, `ERP_DB_PASSWORD`, `ERP_BOOTSTRAP_ADMIN_USERNAME`, `ERP_BOOTSTRAP_ADMIN_PASSWORD` đặt trong `EnvironmentFile` của service trên máy chủ, không truyền qua log deploy.
- Máy chủ cần Java 21, Node không bắt buộc nếu phục vụ `dist` bằng Nginx, PostgreSQL trung tâm và reverse proxy chuyển `/api` tới Spring Boot. Không đưa secret vào repository hoặc artifact.

### Chuẩn bị máy chủ một lần

Tạo sẵn hai thư mục mà GitHub Actions sẽ chép file vào, cài Java 21 và tạo service chạy JAR:

```ini
# /etc/systemd/system/erp-backend.service
[Unit]
Description=ERP Vinamik backend
After=network-online.target

[Service]
User=erp
WorkingDirectory=/opt/erp_vinamik
EnvironmentFile=/etc/erp-vinamik/backend.env
ExecStart=/usr/bin/java -jar /opt/erp_vinamik/erp_backend.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Trong `/etc/erp-vinamik/backend.env` đặt `ERP_DB_URL`, `ERP_DB_USERNAME`, `ERP_DB_PASSWORD` và các biến bootstrap cần thiết; phân quyền file chỉ cho user root đọc. Sau đó chạy `systemctl daemon-reload`, `systemctl enable erp-backend` và `systemctl start erp-backend`. Nginx phục vụ `/var/www/erp_vinamik` và proxy `/api` về `127.0.0.1:8080`; `DEPLOY_APP_DIR` trỏ `/opt/erp_vinamik`, `DEPLOY_FRONTEND_DIR` trỏ `/var/www/erp_vinamik`.

Tạo GitHub Environment tên `production`, thêm sáu secret triển khai ở trên, rồi chọn **Actions → Deploy ERP Vinamik → Run workflow** hoặc đẩy tag, ví dụ `git tag v2026.09.28 && git push origin v2026.09.28`. Workflow sẽ chạy kiểm tra, chép artifact, restart service và gọi readiness endpoint; nếu endpoint lỗi thì job deploy thất bại.

### Deploy tự động lên Render cho link demo

Repository có thêm `render.yaml`, `ERP_Backend/Dockerfile` và `.github/workflows/deploy_render.yml`. Blueprint tạo một backend Docker, một frontend static và một PostgreSQL; frontend proxy `/api/*` về backend nên trình duyệt vẫn dùng cùng origin và cookie CSRF không bị tách domain. Render sẽ cung cấp URL `onrender.com`; GitHub Action chỉ gọi hai deploy hook sau khi CI thành công.

Thiết lập một lần trên Render:

1. Chọn **New → Blueprint**, kết nối repository và chọn `render.yaml`.
2. Nhập `ERP_BOOTSTRAP_ADMIN_USERNAME` và `ERP_BOOTSTRAP_ADMIN_PASSWORD` khi Render yêu cầu; không ghi hai giá trị này vào YAML.
3. Lấy deploy hook của hai service, lưu vào GitHub Environment `production` với tên `RENDER_BACKEND_DEPLOY_HOOK_URL` và `RENDER_FRONTEND_DEPLOY_HOOK_URL`.
4. Tạo Environment variable `RENDER_BACKEND_PUBLIC_URL` với giá trị `https://erp-vinamik-demo-backend.onrender.com`.
5. Push `main`; workflow `deploy_render.yml` chạy test, build, gọi Render và đợi readiness.

Nếu Render tạo URL có hậu tố khác do tên bị trùng, cập nhật destination `/api/*` trong `render.yaml` theo URL backend thực tế rồi đồng bộ Blueprint. Gói miễn phí phù hợp demo nhưng backend có thể ngủ khi không có truy cập và database miễn phí có thời hạn; dùng gói trả phí nếu cần link hoạt động liên tục.

### Demo nhanh bằng một link tạm thời

Nếu chỉ cần giáo viên mở trong buổi demo và máy phát triển có thể bật liên tục, không cần dựng VPS. Chạy backend ở `localhost:8080`, chạy Vite ở `0.0.0.0:5173` để proxy `/api` về backend, sau đó dùng Cloudflare Quick Tunnel:

```powershell
cd D:\WorkSpace\envCode\ERP_Vinamik\ERP_Client
npm run dev -- --host 0.0.0.0

# Mở terminal khác sau khi đã cài cloudflared
cloudflared tunnel --url http://localhost:5173
```

Gửi URL `https://<ten-ngau-nhien>.trycloudflare.com` mà lệnh in ra cho giáo viên. Đây là link tạm cho kiểm thử, máy và hai tiến trình phải vẫn chạy; không mở cổng PostgreSQL ra Internet. Khi cần link ổn định hoặc demo ngoài giờ, dùng workflow deploy lên máy chủ Linux ở phần trên.
