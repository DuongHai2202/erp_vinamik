# Chính sách tính lương phiên bản 1

Nguồn nghiệp vụ là chức năng Tính lương trong `docs/ERP.docx`: tiền thực nhận bằng lương theo hợp đồng, trừ nghỉ không lương, cộng thưởng và trừ phạt. Phiên bản triển khai là `monthly_mon_sat_v1`.

## Kỳ và ngày công chuẩn

- Mỗi kỳ là trọn một tháng dương lịch.
- Ngày công chuẩn là số ngày từ thứ Hai đến thứ Bảy trong tháng. Chủ nhật không tính.
- Dự án chưa có lịch ngày lễ, ca theo ngày hoặc máy chấm công nên phiên bản này không tự suy diễn các dữ liệu đó.
- Hợp đồng dùng để tính phải ở trạng thái `active` và bao phủ toàn bộ kỳ. Nếu một nhân viên có nhiều hợp đồng `active` chồng lấn trong kỳ hoặc chỉ có hợp đồng bao phủ một phần kỳ, hệ thống từ chối tính để HR sửa dữ liệu thay vì tạo lương sai.
- Tất cả hợp đồng trong cùng một kỳ phải dùng một loại tiền để tổng kỳ có ý nghĩa kế toán rõ ràng.

## Công thức

`daily_rate_6dp = round(base_salary / standard_working_days, 6)`

`unpaid_leave_amount = round(daily_rate_6dp * approved_unpaid_working_days, 2)`

`gross_amount = base_salary + approved_reward_amount`

`deduction_amount = unpaid_leave_amount + approved_discipline_amount`

`net_amount = gross_amount - deduction_amount`

Ngày nghỉ không lương chỉ đếm phần giao với kỳ và chỉ đếm thứ Hai đến thứ Bảy. Đơn giá ngày được lưu 6 chữ số thập phân; từng số tiền được làm tròn `HALF_UP` về 2 chữ số. Kết quả âm được giữ nguyên để phản ánh đúng công thức và cần HR xử lý nghiệp vụ, không tự ép về 0.

## Snapshot và vòng đời

- Chỉ dùng nghỉ, thưởng và phạt ở trạng thái `approved` và có ngày hiệu lực trong kỳ.
- Loại tiền của thưởng/phạt phải trùng loại tiền hợp đồng; hệ thống từ chối kỳ có dữ liệu khác loại tiền.
- Mỗi lần tính lại kỳ chưa duyệt sẽ xóa snapshot cũ rồi tạo lại trong cùng transaction, không nhân đôi dòng.
- Transaction tính lương dùng snapshot isolation `REPEATABLE_READ`; nếu dữ liệu bị thay đổi cạnh tranh, API trả xung đột để client tải lại và thử lại thay vì trộn input ở nhiều thời điểm.
- Vòng đời là `draft` hoặc `rejected` → `calculated` → `approved` → `locked`.
- Kỳ `locked`, bản ghi và dòng lương snapshot bị chặn sửa/xóa bằng trigger PostgreSQL. Nguồn HR vẫn tiếp tục vòng đời bình thường; mọi thay đổi về sau không làm thay đổi snapshot và kết quả của kỳ đã khóa.
- Phiên bản này không tính thuế, bảo hiểm, phụ cấp, làm thêm giờ hoặc thanh toán ngân hàng.


## Ví dụ dữ liệu đã tính

Kỳ payroll_2026_02 trong môi trường dữ liệu kiểm thử đã được tính ở trạng thái calculated, gồm 570 nhân viên và tổng thực lĩnh 13.677.937.500 VND.

- Nhân viên vmk0023 – Đỗ Mạnh Duy: lương hợp đồng 26.500.000 VND, ngày công chuẩn 24, nghỉ không lương được duyệt 1 ngày.
- Đơn giá ngày = 26.500.000 / 24 = 1.104.166,666667 VND.
- Khoản trừ nghỉ không lương = 1.104.166,666667, làm tròn 1.104.166,67 VND.
- Thực lĩnh = 26.500.000 - 1.104.166,67 = 25.395.833,33 VND.

Một ví dụ khác là vmk0096 – Dương Xuân Tiến: lương hợp đồng 17.250.000 VND, thưởng đã duyệt 9.250.000 VND, không có nghỉ không lương hoặc phạt; tổng và thực lĩnh là 26.500.000 VND. Các dòng cấu thành được lưu snapshot cùng kỳ, nên khi kỳ chuyển sang locked, thay đổi hợp đồng hoặc hồ sơ nguồn về sau không làm thay đổi kết quả đã khóa.

Giao diện hiển thị nhãn tiếng Việt như “Lương cơ bản”, “Nghỉ không lương”, “Thưởng”, “Kỷ luật” và “Hợp đồng lao động”; mã dòng kỹ thuật vẫn giữ trong API để liên kết và kiểm toán.
