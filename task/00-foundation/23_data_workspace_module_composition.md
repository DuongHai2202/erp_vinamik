# FND-23 — Data workspace và bố cục riêng cho module ưu tiên

- Trạng thái: `IN_PROGRESS`
- Phụ thuộc: `00-foundation/20-frontend-ui-ux-standard.md`, `00-foundation/21-frontend-performance-standard.md`, `22_frontend_action_icon_standard.md`
- Phạm vi: data workspace dùng chung và trang gốc của Human resources, Inventory, Production.
- Mục tiêu: xử lý dữ liệu rộng mà không biến mọi module thành cùng một màn hình, đồng thời giữ chung typography, permission, loading và action contract.

## 1. Đã thực hiện

### Data workspace

File: `ERP_Client/src/platform/layout/data_workspace.jsx`.

- Bộ chọn `Bố cục` vẫn dùng preset `Tổng quan`, `Chi tiết`, `Kiểm tra`.
- Nút `Cột` mở drawer riêng, tìm được tên cột và chỉ áp dụng sau khi người dùng bấm `Áp dụng`.
- Cột `actions` luôn được giữ lại để người dùng không mất thao tác bản ghi khi đổi preset.
- Cấu hình cột tùy chỉnh lưu theo storage_key của tài khoản/module trong trình duyệt; dữ liệu nghiệp vụ vẫn luôn lấy từ máy chủ trung tâm, còn preference hiển thị không được xem là dữ liệu nghiệp vụ.
- Bảng lấy chiều cao cuộn theo viewport, giới hạn trong khoảng 300–720px; resize chỉ cập nhật một số đo nhỏ và không render lại dữ liệu server.
- Drawer chi tiết và drawer chọn cột có vùng cuộn độc lập, không làm tràn modal hoặc đẩy layout chính.

### Bố cục module

Các trang ưu tiên có wrapper riêng nhưng không sao chép data table:

- `module_page_human_resources`: bố cục mềm, marker tròn, context `Hồ sơ · hợp đồng · sự kiện`.
- `module_page_inventory`: bố cục ledger, góc gọn, context `Chứng từ → số dư → truy vết`, switch danh mục/số dư rõ ràng.
- `module_page_production`: bố cục điều độ, thanh giai đoạn `Lập kế hoạch → Phát hành lệnh → Thực hiện → Hoàn tất`.
- Route human_resources/contracts dùng lại composition HR, DataWorkspace, action rail và drawer/modal sizing chung; cảnh báo 15 ngày chỉ thay đổi nội dung nghiệp vụ.

Ba module dùng cùng token Vinamik và dải thao tác; sự khác nhau chỉ nằm ở thứ bậc thông tin và nhịp đọc nghiệp vụ.

## 2. Quy tắc tiếp tục triển khai

- Không đưa 200–300 cột vào preset mặc định. Preset đầu tiên chỉ có trường định danh, trạng thái và hành động.
- Không lưu dữ liệu nghiệp vụ hoặc quyền trong localStorage. Chỉ lưu preset/cột/thông tin hiển thị theo tài khoản.
- Khi đổi bộ lọc, hủy request cũ nếu API client đã hỗ trợ AbortController; response cũ không được ghi đè trang mới.
- Không thay `Table` bằng grid hai chiều khi chưa có benchmark. Nếu cần, tạo spike riêng theo FND-21.
- Không thêm animation cho row/cell. Chỉ dùng transition màu, opacity và transform nhẹ ở control.
- Module mới phải chọn một composition riêng, nhưng bắt buộc dùng `.data_workspace`, `RecordActionBar`, token spacing và contract quyền chung.

## 3. Tiêu chí nghiệm thu

- Drawer chọn cột tìm được cột theo tên tiếng Việt, cuộn độc lập và không làm thay đổi chiều rộng cột action.
- Đổi preset hoặc cột không gọi lại API dữ liệu.
- Refresh trang vẫn giữ preset/cột của đúng tài khoản; đổi tài khoản không dùng nhầm preference.
- Bảng có thể cuộn trong vùng nội dung ở viewport desktop và mobile, không tạo scroll lồng gây kẹt thao tác.
- HR, Inventory và Production có ba composition nhìn khác nhau ở phần đầu trang nhưng bảng, filter, detail và action vẫn cùng quy tắc.
- Người chỉ đọc vẫn mở được detail; không xuất hiện action ghi trái quyền.
- `npm run lint`, `npm run build`, `git diff --check` đạt.

## 4. Đã hoàn tất trong slice này

1. Action workflow có loading theo action và record, chỉ khóa nút của dòng đang chạy và giữ nguyên chiều rộng cột.
2. request_api đã nhận AbortSignal; các loader HR, Inventory, Production và bảng workflow hủy request cũ trước khi gửi request mới.
3. Inventory có drawer lọc loại danh mục bằng item_type; Production có drawer lọc trạng thái kế hoạch. Cả hai gửi đúng tham số backend và đồng bộ URL.
4. Các action ngừng sử dụng/chuyển trạng thái/xóa ở ba trang ưu tiên cũng hiển thị loading theo dòng.

## 5. Phần còn lại để đóng task

1. Chụp baseline 1366px, 1280px, 1024px và mobile; kiểm tra sidebar thu gọn, header, drawer và bảng.
2. Đo dataset lớn bằng React Profiler trước khi tối ưu thêm.
3. Walkthrough keyboard và kiểm tra screen reader cho action rail, drawer và pagination.

## 6. Kiểm tra hiện tại

- `npm run lint`: đạt.
- `npm run build`: đạt, 3.054 module được transform.
- Bundle đã được tách theo route và vendor icon; production build hiện không còn cảnh báo chunk vượt 500 kB. Chunk lớn nhất hiện tại là `vendor_react` khoảng 222 kB minified và dải action khoảng 190 kB.

## 7. Cập nhật visual refresh

- Phần đầu trang module không còn là một card lớn có viền bao quanh. HR dùng rail dọc, Inventory dùng dấu góc lệch, Production dùng execution rail; ba module vẫn dùng chung lưới title/description/context.
- `DataWorkspace` tắt pagination nội bộ của `Table` và render footer dùng chung. `data_workspace_total` tính từ `pagination.total`, `pagination.current` và `pagination.page_size`, nên kết quả sau search/filter hiển thị đúng dạng `1–10 / 10 hồ sơ`.
- Footer đặt tổng bản ghi bên trái, `Pagination` bên phải; vùng chọn page size và các nút trang có `cursor: pointer`, không thay đổi chiều rộng khi hover.
- Modal/drawer dùng ba vùng ổn định (header, body cuộn, footer), detail grid không còn các ô lồng kiểu card; chuyển động chỉ dùng transform/color và tôn trọng reduced motion.
## 8. Modal task-sheet

- Form modal dài dùng `entity_form_modal`, header có ngữ cảnh, form hai cột trên desktop và một cột trên mobile.
- Nút hành động nằm ở footer cố định, không bị cuộn khỏi màn hình; body modal là vùng cuộn duy nhất.
- Modal xác nhận/chi tiết không dùng hai cột form; chọn kích thước theo lượng nội dung và chuyển workflow rất dài sang route.