# INV-12 — API điều chuyển kho/vị trí
- Chức năng: 5/5 — Điều chuyển
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `04-stock-ledger-and-balance-query.md`, `07-issue-posting-api.md`
## Mục tiêu
Chuyển vật tư giữa hai vị trí và giữ cân bằng nguồn/đích.
## Phạm vi
Tạo inventory.transfer header và một hoặc nhiều inventory.transfer_line; mỗi dòng có mặt hàng/lô/số lượng và vị trí nguồn/đích hợp lệ. Khi post tạo hai movement đối ứng trong cùng transaction.
## Tiêu chí nghiệm thu
- Mỗi transfer line phải có nguồn và đích khác nhau, vật tư/lô và quantity hợp lệ.
- Một phiếu chuyển có thể có nhiều dòng; tổng movement âm bằng tổng movement dương theo từng item/lot.
- Tồn nguồn không âm sau điều chuyển.
- Nếu một đầu ghi lỗi thì cả hai đầu rollback.
## Kiểm tra
Test thành công, thiếu tồn, đích không hợp lệ, retry và rollback.
## Ngoài phạm vi
Không xử lý vận chuyển liên công ty hoặc nhiều bước transit phức tạp.

