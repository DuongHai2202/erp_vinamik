# PROD-08 — Giao diện quản lý vật tư sản xuất
- Nhóm chức năng: 2/5 — Quản lý nguyên vật liệu
- Trạng thái: TODO
- Phụ thuộc: PROD-04, PROD-05, PROD-06, PROD-07

## Mục tiêu
Planner/người sản xuất xem vật tư, định mức, nhu cầu và tiêu hao theo lệnh.

## Phạm vi
Danh sách tra cứu vật tư; màn hình BOM; bảng nhu cầu/tồn/thiếu; lịch sử cấp và form ghi actual consumption.

## Tiêu chí nghiệm thu
- Phân biệt rõ số lượng định mức, cần, tồn khả dụng, đã cấp và đã tiêu hao.
- CRUD vật tư chỉ gọi luồng do Kho sở hữu.
- UI tiếng Việt; thông báo success/error tiếng Anh.

## Kiểm tra
Thử lệnh có BOM, thiếu vật tư, user chỉ xem và user thiếu quyền Kho.

## Ngoài phạm vi
Không xây dashboard MRP độc lập.

