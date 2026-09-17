# HR-01 — Mô hình và migration hồ sơ nhân viên
- Chức năng: 1/5 — Hồ sơ nhân viên
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `../../00-foundation/04-database-migrations.md`
## Mục tiêu
Lưu hồ sơ nhân viên có mã định danh duy nhất và trạng thái làm việc.
## Phạm vi
Tạo hr.department, hr.job_title và hr.employee theo docs/database_design.md. Hồ sơ dùng mã nhân viên unique, họ tên, ngày sinh nullable, thông tin liên hệ được tài liệu môn học yêu cầu, trạng thái làm việc, ngày vào/nghỉ và FK nội bộ tới phòng ban/chức danh. Phone được chuẩn hóa về chữ số Việt Nam bắt đầu bằng `0`; ngày sinh không sau ngày vào và API kiểm tra tuổi tối thiểu 18 tại ngày vào. Quan hệ quản lý nếu có dùng manager_employee_id tự tham chiếu.
## Tiêu chí nghiệm thu
- Mã nhân viên không trùng.
- Trạng thái hoạt động/ngừng hoạt động được lưu rõ.
- Phòng ban và chức danh không bị lặp dưới dạng text tự do khi đã quản lý bằng danh mục.
- Hồ sơ không lưu username/password/role; tài khoản thuộc identity.user_account.
- Ngày sinh và phone hợp lệ được lưu/đọc qua API; migration không làm mất dữ liệu hồ sơ hiện hữu.
- Migration không xóa dữ liệu khi chạy lại.
## Kiểm tra
Test unique code, required fields và migration trên database thử nghiệm.
## Ngoài phạm vi
Không lưu password trong hồ sơ nhân viên và không xây module tuyển dụng/chấm công.
