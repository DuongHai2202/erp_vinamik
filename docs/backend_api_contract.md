# backend api contract for antigravity ui

This document is the integration boundary between the React client and the Spring Boot backend. Antigravity may replace or extend the UI, but it must call these APIs and must not connect to PostgreSQL directly.

## common rules

- Base URL is the same origin as the web application; development Vite proxies `/api` to the backend.
- Send cookies with requests (`credentials: include`). The session is an HttpOnly `erp_session` cookie.
- Call `GET /api/v1/auth/csrf` before a mutating request and send the returned CSRF token in the `X-XSRF-TOKEN` header when the client library requires it.
- Request and response field names use lowercase snake_case.
- API success and error messages are English. UI labels and helper text are Vietnamese.
- A missing session returns `401`; an authenticated account without the permission returns `403`; validation returns `400`; a duplicate code or business conflict returns `409`.
- Paginated responses use `{ data: { items, page, page_size, total_items, total_pages } }`. `page` is zero-based in the API.

## authentication

| Method | Path | Permission | Purpose |
|---|---|---|---|
| GET | `/api/v1/auth/csrf` | public | Create/read the CSRF cookie and token. |
| POST | `/api/v1/auth/login` | public | Body: `{ username, password }`. |
| GET | `/api/v1/auth/me` | authenticated | Return the current user and permission codes. |
| POST | `/api/v1/auth/logout` | authenticated | Revoke the current session. |

Login and `/auth/me` return `user_id`, `username`, `permission_codes` and `super_admin`. The UI may use `super_admin` to display protected-account state, but the backend remains the authority for every command. The UI must treat `current_user.permission_codes` as display guidance only.

## human resources

### employee

Resource: `/api/v1/human_resources/employees`

- `GET`: `search`, `status`, `page`, `page_size`; permission `hr_employee_read`.
- `GET /{employee_id}`; permission `hr_employee_read`.
- `POST`: employee fields `employee_code`, `full_name`, `date_of_birth`, `phone_number`, `email`, `department_id`, `job_title_id`, `manager_employee_id`, `employment_status`, `hired_on`, `terminated_on`, `notes`; permission `hr_employee_create`. `date_of_birth` is optional and uses ISO `yyyy-MM-dd`; `phone_number` is normalized to Vietnamese digits beginning with `0` after accepting common `+84`/spacing separators.
- `PUT /{employee_id}`: same fields; permission `hr_employee_update`.
- `DELETE /{employee_id}`: soft deactivation; permission `hr_employee_deactivate`.

### HR reference master data

Resource: `/api/v1/human_resources/master_data`

- `/departments`, `/job_titles` and `/work_shifts` support server-side `search`, `status`, `page` and `page_size`; read permission is `hr_master_read`.
- `POST`, `PUT /{id}` and `DELETE /{id}` are available for each resource with `hr_master_create`, `hr_master_update` and `hr_master_deactivate`. Delete means soft deactivation; referenced rows are retained.
- Department requests may include an active `parent_department_id`; self-parent and descendant-parent assignments are rejected to keep the hierarchy acyclic. Work shift requests contain `shift_code`, `shift_name`, `starts_at`, `ends_at` and `status`; equal start/end times are rejected.
- `GET /work_shifts/active` is a read-only lookup also available to production assignment permissions, so Production does not query HR tables directly.
### employment contract

Resource: `/api/v1/human_resources/contracts`

- `GET`: `search`, `employee_id`, `status`, `expiring_only`, `page`, `page_size`; permission `hr_contract_read`.
- `GET /{employment_contract_id}`; permission `hr_contract_read`.
- `POST`: `contract_code`, `employee_id`, `contract_type`, `effective_from`, `effective_to`, `base_salary`, `currency_code`, `status`, `notes`; permission `hr_contract_create`. New records must use `draft` status.
- `PUT /{employment_contract_id}`: edits draft records; permission `hr_contract_update`.
- `POST /{employment_contract_id}/status`: body `{ status, notes }`; permission `hr_contract_approve`. Allowed transitions are `draft -> active|cancelled` and `active -> terminated|expired`.

