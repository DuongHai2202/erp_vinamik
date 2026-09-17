# HR-15 — Duyệt và khóa kỳ lương
- Chức năng: 5/5 — Tính lương
- Trạng thái: DONE
- Phụ thuộc: `14-payroll-calculator.md`, `../../00-foundation/12-backend-rbac-enforcement.md`
## Mục tiêu
Kết quả lương có trạng thái rõ và không đổi âm thầm sau khi duyệt.
## Phạm vi
Tính lại draft, gửi duyệt, approve/reject, lock kỳ; ghi actor/time và snapshot các dòng lương.
## Tiêu chí nghiệm thu
- Chỉ người đủ quyền mới duyệt/chốt.
- Kỳ locked không cho sửa input/tính lại bằng API thông thường.
- Việc chạy lại endpoint không nhân đôi dòng lương.
## Kiểm tra
Đã test state transition calculate → approve → lock, tính lại không nhân đôi dòng, khóa chặn tính lại, trigger chặn chuyển/sửa snapshot đã khóa và HTTP 403 khi quyền chỉ đọc gọi calculate.
## Ngoài phạm vi
Không tích hợp thanh toán lương.
