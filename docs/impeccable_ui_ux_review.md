# Vinamik UI/UX review

Nguồn tham chiếu: repository Impeccable đã pull tại .impeccable/, PRODUCT.md, DESIGN.md và source React hiện tại.

## Kết quả chạy Impeccable

- context: nhận diện project web vận hành và đọc PRODUCT.md.
- concept-seed --scope direction --mode operate: gợi ý các hệ visual; Vinamik chọn hướng factory control board, nhưng diễn giải bằng dữ liệu và thao tác nghiệp vụ thay vì trang trí.
- Detector layout và typography: không phát hiện anti-pattern cơ học.
- Frontend lint và production build: pass.

Detector sạch chỉ chứng minh một số lỗi máy có thể nhận biết đã được tránh. Nó không chứng minh dashboard đã trả lời đúng câu hỏi vận hành.

## Điểm sức khỏe hiện tại

| Nhóm | Điểm | Nhận xét |
| --- | ---: | --- |
| Accessibility | 3/4 | Có focus ring, aria label và reduced-motion; vẫn cần làm sạch nested interactive controls. |
| Performance | 3/4 | Shell nhẹ; chiến lược bảng 200–300 cột chưa được hiện thực hóa. |
| Theming | 2/4 | Có theme context, nhưng accent/radius chưa lan đầy đủ sang module token. |
| Responsive | 3/4 | Breakpoint tốt; customizer cần giới hạn theo viewport 320px. |
| Implementation integrity | 3/4 | Module matrix có bản sắc; một số fallback route và CTA chưa đúng ngữ nghĩa. |

Review UX nguồn code đạt khoảng 2.5/4. Điểm thấp chủ yếu đến từ việc dashboard đang làm nhiệm vụ chọn màn hình, chưa làm nhiệm vụ điều hành công việc.

## Hướng thiết kế nên chốt

Giữ module_matrix làm signature của Vinamik, nhưng chuyển trang tổng quan thành role-based operations home.

Thanh đầu trang: ngữ cảnh vai trò · đồng bộ dữ liệu · tìm màn hình

CẦN XỬ LÝ
Nhân sự              3 yêu cầu nghỉ chờ duyệt       Mở hàng đợi
Kho                  5 phiếu cần ghi sổ             Mở hàng đợi
Sản xuất             2 lệnh trễ kế hoạch            Mở hàng đợi

BẢNG ĐIỀU PHỐI
Nhân sự       [hồ sơ] [hợp đồng] [nghỉ] [thưởng] [lương]
Kho          [vật tư] [nhập]    [xuất] [kiểm kê] [điều chuyển]
Sản xuất     [kế hoạch] [định mức] [lệnh] [phân công] [thành phẩm]

Hoạt động gần đây                         Trạng thái dữ liệu trung tâm

Các số liệu trong CẦN XỬ LÝ và Hoạt động gần đây chỉ được render khi API trả dữ liệu thật. Khi không có dữ liệu, dùng empty state có hướng dẫn; không dùng số liệu giả để làm dashboard trông “đầy”.

## Ưu tiên P0: sửa ngữ nghĩa và quyền

1. Đổi CTA Mở công việc gần nhất thành Mở hồ sơ nhân viên/Mở phân hệ đầu tiên cho tới khi có activity API thật. Không gọi một route cố định là “gần nhất”.
2. Chỉ hiển thị Kiểm tra quyền khi user có identity_user_read; nếu không, điều hướng về trang hợp lệ hoặc bỏ CTA.
3. Không lồng Button trong Link và không lồng Link trong item Menu. Dùng một interactive element duy nhất.
4. Route/module không tồn tại phải trả trạng thái 404, không fallback sang Nhân sự.
5. overview_hero_signal không được aria-hidden nếu số phân hệ là thông tin hữu ích.

## Ưu tiên P1: biến dashboard thành control room

