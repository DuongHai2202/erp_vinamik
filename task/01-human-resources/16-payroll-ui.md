# HR-16 — Giao diện bảng lương theo kỳ
- Chức năng: 5/5 — Tính lương
- Trạng thái: TODO
- Phụ thuộc: `15-payroll-lock-and-approval.md`
## Mục tiêu
HR/approver xem các khoản tạo nên kết quả lương và trạng thái kỳ.
## Phạm vi
Chọn kỳ, chạy tính khi có quyền, xem bảng chi tiết, gửi duyệt, duyệt/từ chối và xem kỳ locked.
## Tiêu chí nghiệm thu
- Mỗi dòng giải thích lương cơ bản, nghỉ không lương, thưởng, phạt và thực lĩnh.
- User chỉ xem có thể xem kết quả nhưng không thao tác.
- Kỳ locked thể hiện trạng thái chỉ đọc.
## Kiểm tra
So sánh hiển thị với kết quả API và role permissions.
## Ngoài phạm vi
Không xuất phiếu lương PDF hoặc gửi lương qua ngân hàng.

