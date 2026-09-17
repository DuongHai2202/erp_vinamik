# HR-05 — API hợp đồng và cảnh báo trước 15 ngày
- Chức năng: 2/5 — Hợp đồng và cảnh báo hết hạn
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `04-contract-schema.md`, `../../00-foundation/12-backend-rbac-enforcement.md`
## Mục tiêu
Quản lý vòng đời hợp đồng và nhận diện hợp đồng sắp hết hạn.
## Phạm vi
API tra cứu/tạo/cập nhật/đóng hợp đồng; truy vấn cảnh báo khi hợp đồng hiệu lực hết hạn trong 15 ngày tới.
## Tiêu chí nghiệm thu
- Cảnh báo dựa trên ngày server và chỉ hợp đồng còn hiệu lực.
- Ngưỡng 15 ngày được kiểm thử tại biên 15, 16 và 0 ngày.
- Mọi thay đổi được kiểm tra quyền tại backend.
## Kiểm tra
Test vòng đời hợp đồng và truy vấn cảnh báo với dữ liệu ở các mốc ngày.
## Ngoài phạm vi
Không gửi email/SMS; task sau chỉ hiển thị cảnh báo trong website.

