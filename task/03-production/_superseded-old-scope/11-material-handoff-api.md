# PROD-11 — API yêu cầu xuất nguyên liệu và nhập thành phẩm
- Chức năng: 5/5 — Bàn giao vật tư/thành phẩm
- Trạng thái: TODO
- Phụ thuộc: `10-inventory-public-contract.md`, `06-execution-api.md`
## Mục tiêu
Gắn movement Kho với lệnh sản xuất ở đúng thời điểm bàn giao.
## Phạm vi
Tạo issue request theo BOM/lệnh; ghi nhận finished output receipt sau sản xuất; lưu các reference ID trả về từ Kho.
## Tiêu chí nghiệm thu
- Cả hai luồng đều gọi contract Kho, không ghi bảng inventory.
- Mỗi yêu cầu liên kết được duy nhất tới lệnh/dòng và retry an toàn.
- Nếu một bước lỗi, trạng thái lệnh cho biết phần nào chưa bàn giao; không giả vờ hoàn tất.
## Kiểm tra
Test issue thành công/thiếu tồn, receipt output, duplicate request và lỗi giữa chừng.
## Ngoài phạm vi
Không tự duyệt chất lượng sản phẩm; việc QC thuộc module deferred.

