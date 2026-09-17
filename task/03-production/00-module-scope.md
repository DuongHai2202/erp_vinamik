# Phạm vi module — Quản lý sản xuất

Trạng thái: Ưu tiên triển khai thứ ba, sau Nhân sự và Kho & nguyên vật liệu.
Nguồn chức năng: ERP_ht.pdf, mục 2.2.1.1, 2.2.2.1 và các đặc tả use case ở mục 2.2.2.2.

## Đúng 5 nhóm chức năng theo use case tổng quát

1. Quản lý kế hoạch sản xuất — FR3–FR8.
2. Quản lý nguyên vật liệu — FR15–FR25.
3. Quản lý lệnh sản xuất — FR9–FR14; use case theo dõi tiến độ nằm bên trong nhóm này.
4. Quản lý phân công nhân sự — FR26–FR33.
5. Quản lý thành phẩm — FR34–FR42.

Đăng nhập và đăng xuất (FR1–FR2) thuộc platform Identity/Auth, không tính vào năm nhóm trên.

## Phân biệt nhóm chức năng và use case con

- Kiểm tra nhu cầu nguyên vật liệu/BOM là use case con của Quản lý nguyên vật liệu, không phải một module MRP riêng.
- Theo dõi tiến độ là use case con của Quản lý lệnh sản xuất, không phải nhóm chức năng thứ tư độc lập.
- Cấp vật tư và nhận thành phẩm là thao tác liên module gắn với nguyên vật liệu/lệnh/thành phẩm; không thay thế nhóm phân công nhân sự.

## Quyền sở hữu dữ liệu giữa module

- Kho sở hữu danh mục vật tư chuẩn, giao dịch nhập/xuất/chuyển/kiểm kê và số dư kho. Use case Quản lý nguyên vật liệu của Sản xuất đọc/chọn vật tư qua hợp đồng công khai; nếu cần CRUD từ màn hình Sản xuất, thao tác phải chuyển tiếp tới API Kho và dùng quyền Kho, không tạo bản sao danh mục.
- Sản xuất sở hữu BOM/định mức theo phiên bản, nhu cầu snapshot theo lệnh và lượng tiêu hao thực tế. Lượng vật tư đã cấp được truy qua hợp đồng Kho, không lưu như một số dư thứ hai. Cấp phát/hoàn trả làm thay đổi tồn phải đi qua hợp đồng Kho.
- Nhân sự sở hữu hồ sơ nhân viên. Phân công trong Sản xuất chỉ tham chiếu employee ID qua hợp đồng công khai, không tạo hồ sơ nhân viên riêng.
- Sản xuất sở hữu kế hoạch, lệnh, phân công, sự kiện tiến độ và bản ghi sản lượng đạt/lỗi theo lô. Kho sở hữu danh mục mặt hàng stockable, định danh lô tồn kho, số lượng vật lý và ledger.
- Nhập thành phẩm sau sản xuất gọi hợp đồng nhập kho để Kho tạo/tra cứu mặt hàng/lô và post receipt; Sản xuất chỉ giữ ID tham chiếu cùng bản ghi đầu ra, không tạo bản sao danh mục hoặc ghi trực tiếp ledger/số dư.
- Kho sở hữu số lượng vật lý trong kho. Nhập thành phẩm sau sản xuất gọi hợp đồng nhập kho; Sản xuất không ghi trực tiếp ledger hoặc số dư Kho.
- Chưa triển khai quyết định kiểm định/chấp nhận chất lượng chính thức; thuộc module Chất lượng đang hoãn.

## Cách hiểu MRP và tiến độ

ERP_ht có use case kiểm tra nhu cầu theo BOM và tồn kho trong nhóm nguyên vật liệu. Backlog giữ đúng use case này nhưng không mở rộng thành MRP tổng thể hoặc đề xuất mua hàng. Theo dõi tiến độ được triển khai trong Quản lý lệnh sản xuất dựa trên trạng thái và ghi nhận thực tế của lệnh.
