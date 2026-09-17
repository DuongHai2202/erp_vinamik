# Hướng thiết kế giao diện Vinamik

## Chẩn đoán nguyên nhân gốc

Các lỗi mất góc, lệch hàng và tràn màn hình đến từ bốn nguyên nhân cùng lúc:

1. Shell của ứng dụng chưa khóa chiều cao theo viewport. Header, sidebar và nội dung đều có thể tạo ra một vùng cuộn riêng, trong khi modal/drawer lại nằm trong các ancestor có `overflow` khác nhau.
2. Header và thanh tìm kiếm đang chuẩn hóa kích thước bằng selector lồng sâu vào DOM của Ant Design. `Input.Search` thực tế gồm nhiều lớp wrapper; ép chiều cao và border cho từng lớp tạo ra góc vuông, viền đôi và trạng thái focus bị lệch.
3. `Card`, `Table`, toolbar và modal đều tự đặt giới hạn kích thước. Khi viewport hẹp hoặc form dài, giới hạn của lớp trong và lớp ngoài xung đột.
4. Ba module đang dùng cùng một composition nên việc đổi một selector chung dễ làm thay đổi cả HR, kho và sản xuất.

## Quy tắc nền tảng

- App shell chiếm `100dvh`; sidebar và header cố định trong shell; chỉ `.app_content` được cuộn.
- Modal và drawer bị giới hạn theo viewport, có header/footer cố định và body cuộn riêng.
- Mọi control trong cùng một hàng dùng một primitive `control_row`: chiều cao 40px, border và focus ring do control ngoài quản lý.
- Bảng lớn dùng phân trang server, virtual rows và scroll cục bộ. Không để table đẩy chiều rộng của document.
- Chuyển cảnh chỉ dùng opacity/transform ngắn; không dùng animation layout hoặc hiệu ứng nền nặng.
- Màu nền và accent lấy từ bộ token Vinamik xanh đậm/xanh cobalt; khác biệt module đến từ nhịp bố cục và dấu hiệu trạng thái, không dùng ba bảng màu rời rạc.

## Ba module, một ngôn ngữ tương tác

### Quản lý nhân sự — directory

Trọng tâm là nhận diện người: tiêu đề có dấu hiệu hồ sơ, bảng ưu tiên mã nhân viên/tên/phòng ban, chi tiết mở bằng drawer. Thanh công cụ giữ nhịp ngang thoáng và dùng trạng thái làm bộ lọc chính. Module dùng bo mềm vừa phải và điểm nhấn tròn ở biểu tượng.

### Kho và nguyên vật liệu — ledger

Trọng tâm là dòng giao dịch: toolbar nằm trong một dải nền phẳng, bảng ưu tiên mã chứng từ/kho/trạng thái, các thao tác ghi sổ nằm ở cuối hàng. Module dùng góc gọn và đường kẻ rõ để người dùng quét nhanh số lượng lớn.

### Quản lý sản xuất — flow

Trọng tâm là tiến trình lệnh: trước bảng có dải trạng thái `Đã lập kế hoạch → Đã phát hành → Đang thực hiện → Đã hoàn thành`; bảng ưu tiên lệnh, thành phẩm, số lượng và hạn. Module dùng nhịp dọc và marker trạng thái, không dùng viền màu dày bao quanh card.

Ba module vẫn dùng chung pagination, preset cột, drawer chi tiết, permission state và thông báo. Chỉ composition, nhịp khoảng trắng, marker và hover state thay đổi.

## Hiệu suất

Không thêm thư viện animation hoặc hình nền động. Các bảng 200–300 cột phải dùng preset cột, scroll ngang của table, virtual rows và memoized columns. Tìm kiếm chỉ gửi request khi người dùng submit hoặc dừng gõ theo debounce; dữ liệu nghiệp vụ không lưu riêng trên từng máy.


## Cách đánh giá

Hướng này được đối chiếu bằng playbook Impeccable trong .agent/skills/impeccable: rà soát context, concept seed, layout/typography detector và UX review sau chỉnh sửa. Detector source hiện không còn phát hiện các anti-pattern đã nêu; cảnh báo transition: height còn lại chỉ xuất hiện ở CSS runtime của thư viện Ant Design và không có rule tương ứng trong source dự án.

## Quy tắc khung tìm kiếm

Ant Design render ô tìm kiếm thành ant-input-group → ant-input-affix-wrapper + ant-input-group-addon. Không đặt border cho cả hai lớp. Lớp group là khung duy nhất; affix wrapper co giãn với flex: 1 1 auto, addon là item cố định 32px và nút bên trong chiếm 32px. Quy tắc này ngăn nút tràn sang bộ lọc kế bên khi chiều rộng thay đổi.
