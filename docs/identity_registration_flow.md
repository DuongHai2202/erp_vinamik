# Luồng đăng ký, đăng nhập và phân quyền Vinamik

## Quyết định nghiệp vụ

Vinamik là ERP nội bộ. Không bật tự đăng ký rồi tự kích hoạt tài khoản và cũng không cho người đăng ký tự chọn vai trò. Người dùng gửi **yêu cầu cấp tài khoản**; yêu cầu ở trạng thái `pending` cho đến khi quản trị viên định danh được nhân viên trong HR, chọn vai trò tối thiểu và duyệt. Từ chối hoặc hết hạn thì không thể đăng nhập.

## Luồng chuẩn

1. Người dùng mở `Đăng ký cấp tài khoản`, nhập họ tên, email công ty, mã nhân viên nếu có, tên đăng nhập và mật khẩu.
2. API chuẩn hóa username/email, băm mật khẩu, lưu yêu cầu có thời hạn 48 giờ và trả cùng một thông báo cho cả trường hợp trùng dữ liệu để tránh dò tài khoản.
3. Quản trị viên có quyền `identity_registration_read` xem hàng đợi yêu cầu.
4. Quản trị viên có quyền `identity_registration_approve` chọn đúng `employee_id` và vai trò nghiệp vụ tối thiểu. Hệ thống tạo `user_account` active, gán role, xóa hash tạm trong yêu cầu và ghi audit.
5. Quản trị viên có thể từ chối kèm lý do. Yêu cầu hết hạn phải được đánh dấu `expired` trước khi tạo tài khoản.
6. Người dùng đăng nhập qua session cookie HttpOnly; backend kiểm tra xác thực và permission cho từng API. Thiếu quyền ghi thì UI hiển thị read-only và API trả 403.

## Quyền và tài khoản đặc biệt

RBAC deny-by-default. `system_admin` là role duy nhất có thể duyệt yêu cầu, gán role quản trị và quản lý identity. Tài khoản super admin được seed từ biến môi trường, không cho disable, lock, xóa hoặc hạ role. Không bao giờ cho đăng ký công khai tạo role `system_admin`.

## Bảo mật cần giữ

- Mật khẩu phải được lưu bằng adaptive hash; không lưu plaintext. Registration dùng tối thiểu 15 và tối đa 128 ký tự.
- CSRF cookie/header dùng cho SPA; sau login/logout làm mới token.
- Login và registration cần rate limit ở reverse proxy hoặc Redis khi triển khai thật; HTTPS bắt buộc.
- Bản production nên thêm xác minh email công ty bằng liên kết một lần có thời hạn, MFA/WebAuthn cho super admin và các lần nâng quyền, session idle timeout và password blocklist.
- Audit log chỉ ghi actor, request id, trạng thái và lý do; không ghi mật khẩu, token hoặc dữ liệu nhạy cảm không cần thiết.

## Giới hạn của bản MVP

Bản đầu dùng duyệt thủ công trong admin console vì dự án chưa có email provider/SSO. Khi triển khai doanh nghiệp, kết nối IdP (OIDC/SAML) hoặc email công ty và đặt rate limit ở gateway trước khi mở rộng.


## Căn cứ bảo mật

- OWASP khuyến nghị mật khẩu phải được lưu bằng thuật toán băm thích ứng có salt; Argon2id là lựa chọn ưu tiên khi nền tảng hỗ trợ: https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
- NIST SP 800-63B-4 quy định cách xử lý mật khẩu, giới hạn thử sai và yêu cầu không ép thay đổi định kỳ nếu không có dấu hiệu bị lộ: https://pages.nist.gov/800-63-4/sp800-63b.html
- OWASP Authorization yêu cầu deny-by-default và kiểm tra quyền ở mọi request: https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html
- Spring Security mô tả cơ chế CSRF cookie/header cho ứng dụng SPA: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
- OWASP khuyến nghị xác minh email bằng liên kết một lần có thời hạn trước khi kích hoạt luồng sử dụng email công ty: https://cheatsheetseries.owasp.org/cheatsheets/Email_Validation_and_Verification_Cheat_Sheet.html
