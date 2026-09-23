# Yêu cầu cấp tài khoản nội bộ

`registration_requests.csv` là danh sách yêu cầu cấp tài khoản được đối chiếu từ các nhân viên đang làm việc trong `data/human_resources/employees.csv`. Mỗi dòng giữ mã nhân viên, email công việc, phòng ban và vai trò tối thiểu đề xuất để quản trị viên xem xét.

- Dữ liệu dùng miền email `.test`, là dữ liệu tổng hợp production-like để kiểm thử quy trình, không phải thông tin cá nhân thật.
- Không lưu mật khẩu trong CSV, Git hoặc migration. Script nạp yêu cầu nhận mật khẩu tạm thời qua biến môi trường hoặc sinh trong bộ nhớ cho một lần chạy.
- Yêu cầu chỉ ở trạng thái `pending` sau khi nạp. Quản trị viên phải đối chiếu hồ sơ HR, chọn `employee_id`, chọn vai trò tối thiểu rồi phê duyệt trên màn hình **Yêu cầu cấp tài khoản**. Backend không cho cấp `system_admin` qua luồng này.
- Sau khi duyệt, quản trị viên nên dùng chức năng **Đặt lại mật khẩu** để cấp mật khẩu riêng cho từng tài khoản.

Nạp 12 yêu cầu bằng mật khẩu tạm thời chỉ tồn tại trong bộ nhớ của tiến trình:

```powershell
py -3 data/load_identity_requests_via_api.py --generate-password
```

Hoặc tự cung cấp mật khẩu tạm thời (tối thiểu 15 ký tự) qua biến môi trường, không ghi vào tệp:

```powershell
$env:ERP_IDENTITY_SEED_PASSWORD = '<mat-khau-tam-thoi-chi-dung-noi-bo>'
py -3 data/load_identity_requests_via_api.py
Remove-Item Env:ERP_IDENTITY_SEED_PASSWORD
```
