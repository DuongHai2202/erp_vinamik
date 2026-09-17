# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Người dùng chính là quản lý và nhân viên nghiệp vụ của Vinamik: nhân sự, kho và nguyên vật liệu, sản xuất, chất lượng và giá thành, dữ liệu và báo cáo. Họ làm việc trên nhiều máy trạm cùng truy cập một hệ thống trung tâm và chỉ được thao tác trong phạm vi quyền được cấp.

Phạm vi người dùng và tình huống sử dụng ở trên được suy ra từ yêu cầu dự án môn học đã chốt.

## Product Purpose

Vinamik là website quản lý nguồn lực doanh nghiệp dùng chung dữ liệu cho bài tập ERP. Hệ thống giúp các bộ phận quản lý dữ liệu nghiệp vụ trên một backend và một PostgreSQL trung tâm, giữ kết quả nhất quán giữa các máy, và kiểm soát thao tác bằng đăng nhập, vai trò và quyền.

Thành công được đo bằng việc người dùng tìm và xử lý đúng nghiệp vụ, dữ liệu liên module không bị nhân bản, phản hồi vẫn nhanh khi có nhiều bản ghi, và tài khoản thiếu quyền chỉ có thể xem.

## Positioning

Điểm khác biệt của Vinamik là một hệ thống vận hành hợp nhất nhưng có ranh giới module rõ ràng: mỗi nghiệp vụ có một chủ sở hữu dữ liệu, còn người dùng vẫn thấy một trải nghiệm thống nhất và nhất quán trên mọi máy trạm.

## Operating Context

Backend Spring Boot/Java phục vụ API, React.js phục vụ giao diện, và PostgreSQL trung tâm lưu dữ liệu. Năm máy trạm truy cập cùng bản triển khai; trình duyệt không kết nối trực tiếp tới database.

Đăng nhập, phân quyền, phiên/token và audit là nền tảng dùng chung. Ba module ưu tiên là quản lý nhân sự, quản lý kho và nguyên vật liệu, và quản lý sản xuất. Quản lý chất lượng và giá thành cùng quản lý dữ liệu và báo cáo được giữ ở backlog cho tới khi được yêu cầu triển khai.

## Capabilities and Constraints

- Mỗi module nghiệp vụ có năm nhóm chức năng theo backlog và tài liệu môn học.
- Backend kiểm tra quyền cho mọi thao tác; thiếu xác thực trả 401, thiếu quyền trả 403.
- Module sản xuất yêu cầu xuất/nhập thông qua hợp đồng công khai của kho; chỉ kho ghi sổ giao dịch và tính số dư.
- PostgreSQL được tạo/nâng cấp bằng migration; Spring Data JPA/JPA là lớp truy cập dữ liệu duy nhất.
- Giao diện phải responsive, dùng được với dữ liệu lớn, hỗ trợ light/dark và tùy chỉnh font, cỡ chữ, mật độ và màu nhấn.

## Brand Commitments

Tên hiển thị thống nhất là **Vinamik**, không thêm tiền tố “ERP” vào thương hiệu giao diện. Toàn bộ menu, nhãn, hướng dẫn và trạng thái giao diện dùng tiếng Việt. Thông báo thành công/lỗi gửi tới người dùng dùng tiếng Anh; log vận hành nội bộ dùng tiếng Việt và không ghi secret.

## Evidence on Hand

- Tài liệu nghiệp vụ và thiết kế dữ liệu trong docs/.
- Dữ liệu kiểm thử khối lượng lớn trong data/.
- Mã giao diện hiện tại trong ERP_Client/.
- Các tài liệu bàn giao cho Antigravity trong docs/antigravity_frontend_handoff.md và docs/antigravity_ui_prompt.md.

Không có yêu cầu dùng ảnh marketing hoặc số liệu thương mại; không được tự tạo claim, testimonial hay số liệu kinh doanh.

## Product Principles

1. Một nguồn dữ liệu trung tâm, kết quả giống nhau trên mọi máy.
2. Quyền thao tác rõ ràng và được kiểm tra ở backend.
3. Mỗi module có ranh giới trách nhiệm nhưng trải nghiệm liền mạch.
4. Người vận hành nhìn thấy trạng thái và bước tiếp theo trong vài giây.
5. Đọc nhanh, phản hồi nhanh, và không hy sinh khả năng truy cập để lấy hiệu ứng.

## Accessibility & Inclusion

Ưu tiên chữ tiếng Việt dễ đọc, cỡ chữ cơ sở đủ lớn, tương phản đạt chuẩn, focus bằng bàn phím rõ ràng, vùng chạm đủ rộng, layout co giãn theo màn hình và tôn trọng prefers-reduced-motion.
