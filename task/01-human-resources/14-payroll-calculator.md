# HR-14 — Dịch vụ tính lương theo tài liệu
- Chức năng: 5/5 — Tính lương
- Trạng thái: DONE
- Phụ thuộc: `13-payroll-period-and-inputs.md`
## Mục tiêu
Tính lương nhất quán từ mức lương hợp đồng và các khoản đã duyệt.
## Phạm vi
Cài công thức đã mô tả: lương cơ bản kỳ − khấu trừ nghỉ không lương + thưởng đã duyệt − phạt đã duyệt; biểu diễn tiền bằng decimal.
## Tiêu chí nghiệm thu
- Cùng một tập input luôn cho cùng một kết quả.
- Record pending/rejected không ảnh hưởng kết quả.
- Quy tắc quy đổi lương tháng/ngày được ghi rõ và không tự suy diễn.
## Kiểm tra
Unit test kiểm tra ngày công chuẩn; integration test PostgreSQL kiểm tra lương cơ bản, nghỉ không lương, thưởng, phạt, làm tròn 6/2 chữ số, input pending/rejected và tính lại cho cùng kết quả. Công thức được version hóa tại `docs/payroll_policy.md`.
## Ngoài phạm vi
Không tự thêm chính sách thuế, bảo hiểm, phụ cấp hay làm thêm giờ.
