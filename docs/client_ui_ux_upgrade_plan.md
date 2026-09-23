# Vinamik client UI/UX upgrade plan

## 1. Design direction

Vinamik should feel like an operations workspace. The first screen answers three questions in this order:

1. Which role and operating context am I in?
2. What needs attention now?
3. What is the next safe action I can take?

The visual language uses the Vinamilk blue from the logo as the brand anchor, quiet paper-like surfaces, thin rules, restrained elevation and generous whitespace. The interface should not be built from repeated cards, floating pills or decorative gradients. A status may use a compact label when it improves scanning, but navigation, actions and page structure use text, rules, spacing and hierarchy.

The three priority modules have distinct working patterns while sharing one geometry and component contract:

- `human_resources`: people index and employee timeline. The important relationship is person → contract → attendance/event history.
- `inventory`: stock ledger and movement flow. The important relationship is item → warehouse balance → receipt/issue/transfer history.
- `production`: execution flow and schedule. The important relationship is plan → order → material readiness → assignment → finished output.

The shell, typography, keyboard behavior, permissions and loading/error states remain common. The module variation comes from information architecture and data visualization, not from unrelated colors or different button behavior.

## 2. Behavioural rules

### Start from user needs

Design each screen around a real task for a role: operator, warehouse keeper, production planner, HR officer, manager and read-only viewer. Record the trigger, required decision, allowed action and evidence needed after completion. Do not start from a database table and expose every column as a control.

### Recognition before recall

Show the employee code, material code, order code, current status, owner and last update next to the decision. Keep the vocabulary used by the company in labels and help text. Search and lookup controls must provide suggestions and context instead of requiring a user to remember an internal code.

### Progressive disclosure

The first viewport contains the fields and actions needed for the common path. Advanced filters, audit fields and rarely used attributes live in a secondary panel or detail drawer. For records with 200–300 columns, show an essential preset first, allow a saved column preset, and place the complete record in a drawer or full detail route.

### Proximity and hierarchy

Use the same 8/16/24 spacing rhythm. Keep a label, field and its validation message together. Separate unrelated groups with whitespace and a thin rule instead of adding another card. Use one page title, one short explanation and one primary action per task context.

### Prevent errors and make recovery obvious

Disable a submit action only when the reason is visible. Preserve entered values after a server error. Show an error summary at the top and the field error beside the field. Confirm only destructive or ledger-posting actions and state the consequence in the confirmation copy. Never use a generic technical error as the only explanation.

### Keep permission state visible

A user who can read but cannot write sees the action in a disabled state with a short explanation. A missing permission is different from a missing feature. The API remains the final authority and the client must never infer permission from hidden menu items.

## 3. Shared visual system

### Brand tokens

Use the existing Vinamilk-inspired blue tokens as the only saturated family:

```css
--brand_blue: #0413b0;
--brand_blue_deep: #020a6e;
--brand_blue_soft: #e9edff;
--ink: #1e2b47;
--ink_muted: #65718c;
--paper: #ffffff;
--canvas: #f4f6fb;
--rule: #d9e0ef;
--focus: #163cff;
```

Module accents should be derived tints of the same blue family, not three unrelated palettes. A module can be distinguished by its layout pattern, section marker and a restrained accent tint. Keep semantic colors independent from module colors: success, warning, error and information always retain their meaning and have a text/icon alternative.

### Typography

Self-host Be Vietnam Pro WOFF2 with only weights 400, 500, 600 and 700. Load it with `font-display: swap` and a Vietnamese-capable fallback. Use tabular numerals for codes, quantities, balances and dates.

Recommended starting scale:

| Use | Size | Weight | Line height |
| --- | ---: | ---: | ---: |
| Page title | 28–32px | 600 | 1.2 |
| Section title | 18–20px | 600 | 1.3 |
| Body/data | 14–15px | 400 | 1.45 |
| Label | 13–14px | 500 | 1.35 |
| Helper text | 12–13px | 400 | 1.4 |
| Dense table data | 13–14px | 400/500 | 1.35 |

Do not use all-caps for entire table headers. Use sentence case in Vietnamese and a little letter spacing only for short section markers.

### Shape, spacing and elevation

