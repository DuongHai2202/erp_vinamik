# PROD-04 — Mô hình và API kế hoạch sản xuất
- Chức năng: 2/5 — Lập kế hoạch sản xuất
- Trạng thái: TODO
- Phụ thuộc: `02-mrp-calculation-api.md`
## Mục tiêu
Lập kế hoạch thành phẩm và khoảng thời gian dự kiến trước khi phát hành lệnh.
## Phạm vi
Kế hoạch/kỳ, mã sản phẩm, số lượng dự kiến, ngày bắt đầu/kết thúc, trạng thái draft/reviewed/released.
## Tiêu chí nghiệm thu
- Quantity dương và ngày kết thúc không trước ngày bắt đầu.
- Kế hoạch có thể truy về lần chạy MRP hoặc ghi rõ input nguồn.
- Chỉ thao tác có quyền mới tạo/sửa/phát hành.
## Kiểm tra
Test validation, state transitions và permission.
## Ngoài phạm vi
Không thêm scheduling engine hoặc kế hoạch mua hàng.

