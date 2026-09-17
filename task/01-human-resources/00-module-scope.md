# Phạm vi module — Quản lý nhân sự
Trạng thái: Ưu tiên triển khai thứ nhất, sau Foundation. Nguồn nghiệp vụ chính: `docs/ERP.docx`.
## Đúng 5 nhóm chức năng
1. Hồ sơ nhân viên: danh sách, tra cứu và thông tin nhân viên.
2. Hợp đồng lao động: quản lý hợp đồng và cảnh báo sắp hết hạn trước 15 ngày.
3. Nghỉ phép/vắng mặt: ghi nhận, xét duyệt và xác định ngày nghỉ không lương.
4. Khen thưởng/kỷ luật: ghi nhận quyết định và khoản tiền đã được duyệt.
5. Tính lương: tính theo kỳ, dựa trên hợp đồng, nghỉ không lương, thưởng và phạt đã duyệt.
## Ranh giới
Tài khoản đăng nhập và quyền là platform, không phải chức năng thứ sáu. HR sở hữu hồ sơ nhân viên, hợp đồng, nghỉ phép, khen thưởng/kỷ luật và kỳ lương. Module khác chỉ tham chiếu employee ID/hợp đồng qua hợp đồng công khai.
## Giới hạn phiên bản môn học
Không tự thêm thuế, bảo hiểm, làm thêm giờ, chấm công máy hoặc tích hợp ngân hàng khi tài liệu chưa yêu cầu. Công thức tính lương cần được xác nhận theo dữ liệu đầu vào mô tả trong ERP.docx.