Each contract response includes `days_until_expiry` and `expiring_soon`. `expiring_only=true` returns active contracts effective today and ending within the next 15 days.

### absence and leave

Resource: `/api/v1/human_resources/absences`

- `GET`: `search`, `employee_id`, `status`, `from_date`, `to_date`, `page`, `page_size`; permission `hr_absence_read`.
- `GET /{leave_request_id}`; permission `hr_absence_read`.
- `POST`: `request_code`, `employee_id`, `leave_type_code`, `starts_on`, `ends_on`, `is_paid`, `reason`, optional `status`; permission `hr_absence_create`. New records start as `pending` unless `draft` is requested.
- `PUT /{leave_request_id}`: edits `draft` or `pending` requests; permission `hr_absence_update`.
- `POST /{leave_request_id}/decision`: body `{ status: "approved"|"rejected", decision_note }`; permission `hr_absence_approve`.
- `POST /{leave_request_id}/cancel`: cancels a draft or pending request; permission `hr_absence_update`.

The backend rejects invalid date ranges and does not approve overlapping approved leave for the same employee. Approved unpaid requests are the source used later by payroll.

### reward and discipline

Resource: `/api/v1/human_resources/rewards_discipline`

- `GET` and `GET /{record_id}` require `hr_reward_read`; list filters include `search`, `employee_id`, `event_type`, `status`, `from_date`, `to_date`, `page` and `page_size`.
- `GET /payroll_inputs` returns only approved monetary inputs in the requested date range and requires `hr_reward_read`.
- `POST` creates a draft record with `event_type` equal to `reward` or `discipline`; permission `hr_reward_create`.
- `PUT /{record_id}` edits draft records and `POST /{record_id}/submit` moves a draft to pending; permission `hr_reward_update`.
- `POST /{record_id}/decision` accepts `approved` or `rejected` with a decision note; permission `hr_reward_approve`.

### payroll

Resource: `/api/v1/human_resources/payroll`

- `GET /periods`, `GET /periods/{payroll_period_id}`, `GET /periods/{payroll_period_id}/records` and `GET /records/{payroll_record_id}` require `hr_payroll_read`. Lists use server pagination.
- `POST /periods` accepts `{ year, month }`, creates one full-calendar-month period and requires `hr_payroll_create`.
- `POST /periods/{payroll_period_id}/calculate` requires `hr_payroll_update`. It replaces the previous unlocked snapshot in one transaction, so recalculation does not duplicate lines.
- `POST /periods/{payroll_period_id}/approve`, `/reject` and `/lock` require `hr_payroll_approve`. Reject requires `{ decision_note }`.
- The lifecycle is `draft|rejected -> calculated -> approved -> locked`. A locked period, its records and lines are immutable at both service and PostgreSQL trigger levels.
- Version `monthly_mon_sat_v1` uses active contracts covering the full month, Monday through Saturday as standard working days, approved unpaid leave, and approved reward/discipline amounts. Overlapping active contracts, partial coverage, mixed contract currencies, and reward/discipline currency mismatches reject the calculation. The precise formula and rounding contract are in `docs/payroll_policy.md`.

## inventory

### stock balances

Resource: `/api/v1/inventory/balances`

- `GET` requires `inventory_material_read` and accepts `search`, `stock_item_id`, `warehouse_id`, `page`, and `page_size`.
- Results come from the inventory balance projection and include item, warehouse, location, optional lot, unit and `on_hand_quantity`.
- The client must treat this as read-only. Receipt, issue, transfer and stocktake posting APIs are the only paths that change the projection.
### receipts and stock ledger

Resource: `/api/v1/inventory/receipts`

- `GET` and `GET /{receipt_id}` require `inventory_receipt_read`.
- `POST` creates a draft receipt with `receipt_code`, `warehouse_id`, optional `supplier_id`, optional source pair (`source_module` and `source_document_id`), optional `reference_number`, `idempotency_key`, `notes`, and a non-empty `lines` array.
- Each line contains `stock_item_id`, `warehouse_location_id`, optional `stock_lot_id`, and positive `quantity` with at most 6 decimal places.
- `POST /{receipt_id}/post` requires `inventory_receipt_post`. It locks the receipt, writes append-only `stock_movement` rows and updates the `stock_balance` projection in one transaction. Repeating a post for an already posted receipt returns the posted receipt without duplicating movement rows.
Resource: `/api/v1/inventory/materials`

