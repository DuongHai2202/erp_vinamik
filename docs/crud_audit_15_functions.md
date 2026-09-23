# audit crud 15 functions

## pham vi va ket luan

Audit này được thực hiện trước khi bổ sung mã nguồn cho đúng 15 chức năng đã chốt trong `task/README.md`:

- Nhân sự: hồ sơ nhân viên, hợp đồng lao động, nghỉ phép/vắng mặt, khen thưởng/kỷ luật, tính lương.
- Kho và nguyên vật liệu: danh mục nguyên vật liệu, nhập kho, xuất kho, kiểm kê kho, điều chuyển kho.
- Sản xuất: kế hoạch sản xuất, nguyên vật liệu/định mức, lệnh sản xuất, phân công nhân sự, thành phẩm.

Không tính đăng nhập/phân quyền vào 15 chức năng. Chất lượng và giá thành, dữ liệu và báo cáo vẫn ở backlog.

Kết luận: backend đã có các vertical slice chính nhưng chưa có CRUD hoàn chỉnh cho cả 15 màn hình; frontend hiện có bốn trang custom cho nhân viên, hợp đồng, danh mục vật tư và kế hoạch, cùng một renderer generic bao phủ 11 màn hình còn lại. Không thể thêm nút `xóa` đồng loạt cho mọi bảng vì dữ liệu ERP có ledger, snapshot lương, lô và lịch sử duyệt. `delete` phải được chuẩn hóa thành một trong các hành vi có kiểm soát:

1. `hard_delete_draft`: chỉ bản nháp chưa phát sinh tham chiếu hoặc movement, có quyền riêng và audit.
2. `soft_deactivate`: dùng cho master data đã hoặc có thể được tham chiếu.
3. `cancel`: dùng cho chứng từ/yêu cầu có vòng đời; giữ bản ghi, actor, thời điểm và lý do.
4. Không có delete/update sau `posted`, `received`, `locked`, `completed` hoặc sau khi đã được snapshot tham chiếu; sai số phải đi qua giao dịch bù trừ hoặc version mới.

## baseline da kiem tra

### backend

- Controller hiện có ở `ERP_Backend/src/main/java/vn/vinamik/erp_backend/human_resources`, `inventory` và `production`.
- API contract hiện hành nằm ở `docs/backend_api_contract.md`.
- State machine và quy tắc dữ liệu nằm trong các task module và `docs/database_design.md`.
- Permission seed hiện chưa có quyền delete riêng cho phần lớn chứng từ; chỉ có `production_plan_delete`, còn master dùng hậu tố `deactivate`.

### frontend

- Route 15 màn hình nằm trong `ERP_Client/src/main.jsx`.
- Hồ sơ nhân viên, hợp đồng, danh mục vật tư và kế hoạch dùng page custom.
- 11 màn hình còn lại dùng `ERP_Client/src/platform/layout/module_data_page.jsx` và `workflow_detail_drawer.jsx`.
- Renderer generic hiện chỉ tạo/sửa khi `create_schema` và `update_permission` được khai báo; không có delete renderer chung.
- Các task UI `TODO` vẫn còn cho nghỉ phép, thưởng/kỷ luật, payroll, bốn màn hình chứng từ Kho và hầu hết màn hình Sản xuất dù code khung đã tồn tại.

## ma tran 15 chuc nang

### nhan su

