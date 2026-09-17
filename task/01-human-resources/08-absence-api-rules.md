# HR-08 — API và quy tắc nghỉ phép
- Chức năng: 3/5 — Nghỉ phép/vắng mặt
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `07-absence-schema.md`, `../../00-foundation/12-backend-rbac-enforcement.md`
## Mục tiêu
Cho phép tạo, tra cứu và duyệt nghỉ theo luồng có trạng thái.
## Phạm vi
Tạo yêu cầu, lọc theo nhân viên/kỳ/trạng thái, approve/reject/cancel; kiểm tra ngày hợp lệ và chồng lấn.
## Tiêu chí nghiệm thu
- Không duyệt yêu cầu trùng ngày cho cùng nhân viên nếu quy tắc dự án cấm chồng lấn.
- Chỉ approver có quyền mới đổi sang approved/rejected.
- Payroll chỉ đọc ngày nghỉ không lương đã duyệt.
## Kiểm tra
Test chồng lấn, sai ngày, thiếu quyền và yêu cầu đã duyệt.
## Ngoài phạm vi
Không tự tính pháp định ngày phép hoặc tích lũy phép năm ngoài tài liệu.

