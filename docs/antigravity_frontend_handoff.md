# Bàn giao frontend cho Antigravity

## Mục tiêu và phạm vi

Backend v1 cho Foundation, Nhân sự, Kho và nguyên vật liệu, Sản xuất đã sẵn sàng để xây giao diện. Antigravity chỉ sửa trong `ERP_Client/`; không đổi migration, controller, service, repository, permission code hoặc công thức nghiệp vụ backend.

Hai module `quality_cost` và `reporting` đang hoãn. Không tạo menu, route, mock data hoặc màn hình giả cho hai module này.

Nguồn chuẩn phải đọc trước khi code:

1. `AGENTS.md` — quy tắc kiến trúc, ngôn ngữ và đặt tên.
2. `task/00-foundation/20-frontend-ui-ux-standard.md` — UI/UX.
3. `task/00-foundation/21-frontend-performance-standard.md` — bảng dữ liệu lớn và hiệu suất.
4. `docs/backend_api_contract.md` — route, request, permission và lifecycle API.
5. `docs/payroll_policy.md` — cách hiển thị và giải thích bảng lương.
6. PRODUCT.md và DESIGN.md — product truth và visual direction đã chốt cho Vinamik.
7. .agent/skills/impeccable/SKILL.md — quy tắc audit, typography, motion và responsive của Impeccable.

## Phần client phải giữ nguyên

- React JavaScript, Vite, Ant Design và React Router hiện có; không chuyển TypeScript hoặc framework khác.
- Dùng `ERP_Client/src/platform/common/api_client.js` cho mọi request. Không gọi `fetch` rải rác trong page/component.
- Giữ `credentials: 'include'`, cookie phiên `HttpOnly`, cookie CSRF `XSRF-TOKEN` và header `X-XSRF-TOKEN` do API client xử lý.
- Dùng đường dẫn tương đối `/api/...`. Vite đã proxy `/api` sang backend port 8080; production phải dùng reverse proxy cùng origin.
- Không lưu token, password hoặc dữ liệu nghiệp vụ trong `localStorage`/`sessionStorage`.
- Giữ lazy loading theo route. Tách mỗi feature thành thư mục riêng trong module; không import implementation của feature khác.

## Hợp đồng chung của API

Success:

```json
{
  "message": "English success message.",
  "data": {}
}
```

Error:

```json
{
  "code": "STABLE_ERROR_CODE",
  "message": "English error message.",
  "correlation_id": "request-id",
  "field_errors": {
    "field_name": "English validation message."
  }
}
```

Trang danh sách phải đọc `data.items`, `data.page`, `data.page_size`, `data.total_items`, `data.total_pages`. Page index bắt đầu từ `0`; UI Ant Design thường bắt đầu từ `1`, vì vậy adapter client phải đổi chỉ số ở một chỗ duy nhất.

Quy tắc xử lý lỗi:

- `400`: hiển thị `field_errors` cạnh field và `message` trong toast/banner.
- `401`: xóa state user trong memory và chuyển về `/login`.
- `403`: giữ nguyên trang, hiển thị `Access denied.`; không thử lại tự động.
- `404`: hiển thị empty/not-found state.
- `409`: hiển thị message; với `CONCURRENT_CHANGE`, tải lại record rồi cho người dùng thử lại.
- `500`: hiển thị message và `correlation_id` để tra log.

Mọi nhãn, menu, title, helper text và trạng thái trên giao diện dùng tiếng Việt. Message success/error từ backend giữ nguyên tiếng Anh theo quy ước dự án.

## Đăng nhập và phân quyền

Luồng khởi động:

1. `GET /api/v1/auth/csrf`.
2. `GET /api/v1/auth/me` để phục hồi phiên; nếu 401 thì hiển thị login.
3. `POST /api/v1/auth/login` với `username`, `password`.
4. Giữ `data.permission_codes` và `data.super_admin` trong React context memory.
5. `POST /api/v1/auth/logout` khi đăng xuất.

`permission_codes` chỉ quyết định việc hiện/disable nút và route trên UI. Backend vẫn là nguồn kiểm tra quyền cuối cùng. Người có quyền `*_read` nhưng không có quyền ghi vẫn xem được trang/dữ liệu; các nút tạo, sửa, duyệt, post, khóa phải ẩn hoặc disable kèm tooltip tiếng Việt.

## Route và màn hình cần làm

### Platform

- `/login`: đăng nhập.
- `/settings/users`: user, trạng thái, reset password, gán role theo API identity.
- `/settings/audit`: tra cứu audit phân trang nếu có `identity_audit_read`.

### Nhân sự — năm chức năng

