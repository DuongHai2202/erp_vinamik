# PROD-02 — API tính nhu cầu MRP
- Chức năng: 1/5 — MRP
- Trạng thái: TODO
- Phụ thuộc: `01-bom-schema.md`, `../02-inventory-materials/04-stock-ledger-and-balance-query.md`
## Mục tiêu
Tính nhu cầu vật tư ròng từ nhu cầu thành phẩm, BOM và tồn khả dụng.
## Phạm vi
Nhận nhu cầu/kế hoạch đầu vào, bung BOM, tính gross requirement, trừ tồn khả dụng và trả shortage; lưu lần chạy để tra cứu.
## Tiêu chí nghiệm thu
- Kết quả có input, thời điểm tính và quantity theo vật tư.
- Tính lại cùng input cho kết quả xác định.
- Chỉ đọc tồn qua query public của Kho.
## Kiểm tra
Test một BOM, nhiều thành phẩm dùng chung vật tư, đủ tồn và thiếu tồn.
## Ngoài phạm vi
Không tạo đơn mua hàng hoặc tối ưu lịch máy nâng cao.

