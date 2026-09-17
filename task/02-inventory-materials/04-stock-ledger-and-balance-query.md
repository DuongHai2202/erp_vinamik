# INV-04 — Sổ giao dịch kho và truy vấn số dư
- Chức năng: Năng lực chung cho chức năng 2–5/5
- Trạng thái: IN_PROGRESS
- Phụ thuộc: `01-material-schema.md`, `../../00-foundation/04-database-migrations.md`
## Mục tiêu
Một nguồn sự thật cho số dư để nhập, xuất, kiểm kê và điều chuyển không làm lệch nhau.
## Phạm vi
Tạo inventory.stock_movement append-only và inventory.stock_balance dạng projection theo docs/database_design.md. Mỗi movement gắn mặt hàng, vị trí kho, lô nếu có, quantity delta có dấu, dòng chứng từ nguồn, actor và thời điểm; truy vấn số dư theo mặt hàng–vị trí–lô từ movement đã post.
## Tiêu chí nghiệm thu
- Không sửa số dư bằng update trực tiếp.
- Movement đã post không bị xóa/sửa; sai sót sửa bằng movement điều chỉnh có tham chiếu.
- Có truy vấn số dư theo vật tư, vị trí và lô.
- Balance có thể đối soát/tái tạo từ ledger và không được coi là nguồn dữ liệu độc lập.
- Receipt, issue, transfer và stocktake line tạo movement trong cùng transaction với post chứng từ.
## Kiểm tra
Test số dư sau receipt, issue, transfer và reversal thử nghiệm.
## Ngoài phạm vi
Không lập thêm chức năng thứ sáu; ledger là nền dùng chung cho 5 chức năng Kho.

