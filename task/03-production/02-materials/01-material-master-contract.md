# PROD-04 — Hợp đồng danh mục vật tư cho Sản xuất
- Nhóm chức năng: 2/5 — Quản lý nguyên vật liệu
- Nguồn: ERP_ht FR15–FR19
- Trạng thái: IN_PROGRESS
- Phụ thuộc: Inventory material API

## Mục tiêu
Đáp ứng tra cứu vật tư từ Sản xuất mà không tạo nguồn dữ liệu vật tư thứ hai.

## Phạm vi
Public contract/DTO để tìm vật tư, đơn vị tính và số lượng khả dụng. Nếu nghiệp vụ cần thêm/sửa vật tư từ màn hình Sản xuất, chuyển tiếp tới API Kho và dùng permission Kho.

## Tiêu chí nghiệm thu
- Sản xuất không import entity/repository hoặc ghi bảng vật tư Kho.
- Mã vật tư dùng cùng một định danh ở hai module.
- Quyền CRUD vật tư do Kho kiểm tra và audit.

## Kiểm tra
Test tra cứu theo mã/tên, vật tư inactive và user thiếu quyền Kho.

## Ngoài phạm vi
Không tạo migration hoặc CRUD vật tư nội bộ riêng trong Production.