Use 8px as the base spacing unit. Use 10–12px radius for surfaces and 8px for controls. Use a 1px border for grouping and one soft shadow token for an active drawer or menu. Avoid stacked shadows, glass blur, gradients and large decorative blobs. A surface should feel light because of spacing and contrast, not because of a heavy shadow.

### Buttons and controls

Use a 40px desktop control height and 44px for touch-critical controls. Keep the complete search frame, select frame and button on the same height. There must be one border-owning wrapper around a search input and its icon button; nested Ant Design borders are a source of the clipped and doubled frames seen in the current screens.

Primary button: solid Vinamilk blue, one per context. Secondary button: white surface with a blue rule. Tertiary action: text button with a visible hover/focus state. Do not use icon-only actions when the meaning is ambiguous; add a label or an accessible name.

### Status representation

Replace the current collection of pill-shaped tags with a status row consisting of a 3px semantic rule, a small icon and text. Reserve a compact label for dense tables when the status must be scanned repeatedly. The text must always remain visible, and color must never be the only signal.

## 4. App shell and responsive behaviour

Use one CSS grid for the shell:

```css
.app_shell {
  min-height: 100dvh;
  display: grid;
  grid-template-columns: var(--sidebar_width) minmax(0, 1fr);
  grid-template-rows: var(--header_height) minmax(0, 1fr);
}

.app_content {
  min-width: 0;
  min-height: 0;
  overflow: auto;
}
```

The body must not compete with the shell for vertical scrolling. A table or a long drawer may have its own local scroll, but there should be one obvious scroll owner on a page. Every flex/grid child that contains a table needs `min-width: 0` and `min-height: 0`.

Use a 64px header and a 240–248px expanded sidebar. The collapsed sidebar is 72px wide, centers its icons, removes label layout entirely and exposes a tooltip only on hover/focus. The header uses one 56–64px row with a fixed-height left zone, a flexible middle zone and a fixed-height right zone; breadcrumbs, search, settings and account controls must share `align-items: center` and the same control height.

At 1024px, reduce content gutters and allow the sidebar to collapse. At 768px and below, turn the sidebar into a drawer and keep the page content full width. At narrow widths, toolbars wrap as a group and tables remain horizontally scrollable inside their own frame; the page itself must not be clipped.

## 5. Home and navigation

The home page is a role-based work queue, not a collection of links:

1. Context row: role, site/warehouse/factory context, last synchronization time and connection state.
2. `Cần xử lý`: real queues for pending leave requests, unposted inventory documents, overdue production orders and other permission-filtered work.
3. Module coordination: three module sections with a count, the next safe action and a link to the full workspace.
4. Recent activity and data health: audit events, failed synchronization and stale data indicators.

The “Mở công việc gần nhất” action must open the actual recent work item returned by the API. If no item exists, it opens the relevant queue with an empty-state explanation. Never route to the first menu item as a fallback.

The command center uses the same navigation registry as the sidebar. It supports Arrow Up/Down, Enter and Escape, highlights the matching module/function and does not render developer route names or permission codes.

## 6. Module patterns

### Human resources

The landing view is a people index. Keep employee code and name sticky on the left, show status as a text marker, and place the next action at the right. Use a compact summary line above the table for active employees, expiring contracts and pending requests rather than a row of decorative cards.

The detail view uses a split layout: identity and employment summary on the left, contract/event timeline on the right. The drawer is for inspection; long forms such as contract creation use a full route. Use sections for profile, employment, manager, documents and audit history. Payroll remains a workflow with an explicit open → calculate → review → lock state.

### Inventory and materials

The landing view is a stock ledger. Put material identity, unit and available balance first. Show receipts, issues, transfers and stocktake in a movement timeline or tabular ledger. Low stock is indicated by a rule and a sentence that explains the threshold; do not turn every row into a colored tag.

Use a two-step posting flow: validate availability and document state, then post the ledger transaction. The confirmation states the warehouse, item, quantity and irreversible consequence. The balance view and document view share the same filters and URL state.

### Production

The landing view is an execution flow. Show plan date, order, line/work center, readiness and current stage in a schedule/queue layout. A horizontal stage rail communicates draft → released → in_progress → paused → completed/cancelled without requiring users to open every record.