| # | Chức năng | Backend hiện có | Frontend hiện có | Thiếu hoặc sai | Cách hoàn thiện đúng nghiệp vụ |
|---|---|---|---|---|---|
| HR-01 | Hồ sơ nhân viên | `GET`, `POST`, `PUT`, `DELETE` mềm tại `human_resources_employee_controller`; `DELETE` gọi deactivate. Có validate mã, tuổi, phone, phòng ban, chức danh và self-reference. | `human_resources_page.jsx` có danh sách, tạo, sửa, ngừng sử dụng và quyền read/create/update/deactivate. | Không có khôi phục hồ sơ inactive; không phải thiếu CRUD vì hồ sơ đã tham chiếu không được hard delete. Cần kiểm tra thêm trạng thái `terminated` và ngày nghỉ trong form. | Giữ `DELETE` mang nghĩa `soft_deactivate`; thêm read-only detail đầy đủ, lọc inactive/terminated và test 401/403/409. Chỉ thêm restore nếu nghiệp vụ xác nhận rõ quyền và state `inactive -> active`. |
| HR-02 | Hợp đồng lao động và cảnh báo 15 ngày | `GET`, `POST`, `PUT` bản nháp và `POST /status`; state `draft -> active/cancelled`, `active -> terminated/expired`. Truy vấn `expiring_only` dùng ngày server. | `contracts_page.jsx` có tạo, sửa bản nháp, duyệt, đóng và hủy bản nháp; có nhãn cảnh báo. | Không có hard delete là đúng. UI chưa có thao tác chuyển `active -> expired`; việc tự chuyển expired chưa được mô tả là job nên không được tự thêm. Cần test boundary 0/15/16 ngày và response detail. | Giữ `cancel`/`terminate` thay cho xóa. Bổ sung filter/chi tiết cảnh báo và chỉ thêm action expired hoặc scheduler sau khi chốt nghiệp vụ. |
| HR-03 | Nghỉ phép/vắng mặt | `GET`, `POST`, `PUT` với `draft/pending`, `POST /decision`, `POST /cancel`. Backend chặn ngày sai, overlap approved và ghi approver/time. | `module_data_page.jsx` có form, sửa draft/pending, duyệt, từ chối, hủy. | **Sai state ở UI:** action hủy đang cho `approved`, trong khi service chỉ cho hủy `draft/pending`. Task UI còn đánh dấu TODO. Chưa có filter ngày/nhân viên dù API hỗ trợ. | Sửa action chỉ còn `draft/pending`; bổ sung bộ lọc kỳ/ngày và drawer chi tiết. `cancel` là delete nghiệp vụ, không hard delete. Test create/update/approve/reject/cancel theo từng state và role. |
| HR-04 | Khen thưởng/kỷ luật | `GET`, `POST`, `PUT` chỉ draft, `POST /submit`, `POST /decision`. Service có enum `cancelled` nhưng không có transition cancel; record payroll-locked bị khóa sửa. | Generic page có tạo, sửa draft, gửi duyệt, duyệt/từ chối. Status filter lại hiển thị `cancelled`. | **Thiếu backend và UI cancel/delete.** Status `cancelled` đang là trạng thái khai báo nhưng không thể đi tới. Task yêu cầu quản lý form nháp và task hiện chưa hoàn tất. | Thêm `POST /{id}/cancel` cho draft (có thể cho pending nếu nghiệp vụ cho phép rút đơn), audit actor/time, permission cancel hoặc update được chốt. Nếu cần hard delete, chỉ cho draft chưa gắn payroll. Sửa status options theo state thật và thêm form/detail test. |
| HR-05 | Tính lương | `GET` kỳ/dòng, `POST` tạo kỳ, calculate, approve, reject, lock. Snapshot locked bất biến, công thức `monthly_mon_sat_v1` đã khóa. | Generic page có tạo kỳ, calculate/approve/reject/lock và drawer xem dòng lương. | Không có edit/delete là **đúng** sau calculate/approve/lock, nhưng kỳ `draft` chưa có hành vi xóa/void. UI task vẫn TODO; chưa có giải thích rõ vì sao kỳ locked chỉ đọc. | Thêm `DELETE /periods/{id}` chỉ cho `draft` chưa có snapshot hoặc thêm `cancelled` nếu nhóm chốt cần giữ lịch sử. Không cho sửa số ngày công/công thức sau calculate; bổ sung bảng giải thích base/unpaid/reward/discipline/net và test HTTP state matrix. |

### kho va nguyen vat lieu

