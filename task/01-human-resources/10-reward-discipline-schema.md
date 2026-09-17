# HR-10 — Mô hình khen thưởng và kỷ luật
- Chức năng: 4/5 — Khen thưởng/kỷ luật
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `01-employee-schema.md`, `04-database-migrations.md`
## Mục tiêu
Lưu quyết định thưởng/phạt và trạng thái duyệt để làm đầu vào lương.
## Phạm vi
Loại sự kiện, nhân viên, ngày, lý do, số tiền tùy chọn, trạng thái, approver và liên kết kỳ lương nếu có.
## Tiêu chí nghiệm thu
- Thưởng và phạt phân biệt rõ dấu/loại, không nhập cùng một giá trị mơ hồ.
- Chỉ sự kiện được duyệt mới là đầu vào payroll.
- Lưu lịch sử trạng thái duyệt.
## Kiểm tra
Test amount precision, trạng thái và tham chiếu nhân viên.
## Ngoài phạm vi
Không tạo hệ thống KPI hoặc quy trình kỷ luật nhiều cấp.

