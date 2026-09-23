# Phạm vi quản lý giá thành

Trạng thái: IN PROGRESS (MVP). Backend/client đã có khung năm chức năng và state machine; dữ liệu nguồn, snapshot chi phí và quyết định định giá tiếp tục được rà soát trước khi nạp dữ liệu nghiệp vụ.

## 1. Vị trí trong phân hệ

Tên hiển thị trên giao diện có thể là `Quản lý chất lượng`; mã kỹ thuật vẫn là `quality_cost`. Phân hệ này giữ đúng năm nhóm chức năng:

1. Kiểm tra chất lượng thành phẩm.
2. Xử lý sản phẩm không phù hợp.
3. Tính giá thành sản phẩm.
4. Đề xuất hoặc ra quyết định điều chỉnh giá bán.
5. Phê duyệt bảng giá.

Tài liệu này mô tả chi tiết nhóm 3 và các dữ liệu mà nhóm 4, 5 sẽ dùng.

## 2. Các chức năng bắt buộc của quản lý giá thành

### C-01. Quản lý kỳ và phiên bản quy tắc giá thành

- Tạo kỳ giá thành theo tháng hoặc theo đợt sản xuất.
- Chọn phạm vi: nhà máy, dây chuyền, sản phẩm, lệnh hoặc lô.
- Chọn phiên bản quy tắc tính giá thành.
- Hiển thị trạng thái `draft`, `calculating`, `calculated`, `rejected`, `approved`, `locked`.
- Không cho hai người tính hoặc khóa cùng một kỳ đồng thời.

### C-02. Tập hợp và kiểm tra đầu vào chi phí

- Đọc lượng vật tư thực tế từ phiếu xuất Kho đã ghi sổ và dòng tiêu hao Sản xuất.
- Đọc sản lượng tốt, sản lượng lỗi và lô từ Sản xuất.
- Đọc giờ lao động thực tế và đơn giá lao động đã được duyệt từ Nhân sự.
- Đọc chi phí chung/overhead đã nhập cho kỳ.
- Kiểm tra thiếu đơn giá, sai đơn vị, sai tiền tệ, nguồn chưa ghi sổ, tiêu hao không thuộc lệnh và sản lượng tốt bằng 0.
- Chụp snapshot đầu vào; không tính trực tiếp trên dữ liệu đang thay đổi.

### C-03. Tính giá thành theo sản phẩm, lệnh và lô

Công thức tổng quát được đề xuất cho MVP:

```text
material_cost = sum(consumed_quantity * material_unit_cost)
direct_labor_cost = sum(actual_hours * labor_hourly_rate)
overhead_cost = sum(allocated_overhead)
total_cost = material_cost + direct_labor_cost + overhead_cost + approved_adjustment
unit_cost = total_cost / good_quantity
```

- Tính được tổng giá thành theo lệnh, lô, sản phẩm và kỳ.
- Hiển thị chi tiết từng dòng vật tư, lao động, overhead và điều chỉnh.
- Làm tròn theo phiên bản quy tắc; không làm tròn sớm ở từng bước nếu làm sai tổng.
- Không tính giá thành nếu còn đầu vào lỗi hoặc chưa được duyệt.

### C-04. Đối chiếu và phân tích chênh lệch

- So sánh định mức BOM với tiêu hao thực tế.
- So sánh sản lượng kế hoạch với sản lượng tốt/lỗi.
- So sánh chi phí vật tư, lao động và overhead giữa các lệnh/kỳ.
- Tính tỷ lệ hao hụt, tỷ lệ lỗi và ảnh hưởng của chúng đến đơn giá.
- Cho phép ghi nhận lý do chênh lệch; không sửa ngược chứng từ Kho hoặc Sản xuất.

### C-05. Duyệt, khóa và điều chỉnh có kiểm soát

- Gửi kết quả tính để kiểm tra.
- Duyệt hoặc từ chối kèm ghi chú.
- Khóa kỳ/kết quả đã duyệt; dữ liệu đã khóa là bất biến.
- Sai sau khi khóa phải tạo phiên bản điều chỉnh mới, không update/delete snapshot cũ.
- Lưu actor, thời điểm, phiên bản quy tắc, nguồn và audit event.

## 3. Dữ liệu phải có

### Dữ liệu thuộc module giá thành

- `cost_period`: kỳ, phạm vi, tiền tệ, trạng thái, phiên bản quy tắc, actor và thời điểm khóa.
- `cost_rule_version`: phương pháp định giá vật tư, cách phân bổ lao động/overhead, độ chính xác và ngày hiệu lực.
- `cost_source_snapshot`: loại nguồn, mã nguồn, số lượng, đơn giá, tiền, tiền tệ và thời điểm chụp.
- `cost_calculation`: kết quả theo kỳ/lệnh/lô/sản phẩm, sản lượng tốt/lỗi, tổng từng nhóm chi phí, tổng giá thành và giá thành đơn vị.
- `cost_calculation_line`: vật tư, lao động, overhead, điều chỉnh, số lượng, đơn giá, số tiền và tham chiếu nguồn.
- `overhead_pool`: nhóm chi phí chung, số tiền được duyệt, kỳ và trung tâm chi phí.
- `overhead_allocation`: cách phân bổ vào lệnh/lô, căn cứ phân bổ và tỷ lệ.
- `cost_adjustment`: điều chỉnh sau đối chiếu, lý do, người duyệt và phiên bản thay thế.
- `price_proposal` và `price_approval`: đề xuất giá bán, căn cứ giá thành, biên lợi nhuận, hiệu lực và lịch sử phê duyệt.

