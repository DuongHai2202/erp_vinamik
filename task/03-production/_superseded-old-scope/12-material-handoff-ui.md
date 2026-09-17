# PROD-12 — Giao diện bàn giao vật tư và thành phẩm
- Chức năng: 5/5 — Bàn giao vật tư/thành phẩm
- Trạng thái: TODO
- Phụ thuộc: `11-material-handoff-api.md`
## Mục tiêu
Nhân viên xem yêu cầu vật tư, kết quả xuất từ Kho và bàn giao thành phẩm.
## Phạm vi
Panel vật tư theo lệnh, trạng thái yêu cầu xuất, action gửi yêu cầu, form số lượng thành phẩm và reference phiếu nhập Kho.
## Tiêu chí nghiệm thu
- Chưa xác nhận từ Kho không hiển thị là đã xuất/đã nhập.
- Thiếu tồn hiển thị rõ và có thể xử lý theo quy trình Kho.
- User chỉ xem không tạo yêu cầu bàn giao.
## Kiểm tra
Thử luồng lệnh → xuất kho → ghi hoàn thành → nhập thành phẩm bằng hai module.
## Ngoài phạm vi
Không có chức năng phân loại đạt/lỗi QC.

