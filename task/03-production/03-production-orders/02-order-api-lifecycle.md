# PROD-10 — API vòng đời lệnh sản xuất
- Nhóm chức năng: 3/5 — Quản lý lệnh sản xuất
- Nguồn: ERP_ht FR9–FR14
- Trạng thái: IN_PROGRESS
- Phụ thuộc: PROD-09; plan API

## Mục tiêu
Quản lý tạo, tra cứu, sửa, hủy và đổi trạng thái lệnh.

## Phạm vi
API list/search/filter, create từ kế hoạch còn hiệu lực, detail, update, cancel và status transition.

## Tiêu chí nghiệm thu
- Không tạo lệnh từ kế hoạch không tồn tại/không còn hiệu lực.
- Kiểm tra lịch dây chuyền có xung đột theo quy tắc đã thống nhất.
- Không sửa trường cốt lõi sau khi lệnh chạy/hoàn tất.
- Hủy lệnh đã phát sinh movement phải dùng quy trình bù trừ, không xóa lịch sử.
- Backend kiểm tra quyền cho từng action.

## Kiểm tra
Test tạo hợp lệ, kế hoạch lỗi, xung đột, cập nhật, hủy và role chỉ xem.

## Ngoài phạm vi
Không tách theo dõi tiến độ thành module chức năng riêng.

