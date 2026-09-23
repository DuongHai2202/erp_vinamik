# Rà soát chất lượng dữ liệu fixture

Ngày rà soát: 23/09/2026

## Kết quả

Bộ dữ liệu trong data/ được sinh lại từ seed 20260916 sau khi sửa quan hệ phiếu xuất. Bộ kiểm tra py -3 data/validate_large_dataset.py trả về valid: true.

| Nhóm | Bản ghi |
|---|---:|
| Nhân sự | 600 nhân viên, 605 hợp đồng, 901 yêu cầu nghỉ, 421 thưởng/kỷ luật, 12 kỳ lương |
| Kho và nguyên vật liệu | 520 mặt hàng, 30 nhà cung cấp, 6 kho, 60 vị trí, 1.180 lô, 953 phiếu nhập, 423 phiếu xuất, 253 điều chuyển, 6 phiên kiểm kê |
| Sản xuất | 250 kế hoạch, 200 BOM, 600 lệnh, 3.000 nhu cầu vật tư, 720 phân công, 720 tiêu hao, 600 sản lượng |
| Sổ kho | 1.120 movement và 846 balance |
| Chất lượng và giá thành | 12 quy tắc, 12 kỳ, 125 biên bản kiểm tra, 100 phiếu không phù hợp, 125 phép tính giá thành, 125 đề xuất giá bán |

## Các kiểm tra đã khóa

- Mã nghiệp vụ bắt buộc, duy nhất và số dòng khớp manifest.json.
- Điện thoại Việt Nam dạng canonical 10 chữ số và cột hiển thị nhóm 4-3-3.
- Ngày lưu ISO yyyy-mm-dd, ngày hiển thị dd/mm/yyyy, thời điểm có múi giờ.
- Tuổi nhân viên tại ngày vào làm, thời hạn hợp đồng, ngày nghỉ và kỳ lương.
- Toàn bộ khóa tham chiếu giữa HR, Kho và Sản xuất.
- Vị trí của dòng nhập/xuất/điều chuyển phải thuộc đúng kho trên chứng từ.
- Sổ movement phải đối chiếu với balance, không âm tồn.
- Lệnh phải khớp dòng kế hoạch và BOM; nhu cầu vật tư khớp công thức định mức; sản lượng không vượt mục tiêu.
- Quality/cost phải khớp mã lệnh, mã vật tư và lô; số lượng đạt/lỗi không vượt số lượng kiểm tra; tổng giá thành và giá thành đơn vị khớp công thức; giá đề xuất khớp biên lợi nhuận.
- Không còn từ ngữ hiển thị kiểu demo, fake, mock, placeholder, sample, mô phỏng hoặc tải lớn trong các trường nghiệp vụ chính.

## Sửa trong lần rà soát

- Bổ sung phủ trạng thái vòng đời cho HR, Kho, Sản xuất và Quality/Cost; thêm bản ghi trạng thái riêng để đồng bộ được vào database đã có mà không sửa ngược chứng từ lịch sử. Phiếu Kho chỉ tạo movement khi ở trạng thái đã ghi sổ; phiên kiểm kê được tạo qua API để chụp số dư và đi qua counting → submitted → approved → posted.
- Sửa generator để hai dòng của một phiếu xuất luôn lấy vị trí trong cùng kho với header. Trước đây dòng thứ hai có thể trỏ sang kho khác.
- Thay tên nhà cung cấp dạng lặp bằng tên pháp nhân tổng hợp có cấu trúc doanh nghiệp Việt Nam.
- Làm lại ghi chú nghiệp vụ cho hồ sơ, hợp đồng, kho và sản xuất để mô tả quy trình thực tế hơn.
- Bổ sung kiểm tra reconciliation movement/balance, location-to-warehouse, BOM formula và production yield vào validator hiện có.
- Tách chuẩn hóa mã và chuẩn hóa tên ở service master data: mã vẫn dùng lower_snake_case, còn tên/địa chỉ tiếng Việt giữ đúng hoa/thường và dấu.
- Đồng bộ lại master data qua API bằng data/sync_master_data_via_api.py: 20 phòng ban, 30 chức danh, 3 ca, 3 đơn vị tính, 9 nhóm hàng, 30 nhà cung cấp, 6 kho và 60 vị trí; tổng 161 bản ghi cập nhật, không bypass service hoặc audit.
- Bổ sung quality_cost bằng data/load_quality_cost_via_api.py: đã tạo 475 bản ghi mới qua service, 24 bản ghi quy tắc/kỳ đã có từ lần chạy đầu được giữ nguyên; không xóa dữ liệu cũ.
- Bổ sung 605 hợp đồng gồm 6 bản nháp có thể mở/sửa/xóa và 5 hợp đồng active có ngày hết hạn từ 30/09/2026 đến 08/10/2026; tại ngày rà soát 23/09/2026, các bản ghi này nằm trong cửa sổ cảnh báo 0–15 ngày.
- Môi trường local đang bật bootstrapper hợp đồng qua `ERP_FIXTURE_CONTRACT_ENABLED=true`: lần khởi động đầu đã tạo 4 bản nháp còn thiếu và đồng bộ 5 ngày hết hạn qua service/audit; các lần sau chạy idempotent.

## Ranh giới dữ liệu

Tên nhân sự, số điện thoại, email và giao dịch là dữ liệu tổng hợp có tính thực tế, không phải dữ liệu cá nhân hoặc giao dịch nội bộ thật của Vinamilk. Catalog sản phẩm chỉ dùng nguồn công khai. Miền email .test được giữ để không thể gửi nhầm thư ra ngoài; không dùng dữ liệu này để liên hệ người thật.

CSV là đầu vào nạp môi trường test. PostgreSQL/backend vẫn là nguồn sự thật khi chạy ứng dụng. Không tự xóa dữ liệu đang có trong database để thay fixture; nếu cần dựng lại database test, phải thực hiện qua migration và quy trình nạp có kiểm soát.

## Cách chạy lại

    py -3 data/generate_large_dataset.py
    py -3 data/validate_large_dataset.py
    $env:ERP_ADMIN_USERNAME="admin"
    $env:ERP_ADMIN_PASSWORD="<secret>"
    py -3 data/sync_master_data_via_api.py
    $env:ERP_ADMIN_PASSWORD="<secret>"
    py -3 data/load_quality_cost_via_api.py