- `/human_resources/employees`: hồ sơ nhân viên; master phụ trợ phòng ban/chức danh.
- `/human_resources/contracts`: hợp đồng, trạng thái và cảnh báo còn tối đa 15 ngày.
- `/human_resources/absences`: yêu cầu nghỉ, lọc kỳ, approve/reject/cancel.
- `/human_resources/rewards_discipline`: thưởng/kỷ luật, submit và quyết định.
- `/human_resources/payroll`: danh sách kỳ, tạo tháng, calculate, approve/reject/lock, danh sách record và drawer dòng chi tiết.

Payroll phải hiển thị `calculation_version`, `standard_working_days`, `currency_code`, tổng kỳ và trạng thái. Chỉ hiện:

- Calculate khi `draft`, `rejected` hoặc `calculated` và có `hr_payroll_update`.
- Approve/Reject khi `calculated` và có `hr_payroll_approve`.
- Lock khi `approved` và có `hr_payroll_approve`.
- Không có thao tác mutate khi `locked`.

### Kho và nguyên vật liệu — năm chức năng

- `/inventory/materials`: danh mục `raw_material` và lookup `finished_product`; đơn vị/nhóm hàng là master phụ trợ.
- `/inventory/receipts`: tạo phiếu nhập nhiều dòng và post.
- `/inventory/issues`: tạo phiếu xuất nhiều dòng và post.
- `/inventory/stocktakes`: tạo phiên, nhập số đếm, submit, approve và post adjustment.
- `/inventory/transfers`: điều chuyển nhiều dòng và post.

Số dư kho là read model dùng trong dashboard/detail; không tạo form sửa số dư. Phiếu đã `posted` là lịch sử bất biến. Không cho nhập số lượng âm hoặc quá 6 chữ số thập phân ở client, nhưng vẫn hiển thị lỗi backend nếu server từ chối.

### Sản xuất — năm chức năng

- `/production/plans`: kế hoạch và nhiều dòng sản phẩm.
- `/production/materials`: BOM/version, kiểm tra nhu cầu và ghi tiêu hao qua Kho.
- `/production/orders`: lệnh, lifecycle, progress/event và material needs.
- `/production/assignments`: phân công nhân sự, ca và lịch; hiển thị lỗi trùng lịch từ backend.
- `/production/finished_products`: sản lượng đạt/lỗi, lô/hạn dùng và bàn giao lượng đạt sang Kho.

Sản xuất chọn vật tư/thành phẩm từ API Inventory và chọn nhân viên/ca từ API HR. Không sao chép danh mục vào state bền vững của Production.

## Cấu trúc frontend đề nghị

```text
ERP_Client/src/
  platform/
    common/
    identity/
    layout/
  modules/
    human_resources/
      employees/
      contracts/
      absences/
      rewards_discipline/
      payroll/
      master_data/
    inventory/
      materials/
      receipts/
      issues/
      stocktakes/
      transfers/
      master_data/
    production/
      plans/
      materials/
      orders/
      assignments/
      finished_products/
```

Mỗi feature nên có `*_page.jsx`, `*_api.js`, `*_form.jsx`/`*_drawer.jsx` khi cần và file mapper/constant cục bộ. Component dùng chung thật sự mới đặt trong `platform/common`; không gom business component của ba module vào common.

## Hiệu suất và UX bắt buộc

- Mọi danh sách gọi server pagination, filter và sort được backend hỗ trợ; mặc định `page_size=50`, không tải toàn bộ record.
- Search debounce khoảng 300 ms và hủy/loại bỏ response cũ khi query mới bắt đầu.
- Bảng nhiều cột dùng cột ghim cho mã/tên/action, horizontal scroll, column visibility và virtualized rows khi lượng hiển thị lớn. Không render đồng thời 200–300 cột DOM nếu người dùng chưa bật chúng.
- Drawer/detail tải khi mở; lookup dùng search từ server và cache ngắn theo query, không preload toàn bộ master data.
- Sau mutation, invalidate đúng list/detail liên quan; không reload toàn trang.
- Action đang gửi phải disable và có loading để tránh double submit. Backend có idempotency ở các luồng post quan trọng nhưng UI vẫn phải ngăn click lặp.
- Animation chỉ dùng opacity/transform ngắn 120–220 ms; tôn trọng `prefers-reduced-motion`.
- Font ưu tiên Be Vietnam Pro, Lexend, rồi Segoe UI; bảo đảm đầy đủ dấu tiếng Việt và số tabular cho tiền/số lượng.
- Tiền tệ hiển thị bằng `Intl.NumberFormat('vi-VN', { style: 'currency', currency })`; không biến đổi giá trị decimal thành dữ liệu dùng để gửi ngược server.

## Fixture tải lớn trong thư mục data

