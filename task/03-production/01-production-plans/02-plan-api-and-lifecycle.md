# PROD-02 — API và vòng đời kế hoạch
- Nhóm chức năng: 1/5 — Quản lý kế hoạch sản xuất
- Nguồn: ERP_ht FR3–FR8
- Trạng thái: IN_PROGRESS
- Phụ thuộc: PROD-01; Foundation RBAC

## Mục tiêu
Cho phép người có quyền xem, tạo, sửa, xóa có điều kiện và đổi trạng thái kế hoạch.

## Phạm vi
API phân trang/tìm kiếm/lọc, chi tiết, tạo, cập nhật, chuyển trạng thái và xóa kế hoạch chưa phát sinh lệnh.

## Tiêu chí nghiệm thu
- Ngày, mã trùng và trường bắt buộc được validate ở backend.
- Không xóa kế hoạch đã được lệnh tham chiếu hoặc đã hoàn tất.
- Mỗi action kiểm tra permission server-side; API success/error message bằng tiếng Anh.

## Kiểm tra
Test CRUD, filter, trạng thái, ràng buộc xóa và tài khoản chỉ xem.

## Ngoài phạm vi
Không tự thêm bước phê duyệt chưa có trong use case.

