# FND-20 — Chuẩn UI/UX frontend

- Trạng thái: Chuẩn đề xuất cho dự án
- Stack đã thống nhất: React.js dùng JavaScript
- Mục tiêu: mọi module trông và hoạt động nhất quán khi năm máy trạm truy cập cùng bản triển khai.

## 1. Đánh giá ba ảnh tham khảo

- `UI_Sample/login.jpg`: thẻ đăng nhập đặt giữa trang, nền xanh đậm và vùng nhập sáng là bố cục dễ hiểu. Giữ tinh thần này; không đưa tài khoản/mật khẩu demo mặc định vào màn hình thật.
- `UI_Sample/home_page.jpg`: thẻ module lớn và biểu tượng giúp người mới chọn phân hệ nhanh. Ảnh dùng một cột hẹp và nhiều khoảng trống; trang ERP nên tận dụng chiều rộng, có thanh điều hướng chung và chỉ hiện module theo quyền.
- `UI_Sample/interface_module.jpg`: sidebar tối, trạng thái mục đang chọn và vùng nội dung tách biệt là mẫu tốt cho ứng dụng quản trị. Cỡ chữ/nội dung trong ảnh hơi nhỏ; cần tăng độ đọc và chuẩn hóa bảng, bộ lọc, biểu mẫu.
- Các ảnh là giao diện quản trị chấm công, nên chỉ tham khảo bố cục/màu; không sao chép chức năng Wi-Fi/GPS hoặc suy ra phạm vi nghiệp vụ từ ảnh.

## 2. Bộ thành phần và ngôn ngữ

- Đề xuất dùng Ant Design React làm bộ component thống nhất cho form, bảng, menu, dialog, phân trang và thông báo; dùng một phiên bản được khóa trong package manager, không trộn nhiều bộ UI trong cùng ứng dụng.
- Đặt theme qua token ở một điểm chung (`ConfigProvider`/theme provider), tạo component dùng lại trong `platform/common`; module đặt component nghiệp vụ trong thư mục riêng.
- Giao diện, nhãn, điều hướng, hướng dẫn và nội dung nghiệp vụ hiển thị bằng tiếng Việt. Toast/banner lỗi hoặc thành công hiển thị bằng tiếng Anh theo quy ước dự án; không để message mặc định của component library lọt ra ngoài mà chưa kiểm tra.
- Đề xuất Be Vietnam Pro dạng WOFF2, tự phục vụ từ bundle ứng dụng và kèm giấy phép font; phải có đủ glyph tiếng Việt/dấu tổ hợp. Chỉ đóng gói weight cần dùng, đặt font-display: swap, fallback Arial, sans-serif; không tải font từ CDN lúc chạy. Xem chuẩn tải font tại 21-frontend-performance-standard.md.
- Cỡ chữ nội dung chính 14–16 px; tiêu đề trang 24–28 px; nhãn trường khoảng 14 px; không thu nhỏ chữ để nhồi nhiều cột. Dùng cấp bậc chữ, độ đậm và khoảng cách nhất quán.

## 3. Layout ứng dụng

- Dùng app shell chung: sidebar khoảng 240 px, thanh đầu trang 56–64 px, vùng nội dung có padding khoảng 24 px. Đây là mốc thiết kế, không phải kích thước cố định làm vỡ layout.
- Sidebar giữ điều hướng phân hệ/chức năng; header hiển thị breadcrumb, tên người dùng và thao tác tài khoản. Mục không có quyền không được hiện hoặc phải bị khóa theo quy tắc điều hướng đã chọn.
- Bảng nghiệp vụ dùng toàn bộ vùng nội dung khả dụng; đặt tìm kiếm/lọc/phân trang gần bảng, giữ trạng thái loading/empty/error rõ ràng. Biểu mẫu dài chia nhóm; dùng hai cột khi đủ rộng và một cột khi cửa sổ hẹp.
- Thiết kế và kiểm tra tối thiểu ở 1366, 1280 và 1024 px; từ màn hình hẹp, sidebar thu gọn thành drawer và bảng có cách xử lý overflow rõ ràng.
- Cùng tài khoản phải nhận cùng dữ liệu từ backend. Không lưu nghiệp vụ chỉ ở bộ nhớ trình duyệt; theme cá nhân nếu bổ sung sau này nên gắn với tài khoản, không làm bản dữ liệu hoặc quyền khác nhau theo máy.

## 4. Màu sắc và chế độ theme

- Dùng một palette thương hiệu xanh navy/xanh dương xuyên suốt module; màu module chỉ dùng làm điểm nhấn nhỏ ở icon/badge nếu cần, không đổi toàn bộ màu giao diện theo từng phân hệ.
- Token màu khởi đầu đề xuất:

| Vai trò | Màu |
|---|---|
| Primary/action | `#1D4ED8` |
| Header/sidebar navy | `#0B1F4D` |
| Nền ứng dụng | `#F5F7FB` |
| Surface/card | `#FFFFFF` |
| Chữ chính | `#0F172A` |
| Chữ phụ | `#475569` |
| Viền | `#E2E8F0` |
| Focus ring | `#2563EB` |
| Success | chữ `#166534`, nền `#DCFCE7` |
| Warning | chữ `#92400E`, nền `#FEF3C7` |
| Error | chữ `#991B1B`, nền `#FEE2E2` |
| Information | chữ `#075985`, nền `#E0F2FE` |

- Chốt light theme làm chế độ MVP để giảm sai khác và tránh tăng khối lượng kiểm thử. Định nghĩa màu bằng token ngay từ đầu để có thể thêm dark theme sau mà không sửa màu rải rác.
- Nếu sau này thêm chọn sáng/tối hoặc theme thương hiệu, áp dụng cho toàn app và lưu lựa chọn theo tài khoản. Không cho từng máy tự sửa palette tùy ý.
- Màu trạng thái chỉ dùng theo nghĩa nhất quán; luôn kèm chữ/icon để không chỉ dùng màu làm tín hiệu. Kiểm tra tương phản theo WCAG AA trước khi chốt token; cặp chữ/nền phải đạt tối thiểu 4.5:1 cho chữ thường.

## 5. Tương tác và motion

- Animation chỉ hỗ trợ hiểu trạng thái: hover/focus, drawer/modal, chuyển tab hoặc loading; thời lượng gợi ý 150–200 ms, easing nhẹ.
- Không dùng parallax, bounce, tự chạy animation kéo dài, hoặc chuyển động làm chậm thao tác nhập liệu/bảng.
- Tôn trọng `prefers-reduced-motion`; trạng thái focus bàn phím phải nhìn thấy rõ. Mọi thao tác quan trọng dùng được bằng bàn phím và có nhãn dễ hiểu.
- Nút lưu/xóa/xác nhận phản hồi rõ ràng; thao tác phá hủy hoặc ghi sổ cần xác nhận phù hợp và chống gửi lặp khi đang xử lý.

## 6. Tiêu chí duyệt giao diện

- Thống nhất shell, font, token màu, khoảng cách, kích thước controls và thông báo giữa các module.
- Có trạng thái loading/empty/error/success, lỗi validation, quyền chỉ đọc và quyền thiếu được thể hiện nhất quán.
- Kiểm tra độ tương phản, bàn phím, focus, kích thước màn hình mục tiêu và nội dung tiếng Việt có dấu.
- Không coi ẩn nút là bảo mật; API/backend vẫn là nơi quyết định quyền.
