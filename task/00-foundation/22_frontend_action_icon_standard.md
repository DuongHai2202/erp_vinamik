# FND-22 — Chuẩn icon và dải thao tác bản ghi

- Trạng thái: `IN_PROGRESS`
- Phụ thuộc: `00-foundation/20-frontend-ui-ux-standard.md`, `00-foundation/21-frontend-performance-standard.md`
- Phạm vi: các bảng dữ liệu của HR, Kho và Sản xuất cùng các màn hình quản trị tài khoản.
- Mục tiêu: thay dãy nút biểu tượng rời rạc bằng một dải thao tác có ngữ nghĩa, ổn định kích thước, dùng được bằng chuột/bàn phím/chạm và không tạo layout shift.

## 1. Quyết định thiết kế

Dải thao tác nằm ở cột cuối, có chiều rộng cố định theo cấu hình bảng. Mỗi hàng bắt đầu bằng thao tác mở bản ghi. Thao tác sửa là biểu tượng riêng, còn các thao tác chuyển trạng thái hoặc phá huỷ nằm trong menu `MoreOutlined` khi có từ hai thao tác phụ trở lên. Khi chỉ có một chuyển trạng thái chính, nó được hiển thị trực tiếp cùng nhãn để người dùng nhận biết bước tiếp theo.

Không dùng biểu tượng mắt, bút chì hoặc thùng rác không có ngữ cảnh ở mọi nơi. Icon phải mô tả hành động cụ thể, còn nhãn/tooltip phải giải thích bằng tiếng Việt. Màu đỏ chỉ dành cho hành động có hậu quả phá huỷ hoặc hủy; không dùng màu đỏ để trang trí.

Cách này giữ cho ba module có cùng nhịp tương tác nhưng vẫn cho phép khác nhau ở luồng nghiệp vụ. Khác biệt của module đến từ hành động hợp lệ và trạng thái bản ghi, không đến từ việc mỗi trang tự vẽ một kiểu nút.

## 2. Bản đồ icon chuẩn

| Hành động | Icon | Hiển thị mặc định | Quy tắc |
| --- | --- | --- | --- |
| Mở/đọc hồ sơ hoặc chứng từ | `FileSearchOutlined` | trực tiếp, kèm nhãn `Mở` ở màn hình rộng | Không yêu cầu quyền ghi; mở drawer hoặc chi tiết theo ngữ cảnh |
| Sửa bản nháp/hồ sơ được phép | `FormOutlined` | trực tiếp, icon-only, tooltip `Chỉnh sửa` | Chỉ hiện khi có permission và trạng thái cho phép |
| Xóa bản nháp | `DeleteOutlined` | menu phụ, mục có `danger` | Chỉ cho bản nháp; luôn xác nhận trước khi gọi API |
| Hủy/từ chối/ngừng sử dụng | `StopOutlined` | menu phụ hoặc chuyển trạng thái trực tiếp | Không gọi là xóa; phải hiển thị hậu quả rõ ràng |
| Tạm dừng | `PauseCircleOutlined` | menu phụ hoặc trực tiếp nếu là bước chính | Chỉ có ở state machine cho phép |
| Bắt đầu/tiếp tục | `PlayCircleOutlined` | trực tiếp nếu là bước kế tiếp | Sau khi thành công cập nhật hàng hiện tại |
| Duyệt/hoàn tất/ghi sổ | `CheckCircleOutlined` | trực tiếp nếu chỉ có một bước chính | Duyệt, hoàn tất và ghi sổ phải có xác nhận khi tạo thay đổi không đảo ngược |
| Gửi/phát hành/tính | `SendOutlined` | trực tiếp hoặc menu phụ | Nhãn phải nói rõ đối tượng được gửi/phát hành |
| Khóa kỳ/khóa dữ liệu | `StopOutlined` | menu phụ | Luôn yêu cầu xác nhận và nêu rằng dữ liệu không sửa trực tiếp được |
| Thao tác khác | `MoreOutlined` | icon-only | Mở menu keyboard-accessible, không làm đổi chiều rộng cột |

`DeleteOutlined` dành riêng cho xóa bản ghi. `StopOutlined` dành cho dừng, hủy, từ chối hoặc ngừng sử dụng. Không dùng chung hai ý nghĩa này.

## 3. Hợp đồng component

File dùng chung: `ERP_Client/src/platform/layout/record_action_bar.jsx`.

