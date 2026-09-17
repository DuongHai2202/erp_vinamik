# Phạm vi module — Quản lý kho và nguyên vật liệu
Trạng thái: Ưu tiên triển khai thứ hai, trước module Sản xuất. Nguồn chính: `docs/quanlykho.docx`.
## Đúng 5 nhóm chức năng
1. Danh mục nguyên vật liệu.
2. Quản lý nhập kho.
3. Quản lý xuất kho.
4. Kiểm kê tồn kho.
5. Điều chuyển giữa kho/vị trí.
## Ranh giới
Kho sở hữu danh mục vật tư, phiếu nhập/xuất/kiểm kê/điều chuyển, sổ giao dịch và số dư. Sản xuất gửi yêu cầu qua hợp đồng công khai; không ghi trực tiếp bảng kho.
## Giới hạn phiên bản môn học
Tài liệu có nội dung kiểm tra chất lượng nguyên liệu; phần kiểm tra/phê duyệt chất lượng thuộc module Chất lượng đang hoãn. Không nhân bản quy trình QC trong Kho. Phiên bản ưu tiên này chỉ làm nhập/xuất/tồn theo luồng Kho đã chốt; ghi rõ điểm nối để bổ sung trạng thái giữ/giải phóng sau này.

