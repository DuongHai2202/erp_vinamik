# HR-13 — Mô hình kỳ lương và đầu vào
- Chức năng: 5/5 — Tính lương
- Trạng thái: DONE
- Phụ thuộc: `04-contract-schema.md`, `07-absence-schema.md`, `10-reward-discipline-schema.md`
## Mục tiêu
Xác định kỳ lương và snapshot các đầu vào được dùng để tính.
## Phạm vi
Kỳ bắt đầu/kết thúc, trạng thái draft/calculated/approved/locked, nhân viên thuộc kỳ, hợp đồng hiệu lực, nghỉ không lương approved, thưởng/phạt approved.
## Tiêu chí nghiệm thu
- Không tính trùng một nhân viên trong cùng kỳ.
- Lưu tham chiếu đầu vào và phiên bản tính để có thể truy vết.
- Dữ liệu đầu vào bị khóa/ghi nhận snapshot khi kỳ được chốt.
## Kiểm tra
Integration test PostgreSQL đã kiểm tra hợp đồng bao phủ kỳ, snapshot/version, loại bỏ khoản pending/rejected, một nhân viên một record và trigger bảo vệ snapshot đã khóa.
## Ngoài phạm vi
Không thêm thuế, bảo hiểm, OT hoặc kết nối ngân hàng nếu chưa có yêu cầu.