| # | Chức năng | Backend hiện có | Frontend hiện có | Thiếu hoặc sai | Cách hoàn thiện đúng nghiệp vụ |
|---|---|---|---|---|---|
| INV-01 | Danh mục nguyên vật liệu | `GET`, `POST`, `PUT`, `DELETE` mềm tại `inventory_stock_item_controller`; item type bất biến, item đã tham chiếu không hard delete. | `inventory_page.jsx` có tìm kiếm, lọc loại/trạng thái, số dư đọc từ balance, tạo/sửa/ngừng sử dụng. | Về CRUD master đã đủ. Cần phân biệt rõ số dư đọc từ Kho với trường được sửa; thiếu khả năng khôi phục inactive nếu chưa có policy. | Giữ soft deactivate; thêm detail/audit và chỉ cho phép activate lại khi không vi phạm trạng thái. Không thêm `DELETE` cứng. Test duplicate code, inactive reference và role read-only. |
| INV-02 | Phiếu nhập kho | Chỉ `GET`, `GET /{id}`, `POST` tạo draft và `POST /post`; post ghi ledger/số dư idempotent. Không có update/delete/cancel. | Generic page có form nhiều dòng, detail và ghi sổ. Không có sửa/xóa/hủy. | **Thiếu backend draft update và delete/cancel; thiếu UI tương ứng.** Refresh/detail có thể xem nhưng không thể sửa bản nháp. | Thêm `PUT /{id}` chỉ `draft`/trạng thái đã chốt, replace header + lines trong transaction; thêm `DELETE /{id}` chỉ draft chưa post hoặc `POST /cancel` cho draft/pending theo quyết định nghiệp vụ. Thêm permission `inventory_receipt_update`, `inventory_receipt_delete`/`cancel`, audit và test retry/rollback. Tuyệt đối không sửa/xóa `posted`. |
| INV-03 | Phiếu xuất kho | Chỉ `GET`, `GET /{id}`, `POST` tạo draft và `POST /post`; post khóa balance và từ chối âm kho. Không có update/delete/cancel. | Generic page có form dòng, hint tồn khả dụng và ghi sổ. Không có sửa/xóa/hủy. | **Thiếu backend draft update và delete/cancel; thiếu UI.** Cần giữ nguồn yêu cầu và idempotency key khi sửa. | Thêm update draft, hard delete draft hoặc cancel pending, khóa balance chỉ khi post. Validate lại tồn tại item/location/lot ở update; không cho sửa posted. Bổ sung integration test concurrent post, update sau post bị từ chối và quyền read-only. |
| INV-04 | Kiểm kê kho | `POST` tạo session/counting snapshot, count line, submit, approve, post adjustment, cancel draft/counting/submitted. Không cần hard delete sau khi bắt đầu. | Generic list + detail drawer có nhập số đếm, submit/approve/post/cancel. | **Sai state ở UI:** action submit đang hiện cả `draft`, trong khi service chỉ nhận `counting`. Header chưa có edit, nhưng line count đã là update nghiệp vụ. Task UI còn TODO. | Sửa submit chỉ `counting`; nếu cần sửa ghi chú/phạm vi chỉ cho `counting` qua `PUT`. Dùng `cancel` thay delete trước post; posted read-only. Kiểm thử snapshot, thiếu line, chênh lệch âm/dương và retry. |
| INV-05 | Điều chuyển kho | `GET`, `GET /{id}`, `POST` tạo draft và `POST /post`; post tạo movement âm/dương atomically. Không có update/delete/cancel. | Generic page có form nhiều dòng, hint tồn nguồn và ghi sổ. Không có sửa/xóa/hủy. | **Thiếu backend draft update và delete/cancel; thiếu UI.** Cần kiểm tra lại source/destination location sau khi sửa. | Thêm update draft; delete draft hoặc cancel pending; giữ `source != destination`, khóa balance khi post, không sửa posted. Thêm permission/action riêng và integration test rollback hai đầu. |

### san xuat

