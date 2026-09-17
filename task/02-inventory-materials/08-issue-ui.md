# INV-08 — Giao diện xuất kho
- Chức năng: 3/5 — Quản lý xuất kho
- Trạng thái: TODO
- Phụ thuộc: `07-issue-posting-api.md`
## Mục tiêu
Lập phiếu xuất thủ công hoặc xem yêu cầu xuất từ Sản xuất theo cùng luồng Kho.
## Phạm vi
Form chọn vật tư/kho/số lượng, danh sách phiếu, xem nguồn yêu cầu và post nếu được cấp quyền.
## Tiêu chí nghiệm thu
- Số lượng khả dụng hiển thị trước post.
- Lỗi tồn không đủ nêu rõ vật tư và số lượng còn thiếu.
- User chỉ xem không post được.
## Kiểm tra
Thử xuất thành công, xuất vượt tồn và role chỉ xem.
## Ngoài phạm vi
Không ghi nhận tiến độ sản xuất trên màn hình Kho.