1. Tạo API tổng hợp work_queue theo permission: việc chờ duyệt, phiếu chưa post, lệnh trễ, kỳ lương đang mở.
2. Mỗi hàng queue có module, loại việc, mã chứng từ, trạng thái, hạn xử lý và action chính.
3. Trạng thái trung tâm dùng health check/read timestamp thật: Đã đồng bộ lúc ..., Đang tải, Mất kết nối, Thử lại. Không hiển thị PostgreSQL trên surface nghiệp vụ.
4. Bỏ quick link tĩnh nếu nó lặp module matrix. Thay bằng activity thật hoặc hướng dẫn theo vai trò.
5. Placeholder developer (API path, permission code, Antigravity) chỉ bật trong development preview; production dùng empty state tiếng Việt.

## Ưu tiên P1: data workspace dùng chung

Tạo một table shell dùng cho HR, Inventory và Production:

- server pagination, filter, sort và debounce search;
- virtualized rows/cells khi dữ liệu lớn;
- ghim cột mã/tên và action;
- ẩn/hiện cột và preset Tổng quan, Chi tiết, Kiểm tra;
- nhóm cột theo nghiệp vụ;
- lưu filter/view theo tài khoản hoặc URL;
- drawer chi tiết khi cần xem 200–300 thuộc tính;
- trạng thái loading, empty, error, retry, forbidden và read-only nhất quán;
- không render đồng thời toàn bộ 200–300 cột nếu người dùng chưa bật chúng.

Header bảng nên dùng sentence case và cỡ 12–13px; nội dung quan trọng 14–15px. Không tăng padding mọi cột cùng lúc vì sẽ làm mất mật độ của ERP.

## Ưu tiên P2: customization thật sự nhất quán

- Tách --brand-accent và --module-accent; cho phép chế độ theo màu hệ thống hoặc màu riêng module.
- Tạo --erp-radius-sm, --erp-radius-md, --erp-radius-lg, thay các giá trị radius rải rác.
- Customizer cần preview trực tiếp của font/màu, trạng thái tương phản và giới hạn Drawer ở min(360px, 100vw).
- Phân biệt workspace defaults và personal preferences. localStorage chỉ giữ preference hiển thị; dữ liệu nghiệp vụ vẫn từ backend.
- Bundle font WOFF2 nếu muốn Be Vietnam Pro giống nhau giữa các máy; nếu chưa bundle, ghi rõ fallback Segoe UI.

## Ưu tiên P2: command center cho người dùng nhanh

Đổi Tìm nhanh thành Tìm màn hình cho tới khi có search bản ghi. Sau đó mở rộng thành ba nhóm:

1. Màn hình.
2. Bản ghi gần đây.
3. Hành động được phép.

Hỗ trợ Arrow Up/Down, Enter, Escape, focus rõ và shortcut. Sidebar, breadcrumb, command palette và quick access phải đọc cùng một navigation registry.

## Bản sắc thị giác

Giữ factory control board ở mức có ích:

- ink rail + paper workspace;
- rule 1px, accent module và typography rõ;
- mã chứng từ/số liệu dùng tabular numerals;
- animation 120–220ms bằng opacity/transform;
- không thêm gradient, icon tile hoặc card chỉ để lấp khoảng trống;
- dùng khoảng trắng để phân vùng, không bọc mọi thứ thành card.

Nếu bỏ slogan và decoration, người dùng vẫn phải biết việc đầu tiên trong 10 giây. Đó là tiêu chuẩn hoàn thiện mới.

## Thứ tự triển khai đề nghị

1. Sửa CTA, route fallback, nested controls, aria và permission link.
2. Tạo data_workspace và trạng thái loading/empty/error/read-only.
3. Bổ sung work_queue và trạng thái đồng bộ thật vào dashboard.
4. Token hóa accent/radius, hoàn thiện customizer và command center.
5. Sau khi có dữ liệu thật, chạy lại audit và kiểm tra với 500+ bản ghi ở cả desktop/mobile.