| # | Chức năng | Backend hiện có | Frontend hiện có | Thiếu hoặc sai | Cách hoàn thiện đúng nghiệp vụ |
|---|---|---|---|---|---|
| PROD-01 | Kế hoạch sản xuất | `GET`, `POST`, `PUT` draft, `POST /status`, `DELETE` draft chưa bị order tham chiếu. | `production_page.jsx` có tạo/sửa draft, chuyển trạng thái và xóa draft. | UI chỉ đi theo `draft -> approved -> released -> completed`, chưa hiển thị action hủy dù backend cho phép hủy trước hoàn tất. Cần kiểm tra quyền/confirmation và detail line. | Giữ hard delete draft; thêm action `cancel` cho state backend cho phép và giữ xóa riêng cho draft. Không xóa kế hoạch đã có order. Test line snapshot và ràng buộc tham chiếu. |
| PROD-02 | Nguyên vật liệu và định mức BOM | Inventory sở hữu master; Production có `GET/POST/PUT` draft BOM và `POST /status` active/inactive. BOM active version bất biến; order lưu snapshot. Nhu cầu, consumption đọc/ghi qua contract Kho. | Generic page có tạo/sửa draft BOM, activate/deactivate và drawer dòng. Order drawer hiển thị material needs/consumption. | Không có hard delete BOM; đây là đúng với version/history nhưng chưa có nút “tạo version mới” rõ ràng. Trang materials chưa có màn hình tra cứu item, nhu cầu theo lệnh và lịch sử cấp/tiêu hao như task UI yêu cầu. | Dùng `inactive` làm soft delete; thêm draft-only delete nếu nhóm thực sự cần. Tách tab `master_lookup`, `bom_versions`, `material_needs`, `consumption_history`; mọi sửa item chuyển về Inventory contract. Không tạo bảng item thứ hai trong Production. |
| PROD-03 | Lệnh sản xuất và tiến độ | `GET`, `POST`, `PUT` draft/planned, release, operational status, complete, cancel, progress read, material needs/consumption. Core fields khóa sau khi lệnh chạy. | Generic page có tạo/sửa draft/planned, release, pause/resume, complete/cancel; drawer có progress, needs, consumption, output. | **Sai state:** UI cho hủy khi `in_progress` nhưng service transition hiện không cho `in_progress -> cancelled`; sẽ trả lỗi. UI chưa có form event tiến độ độc lập, nhưng backend thiết kế tiến độ từ status/output nên không được tự tạo module mới. | Chọn một policy: bỏ hủy ở `in_progress` khỏi UI hoặc bổ sung backend compensation/cancel rule có kiểm tra movement. Giữ cancel là delete nghiệp vụ, không hard delete. Thêm progress timeline/filter và test state matrix trước khi sửa UI. |
| PROD-04 | Phân công nhân sự | `GET` filter order/employee/status/time, `POST`, `PUT` planned, `POST /status` planned/active/completed/cancelled; HR lookup tối thiểu. | Generic page có tạo/sửa planned, start/complete/cancel. | UI search text đang disabled (`search_param: null`), chưa có filter nhân viên/lệnh/khoảng thời gian; thiếu read aggregation/calendar theo task monitoring. Không hard delete khi assignment đã active/completed. | Bổ sung filter lookup và view lịch ngày/tuần/tháng, tổng hợp assigned/active/completed. Dùng cancel planned/active theo state; nếu cần xóa chỉ hard delete planned chưa ghi nhận. Test overlap đồng thời và 403 khi thiếu quyền. |
| PROD-05 | Thành phẩm và sản lượng | `GET outputs`, `POST output` draft/pending_receipt, `POST output/{id}/post`; chỉ good quantity gửi Inventory, idempotency theo lot/key. Không có update/delete output. | Màn hình dùng danh sách orders + drawer; có ghi nhận output và ghi sổ sang Kho. | **Thiếu CRUD output và UI riêng:** không có danh sách sản phẩm/lô/hạn dùng độc lập, không sửa output draft, không hủy/xóa draft/pending, không xem liên kết receipt đầy đủ. | Thêm `GET` danh sách output có phân trang/filter, `PUT` chỉ draft, `DELETE` draft hoặc `POST /cancel` pending chưa nhận, permission riêng; received immutable. Xây tab thành phẩm/lô, bảng good/defective và receipt reference. Không tự sửa tồn Kho hay quyết định QC. |

## loi lien ket can sua truoc khi them crud

Các lỗi sau đã thấy trực tiếp từ mã nguồn, không phải giả định thiết kế:

1. `ERP_Client/src/platform/layout/module_data_page.jsx`: action hủy nghỉ phép khai báo cả `approved`, nhưng `human_resources_absence_service` chỉ cho hủy `draft/pending`.
2. Cùng file: action gửi duyệt kiểm kê khai báo cả `draft`, nhưng `inventory_stocktake_service.submit` chỉ nhận `counting`.
3. Cùng file: action hủy lệnh sản xuất khai báo `in_progress`, nhưng `production_order_service.allowed_transition` hiện chỉ nhận `paused/completed` từ `in_progress`.
4. `ERP_Client/src/modules/production/production_page.jsx`: không có action hủy kế hoạch dù backend có transition `draft/approved/released -> cancelled`; xóa draft và hủy là hai hành vi khác nhau.
5. `feature_catalog` hiển thị trạng thái `cancelled` cho thưởng/kỷ luật nhưng controller/service chưa có endpoint chuyển sang `cancelled`.
6. Các config `receipts`, `issues`, `transfers` chỉ khai báo `create_schema` và `post`; generic renderer vì vậy không thể mở form sửa hoặc xóa bản nháp dù người dùng yêu cầu CRUD.
7. `assignments` có API filter theo employee/order/time nhưng config frontend đặt `search_param: null`, nên người dùng không lọc được theo các trường task đã nghiệm thu.
8. `finished_products` dùng endpoint danh sách orders và chỉ mở form output trong drawer; đây chưa phải màn hình thành phẩm/lô đầy đủ theo `PROD-21`.

## phan quyen can chot truoc khi code

Không dùng một quyền tổng cho mọi thao tác. Giữ read/write tách biệt và thêm quyền tối thiểu cho các thao tác mới:

| Nhóm | Quyền đã có | Quyền cần xem xét |
|---|---|---|
| HR reward | `hr_reward_read/create/update/approve` | `hr_reward_cancel` hoặc quy ước cancel dùng `hr_reward_update`; nếu cho hard delete draft thì `hr_reward_delete` |
| HR payroll | `hr_payroll_read/create/update/approve` | `hr_payroll_delete` chỉ cho kỳ draft chưa tính, nếu nhóm chọn hard delete |
| Inventory receipt/issue/transfer | `*_read/create/post` | `*_update`, `*_cancel`, `*_delete` theo quyết định draft-only |
| Inventory stocktake | `*_read/create/adjust` | `inventory_stocktake_update` chỉ cho header/count nếu tách quyền; cancel có thể giữ ở create theo contract hiện tại |
| Production BOM | `production_bom_read/create/update/approve` | `production_bom_delete` chỉ draft nếu cần hard delete; active/inactive vẫn dùng approve |
| Production output | `production_output_read/create/post` | `production_output_update`, `production_output_cancel`/`delete` cho output chưa nhận |

Mọi quyền mới phải có migration idempotent, gắn role seed least privilege, kiểm tra `@PreAuthorize`, audit event và test 401/403. Super admin vẫn được bảo vệ theo rule hiện có.

## ke hoach trien khai sau audit

### pha 1 — khóa hợp đồng và state machine

1. Chốt bảng mapping `create/read/update/delete` ở trên thành API contract.
2. Sửa ba mismatch state đã nêu trước khi thêm endpoint mới.
3. Chốt delete semantics cho từng nhóm: draft hard delete, soft deactivate hay cancel.
4. Chốt permission code và migration seed; không dùng quyền write của module khác.

### pha 2 — backend theo chủ sở hữu dữ liệu

1. HR: reward cancel/delete draft, payroll draft delete nếu được duyệt; kiểm tra contract expiry không tự đổi state ngoài policy.
2. Inventory: repository JPA cho update/delete/cancel receipt, issue, transfer; stocktake header update nếu cần; giữ post transaction và append-only ledger.
3. Production: plan cancel UI/backend contract, BOM draft delete/version creation, order cancellation rule, assignment monitoring query, output CRUD trước khi post.
4. Tất cả truy vấn mới đặt trong repository JPA của module; không dùng JdbcTemplate/JDBC trực tiếp.
5. Cập nhật `docs/backend_api_contract.md` và task tương ứng sau mỗi vertical slice.