- Read/list and detail require inventory_material_read; production read permissions (production_plan_read, production_bom_read, production_order_read, or production_output_read) may use the same read-only lookup for cross-module selection. Write/deactivate permissions remain Inventory-only.
- Create requires `inventory_material_create`.
- Update requires `inventory_material_update`.
- Deactivation requires `inventory_material_deactivate`.
- The create/update body accepts optional item_type: raw_material (default) or finished_product. Both types remain owned by Inventory; the type is immutable after creation. Production uses the same resource with item_type=finished_product to select products.

Use `/api/v1/inventory/master_data/units` and `/api/v1/inventory/master_data/categories` for lookup options. Production selects active finished products through the same material contract with `item_type=finished_product`.

### Inventory reference master data

Resource: `/api/v1/inventory/master_data`

- Active lookup lists are available at `/units`, `/categories`, `/suppliers`, `/warehouses` and `/warehouses/{warehouse_id}/locations` for permitted transaction/production screens.
- Management lists are paginated at `/manage/units`, `/manage/categories`, `/manage/suppliers`, `/manage/warehouses` and `/manage/warehouses/{warehouse_id}/locations`; filters are `search`, `status`, `page` and `page_size`.
- `POST`, `PUT /{id}` and `DELETE /{id}` are available for units, categories, suppliers, warehouses and locations with `inventory_master_create`, `inventory_master_update` and `inventory_master_deactivate`; deletion is soft deactivation.
- A location must belong to an active warehouse when created and cannot be moved to another warehouse after creation. Codes are normalized to lowercase and conflicts return `409`.
Inventory transaction APIs are owned by Inventory; Production must never write inventory balances directly.


### issues, transfers and stocktakes

Resource: `/api/v1/inventory/issues`

- `GET` and `GET /{issue_id}` require `inventory_issue_read`.
- `POST` creates a draft issue with `issue_code`, `warehouse_id`, optional source pair, reason, idempotency key, notes and positive `lines`.
- `POST /{issue_id}/post` requires `inventory_issue_post`; the backend locks balances, rejects insufficient stock and writes the issue ledger atomically.

Resource: `/api/v1/inventory/transfers`

- `GET` and `GET /{transfer_id}` require `inventory_transfer_read`.
- `POST` creates a draft transfer with source/destination warehouses and lines containing item, optional lot, source location, destination location and quantity.
- `POST /{transfer_id}/post` requires `inventory_transfer_post`; source and destination movements are written together and the source balance cannot become negative.

Resource: `/api/v1/inventory/stocktakes`

- `GET` and `GET /{stocktake_id}` require `inventory_stocktake_read`.
- `POST` creates a counting session and snapshots current balances; permission `inventory_stocktake_create`.
- `POST /{stocktake_id}/lines/{stocktake_line_id}/count` records a non-negative counted quantity; permission `inventory_stocktake_create`.
- `POST /{stocktake_id}/submit` submits a fully counted session; permission `inventory_stocktake_create`.
- `POST /{stocktake_id}/approve` reviews the session; permission `inventory_stocktake_adjust`.
- `POST /{stocktake_id}/post` writes append-only `stocktake_adjustment` movements and updates balances atomically; permission `inventory_stocktake_adjust`.
- `POST /{stocktake_id}/cancel` cancels an unposted session; permission `inventory_stocktake_create`.

## production

Resource: `/api/v1/production/plans`

- `GET` and `GET /{production_plan_id}` require `production_plan_read`.
- `POST` requires `production_plan_create`.
- `PUT /{production_plan_id}` edits draft plans and requires `production_plan_update`.
- `POST /{production_plan_id}/status` changes the lifecycle with the corresponding approval permission.
- `DELETE /{production_plan_id}` is a draft-only delete and requires `production_plan_delete`.

Plan lines reference inventory-owned material IDs. The backend validates active items through the inventory public contract and stores the item code/name/unit snapshot for the plan line.


