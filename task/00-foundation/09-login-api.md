# FND-09 — Xây API đăng nhập và phiên
- Nhóm: Foundation
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `08-auth-schema.md`, `07-api-error-contract.md`
## Mục tiêu
Xác thực user tập trung và phát hành/duy trì phiên an toàn cho web client.
## Phạm vi
Đăng nhập, lấy user hiện tại, đăng xuất/thu hồi phiên; hash mật khẩu bằng thư viện chuẩn; giới hạn lỗi đăng nhập cơ bản.
## Tiêu chí nghiệm thu
- Sai thông tin không tiết lộ username có tồn tại hay không.
- Tài khoản disabled/locked bị từ chối.
- Secret phiên không được ghi vào log.
## Kiểm tra
Kiểm thử thành công, sai password, tài khoản khóa, đăng xuất và truy cập endpoint cần đăng nhập.
## Ngoài phạm vi
Không thêm đăng nhập mạng xã hội, SSO hoặc tự đăng ký công khai.