```jsx
<RecordActionBar
  on_open={() => open_detail(record)}
  on_edit={() => open_edit(record)}
  can_edit={can_edit}
  edit_status_allowed={is_editable}
  workflow_actions={[
    {
      key: 'delete',
      label: 'Xóa bản nháp',
      permission: 'inventory_material_delete',
      on_click: () => delete_draft(record),
    },
  ]}
/>
```

Component không tự quyết định quyền nghiệp vụ. Trang gọi nó phải lọc permission và state bằng dữ liệu backend; component chỉ chịu trách nhiệm trình bày, gọi callback và giữ hình học ổn định. Mỗi action có tối thiểu `key`, `label` và `on_click`; `icon` chỉ truyền vào khi hành động thật sự có biểu tượng riêng ngoài bản đồ chuẩn.

`on_open` luôn là thao tác đọc. Nếu người dùng chỉ có quyền đọc, vẫn hiển thị `Mở` và không hiển thị nút sửa/xóa. Backend vẫn trả `401` hoặc `403` đúng hợp đồng dù client đã ẩn nút.

## 4. Hình học và trạng thái tương tác

- Kích thước tối thiểu của vùng bấm là 36px trong bảng desktop và không nhỏ hơn 44px khi có chế độ touch; không dùng icon 16px làm kích thước vùng bấm.
- Dải dùng `inline-flex`, gap 4px và chiều cao tối thiểu 36px. Cột thao tác dùng `fixed: right` và width ổn định theo preset.
- `Mở` là nút text + icon trên màn hình rộng để giảm việc phải nhớ icon. Ở màn hình hẹp nhãn được ẩn bằng kỹ thuật accessible name, vùng bấm vẫn giữ nguyên.
- Hover chỉ đổi nền/viền/chữ; không đổi padding, border width hoặc font weight theo cách làm hàng nhảy.
- Focus-visible có vòng focus rõ; mọi icon-only button có `aria-label` và tooltip khi cần.
- Press feedback chỉ dịch 1px trong khoảng 100–150ms. Khi `prefers-reduced-motion: reduce`, bỏ transition.
- Loading thay icon tại chỗ hoặc khóa đúng nút, không đổi độ rộng dải thao tác.
- Menu phụ dùng keyboard bằng Arrow Up/Down, Enter và Escape theo hành vi của Ant Design Dropdown.

CSS dùng token Vinamik (`--erp-accent`, `--erp-accent-soft`, `--erp-border`, `--erp-surface`, `--erp-text-soft`) tại `ERP_Client/src/styles/global.css`. Không gắn màu module trực tiếp vào action chung.

## 5. Áp dụng theo module

### Human resources

- Hồ sơ nhân viên: `Mở`, `Chỉnh sửa`, `Ngừng sử dụng`.
- Hợp đồng: `Mở`, `Chỉnh sửa` khi nháp, `Duyệt hợp đồng`, `Đóng hợp đồng`, `Hủy hợp đồng`.
- Nghỉ phép, khen thưởng/kỷ luật, kỳ lương: dùng renderer chung trong `module_data_page.jsx`; không thêm nút icon thủ công bên ngoài state machine.
- Không có thao tác xóa nhân viên hoặc kỳ lương đã khóa.

### Inventory

- Danh mục vật tư: `Mở`, `Chỉnh sửa`, `Ngừng sử dụng`.
- Phiếu nhập/xuất/chuyển: `Mở`, `Chỉnh sửa` khi nháp, `Ghi sổ`, `Hủy`, `Xóa bản nháp` nếu hợp đồng API cho phép.
- Kiểm kê: dùng `EditOutlined` cho nhập số đếm và `CheckCircleOutlined` cho đóng phiên; không dùng `DeleteOutlined` để xóa số đếm.
- Phiếu đã ghi sổ chỉ có đọc và các bước được backend cho phép; không hiển thị xóa.

### Production

- Kế hoạch: `Mở`, `Sửa bản nháp`, `Chuyển trạng thái`, `Xóa kế hoạch` khi nháp.
- Lệnh: `Mở`, `Phát hành`, `Bắt đầu`, `Tạm dừng`, `Tiếp tục`, `Hoàn tất`, `Hủy` theo state machine.
- Phân công: đọc, thêm/sửa phân công theo quyền; không cho người sản xuất đọc toàn bộ hồ sơ HR, chỉ dùng lookup mã/tên nhân viên đang làm việc.
- Nhập thành phẩm và tiêu hao vật tư phải gọi hợp đồng Kho, không tạo nút ghi sổ cục bộ trong Production.

## 6. Quy tắc xác nhận và phản hồi

