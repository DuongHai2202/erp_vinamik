# Quản lý chất lượng và giá thành
Trạng thái: MVP đã triển khai dữ liệu và API; tiếp tục hoàn thiện theo tiêu chí nghiệm thu từng chức năng.
Nguồn tham khảo: `docs/erp nhóm 4.docx` và các phần liên quan trong `docs/quanlykho.docx`.
## Đúng 5 nhóm chức năng để lập task chi tiết sau
1. Kiểm tra chất lượng thành phẩm.
2. Xử lý sản phẩm không phù hợp.
3. Tính giá thành sản phẩm.
4. Đề xuất/ra quyết định điều chỉnh giá bán.
5. Phê duyệt bảng giá.
## Ranh giới dự kiến
QC là nơi quyết định trạng thái đạt/không đạt và giữ/giải phóng lô. Kho tiếp tục sở hữu movement; module giá thành đọc giao dịch liên quan qua hợp đồng công khai. Migration, API và giao diện MVP đã có; các bản ghi fixture liên kết được nạp qua data/load_quality_cost_via_api.py. Bổ sung nghiệp vụ chuyên sâu chỉ thực hiện khi có yêu cầu riêng.

