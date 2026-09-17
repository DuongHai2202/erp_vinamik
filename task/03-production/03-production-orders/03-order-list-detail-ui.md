# PROD-11 — Giao diện danh sách và chi tiết lệnh
- Nhóm chức năng: 3/5 — Quản lý lệnh sản xuất
- Trạng thái: TODO
- Phụ thuộc: PROD-10

## Mục tiêu
Người quản lý tìm lệnh nhanh và xem thông tin vận hành của lệnh.

## Phạm vi
Bảng lệnh tìm theo mã/sản phẩm, lọc ngày/trạng thái, trang chi tiết và action tạo/sửa/hủy theo quyền.

## Tiêu chí nghiệm thu
- Bảng có pagination, loading, empty và error state.
- Trạng thái Chờ thực hiện/Đang thực hiện/Hoàn thành/Hủy hiển thị rõ.
- User chỉ xem thấy dữ liệu nhưng không có action ghi.

## Kiểm tra
Test filter, detail, role chỉ xem và layout desktop/tablet.

## Ngoài phạm vi
Không dựng biểu đồ tổng quan thay cho danh sách lệnh.

