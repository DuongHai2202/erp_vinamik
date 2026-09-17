# FND-21 — Chuẩn hiệu suất frontend và dữ liệu lớn

- Trạng thái: Chuẩn đề xuất cho dự án
- Phạm vi: React app shell, màn hình danh sách và bảng dữ liệu nhiều trường.

## 1. Nguyên tắc UX cho dữ liệu rất rộng

- 200–300 cột là trường hợp bảng rất rộng; không mặc định mở hết cột cho người dùng. Màn hình mặc định chỉ bật các cột thiết yếu; cung cấp bộ chọn cột có tìm kiếm, nhóm theo nghiệp vụ, preset như “Tổng quan”/“Chi tiết”, ghim một vài cột định danh và xem bản ghi đầy đủ trong panel chi tiết.
- Cho phép cuộn ngang khi cần, giữ tiêu đề và cột định danh ổn định. Giữ thứ tự/cấu hình cột theo tài khoản nếu người dùng tùy chỉnh; không ảnh hưởng dữ liệu nghiệp vụ.
- Nếu nghiệp vụ bắt buộc người dùng xem đủ 200–300 cột trong cùng một lưới, làm spike riêng cho grid ảo hóa hai chiều trước khi chốt component. Giữ hỗ trợ bàn phím, focus, đọc tiêu đề và thao tác chọn ô; không hy sinh khả năng sử dụng để đổi lấy benchmark đẹp.
- Có đường xuất báo cáo/file đầy đủ riêng cho trường hợp cần tất cả trường; không ép người dùng đọc hàng trăm cột trong viewport.

## 2. Virtualization và component table

- Với bảng ERP thông thường, dùng Ant Design Table, phân trang phía server; bật virtual row scrolling khi số dòng lớn và cấu hình đúng vùng cuộn. `virtual` của Ant Design là virtual list cho bảng; không được giả định nó tự ảo hóa cả 200–300 cột.
- Nếu thật sự cần cuộn đồng thời trên hàng nghìn dòng và hàng trăm cột, chỉ render các hàng/cột nhìn thấy cùng một overscan nhỏ. Có thể dựng grid chuyên biệt bằng `@tanstack/react-virtual`, vốn hỗ trợ virtualizer theo trục dọc/ngang và kết hợp thành lưới hai chiều; nhóm phải tự xây markup, style, bàn phím và accessibility.
- Dùng Ant Design cho form, filter, dialog và bảng nghiệp vụ thường. Grid đặc biệt vẫn lấy màu, font, khoảng cách và control từ design tokens chung để không trông như ứng dụng khác.
- Không vẽ mỗi ô thành component nặng; tránh tooltip, formatter, input hoặc callback riêng cho mọi ô nếu không cần. Dùng key ổn định theo ID, giữ state ở phạm vi nhỏ và chỉ memoize khi đã đo thấy cần.

## 3. Tải và xử lý dữ liệu

- Không tải toàn bộ bản ghi rồi mới lọc ở trình duyệt. Tìm kiếm, lọc, sort và phân trang lớn phải được thực hiện ở server; giới hạn page size, mặc định đề xuất 50 và chỉ tăng trong kiểm thử phù hợp.
- API chỉ trả projection các trường được yêu cầu/cho phép; không gửi 300 trường cho mọi màn hình khi người dùng đang xem preset chỉ có 12 trường. Field selection phải qua allowlist và quyền truy cập, không nhận tên cột SQL tùy ý từ client.
- Tách metadata danh sách cột khỏi dữ liệu từng trang. Tải các nhóm dữ liệu phụ theo nhu cầu; tránh request N+1. Hủy request cũ khi bộ lọc thay đổi liên tiếp và không để response cũ ghi đè kết quả mới.
- Dùng debounce hợp lý cho tìm kiếm văn bản; cập nhật UI ngay cho thao tác cục bộ rồi tải dữ liệu bất đồng bộ. Cache/dedupe API dùng chung trong client, và invalidation rõ ràng sau khi ghi dữ liệu.
- Lazy-load route/module để trang đăng nhập hoặc một phân hệ không phải tải toàn bộ mã của mọi module; lazy-load ảnh/biểu đồ nặng khi chúng thực sự xuất hiện.

## 4. Font và tải tài nguyên

- Dùng Be Vietnam Pro dạng WOFF2 tự phục vụ, có đầy đủ glyph tiếng Việt và dấu tổ hợp; chỉ đưa vào bundle các weight thực sự dùng (đề xuất 400/500/600/700), khai báo `font-display: swap` và fallback `Arial, sans-serif`.
- Preload tối đa file font chính dùng cho nội dung đầu trang; không tải font từ CDN lúc chạy. Tránh nhúng nhiều bản font trùng nhau hoặc tải mọi weight/subset không sử dụng.
- Kiểm tra đủ chữ hoa/thường tiếng Việt, dấu thanh, dấu tổ hợp, tên dài và wrap trong nút, bảng, form, lỗi validation; không để fallback font làm nhảy layout đáng kể sau khi tải.

## 5. Tiêu chí đo và nghiệm thu

- Đo trên máy trạm yếu nhất trong năm máy mục tiêu, cùng trình duyệt, kích thước màn hình, kết nối và bộ dữ liệu kiểm thử đã ghi lại. Tách thời gian API/DB khỏi thời gian render frontend.
- Bài kiểm tra bảng rộng cần dùng dataset có 200–300 định nghĩa cột và số dòng đủ lớn để bắt lỗi (đề xuất ít nhất 10.000 dòng trong môi trường test), nhưng lần tải đầu chỉ nhận một trang nhỏ và các trường đang hiển thị.
- Kiểm tra mở trang, thay preset, bật/tắt cột, tìm kiếm cột, lọc, phân trang, cuộn dọc/ngang và mở chi tiết. Không được có thao tác đồng bộ duyệt cả dataset hoặc tạo DOM node cho mọi giao điểm dòng-cột.
- Mục tiêu sơ bộ: trang có thể thao tác trong 2 giây sau khi điều hướng trên môi trường QA đã khóa; thao tác cục bộ như đổi preset/bật cột phản hồi trong khoảng 100 ms; cuộn không đóng băng hoặc nhảy vị trí. Ghi số đo p95 và điều kiện test thay vì khẳng định nhanh chung chung.
- Dùng React Profiler và Performance panel để tìm đúng đoạn render/query gây chậm; so sánh trước-sau trên cùng máy/dataset. Không thêm `memo`/cache phức tạp nếu chưa có số đo chứng minh lợi ích.
- Nếu không đạt tiêu chí, giảm số cột hiển thị mặc định, kiểm tra query/index/payload, rồi mới tối ưu thêm component. Không khắc phục bằng cách ẩn loading/error hoặc làm mất dữ liệu người dùng cần xem.