Fixture kiểm thử nằm trong `data/`. Không đưa toàn bộ CSV vào React state và không coi CSV là nguồn dữ liệu nghiệp vụ khi chạy ứng dụng. Backend/PostgreSQL vẫn là nguồn sự thật; CSV dùng để nạp vào môi trường test hoặc để kiểm tra dữ liệu mẫu trước khi gọi API.

- `data/human_resources/employees.csv` có 600 nhân viên tổng hợp với tên Việt Nam tự nhiên, ngày sinh, số điện thoại 10 chữ số và phòng ban, chức danh, trạng thái; `phone_number` là giá trị chuẩn hóa để gửi API, `phone_number_display` dùng nhóm 4-3-3, còn các cột ngày hậu tố `_display` dùng `dd/mm/yyyy`. Các file hợp đồng, nghỉ, thưởng/kỷ luật dùng cùng `employee_code`.
- `data/inventory/stock_items.csv` có 520 mặt hàng, gồm nguyên vật liệu và thành phẩm tham chiếu các dòng sản phẩm công khai của Vinamilk; phiếu, dòng phiếu, lô, ledger và số dư dùng business key liên kết.
- `data/production/orders.csv` có 600 lệnh; BOM, nhu cầu vật tư, sự kiện tiến độ, phân công, tiêu hao và sản lượng đều có file liên quan.
- `data/manifest.json` ghi số lượng từng file. Chạy `py -3 data/validate_large_dataset.py` trước khi dùng fixture; validator phải trả `"valid": true`.
- Dùng `py -3 data/generate_large_dataset.py` để sinh lại cùng bộ dữ liệu từ seed `20260916`. Dữ liệu cá nhân là dữ liệu tổng hợp, email dùng miền `.test`; không coi đây là dữ liệu nhân sự thật.

Khi làm UI, hãy dùng fixture để kiểm tra server pagination, debounce tìm kiếm, virtualized rows, horizontal scroll và drawer chi tiết. Không preload 600 nhân viên hoặc 520 mặt hàng vào một màn hình; mỗi page chỉ gọi trang dữ liệu cần hiển thị.

## Thứ tự giao Antigravity

1. Hoàn thiện foundation component: API error mapping, pagination adapter, permission guard, table shell, status badge, confirm modal, form field errors.
2. Nhân sự: employees → contracts → absences → rewards/discipline → payroll.
3. Kho: materials/master data → receipts → issues → stocktakes → transfers.
4. Sản xuất: plans → BOM/material needs → orders/progress → assignments → finished products.
5. Chạy `npm run lint` và `npm run build`; sau đó smoke test bằng backend thật, không dùng mock để nghiệm thu.

Mỗi feature chỉ được coi là xong khi list/detail, loading/empty/error, quyền read-only, mutation, validation và refresh từ API đều hoạt động. Dữ liệu sau refresh phải giống giữa các máy vì tất cả đọc cùng backend/PostgreSQL.

## Prompt giao trực tiếp cho Antigravity

```text
Bạn chỉ làm frontend trong ERP_Client cho Vinamik. Trước khi sửa, đọc AGENTS.md, PRODUCT.md, DESIGN.md, docs/antigravity_frontend_handoff.md, docs/backend_api_contract.md, task/00-foundation/20-frontend-ui-ux-standard.md và task/00-foundation/21-frontend-performance-standard.md. Giữ React JavaScript + Vite + Ant Design, giữ api_client.js và cơ chế cookie/CSRF hiện có. Không sửa ERP_Backend, migration, permission code hoặc tự tạo nghiệp vụ mới. UI hiển thị hoàn toàn bằng tiếng Việt; success/error message từ API giữ tiếng Anh. Dùng server pagination, permission_codes để điều khiển action, lazy route, table hiệu suất cao và dữ liệu thật từ backend. Theo visual direction factory_control_board: dùng module matrix để thể hiện một module với 5 chức năng, chữ cơ sở tối thiểu 15px, Be Vietnam Pro mặc định, không thêm ERP vào branding hiển thị, không quay lại gradient hoặc card lặp. Triển khai theo thứ tự Foundation UI → Nhân sự → Kho → Sản xuất; chưa làm Quality/Cost và Reporting. Sau mỗi feature chạy npm run lint và npm run build, rồi ghi rõ route/API/permission đã dùng và phần chưa nghiệm thu bằng backend thật.
```

## Chạy môi trường phát triển

Backend nhận cấu hình bằng environment variables theo `.env.example`; không chép password vào source.

```powershell
cd ERP_Backend
.\mvnw.cmd spring-boot:run
```

```powershell
cd ERP_Client
npm ci
npm run dev
```

Vite chạy port 5173 và proxy `/api` sang `http://localhost:8080`. Hợp đồng triển khai nhiều máy nằm trong `docs/central_deployment_runbook.md`.

