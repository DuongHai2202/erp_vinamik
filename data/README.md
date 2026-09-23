# Bộ dữ liệu tải lớn ERP Vinamik

Thư mục này chứa fixture CSV để kiểm thử giao diện, phân trang, bộ lọc, quyền chỉ đọc và các luồng nghiệp vụ của bốn module đang ưu tiên:

- Nhân sự: 600 nhân viên, 605 hợp đồng, 901 yêu cầu nghỉ và 421 hồ sơ thưởng/kỷ luật.
- Kho và nguyên vật liệu: hơn 500 mặt hàng, 953 phiếu nhập, 423 phiếu xuất, 253 phiếu điều chuyển, 6 phiên kiểm kê, lô hàng và số dư.
- Sản xuất: 250 kế hoạch, 500 dòng kế hoạch, 200 BOM, 600 lệnh, hơn 3.000 nhu cầu vật tư, phân công, sự kiện, tiêu hao và 600 bản ghi sản lượng.
- Chất lượng và giá thành: 12 quy tắc chi phí, 12 kỳ giá thành, 125 biên bản kiểm tra, 100 phiếu không phù hợp, 125 phép tính giá thành và 125 đề xuất giá bán; mỗi bản ghi tham chiếu mã vật tư, lệnh sản xuất và lô để kiểm thử luồng liên module.

Dữ liệu được sinh xác định bằng seed `20260916`, nên chạy lại sẽ cho cùng mã nghiệp vụ và cùng quan hệ giữa các file. File CSV dùng UTF-8 BOM để mở được bằng Excel và vẫn giữ dấu tiếng Việt. Các số điện thoại trong `employees.csv` được lưu dạng chuẩn hóa chỉ gồm 10–11 chữ số bắt đầu bằng `0`; cột `phone_number_display` dùng nhóm `xxxx xxx xxx` để đọc nhanh theo thói quen Việt Nam. Các trạng thái vòng đời được phủ đủ theo từng API (bản nháp, chờ duyệt, đã duyệt/ghi sổ, từ chối, hủy và các trạng thái chuyên biệt); chứng từ kho chỉ tạo movement và số dư khi đã ghi sổ. Cụ thể: HR dùng draft/active/expired/terminated/cancelled, nghỉ và thưởng dùng draft/pending/approved/rejected/cancelled, Kho dùng draft/pending/posted/cancelled, Sản xuất dùng draft/pending_receipt/received/failed/cancelled cho sản lượng, còn Chất lượng và giá thành có đủ các trạng thái phê duyệt, tính toán, giữ và công bố theo từng nghiệp vụ. Các cột ngày gốc (`date_of_birth`, `hired_on`, `terminated_on`) dùng ISO `yyyy-mm-dd` để import an toàn; cột `*_display` hiển thị `dd/mm/yyyy` cho người dùng Việt Nam. Đây là dữ liệu tổng hợp theo quy mô và quy trình doanh nghiệp (production-like), không phải dữ liệu demo rút gọn; mọi bản ghi vẫn được đánh dấu synthetic trong manifest để tránh nhầm với dữ liệu thật.

## Nguồn và phạm vi dữ liệu

Tên sản phẩm, nhóm sản phẩm và thương hiệu trong `reference/vinamilk_product_catalog.csv` được lấy làm tham chiếu từ các trang công khai của Vinamilk. Đây là dữ liệu catalog công khai, không phải dữ liệu bán hàng nội bộ. Các tên nhân sự, email, hồ sơ và giao dịch vận hành là dữ liệu tổng hợp có tính thực tế cho doanh nghiệp, không đại diện cho nhân viên hoặc giao dịch thật của Vinamilk. Email dùng miền `.test` để không gửi nhầm thư ra ngoài.

Các nguồn tham chiếu sản phẩm:

- https://www.vinamilk.com.vn/products/sua-dau-nanh-vinamilk-super-soy-gap-doi-canxi
- https://partners.vinamilk.com.vn/collections/all-products
- https://new.vinamilk.com.vn/collections/sua-trai-cay
- https://new.vinamilk.com.vn/collections/nuoc-giai-khat
- https://livestream.vinamilk.com.vn/
- https://www.vinamilk.com.vn/cong-bo-san-pham/wp-content/uploads/cbsp/pa-000001-tcbsp-09-ncpt-24-final.pdf
- https://www.vinamilk.com.vn/bao-cao-thuong-nien/bao-cao/2024/doc/vi/full-bao-cao.pdf

