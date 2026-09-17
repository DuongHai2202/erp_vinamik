# PROD-13 — Kiểm thử tích hợp module Sản xuất
- Chức năng: Tích hợp 1–5/5
- Trạng thái: TODO
- Phụ thuộc: PROD-03, PROD-05, PROD-07, PROD-09, PROD-12
## Mục tiêu
Kiểm chứng MRP → kế hoạch → lệnh → thực thi → bàn giao Kho.
## Phạm vi
Một kịch bản có BOM, nhu cầu, tồn đủ/thiếu, phát hành lệnh, ghi tiến độ, xuất vật tư và nhập thành phẩm.
## Tiêu chí nghiệm thu
- MRP chỉ đọc số tồn qua hợp đồng Kho.
- Movement tạo từ lệnh có source reference.
- Không có quyền sản xuất thì user không phát hành/ghi thực hiện.
## Kiểm tra
Chạy integration test với PostgreSQL thử nghiệm và xác nhận ledger Kho sau kịch bản.
## Ngoài phạm vi
Không kiểm thử phân loại chất lượng hoặc giá thành sản phẩm.

