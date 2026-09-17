# FND-18 — Bản đồ chức năng, task và nguồn yêu cầu
- Nhóm: Foundation / kiểm soát phạm vi
- Trạng thái: TODO
- Phụ thuộc: Các file 00-module-scope.md của từng module

## Mục tiêu
Giữ đủ năm nhóm chức năng mỗi module, tách login/permission khỏi nghiệp vụ và giữ đúng owner dữ liệu.

## Ma trận ưu tiên

| Ưu tiên | Module | 5 nhóm chức năng | Nguồn |
|---|---|---|---|
| 1 | Nhân sự | Hồ sơ nhân viên; hợp đồng/cảnh báo 15 ngày; nghỉ phép/vắng mặt; khen thưởng/kỷ luật; tính lương | docs/ERP.docx |
| 2 | Kho và nguyên vật liệu | Danh mục vật tư; nhập kho; xuất kho; kiểm kê; điều chuyển | docs/quanlykho.docx |
| 3 | Sản xuất | Kế hoạch sản xuất; nguyên vật liệu; lệnh sản xuất; phân công nhân sự; thành phẩm | docs/ERP_ht.pdf, mục 2.2 |
| Hoãn | Chất lượng và giá thành | Kiểm tra chất lượng; xử lý không phù hợp; tính giá thành; đề xuất/ra quyết định điều chỉnh giá; phê duyệt bảng giá | docs/erp nhóm 4.docx |
| Hoãn | Dữ liệu và báo cáo | Quản lý danh mục dữ liệu; tra cứu; tạo báo cáo; xem/phân tích; xuất báo cáo | docs/ERP_Nhom5.docx |

## Phạm vi chi tiết Sản xuất

- Kế hoạch sản xuất: FR3–FR8.
- Nguyên vật liệu: FR15–FR25; BOM và kiểm tra nhu cầu là use case con.
- Lệnh sản xuất: FR9–FR14; theo dõi tiến độ là use case con của lệnh.
- Phân công nhân sự: FR26–FR33.
- Thành phẩm: FR34–FR42.
- FR1–FR2 đăng nhập/đăng xuất thuộc platform. Không dùng MRP hoặc theo dõi tiến độ làm nhóm cấp một riêng.

## Ranh giới liên module

- Login, user, role, permission và audit là platform; không tính vào năm chức năng.
- Kho là owner của vật tư chuẩn, warehouse movements và số dư; Production đọc/gửi yêu cầu qua public contract.
- HR là owner hồ sơ nhân viên; Production tham chiếu employee ID khi phân công.
- Production là owner kế hoạch/dòng kế hoạch, lệnh, phân công, BOM theo phiên bản, snapshot nhu cầu, tiến độ và đầu ra/sản lượng. Danh mục stock item và lot dùng cho tồn kho thuộc Inventory; Production gọi contract và chỉ lưu ID tham chiếu, không tạo master trùng.
- Hai module deferred chỉ giữ scope, không triển khai trước ba module ưu tiên.
