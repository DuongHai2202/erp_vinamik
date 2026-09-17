# PROD-24 — Kịch bản demo HR, Kho và Sản xuất
- Nhóm: End-to-end demo
- Trạng thái: IN_PROGRESS
- Phụ thuộc: PROD-23, HR integration tests, Inventory integration tests, FND-17

## Mục tiêu
Có kịch bản lặp lại được cho ba module ưu tiên và xác nhận dữ liệu dùng chung trên năm máy.

## Phạm vi
Tạo user/role, nhân viên, vật tư và tồn; lập kế hoạch/lệnh; gán nhân viên; cấp/ghi tiêu hao; ghi sản lượng/lô; nhận thành phẩm vào Kho; xem từ máy trạm khác.

## Tiêu chí nghiệm thu
- Các máy xem cùng dữ liệu qua cùng URL.
- Role chỉ xem không ghi được qua bất kỳ API nào.
- Kịch bản nêu Quality/Cost và Reporting đang hoãn. (Chưa chạy runbook qua năm máy và chưa có kiểm thử HTTP role chỉ xem.)

## Kiểm tra
Chạy runbook deployment tập trung và ghi kết quả từng bước.

## Ngoài phạm vi
Không mô phỏng Quality/Cost hoặc báo cáo nâng cao.

