# HR-17 — Kiểm thử xuyên suốt module Nhân sự
- Chức năng: Tích hợp 1–5/5
- Trạng thái: IN_PROGRESS
- Phụ thuộc: HR-03, HR-06, HR-09, HR-12, HR-16
## Mục tiêu
Xác nhận năm nhóm HR dùng chung employee và permission mà không làm sai dữ liệu.
## Phạm vi
Kịch bản hồ sơ → hợp đồng → nghỉ không lương/ thưởng-phạt duyệt → tính và khóa kỳ lương.
## Tiêu chí nghiệm thu
- 15-day expiry alert xuất hiện đúng.
- Payroll chỉ lấy input đã duyệt và khớp công thức `monthly_mon_sat_v1` đã chốt trong `docs/payroll_policy.md`. (Đã kiểm tra trên PostgreSQL.)
- Role chỉ xem không thay đổi được bất kỳ record nào.
## Kiểm tra
Chạy automated integration test trên DB thử nghiệm; ghi các test chưa tự động hóa.
Đã có kiểm thử tích hợp opt-in `human_resources_module_integration_tests` trên PostgreSQL cục bộ: tạo hồ sơ nhân viên, kích hoạt hợp đồng, kiểm tra cảnh báo hết hạn trong 15 ngày, duyệt nghỉ không lương, thưởng/phạt, loại bỏ khoản pending/rejected, tính lại không trùng, duyệt/khóa kỳ, bảo vệ snapshot và xác nhận hợp đồng nguồn vẫn tiếp tục vòng đời. Chạy bằng `$env:ERP_RUN_INTEGRATION_TESTS='true'; .\mvnw.cmd test -q -Dtest=human_resources_module_integration_tests`.

Còn mở của task tích hợp toàn module: UI payroll và kiểm thử người dùng trên giao diện. Backend đã có HTTP 401/403 đại diện, snapshot isolation khi tính và phản hồi `409 CONCURRENT_CHANGE` khi cạnh tranh transaction.

## Ngoài phạm vi
Không đưa kiểm thử kết nối chấm công hoặc thuế vào đây.