The order detail keeps material need, assignment, progress and finished output in sections. Material readiness is fetched through the inventory contract and appears before the “Bắt đầu” action. Pause/resume controls follow the backend state machine and explain why an action is unavailable.

## 7. Lists, forms and detail surfaces

A list page follows this geometry:

```text
page title + one sentence
toolbar: search · filters · saved view · one primary action
result summary and permission/read-only message
data table with stable row/header geometry
pagination and density controls
```

Do not put a toolbar inside another rounded card. The page surface owns the border; controls sit directly in the toolbar. Keep table header and row heights consistent and give the table most of the available width.

Use server pagination with a default of 50, debounced search, server sort/filter and stale-request cancellation. Save only view preferences (columns, density, preset) locally; all business data remains server-backed.

Forms use logical sections and a two-column grid on desktop, one column on narrow screens. Put the most common path first. Show unsaved-change protection when leaving a dirty form. Use inline validation after the user finishes a field or submits, not on every keystroke. Display English success/error messages to match the project language rule, while field labels and instructions remain Vietnamese.

Use a drawer for a quick record inspection, a full page for a long workflow or a decision that needs broad context. Never put a complex editable data table inside a confirmation modal.

Every page implements loading, empty, error, forbidden/read-only and stale-data states. Skeletons reserve the final geometry so the header, toolbar and table do not jump while data loads.

## 8. Motion and tactile feedback

Motion is feedback, not decoration. On desktop, use 150–200ms transitions; on mobile, use about 200–300ms only for a surface that changes hierarchy. Use opacity and transform, never animate layout dimensions, shadows or large filters. Use a short press response (`translateY(1px)` or a small brightness change), not a bounce or a persistent scale effect.

Recommended motion tokens:

```css
--motion_fast: 120ms;
--motion_standard: 180ms;
--motion_surface: 220ms;
--ease_standard: cubic-bezier(.2, .8, .2, 1);
```

Route changes fade the content shell once. Drawers slide from their edge once. Rows do not animate individually when a page of data arrives. Skeleton shimmer is subtle and disabled under `prefers-reduced-motion: reduce`.

## 9. Performance budget

- Keep route-level lazy loading and load a module only after its route is visited.
- Keep the first list response at 50 rows; debounce search by 250–350ms and cancel stale requests.
- Memoize column definitions and lookup options. Do not create heavy form or icon components for every cell.
- Use Ant Design virtual rows for ordinary vertical lists. Introduce two-dimensional virtualization only after profiling a real 200–300-column use case.
- Keep the first viewport to essential columns. Provide column presets and CSV/export for the complete record.
- Avoid `backdrop-filter`, animated gradients, parallax, large box shadows and per-row motion.
- Measure p95 interaction time, API latency and render time in a seeded 500+ record environment. The target is an interactive page within about 2 seconds on the QA machine and local control feedback below 100ms.
- The Ant Design vendor warning is resolved by route-level lazy loading and a separate icon vendor chunk. Keep monitoring the largest chunks when new shared components are added; do not trade initial performance for decorative dependencies.

## 10. Implementation order

### P0 — geometry and shared primitives

1. Tokenize colors, typography, spacing, radius, control heights and motion in `ERP_Client/src/styles/global.css`.
2. Fix the shell scroll owner and `min-width/min-height` constraints in `app_shell` and the layout route.
3. Replace nested search/select frames with one shared control wrapper.
4. Standardize header zones, collapsed sidebar width and focus states.
5. Add Be Vietnam Pro assets and font loading.
6. Make `data_workspace` own the table, toolbar, pagination, presets, empty/error/read-only states and detail drawer contract.

### P1 — role-based work

1. Add the real work queue and synchronization health endpoint to the home page.
2. Connect the navigation registry to the sidebar and command center.
3. Correct recent-work routing, permission messaging and 404 handling.

### P2 — three module experiences

1. Refactor `human_resources` into the people index/timeline pattern.
2. Refactor `inventory` into the ledger/balance pattern.
3. Refactor `production` into the execution flow/stage rail pattern.
4. Keep the same table, form, drawer, confirmation and error contracts across all three.

### P3 — validation and polish

