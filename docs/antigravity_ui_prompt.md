# antigravity ui implementation brief

Use this repository as the source of truth for the ERP Vinamik React client. The backend contract is in `docs/backend_api_contract.md`; do not infer a new database model or call PostgreSQL from the browser.

## scope

Improve the React UI only. Keep the existing Vite/React JavaScript stack and routes. The first usable screens are:

1. HR employee list and form.
2. HR employment contract list, 15-day expiry alert, form, and status actions.
3. Inventory material master list and form.
4. Production plan list, multi-line form, detail and lifecycle actions.

The HR absence and reward/discipline APIs are ready for UI integration but do not require a final screen until their endpoint behavior is verified with PostgreSQL.


## reference master-data screens

Add permission-aware management screens after the first four screens are stable:

- HR: `/api/v1/human_resources/master_data/departments`, `/job_titles`, `/work_shifts`; use `/work_shifts/active` for production assignment lookup.
- Inventory: active lookup endpoints `/api/v1/inventory/master_data/units`, `/categories`, `/suppliers`, `/warehouses` and `/warehouses/{warehouse_id}/locations`.
- Inventory management lists use `/api/v1/inventory/master_data/manage/units`, `/categories`, `/suppliers`, `/warehouses` and `/manage/warehouses/{warehouse_id}/locations`.
- Create/update/deactivate buttons require the matching `hr_master_*` or `inventory_master_*` permission. Deactivate is a soft state change; do not present it as physical deletion.
- Keep the server-side page contract `{ items, page, page_size, total_items, total_pages }` and always load location options after a warehouse is selected.
## non-negotiable rules

- Keep code identifiers, file names, routes, API fields and comments in lowercase English snake_case. React component symbols may use PascalCase because JSX requires it.
- Display all menus, labels, table headings, empty states and help text in Vietnamese.
- Display API success/error and validation messages in English. Do not translate API messages into Vietnamese in the UI.
- Use the shared `request_api` client and `use_auth` context. Do not store passwords, session tokens or business data in localStorage.
- Render actions based on permission codes, but never replace backend authorization with a client check.
- Use server-side pagination, filtering and search. Do not load an unbounded table into the browser.
- Keep the `app_shell` layout, protected routes and API paths stable unless the backend contract is updated at the same time.
- Avoid duplicating business rules such as contract lifecycle or leave overlap in JavaScript. Send commands to the backend and render the returned state.

## visual and interaction direction

- Follow PRODUCT.md and DESIGN.md. The visible product name is Vinamik; do not put ERP in the brand lockup.
- Use the factory_control_board direction: ink navigation rail, paper workspace, thin rules and a module matrix where each module exposes its five functions as lanes. Avoid gradients, nested cards and repeated icon-card stacks.
- Use Be Vietnam Pro as the default Vietnamese font, with Lexend and Segoe UI as selectable fallbacks. Keep the base size at 15px or larger and use tabular numerals for money and quantities.
- Use a dense but readable data table with sticky identifier/action columns, visible status tags, clear empty/loading/error states and keyboard-accessible actions.
- Use responsive cards on narrow screens instead of forcing the full table width.
- Keep destructive actions behind a confirmation dialog and show a clear English success/error toast after the API result.
- For large datasets, debounce search, cancel stale requests when practical, and keep pagination state in the URL when the screen needs shareable filters.
- Respect `prefers-reduced-motion`; animations should communicate state and stay short.

## validation

Run from `ERP_Client`:

```powershell
npm run lint
npm run build
```

Test at least one read-only user, one HR staff/manager user and one inventory/production user. Confirm that missing write permissions hide or disable commands while direct API calls still receive `403` from the backend.