## Sinh lại dữ liệu

Chạy từ thư mục gốc repository:

```powershell
python data/generate_large_dataset.py
```

Lệnh này ghi đè các CSV sinh ra trong `data/human_resources`, `data/inventory`, `data/production` và `data/reference`, đồng thời cập nhật `data/manifest.json`. Không chạy generator trong thư mục database production; fixture không tự động chèn dữ liệu vào PostgreSQL.

## Cách dùng

CSV là đầu vào theo mã nghiệp vụ (`employee_code`, `item_code`, `order_code`...) để không phụ thuộc ID identity của từng database. Khi nạp vào môi trường test, nạp theo thứ tự: danh mục HR → nhân viên → hợp đồng/nghỉ/thưởng → danh mục Kho → lô và chứng từ Kho → phiên kiểm kê → BOM/kế hoạch → lệnh và dữ liệu Sản xuất. Người nạp phải dùng migration hiện tại và tài khoản có permission tương ứng; không bỏ qua service nếu đang kiểm thử rule nghiệp vụ, idempotency hoặc ledger. Sau khi nạp Sản xuất, chạy thêm data/load_quality_cost_via_api.py để tạo dữ liệu kiểm tra chất lượng, kỳ giá thành, phép tính và đề xuất giá bán liên kết. Nếu danh mục Kho và số dư đã có nhưng thiếu phiên kiểm kê, chạy riêng `py -3 data/load_fixture_via_api.py --phase stocktakes`; phase này chụp số dư qua API và không chèn trực tiếp vào database.

Để kiểm thử màn hình hợp đồng ngay trên môi trường local đã có dữ liệu, có thể bật `${ERP_FIXTURE_CONTRACT_ENABLED=true}` khi khởi động backend. Bootstrapper sẽ bổ sung bốn bản nháp và đồng bộ năm hợp đồng đang hiệu lực vào cửa sổ cảnh báo 15 ngày qua service, kèm audit; cờ này mặc định tắt và không dùng cho production.

Nếu database đã có bản ghi theo cùng mã và cần đồng bộ lại tên hiển thị, địa chỉ hoặc liên hệ theo CSV, chạy data/sync_master_data_via_api.py với ERP_ADMIN_USERNAME và ERP_ADMIN_PASSWORD. Ở local/test có thể bật `ERP_FIXTURE_STOCKTAKE_ENABLED=true` khi khởi động backend để tạo sáu phiên kiểm kê qua service nếu database chưa có phiên nào; cờ này mặc định tắt và không dùng cho production. Script chỉ cập nhật master data qua API, ghi audit theo service và không xóa bản ghi đang có.

Các file có hậu tố `_code` là khóa tham chiếu giữa các CSV. File `manifest.json` ghi số dòng của từng file và cờ dữ liệu tổng hợp để pipeline test kiểm tra trước khi nạp.

## Kiểm tra nhanh

```powershell
python data/generate_large_dataset.py
python data/validate_large_dataset.py
```

Script kiểm tra số lượng tối thiểu, mã duy nhất, khóa tham chiếu, ngày/số lượng, trạng thái phiên kiểm kê, vị trí theo kho, reconciliation movement/balance, BOM formula, production yield và công thức tách đạt/lỗi, giá thành đơn vị, biên lợi nhuận của quality_cost. Kết quả rà soát chi tiết nằm tại docs/data_quality_audit.md. Đây là fixture production-like cho môi trường test; dữ liệu production phải được tạo qua quy trình nghiệp vụ và được phê duyệt riêng.

## Dữ liệu tài khoản và yêu cầu cấp quyền

Danh sách `data/identity/registration_requests.csv` chứa 12 yêu cầu cấp tài khoản được đối chiếu từ nhân viên đang làm việc trong `human_resources/employees.csv`, kèm phòng ban và vai trò tối thiểu đề xuất. Nạp qua `data/load_identity_requests_via_api.py`; mật khẩu chỉ nhận từ biến môi trường hoặc sinh trong bộ nhớ, không lưu trong CSV. Các yêu cầu vẫn ở trạng thái chờ duyệt để quản trị viên đối chiếu `employee_id`, gán vai trò tối thiểu và phê duyệt theo đúng phân quyền; xem hướng dẫn chi tiết tại `data/identity/README.md`.