1. Run keyboard and screen-reader checks for navigation, tables, drawers and form errors.
2. Test 1366, 1280, 1024, 768 and 375px widths, including collapsed sidebar and long dialogs.
3. Test 500+ records and a 200–300-column detail record.
4. Run reduced-motion, slow-network and API-error scenarios.
5. Profile the production build before adding any additional visual dependency.

## 11. Acceptance checklist

- No header, toolbar, modal or table frame overlaps at the supported widths.
- Exactly one page scroll owner is visible; long forms and tables scroll inside their intended surface.
- Header controls have equal height and vertical alignment. Search and select controls share one frame geometry.
- Important pointer targets are at least 44px; keyboard focus is visible and not hidden behind a sticky header.
- A read-only user understands what is unavailable and why.
- Every destructive/posting action names its consequence and is recoverable before confirmation.
- Data loading preserves geometry; empty, error, forbidden and stale states have an action.
- No primary navigation or action depends on pill/tag styling.
- Motion is short, consistent and disabled/reduced when the user requests reduced motion.
- A seeded 500+ record test remains responsive, and the complete 200–300-column record is available without rendering all columns in the first viewport.

## 12. Reference material

- [ISO 9241-210:2019 — Human-centred design for interactive systems](https://www.iso.org/standard/77520.html)
- [Nielsen Norman Group — Recognition rather than recall](https://www.nngroup.com/articles/recognition-and-recall/)
- [Nielsen Norman Group — Progressive disclosure](https://www.nngroup.com/articles/progressive-disclosure/)
- [Nielsen Norman Group — Reducing cognitive load in forms](https://www.nngroup.com/articles/4-principles-reduce-cognitive-load/)
- [Nielsen Norman Group — Error messages in forms](https://www.nngroup.com/articles/errors-forms-design-guidelines/)
- [Nielsen Norman Group — Fitts’s law](https://www.nngroup.com/articles/fitts-law/)
- [W3C — Web Content Accessibility Guidelines 2.2](https://www.w3.org/TR/WCAG22/)
- [Ant Design — Proximity and spacing](https://ant.design/docs/spec/proximity)
- [Ant Design — Data list](https://ant.design/docs/spec/data-list/)
- [Carbon Design System — Data table accessibility](https://v10.carbondesignsystem.com/components/data-table/accessibility/)
- [Carbon Design System — Data table usage](https://v10.carbondesignsystem.com/components/data-table/usage/)
- [GOV.UK Design System — Form structure](https://www.gov.uk/service-manual/design/form-structure)
- [GOV.UK Design System — Error message](https://design-system.service.gov.uk/components/error-message/)
- [Material Design — Duration and easing](https://m1.material.io/motion/duration-easing.html)
- [Material Design — Motion principles](https://m1.material.io/motion/material-motion.html)
- [Impeccable — UI critique and detector playbook](https://github.com/pbakaus/impeccable)

## 13. Action icon and row-action specification

The current edit/delete/read icons look like generic CRUD controls because every action has the same visual weight and the icon meaning is disconnected from the record state. Use a quiet action rail: one obvious primary action, secondary actions behind `MoreOutlined`, and destructive actions only when the business state allows them.

Use the existing `@ant-design/icons` package. Keep one outlined icon family. Use a 16px glyph inside a 36px visual button with a 44px pointer/keyboard hit area. Filled icons are reserved for an active state or a strong workflow transition.

| Intent | Icon | Vietnamese label | Rule |
| --- | --- | --- | --- |
| Open record | `FolderOpenOutlined` / `FileSearchOutlined` | `Mở hồ sơ` / `Xem chi tiết` | Available when the user can read |
| Edit draft | `FormOutlined` | `Chỉnh sửa` | State and permission must allow it |
| History | `HistoryOutlined` | `Xem lịch sử` | Only when audit data exists |
| More | `MoreOutlined` | `Thao tác khác` | Secondary actions |
| Delete draft | `DeleteOutlined` | `Xóa bản nháp` | Draft only, with consequence |
| Deactivate | `StopOutlined` | `Ngừng sử dụng` | Retain history instead of deleting |
| Resume | `PlayCircleOutlined` | `Tiếp tục` | Paused production order |
| Pause | `PauseCircleOutlined` | `Tạm dừng` | In-progress production order |
| Post | `SendOutlined` / `CheckCircleOutlined` | `Ghi sổ` | Validated document and permission |

`EyeOutlined` should not be the default read icon. It suggests preview and is ambiguous for an ERP record. Use `FolderOpenOutlined` for a profile/document and `FileSearchOutlined` for a review workflow. Use `DeleteOutlined` only for a real draft deletion; use `StopOutlined` or `InboxOutlined` when the record must remain for audit.

### Action rail

Reserve a fixed-width action column so hover never changes table geometry:

```text
desktop:  [Mở hồ sơ]  [Chỉnh sửa]  [⋯]
mobile:   [Mở hồ sơ]  [⋯]
```

The primary action is an icon plus a text label. Secondary actions move to `MoreOutlined` on narrow widths. Destructive actions remain inside the menu unless deletion is the only operation. Do not use circular icon buttons, colored icon tiles, emoji, mixed icon families or pill/tag containers for actions.

Normal actions use ink or Vinamilk blue. Edit uses a quiet blue-tinted hover surface. Delete uses red only on hover, focus or confirmation. Workflow transitions use a semantic color with a text label; they are not represented by a generic pencil or trash icon.

### Shared React contract

Render all row actions through one shared component such as `record_action_bar`. Module pages provide definitions; the shared component owns spacing, tooltip, focus, responsive collapse and confirmation behavior.

```js
{
  primary_action,
  secondary_actions,
  record_state,
  can_read,
  can_edit,
  can_delete,
  on_action,
}
```

Each action definition contains `key`, `label`, `icon`, `permission`, `enabled_states`, `danger`, `confirm` and `reason_when_disabled`. The component filters by permission/state, prevents nested buttons when a row is clickable, adds `aria-label`, and keeps the action rail width stable.

States are `rest`, `hover`, `focus-visible`, `pressed`, `loading`, `disabled` and `read_only`. Focus uses a visible 2px ring. Press feedback is a 1px downward translation or small brightness change for 100–120ms. Loading replaces only the glyph so the button width does not jump. Reduced-motion removes reveal transitions.

### Module mapping

**Human resources:** `FolderOpenOutlined` → `Mở hồ sơ`; `FormOutlined` → `Chỉnh sửa`; `HistoryOutlined` → `Xem lịch sử`; `StopOutlined` → `Ngừng sử dụng`. Contracts use `FileSearchOutlined`; leave requests use `CheckOutlined` and `CloseOutlined`; payroll uses `CalculatorOutlined` and `LockOutlined`. Never delete an employee or posted payroll record.

**Inventory:** `FolderOpenOutlined` → `Xem vật tư`; `FormOutlined` → `Sửa danh mục`; `FileSearchOutlined` → `Xem phiếu`; `SendOutlined` → `Ghi sổ`; `DeleteOutlined` → `Xóa bản nháp`. Stocktake uses `EditOutlined` for counting and `CheckCircleOutlined` for closing. A posted document never exposes delete.

**Production:** `FolderOpenOutlined` → `Xem kế hoạch`; `FormOutlined` → `Sửa bản nháp`; `SendOutlined` → `Phát hành`; `FileSearchOutlined` → `Xem lệnh`; `PlayCircleOutlined` → `Bắt đầu`; `PauseCircleOutlined` → `Tạm dừng`; `StopOutlined` → `Hủy lệnh`; `UserSwitchOutlined` → `Phân công`. Actions follow the backend state machine; a released or started order has no delete metaphor.

### Confirmation and feedback

Confirm only deletion, posting, locking, cancellation and other irreversible transitions. State the object and consequence:

```text
Ghi sổ phiếu xuất kho?
PX-00042 · Kho nguyên liệu · 120 kg sữa bột
Số dư tồn kho sẽ được cập nhật và không thể sửa trực tiếp.
```

Opening, viewing and editing a draft do not need confirmation. After success, show a short global message and update the affected row without reloading the whole page. Preserve form values after validation failure and show the actionable error beside the relevant field, following the [GOV.UK error message pattern](https://design-system.service.gov.uk/components/error-message/).

### Action review checklist

- Shared semantic icon and Vietnamese label.
- Stable English permission key and backend state check.
- At least 44px hit area and no hover-induced layout shift.
- Visible keyboard focus and an accessible name.
- Destructive consequence shown before confirmation.
- No reliance on color alone.
- Readable text action on mobile or explicitly named overflow menu.
- Same behavior in HR, inventory and production.

## 14. Implementation handoff: modern record actions

The icon and row-action work is split into a small task so another implementer can continue without redesigning the interaction model. Read [FND-22](../task/00-foundation/22_frontend_action_icon_standard.md) before touching an action column.

### Current implementation

- Shared component: `ERP_Client/src/platform/layout/record_action_bar.jsx`.
- Shared tokens and interaction states: `.record_action_*` in `ERP_Client/src/styles/global.css`.
- Generic data lists: `ERP_Client/src/platform/layout/module_data_page.jsx`.
- Priority list pages: `human_resources_page.jsx`, `contracts_page.jsx`, `inventory_page.jsx`, `production_page.jsx`.
- Identity pages: `users_page.jsx`, `registration_requests_page.jsx`.

### Next slices in order

1. Replace any remaining manual action columns with `RecordActionBar`; do not introduce another action-button component.
2. Verify each action against the backend permission and lifecycle state. The client may hide or disable an action for clarity, but the API remains authoritative.
3. Add a request-bound `loading_key` to the action contract so only the clicked row action is busy and the column width remains unchanged.
4. Add the four viewport screenshots and keyboard walkthrough described in FND-22 to the review record.
5. Measure a large table before enabling extra motion or two-dimensional virtualization. The action rail must stay outside expensive cell renderers.

### Review questions

- Can a first-time operator tell which button opens the record without memorizing an eye icon?
- Can a read-only user inspect the record without seeing a misleading disabled destructive control?
- Is `DeleteOutlined` used only for deletion, with `StopOutlined` reserved for stop/cancel/reject/deactivate?
- Does the action rail retain its width during hover, focus, loading, permission filtering and responsive collapse?
- Does the confirmation state the record and consequence in Vietnamese while the resulting success/error message follows the project language rule?

The detailed contract, module mapping, state rules and acceptance checklist live in [task/00-foundation/22_frontend_action_icon_standard.md](../task/00-foundation/22_frontend_action_icon_standard.md).

## 15. First implementation slice: data workspace and module composition

The first upgrade slice is now implemented behind two stable contracts:

- `DataWorkspace` controls server-paginated data, column presets, personal column selection, detail drawer and viewport-bound table scrolling.
- `RecordActionBar` controls read/edit/workflow actions and keeps the last column stable.

The three priority entry pages now use separate compositions: people timeline for Human resources, ledger flow for Inventory and stage flow for Production. Their data and permission rules remain shared. FND-23 đã có row-level loading, hủy request cũ và filter drawer cho Inventory/Production. Phần cần chốt tiếp là baseline đa viewport, keyboard walkthrough và đo React Profiler trước khi thêm polish trang trí.


## 16. Visual refresh and result pagination

The legacy module intro card and repeated dashboard metric cards are replaced by lighter compositions:

- Module pages use a border-bottom masthead with one module-specific signal: HR uses a vertical rail, Inventory uses a notched inventory mark, and Production uses a short execution rail. The title, description and context remain aligned to the same grid.
- The home work queue is one surface split into signal rows. Each row has a stable index, semantic module accent, count and destination. It does not use four repeated top-bordered cards or large decorative shadows.
- Modal and drawer surfaces keep a stable header/body/footer contract. The body owns scrolling, details use progressive disclosure, and only transform/color feedback is animated.
- `DataWorkspace` renders server totals in a dedicated footer: `1–10 / 10 hồ sơ` is calculated from the response after search and filter, while pagination and page-size controls stay on the right. The page-size selector and its full hit area use a pointer cursor.

This direction follows Carbon dashboard hierarchy and data-table guidance, plus Ant Design's modal context and token guidance. It keeps the Vinamilk blue family as the visual anchor while lowering border, shadow and repeated-card noise.
## 17. Modal task-sheet pattern

Popup nghiệp vụ dài dùng `entity_form_modal` thay vì modal mặc định hẹp:

- Header gồm nhãn ngữ cảnh, động từ + đối tượng và một câu mô tả ngắn.
- Desktop dùng hai cột cho trường độc lập; mobile chuyển về một cột.
- Body là vùng cuộn duy nhất; footer chứa `Hủy` và động từ xác nhận, luôn nằm ngoài vùng cuộn.
- Form submit dùng `form` id từ footer để giữ action rail cố định mà không lặp nút trong body.
- Mask giảm độ đục, không dùng blur hoặc hiệu ứng nền nặng; phản hồi bấm chỉ dùng transform/color.
- Popup xác nhận giữ ngắn và chỉ dành cho quyết định không thể đảo ngược. Chi tiết nhiều trường dùng drawer hoặc modal rộng; workflow rất dài nên chuyển thành route.

Các modal form HR employee, HR contract, Inventory material, Production plan, Production output và Material consumption đã dùng pattern này. Quy tắc này dựa trên vùng header/body/footer và max-height của [Carbon Modal](https://carbondesignsystem.com/components/modal/usage/), hành vi form phức tạp của [Material Dialog](https://m1.material.io/components/dialogs.html), và semantic styling của [Ant Design Modal](https://ant.design/components/modal/?locale=en-US).

## 10. Slice nâng cấp geometry và tương tác — 2026-09-18

Đã hợp nhất CSS sau các lần thử giao diện trước. Nguyên nhân của hiện tượng viền kép, khung bị đè, search co lệch và drawer/modal tràn là nhiều block cùng selector được nối ở cuối stylesheet, trong đó geometry của cùng một component bị ghi đè theo thứ tự tải. Hệ thống hiện có một contract cuối cùng cho:

- App shell: header dùng grid hai vùng, control cùng chiều cao, sidebar thu gọn có vùng bấm và trạng thái active ổn định.
- Toolbar: search, select và nút dùng một chiều cao 40px; wrapper của Ant Space cũng tham gia flex layout để không làm search bị co hoặc mất góc.
- Data workspace: bảng có vùng cuộn ngang rõ ràng, row không dùng content-visibility gây sai hình học với virtual table, footer giữ tổng 1–n / tổng ở bên trái và phân trang ở bên phải.
- Module masthead: HR, Kho và Sản xuất có marker/nhịp thông tin khác nhau nhưng dùng chung lưới, typography và control contract.
- Sheet surface: modal/drawer có ba vùng header/body/footer; body là vùng cuộn duy nhất, footer luôn nằm trong viewport, không dùng gradient hoặc animation trang trí.
- Quyền: trạng thái Chỉ xem hiển thị như dòng thông tin nhẹ, không phụ thuộc pill tag; backend vẫn là nơi quyết định quyền.

Không thêm animation liên tục, không dùng blur/parallax; transition chỉ đổi màu, opacity hoặc dịch 1px trong thời gian ngắn và có prefers-reduced-motion. Kiểm tra hiện tại: npm run lint, npm run build và git diff --check đạt; cần chụp baseline ở 1366/1280/1024/mobile khi có phiên đăng nhập và dữ liệu trung tâm để khóa nghiệm thu trực quan.

## 18. Rà soát nguyên nhân gốc và đợt sửa 2026-09-21

### Các điểm chưa tốt đã xác định

- Một số CTA được viết dạng `Link` bọc `Button` hoặc `Button` bọc `Link`. Hai phần tử đều có vùng tương tác riêng nên trình duyệt có thể nhận sai phần tử, focus bị lệch và hit area bị chồng. Đây là nguyên nhân của lỗi “bấm không có tác dụng” ở trang tổng quan và placeholder.
- Popup xác nhận/thông tin từng gọi qua API tĩnh của Ant Design. Khi popup nằm dưới `App` provider của ứng dụng, API tĩnh không nhận đầy đủ theme, locale và ngữ cảnh; trạng thái hiển thị có thể lệch với trang đang mở. Các popup nghiệp vụ đã chuyển sang `App.useApp()`.
- Cột thao tác từng bị rule CSS cũ ghi đè: nút có `width: 100%` nhưng thanh hành động lại ép `width`, khiến nhãn “Mở”, biểu tượng và nút workflow đè lên nhau. Cột cố định phải `overflow: visible`, action rail có `z-index` ổn định và nút icon/labeled có kích thước riêng.
- Trạng thái hành động chưa được giải thích đủ. Nút sửa của bản ghi đã ghi sổ/hết hiệu lực cần hiển thị lý do bị khóa; API vẫn là nơi kiểm tra quyền và state machine cuối cùng.

### Nguyên tắc thiết kế chuyên nghiệp áp dụng

- Mỗi hành động chỉ có một phần tử interactive; liên kết điều hướng dùng `Link`, thao tác tại chỗ dùng `Button`.
- Header, body, footer của popup là ba vùng cố định; chỉ body cuộn. Footer không bị đẩy khỏi viewport khi form dài.
- Action rail ưu tiên “Mở” bằng văn bản, các thao tác phụ dùng icon có tooltip; không đổi chiều rộng cột khi hover, loading hoặc thiếu quyền.
- Hierarchy dùng khoảng trắng, đường kẻ mảnh và typography; màu Vinamilk blue là accent, không dùng shadow/gradient/animation liên tục.
- Feedback chỉ dùng transition màu/opacity/dịch 1px trong 160ms, có `prefers-reduced-motion`; tải dữ liệu bằng skeleton/loading cục bộ, không khóa toàn trang.

### Thay đổi đã thực hiện

- Dashboard CTA và trang placeholder dùng styled link đơn, không còn Link/Button lồng nhau.
- Popup xác nhận/thông tin ở HR, Kho, Sản xuất, workflow drawer và identity dùng `App.useApp()`.
- Action rail dùng tooltip cho trạng thái bị khóa, giữ kích thước cột và cho phép nút “Mở” không bị nhãn workflow che.
- Cột thao tác fixed-right được phép hiển thị tooltip/label đúng vùng, không cắt mất góc hoặc chồng lên cột hồ sơ.

### Kiểm tra

- `ERP_Client`: `npm run lint` đạt.
- `ERP_Client`: `npm run build` đạt, bundle production tạo thành công.
- Không còn mẫu `Link`/`Button` lồng nhau trong các màn hình layout/module đã rà soát.
- Cần thực hiện visual QA có đăng nhập ở bốn viewport 1366, 1280, 1024 và 375px; kiểm tra bàn phím `Tab/Enter/Escape`, modal form dài và action rail khi cuộn bảng.
## 19. Module masthead — 2026-09-21

Header của ba phân hệ ưu tiên đã chuyển sang một component dùng chung `ERP_Client/src/platform/layout/module_masthead.jsx`.

- Khối đầu có icon mô tả đúng phân hệ, tên phân hệ lớn và một dòng mô tả nghiệp vụ ngay dưới.
- Bên phải hiển thị ngữ cảnh dữ liệu và chức năng đang mở.
- Bên dưới có năm chức năng con, mỗi chức năng có icon, số thứ tự, trạng thái đang mở và trạng thái khóa theo permission.
- HR dùng hình học tròn cho nhận diện con người; Kho dùng nét đứt và góc kho; Sản xuất dùng nhịp góc lệch và đường tiến độ. Lưới, khoảng cách, typography và chiều cao vẫn thống nhất.
- Ở màn hình 1100px thanh chức năng chuyển ba cột; ở mobile chuyển hai cột, không tạo overflow ngang. Không dùng animation liên tục hoặc hiệu ứng nặng.

Các trang hồ sơ nhân viên, hợp đồng, danh mục/số dư kho, kế hoạch sản xuất và toàn bộ chức năng con dùng `ModuleMasthead`; không còn header riêng lẻ làm lệch tông giữa các route.
## 20. Sidebar accordion theo phân hệ — 2026-09-21

Thanh điều hướng chính chỉ giữ một nhóm chức năng mở tại một thời điểm. Khi chọn chức năng thuộc module khác, nhóm đang mở được thay thế bằng nhóm mới; khi chọn Tổng quan, tất cả nhóm được đóng. Điều hướng bằng command center hoặc route trực tiếp cũng đồng bộ trạng thái này. Sidebar thu gọn và menu mobile vẫn dùng dropdown riêng, không bị ảnh hưởng.