### Dữ liệu đọc qua hợp đồng module khác

- Kho: `stock_item`, đơn vị, lot, issue/issue_line, movement đã post và lớp định giá vật tư.
- Sản xuất: BOM snapshot, production order, material consumption, good/defective output và production lot.
- Nhân sự: employee, assignment/giờ thực tế, hợp đồng và đơn giá lao động được duyệt.
- Chất lượng: inspection result và quyết định hold/release để chỉ lượng đạt được tính là thành phẩm khả dụng.

## 4. Khoảng thiếu đã phát hiện trong dữ liệu hiện tại

Bộ CSV hiện tại qua validator kỹ thuật nhưng chưa đủ để tính giá thành chính xác:

- `inventory.receipt`, `receipt_line` và `stock_movement` mới có số lượng; chưa có đơn giá, tiền tệ, phương pháp định giá hoặc lớp giá vốn.
- `production_assignment` mới là lịch phân công dự kiến; chưa có giờ thực tế và đơn giá lao động dùng cho costing.
- Chưa có kỳ giá thành, phiên bản công thức, pool overhead và bảng phân bổ.
- Chưa có phiếu kiểm tra chất lượng, quyết định cách ly/giải phóng và quy tắc xử lý sản phẩm lỗi.
- Chưa có snapshot chi phí, kết quả tính, chênh lệch, điều chỉnh, đề xuất giá và phê duyệt giá.
- Một số fixture vẫn chứa câu mang tính kiểm thử như `Dữ liệu nhân sự tổng hợp phục vụ kiểm thử tải`, `Kế hoạch dữ liệu tải lớn` và `Định mức mô phỏng`; đây là dữ liệu tải, chưa phải dữ liệu trình diễn nghiệp vụ.
- `decision_note` của nghỉ phép và thưởng/kỷ luật còn tiếng Anh; số điện thoại nhà cung cấp đang rỗng; nhiều URL catalog sản phẩm chưa khớp tên sản phẩm.
- Email `.test` và tên nhân sự tổng hợp phải được giữ ở môi trường test để tránh dùng dữ liệu cá nhân thật; không được ghi là nhân sự thật của Vinamilk.

## 5. Chuẩn dữ liệu Việt Nam

- Database/API lưu ngày theo `yyyy-MM-dd`, timestamp ISO 8601 có múi giờ `+07:00`; client hiển thị `dd/MM/yyyy` và `dd/MM/yyyy HH:mm`.
- Số điện thoại lưu dạng 10 hoặc 11 chữ số bắt đầu bằng `0`; giao diện có thể nhóm `xxxx xxx xxx`.
- Số lượng và tiền lưu số thập phân bằng dấu chấm trong API/CSV; giao diện dùng `vi-VN` để hiển thị dấu chấm phân cách nghìn và dấu phẩy thập phân.
- Mã kỹ thuật dùng ASCII `lower_snake_case`; nhãn và ghi chú nghiệp vụ dùng tiếng Việt có dấu.
- Đơn vị kỹ thuật như `kg`, `litre`, `piece` giữ ổn định trong API; giao diện ánh xạ thành `kg`, `lít`, `cái`.
- Dữ liệu hiển thị không dùng các từ `test`, `demo`, `fake`, `mô phỏng`, `tải lớn`; nguồn fixture được ghi ở metadata/manifest thay vì ghi vào tên nghiệp vụ.

## 6. Điều kiện trước khi lập trình

Cần duyệt các quyết định sau trước khi tạo migration:

1. Giá vật tư dùng giá thực tế theo receipt, FIFO hay bình quân gia quyền.
2. Giờ công lấy từ chấm công thực tế hay từ assignment đã duyệt.
3. Cách phân bổ overhead: giờ công, giờ máy, số lượng hay tỷ lệ cố định.
4. Chi phí sản phẩm lỗi, tái chế và hao hụt được giữ ở lô nào.
5. Kỳ giá thành, tiền tệ, quy tắc làm tròn và quyền duyệt/khóa.

Nếu thiếu một trong các đầu vào trên, API phải trả lỗi rõ ràng và không tạo kết quả giá thành một phần.

## 7. Tiêu chí nghiệm thu

- Mỗi kết quả truy ngược được tới lệnh, lô, vật tư, dòng xuất, giờ công, overhead và phiên bản quy tắc.
- Cùng một snapshot và cùng phiên bản quy tắc cho cùng một kết quả khi tính lại.
- Không tính hai lần khi gửi lại request; có idempotency key.
- Không sửa/xóa được kỳ hoặc snapshot đã khóa; điều chỉnh tạo phiên bản mới.
- Người chỉ có quyền xem không thể tính, duyệt, khóa hoặc xuất dữ liệu nhạy cảm.
- Bộ fixture có đủ trạng thái nháp, đã tính, bị từ chối, đã duyệt, đã khóa và lỗi đầu vào; mọi mã tham chiếu đều hợp lệ.
