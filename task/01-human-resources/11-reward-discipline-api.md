# HR-11 — API khen thưởng và kỷ luật
- Chức năng: 4/5 — Khen thưởng/kỷ luật
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `10-reward-discipline-schema.md`, `../../00-foundation/12-backend-rbac-enforcement.md`
## Mục tiêu
Quản lý quyết định thưởng/phạt có thể kiểm tra và duyệt.
## Phạm vi
Tạo, tìm kiếm, xem, cập nhật khi còn nháp và duyệt/từ chối; khóa chỉnh sửa sau khi được payroll chốt.
## Tiêu chí nghiệm thu
- Chỉ record approved được trả như payroll input.
- Không sửa record đã gắn kỳ payroll locked.
- Thiếu quyền duyệt bị từ chối ở backend.
## Kiểm tra
Test state transitions và giới hạn sửa sau chốt payroll.
## Ngoài phạm vi
Không tự ghi trực tiếp vào bảng payroll.