### BOM and production orders

Resource: `/api/v1/production/boms`

- `GET` and `GET /{bom_id}` require `production_bom_read`.
- `POST` creates a draft BOM from an active Inventory finished product and raw materials; permission `production_bom_create`.
- `PUT /{bom_id}` edits only draft BOMs; permission `production_bom_update`.
- `POST /{bom_id}/status` activates or deactivates a version; permission `production_bom_approve`.

Resource: `/api/v1/production/orders`

- `GET` and `GET /{production_order_id}` require `production_order_read`.
- `POST` creates a planned order from a plan line and an active effective BOM, snapshots material requirements, and requires `production_order_create`.
- `PUT /{production_order_id}` edits draft/planned orders and regenerates their requirement snapshot; permission `production_order_update`.
- `POST /{production_order_id}/release` releases an order; permission `production_order_release`.
- `POST /{production_order_id}/status` accepts operational statuses `in_progress`, `paused`, or `cancelled`; permission `production_order_update`.
- `POST /{production_order_id}/complete` completes an order; permission `production_order_complete`.
- `POST /{production_order_id}/cancel` cancels through the operational status rule; permission `production_order_update`.
- `GET /{production_order_id}/material-needs` calculates required quantity, available quantity from the Inventory public contract, and shortage without writing inventory data.


Resource: `/api/v1/production/assignments`

- `GET` and `GET /{production_assignment_id}` require `production_assignment_read` and can filter by order, employee, status and time range.
- `POST` creates a planned assignment after validating an active employee through the HR public contract and requires `production_assignment_create`.
- `PUT /{production_assignment_id}` edits planned assignments and requires `production_assignment_update`.
- `POST /{production_assignment_id}/status` accepts valid assignment lifecycle transitions and requires `production_assignment_update`.
- Production does not join HR tables directly; employee and shift labels come from public HR contracts.

## ui implementation guidance

Antigravity should keep page components focused on layout and interaction. Put HTTP calls in a reusable client, preserve server pagination, render only actions allowed by permission codes, and still display API errors from the backend. Do not duplicate lifecycle or overlap rules in the client. A refresh must reconstruct the same view from API data; business state must not be stored only in localStorage.

### progress, material consumption and finished outputs

Resource: /api/v1/production/orders

- GET /{production_order_id}/progress requires production_order_read. It returns planned quantity, good quantity, defective quantity, total actual quantity, remaining quantity, completion percentage and the ordered status event timeline.
- GET /{production_order_id}/material-consumption requires production_order_read.
- POST /{production_order_id}/material-consumption requires production_order_update. The request contains warehouse_id, a safe idempotency key, notes and material lines. Production sends the issue command through the Inventory public contract and stores the returned issue line references.
- GET /{production_order_id}/outputs requires production_output_read.
- POST /{production_order_id}/outputs requires production_output_create. It records good and defective quantities, lot/date information and the target warehouse location; it does not post inventory yet.
- POST /{production_order_id}/outputs/{output_id}/post requires production_output_post. It asks Inventory to ensure the canonical lot and posts only the good quantity through the receipt contract. Repeating the request returns the existing result without a duplicate receipt.


### identity administration

Resource: `/api/v1/identity`

- `GET /users` and `GET /users/{user_id}` require `identity_user_read`; both support server-side search, status filtering and pagination.
- `GET /roles` requires `identity_role_read` and returns active roles available for assignment.
- `POST /users` requires `identity_user_create`; a password is hashed by the backend and an empty role list defaults to `read_only`.
- `PUT /users/{user_id}` and `POST /users/{user_id}/reset-password` require `identity_user_update`.
- `PUT /users/{user_id}/roles` requires `identity_role_update`. Assigning `system_admin` or changing the current user's roles additionally requires `identity_role_admin`; a non-elevated administrator cannot self-escalate.
- The single account returned with `super_admin: true` has every active permission. Its status cannot be changed away from `active`, its roles cannot remove `system_admin`, and there is no delete operation for it.
- Disabled/locked users are rejected by the next authentication request. Password reset clears failed-login lock state without returning the password.