### pha 3 — frontend theo component dùng chung

1. Mở rộng `module_data_page.jsx` để hỗ trợ `update_schema`, `delete_action`/`cancel_action`, edit detail và action theo state thay vì hard-code từng bảng.
2. Dùng `RecordActionBar`: `Mở`, `Chỉnh sửa`, `Xóa bản nháp`, `Hủy`, workflow; không hiển thị action nếu thiếu permission hoặc state không hợp lệ.
3. Tách form nhiều dòng thành editor có thêm/xóa dòng, giữ giá trị khi lỗi và gửi idempotency key ổn định.
4. Bổ sung filter chuyên biệt cho absences, stock documents, assignments và output; giữ server pagination, abort request cũ và URL state.
5. Tạo trang thành phẩm/lô và lịch phân công; không biến progress/needs thành menu cấp một mới.

### pha 4 — test và nghiệm thu

Mỗi chức năng phải có:

- happy path create/read/update/delete-or-cancel;
- state matrix hợp lệ và state bị từ chối;
- role read-only nhận 403 cho mọi mutate;
- duplicate code/idempotency/concurrent request;
- audit actor/time và response message tiếng Anh;
- UI loading/empty/error/success, field validation và refresh giữ đúng dữ liệu;
- integration PostgreSQL đối soát ledger, snapshot và cross-module contract.

Không đánh dấu hoàn tất chỉ vì build frontend hoặc compile backend. Chỉ đóng chức năng khi API, UI, permission, migration, test và walkthrough role đều đạt.

## thu tu uu tien de bat dau code

1. Sửa mismatch state và test hồi quy.
2. Hoàn thiện receipt/issue/transfer draft update + cancel/delete vì đây là ba thiếu hụt CRUD lớn nhất của Kho.
3. Hoàn thiện output CRUD và màn hình thành phẩm/lô.
4. Hoàn thiện reward cancel, payroll draft policy, BOM version/delete semantics.
5. Bổ sung assignment filters/calendar và plan cancel UI.
6. Bổ sung test ma trận HTTP và cập nhật tài liệu/API contract.

Audit này là baseline nghiệp vụ; phần triển khai bên dưới ghi nhận trạng thái hiện tại để tránh thêm nút xóa làm hỏng ledger, snapshot lương hoặc lịch sử ERP.


## trang thai trien khai

Đợt triển khai CRUD hiện tại đã hoàn thành các vertical slice sau:

- HR: khen thưởng/kỷ luật có cancel cho draft/pending và delete draft có kiểm tra payroll lock; kỳ lương có delete draft trước khi tính. Hồ sơ nhân viên, hợp đồng và nghỉ phép giữ semantics soft deactivate/cancel theo state machine.
- Inventory: receipt, issue và transfer có PUT cho draft/pending, cancel trước post và DELETE draft; posted chứng từ vẫn bất biến. Các thao tác đều đi qua repository JPA/native query executor của module, có audit và permission riêng.
- Production: kế hoạch đã có cancel UI; BOM có delete draft; output thành phẩm có PUT draft, cancel chưa nhận, DELETE draft và post qua Inventory contract. Lệnh sản xuất dùng cancel thay hard delete sau khi tạo planned; phân công dùng cancel theo lifecycle.
- Frontend generic workflow renderer đã hiển thị action theo permission và state; payroll/BOM/receipt/issue/transfer/reward đã có action delete draft. Drawer sản lượng hỗ trợ tạo/sửa/xóa/hủy/ghi sổ.
- Ba mismatch state đã khóa: hủy nghỉ phép chỉ draft/pending, gửi duyệt kiểm kê chỉ counting, hủy lệnh không hiển thị ở in_progress.

Các hạng mục chưa coi là hoàn tất trong đợt này: trang output độc lập có filter lot/receipt, bộ lọc nâng cao và lịch của phân công, chỉnh header phiên kiểm kê, cùng test integration đầy đủ với PostgreSQL cho từng ma trận role/state. Chúng là bước tiếp theo, không thay đổi nguyên tắc ledger append-only, snapshot bất biến và cross-module contract.