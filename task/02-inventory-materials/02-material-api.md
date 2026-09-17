# INV-02 — API danh mục nguyên vật liệu
- Chức năng: 1/5 — Danh mục nguyên vật liệu
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `01-material-schema.md`, `../../00-foundation/12-backend-rbac-enforcement.md`
## Mục tiêu
Tra cứu và duy trì danh mục vật tư qua API có phân quyền.
## Phạm vi
List/search/detail/create/update/deactivate với validation mã, tên và đơn vị tính.
## Tiêu chí nghiệm thu
- Tìm theo mã/tên và lọc active.
- Không deactivate record đang được giao dịch tham chiếu nếu gây sai lịch sử.
- Endpoint ghi kiểm tra permission backend.
## Kiểm tra
Test tìm kiếm, duplicate code, deactivate và role chỉ xem.
## Ngoài phạm vi
Không cho client truy vấn trực tiếp database.