Chỉ xác nhận trước thao tác không đảo ngược hoặc thay đổi sổ: xóa, ghi sổ, khóa, hủy, từ chối, đóng hợp đồng và kết thúc lệnh. Nội dung xác nhận phải có mã bản ghi, trạng thái hiện tại và hậu quả.

```text
Xóa bản nháp phiếu nhập?
PN-00042 · Kho nguyên liệu · 12 dòng
Bản nháp sẽ bị xóa và không thể khôi phục.
```

Sau thành công, thông báo cho client dùng tiếng Anh theo quy ước dự án, ví dụ `The draft was deleted successfully.`; nhãn giao diện và nội dung xác nhận vẫn là tiếng Việt. Cập nhật hàng hoặc projection hiện tại thay vì tải lại cả trang. Sau lỗi, giữ bộ lọc và giá trị form, hiển thị lỗi cạnh trường hoặc hành động gây lỗi.

## 7. Hiệu suất và accessibility

- Không tạo một component nặng cho mỗi ô ngoài cột thao tác; `RecordActionBar` chỉ được mount cho trang đang nhìn thấy nếu bảng đã bật virtualization.
- Không gắn tooltip cho mọi ô dữ liệu; tooltip chỉ dùng cho icon-only action hoặc nội dung bị cắt.
- Callback phải dùng ID bản ghi ổn định; không truyền toàn bộ object lớn vào menu nếu chỉ cần khóa bản ghi.
- Không tải icon qua ảnh hoặc mạng; dùng icon tree-shake được từ `@ant-design/icons`.
- Kiểm tra keyboard, focus, screen reader name, tương phản và hành vi 200–300 cột theo `FND-21`.
- Không dùng màu là tín hiệu duy nhất; destructive action luôn có icon, nhãn trong menu và confirmation.

## 8. Trạng thái triển khai và phần tiếp theo

Đã áp dụng component chung cho:

- `human_resources_page.jsx`, `contracts_page.jsx`;
- `inventory_page.jsx`;
- `production_page.jsx`;
- `module_data_page.jsx` cho các danh sách workflow;
- `users_page.jsx`, `registration_requests_page.jsx`.

Đã thêm CSS token hóa cho hover, focus, active, danger, responsive và reduced motion. Phần tiếp theo cần làm theo thứ tự:

1. Chụp kiểm thử ở 1366px, 1280px, 1024px và viewport touch; kiểm tra không tràn cột action.
2. Kiểm tra từng state backend để không hiện action sai trạng thái.
3. Kiểm thử người chỉ đọc, người có quyền ghi và người có quyền quản trị.
4. Đã bổ sung loading/disabled state theo request đang chạy ở action rail dùng chung và ba trang ưu tiên; chiều rộng action rail được giữ ổn định.
5. Đo render bảng lớn trước/sau; chỉ bật virtualization hai chiều khi benchmark cho thấy cần.
6. Cập nhật ảnh baseline trong review UI/UX và đánh dấu task `DONE` khi toàn bộ tiêu chí dưới đây đạt.

## 9. Tiêu chí nghiệm thu

- Mỗi bảng ưu tiên có một dải action duy nhất, không còn dãy icon mắt/bút chì/xóa không có nhãn hoặc menu.
- Icon đọc, sửa, xóa, dừng và workflow phân biệt đúng nghĩa.
- Cột cuối không nhảy khi hover, loading hoặc đổi quyền.
- Người chỉ đọc vẫn mở được chi tiết nhưng không thấy thao tác ghi.
- Xóa và ghi sổ có xác nhận; bản ghi đã ghi sổ/đã khóa không có xóa.
- Keyboard và screen reader đọc được tên hành động; focus-visible rõ.
- `npm run lint`, `npm run build` và `git diff --check` đạt; cảnh báo bundle phải được ghi lại nếu vẫn còn.

## 10. Validation record

Lần kiểm tra gần nhất trên `ERP_Client`:

- `npm run lint`: đạt.
- `npm run build`: đạt, 3.054 module được transform; không còn cảnh báo chunk vượt 500 kB sau khi lazy-load shell/route và tách `@ant-design/icons` thành vendor riêng.
- `git diff --check`: không có lỗi whitespace; các cảnh báo còn lại chỉ là chuyển đổi line ending LF/CRLF của Git trên Windows.

Task vẫn giữ `IN_PROGRESS` cho đến khi hoàn thành kiểm thử trực quan ở bốn viewport và walkthrough keyboard của mục 8.
