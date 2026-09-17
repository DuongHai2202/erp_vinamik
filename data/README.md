# Bộ dữ liệu tải lớn ERP Vinamik

Thư mục này chứa fixture CSV để kiểm thử giao diện, phân trang, bộ lọc, quyền chỉ đọc và các luồng nghiệp vụ của ba module đang ưu tiên:

- Nhân sự: 600 nhân viên, 600 hợp đồng, 900 yêu cầu nghỉ và 420 hồ sơ thưởng/kỷ luật.
- Kho và nguyên vật liệu: hơn 500 mặt hàng, 950 phiếu nhập, 420 phiếu xuất, 250 phiếu điều chuyển, lô hàng và số dư.
- Sản xuất: 250 kế hoạch, 500 dòng kế hoạch, 200 BOM, 600 lệnh, hơn 3.000 nhu cầu vật tư, phân công, sự kiện, tiêu hao và 600 bản ghi sản lượng.

Dữ liệu được sinh xác định bằng seed `20260916`, nên chạy lại sẽ cho cùng mã nghiệp vụ và cùng quan hệ giữa các file. File CSV dùng UTF-8 BOM để mở được bằng Excel và vẫn giữ dấu tiếng Việt. Các số điện thoại trong `employees.csv` được lưu dạng chuẩn hóa chỉ gồm 10–11 chữ số bắt đầu bằng `0`; cột `phone_number_display` dùng nhóm `xxxx xxx xxx` để đọc nhanh theo thói quen Việt Nam. Các cột ngày gốc (`date_of_birth`, `hired_on`, `terminated_on`) dùng ISO `yyyy-mm-dd` để import an toàn; cột `*_display` hiển thị `dd/mm/yyyy` cho người dùng Việt Nam.

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

CSV là đầu vào theo mã nghiệp vụ (`employee_code`, `item_code`, `order_code`...) để không phụ thuộc ID identity của từng database. Khi nạp vào môi trường test, nạp theo thứ tự: danh mục HR → nhân viên → hợp đồng/nghỉ/thưởng → danh mục Kho → lô và chứng từ Kho → BOM/kế hoạch → lệnh và dữ liệu Sản xuất. Người nạp phải dùng migration hiện tại và tài khoản có permission tương ứng; không bỏ qua service nếu đang kiểm thử rule nghiệp vụ, idempotency hoặc ledger.

Các file có hậu tố `_code` là khóa tham chiếu giữa các CSV. File `manifest.json` ghi số dòng của từng file và cờ dữ liệu tổng hợp để pipeline test kiểm tra trước khi nạp.

## Kiểm tra nhanh

```powershell
python data/generate_large_dataset.py
python data/validate_large_dataset.py
```

Script kiểm tra số lượng tối thiểu, mã duy nhất, khóa tham chiếu và các ngày/số lượng cơ bản. Đây là fixture kiểm thử tải; dữ liệu production phải được tạo qua quy trình nghiệp vụ và được phê duyệt riêng.
