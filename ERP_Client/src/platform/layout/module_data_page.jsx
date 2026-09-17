
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { App as antd_app, Button, Checkbox, Form, Input, InputNumber, Modal, Select, Space, Tag, Typography } from 'antd';
import { CheckOutlined, DatabaseOutlined, EyeOutlined, LockOutlined, PlusOutlined, SendOutlined, StopOutlined, TeamOutlined, ToolOutlined } from '@ant-design/icons';
import { useSearchParams } from 'react-router-dom';
import { has_permission } from '../common/permission_utils';
import { request_api } from '../common/api_client';
import { use_auth } from '../identity/auth_context';
import DataWorkspace from './data_workspace';

const AntdApp = antd_app;

const status_labels = {
  draft: 'Bản nháp',
  pending: 'Chờ duyệt',
  submitted: 'Đã gửi',
  calculated: 'Đã tính',
  approved: 'Đã duyệt',
  released: 'Đã phát hành',
  in_progress: 'Đang thực hiện',
  completed: 'Đã hoàn thành',
  posted: 'Đã ghi sổ',
  rejected: 'Từ chối',
  cancelled: 'Đã hủy',
  active: 'Đang hoạt động',
  inactive: 'Ngừng hoạt động',
  planned: 'Đã lập kế hoạch',
  counted: 'Đã kiểm đếm',
  locked: 'Đã khóa',
};

const module_visuals = {
  human_resources: { icon: <TeamOutlined />, context: 'Hồ sơ và vòng đời nhân sự' },
  inventory: { icon: <DatabaseOutlined />, context: 'Dòng chứng từ và tồn kho' },
  production: { icon: <ToolOutlined />, context: 'Kế hoạch đến thành phẩm' },
};

const status_colors = {
  pending: 'gold', submitted: 'gold', calculated: 'blue', approved: 'green', released: 'blue',
  in_progress: 'processing', completed: 'green', posted: 'green', rejected: 'red', cancelled: 'default',
  active: 'green', inactive: 'default', planned: 'blue', counted: 'cyan', locked: 'purple', draft: 'default',
};

const feature_catalog = {
  absences: {
    module_key: 'human_resources', title: 'Nghỉ phép và vắng mặt', description: 'Tiếp nhận, theo dõi và phê duyệt đơn nghỉ từ dữ liệu nhân sự trung tâm.', endpoint: '/api/v1/human_resources/absences', row_key: 'leave_request_id', search_placeholder: 'Tìm theo mã đơn hoặc nhân viên', search_param: 'search', status_param: 'status', read_permission: 'hr_absence_read', create_permission: 'hr_absence_create',
    status_options: ['pending', 'approved', 'rejected', 'cancelled'],
    columns: [
      ['request_code', 'Mã đơn', 165], ['employee_code', 'Mã nhân viên', 130], ['employee_name', 'Nhân viên', 190], ['leave_type_code', 'Loại nghỉ', 145], ['starts_on', 'Từ ngày', 120], ['ends_on', 'Đến ngày', 120], ['day_count', 'Số ngày', 90], ['status', 'Trạng thái', 125],
    ],
    actions: [{ key: 'approve', label: 'Duyệt', target_status: 'approved', permission: 'hr_absence_approve', endpoint: (record) => `/api/v1/human_resources/absences/${record.leave_request_id}/decision`, body: { status: 'approved', decision_note: null }, statuses: ['pending'] }, { key: 'reject', label: 'Từ chối', target_status: 'rejected', permission: 'hr_absence_approve', endpoint: (record) => `/api/v1/human_resources/absences/${record.leave_request_id}/decision`, body: { status: 'rejected', decision_note: null }, statuses: ['pending'] }],
  },
  rewards_discipline: {
    module_key: 'human_resources', title: 'Khen thưởng và kỷ luật', description: 'Quản lý các quyết định thưởng, phạt và đầu vào đã duyệt cho kỳ lương.', endpoint: '/api/v1/human_resources/rewards_discipline', row_key: 'employee_reward_discipline_id', search_placeholder: 'Tìm theo mã hoặc nhân viên', search_param: 'search', status_param: 'status', read_permission: 'hr_reward_read', create_permission: 'hr_reward_create',
    status_options: ['draft', 'pending', 'approved', 'rejected', 'cancelled'],
    columns: [['record_code', 'Mã quyết định', 165], ['employee_code', 'Mã nhân viên', 130], ['employee_name', 'Nhân viên', 190], ['event_type', 'Loại sự kiện', 135], ['effective_on', 'Ngày hiệu lực', 130], ['amount', 'Số tiền', 125], ['currency_code', 'Tiền tệ', 95], ['status', 'Trạng thái', 125]],
    actions: [{ key: 'approve', label: 'Duyệt', target_status: 'approved', permission: 'hr_reward_approve', endpoint: (record) => `/api/v1/human_resources/rewards_discipline/${record.employee_reward_discipline_id}/decision`, body: { status: 'approved', decision_note: null }, statuses: ['pending'] }, { key: 'reject', label: 'Từ chối', target_status: 'rejected', permission: 'hr_reward_approve', endpoint: (record) => `/api/v1/human_resources/rewards_discipline/${record.employee_reward_discipline_id}/decision`, body: { status: 'rejected', decision_note: null }, statuses: ['pending'] }],
  },
  payroll: {
    module_key: 'human_resources', title: 'Tính lương', description: 'Theo dõi kỳ lương, snapshot kết quả và trạng thái duyệt khóa.', endpoint: '/api/v1/human_resources/payroll/periods', row_key: 'payroll_period_id', search_placeholder: 'Lọc theo mã kỳ lương', search_param: null, status_param: 'status', read_permission: 'hr_payroll_read', create_permission: 'hr_payroll_create',
    status_options: ['draft', 'calculated', 'approved', 'rejected', 'locked'],
    columns: [['period_code', 'Mã kỳ lương', 170], ['starts_on', 'Từ ngày', 120], ['ends_on', 'Đến ngày', 120], ['standard_working_days', 'Ngày công chuẩn', 135], ['calculation_version', 'Công thức', 210], ['record_count', 'Số nhân sự', 110], ['total_net_amount', 'Thực lĩnh', 135], ['status', 'Trạng thái', 125]],
    actions: [{ key: 'calculate', label: 'Tính lương', target_status: 'calculated', permission: 'hr_payroll_update', endpoint: (record) => `/api/v1/human_resources/payroll/periods/${record.payroll_period_id}/calculate`, statuses: ['draft', 'rejected'] }, { key: 'approve', label: 'Duyệt', target_status: 'approved', permission: 'hr_payroll_approve', endpoint: (record) => `/api/v1/human_resources/payroll/periods/${record.payroll_period_id}/approve`, statuses: ['calculated'] }, { key: 'lock', label: 'Khóa kỳ', target_status: 'locked', permission: 'hr_payroll_approve', endpoint: (record) => `/api/v1/human_resources/payroll/periods/${record.payroll_period_id}/lock`, statuses: ['approved'] }],
  },
  receipts: {
    module_key: 'inventory', title: 'Phiếu nhập kho', description: 'Kiểm tra chứng từ nhập và ghi sổ tập trung theo kho.', endpoint: '/api/v1/inventory/receipts', row_key: 'receipt_id', search_placeholder: 'Tìm theo mã phiếu hoặc kho', search_param: 'search', status_param: 'status', read_permission: 'inventory_receipt_read', create_permission: 'inventory_receipt_create',
    status_options: ['draft', 'posted', 'cancelled'], columns: [['receipt_code', 'Mã phiếu', 190], ['warehouse_code', 'Kho', 180], ['line_count', 'Số dòng', 100], ['created_at', 'Ngày lập', 190], ['posted_at', 'Ngày ghi sổ', 190], ['status', 'Trạng thái', 125]],
    actions: [{ key: 'post', label: 'Ghi sổ', target_status: 'posted', permission: 'inventory_receipt_post', endpoint: (record) => `/api/v1/inventory/receipts/${record.receipt_id}/post`, statuses: ['draft'] }],
  },
  issues: {
    module_key: 'inventory', title: 'Phiếu xuất kho', description: 'Xuất vật tư theo chứng từ, kiểm tra tồn và ghi sổ không âm kho.', endpoint: '/api/v1/inventory/issues', row_key: 'issue_id', search_placeholder: 'Tìm theo mã phiếu hoặc kho', search_param: 'search', status_param: 'status', read_permission: 'inventory_issue_read', create_permission: 'inventory_issue_create',
    status_options: ['draft', 'posted', 'cancelled'], columns: [['issue_code', 'Mã phiếu', 205], ['warehouse_code', 'Kho', 180], ['line_count', 'Số dòng', 100], ['created_at', 'Ngày lập', 190], ['posted_at', 'Ngày ghi sổ', 190], ['status', 'Trạng thái', 125]],
    actions: [{ key: 'post', label: 'Ghi sổ', target_status: 'posted', permission: 'inventory_issue_post', endpoint: (record) => `/api/v1/inventory/issues/${record.issue_id}/post`, statuses: ['draft'] }],
  },
  stocktakes: {
    module_key: 'inventory', title: 'Kiểm kê kho', description: 'Mở phiên kiểm kê, theo dõi tiến độ đếm và ghi nhận chênh lệch có kiểm soát.', endpoint: '/api/v1/inventory/stocktakes', row_key: 'stocktake_id', search_placeholder: 'Tìm theo mã phiên hoặc kho', search_param: 'search', status_param: 'status', read_permission: 'inventory_stocktake_read', create_permission: 'inventory_stocktake_create',
    status_options: ['draft', 'counting', 'submitted', 'approved', 'posted', 'cancelled'], columns: [['stocktake_code', 'Mã phiên', 190], ['warehouse_code', 'Kho', 180], ['line_count', 'Số dòng', 100], ['counted_line_count', 'Đã đếm', 100], ['started_at', 'Bắt đầu', 190], ['posted_at', 'Ngày ghi sổ', 190], ['status', 'Trạng thái', 125]],
    actions: [{ key: 'submit', label: 'Gửi duyệt', target_status: 'submitted', permission: 'inventory_stocktake_create', endpoint: (record) => `/api/v1/inventory/stocktakes/${record.stocktake_id}/submit`, statuses: ['counting', 'draft'] }, { key: 'approve', label: 'Duyệt', target_status: 'approved', permission: 'inventory_stocktake_adjust', endpoint: (record) => `/api/v1/inventory/stocktakes/${record.stocktake_id}/approve`, statuses: ['submitted'] }, { key: 'post', label: 'Ghi sổ', target_status: 'posted', permission: 'inventory_stocktake_adjust', endpoint: (record) => `/api/v1/inventory/stocktakes/${record.stocktake_id}/post`, statuses: ['approved'] }],
  },
  transfers: {
    module_key: 'inventory', title: 'Điều chuyển kho', description: 'Điều chuyển vật tư giữa các kho trong một giao dịch có thể truy vết.', endpoint: '/api/v1/inventory/transfers', row_key: 'transfer_id', search_placeholder: 'Tìm theo mã phiếu hoặc kho', search_param: 'search', status_param: 'status', read_permission: 'inventory_transfer_read', create_permission: 'inventory_transfer_create',
    status_options: ['draft', 'posted', 'cancelled'], columns: [['transfer_code', 'Mã điều chuyển', 190], ['source_warehouse_code', 'Kho xuất', 160], ['destination_warehouse_code', 'Kho nhận', 160], ['line_count', 'Số dòng', 100], ['created_at', 'Ngày lập', 190], ['status', 'Trạng thái', 125]],
    actions: [{ key: 'post', label: 'Ghi sổ', target_status: 'posted', permission: 'inventory_transfer_post', endpoint: (record) => `/api/v1/inventory/transfers/${record.transfer_id}/post`, statuses: ['draft'] }],
  },
  materials: {
    module_key: 'production', title: 'Định mức và vật tư', description: 'Quản lý phiên bản BOM, định lượng cơ sở và trạng thái hiệu lực cho sản phẩm.', endpoint: '/api/v1/production/boms', row_key: 'bom_id', search_placeholder: 'Tìm theo mã BOM hoặc thành phẩm', search_param: 'search', status_param: 'status', read_permission: 'production_bom_read', create_permission: 'production_bom_create',
    status_options: ['draft', 'active', 'inactive'], columns: [['bom_code', 'Mã định mức', 165], ['product_item_code', 'Mã thành phẩm', 150], ['product_item_name', 'Tên thành phẩm', 270], ['version_number', 'Phiên bản', 100], ['valid_from', 'Hiệu lực từ', 130], ['valid_to', 'Hiệu lực đến', 130], ['line_count', 'Số dòng', 100], ['status', 'Trạng thái', 125]],
    actions: [{ key: 'activate', label: 'Kích hoạt', target_status: 'active', permission: 'production_bom_approve', endpoint: (record) => `/api/v1/production/boms/${record.bom_id}/status`, body: { status: 'active' }, statuses: ['draft', 'inactive'] }, { key: 'deactivate', label: 'Ngừng hiệu lực', target_status: 'inactive', permission: 'production_bom_approve', endpoint: (record) => `/api/v1/production/boms/${record.bom_id}/status`, body: { status: 'inactive' }, statuses: ['active'] }],
  },
  orders: {
    module_key: 'production', title: 'Lệnh sản xuất', description: 'Phát hành và theo dõi tiến độ lệnh theo kế hoạch và định mức đã duyệt.', endpoint: '/api/v1/production/orders', row_key: 'production_order_id', search_placeholder: 'Tìm theo mã lệnh hoặc thành phẩm', search_param: 'search', status_param: 'status', read_permission: 'production_order_read', create_permission: 'production_order_create',
    status_options: ['planned', 'released', 'in_progress', 'completed', 'cancelled'], columns: [['order_code', 'Mã lệnh', 205], ['stock_item_code', 'Mã thành phẩm', 145], ['stock_item_name', 'Tên thành phẩm', 270], ['target_quantity', 'Số lượng kế hoạch', 145], ['planned_starts_on', 'Bắt đầu', 125], ['planned_ends_on', 'Kết thúc', 125], ['status', 'Trạng thái', 125]],
    actions: [{ key: 'release', label: 'Phát hành', target_status: 'released', permission: 'production_order_release', endpoint: (record) => `/api/v1/production/orders/${record.production_order_id}/release`, statuses: ['planned'] }, { key: 'complete', label: 'Hoàn tất', target_status: 'completed', permission: 'production_order_complete', endpoint: (record) => `/api/v1/production/orders/${record.production_order_id}/complete`, statuses: ['in_progress'] }, { key: 'cancel', label: 'Hủy lệnh', target_status: 'cancelled', permission: 'production_order_update', endpoint: (record) => `/api/v1/production/orders/${record.production_order_id}/cancel`, statuses: ['planned', 'released', 'in_progress'] }],
  },
  assignments: {
    module_key: 'production', title: 'Phân công nhân sự', description: 'Xếp nhân viên vào ca và lệnh sản xuất, tránh trùng lịch làm việc.', endpoint: '/api/v1/production/assignments', row_key: 'production_assignment_id', search_placeholder: 'Lọc theo mã lệnh hoặc nhân viên', search_param: null, status_param: 'status', read_permission: 'production_assignment_read', create_permission: 'production_assignment_create',
    status_options: ['planned', 'assigned', 'in_progress', 'completed', 'cancelled'], columns: [['order_code', 'Mã lệnh', 205], ['employee_code', 'Mã nhân viên', 130], ['employee_name', 'Nhân viên', 190], ['shift_name', 'Ca làm việc', 145], ['assignment_name', 'Nhiệm vụ', 210], ['starts_at', 'Bắt đầu', 190], ['ends_at', 'Kết thúc', 190], ['status', 'Trạng thái', 125]],
    actions: [{ key: 'start', label: 'Bắt đầu', target_status: 'in_progress', permission: 'production_assignment_update', endpoint: (record) => `/api/v1/production/assignments/${record.production_assignment_id}/status`, body: { status: 'in_progress' }, statuses: ['planned', 'assigned'] }, { key: 'complete', label: 'Hoàn tất', target_status: 'completed', permission: 'production_assignment_update', endpoint: (record) => `/api/v1/production/assignments/${record.production_assignment_id}/status`, body: { status: 'completed' }, statuses: ['in_progress'] }],
  },
  finished_products: {
    module_key: 'production', title: 'Sản lượng thành phẩm', description: 'Theo dõi các lệnh sản xuất đã phát hành để mở chi tiết sản lượng và bàn giao thành phẩm.', endpoint: '/api/v1/production/orders', row_key: 'production_order_id', search_placeholder: 'Tìm theo mã lệnh hoặc thành phẩm', search_param: 'search', status_param: 'status', read_permission: 'production_order_read', create_permission: 'production_output_create',
    status_options: ['released', 'in_progress', 'completed'], columns: [['order_code', 'Mã lệnh', 205], ['stock_item_code', 'Mã thành phẩm', 145], ['stock_item_name', 'Tên thành phẩm', 270], ['target_quantity', 'Sản lượng kế hoạch', 150], ['planned_ends_on', 'Hạn hoàn thành', 135], ['status', 'Trạng thái', 125]],
    actions: [],
  },
};

const create_schemas = {
  absences: [
    { key: 'request_code', label: 'Mã đơn', required: true }, { key: 'employee_id', label: 'Mã định danh nhân viên', type: 'number', required: true }, { key: 'leave_type_code', label: 'Loại nghỉ', type: 'select', options: [{ value: 'annual_leave', label: 'Nghỉ phép năm' }, { value: 'sick_leave', label: 'Nghỉ ốm' }, { value: 'family_leave', label: 'Nghỉ việc gia đình' }, { value: 'unpaid_leave', label: 'Nghỉ không lương' }], required: true }, { key: 'starts_on', label: 'Từ ngày', type: 'date', required: true }, { key: 'ends_on', label: 'Đến ngày', type: 'date', required: true }, { key: 'is_paid', label: 'Có hưởng lương', type: 'checkbox', default_value: true }, { key: 'reason', label: 'Lý do', type: 'textarea', required: false },
  ],
  rewards_discipline: [
    { key: 'record_code', label: 'Mã quyết định', required: true }, { key: 'employee_id', label: 'Mã định danh nhân viên', type: 'number', required: true }, { key: 'event_type', label: 'Loại sự kiện', type: 'select', options: [{ value: 'reward', label: 'Khen thưởng' }, { value: 'discipline', label: 'Kỷ luật' }], required: true }, { key: 'effective_on', label: 'Ngày hiệu lực', type: 'date', required: true }, { key: 'reason', label: 'Lý do', type: 'textarea', required: true }, { key: 'amount', label: 'Số tiền', type: 'number', required: false, default_value: 0 }, { key: 'currency_code', label: 'Mã tiền tệ', default_value: 'VND', required: true },
  ],
  payroll: [{ key: 'year', label: 'Năm', type: 'number', required: true }, { key: 'month', label: 'Tháng', type: 'number', required: true }],
  receipts: [{ key: 'receipt_code', label: 'Mã phiếu', required: true }, { key: 'warehouse_id', label: 'Mã định danh kho', type: 'number', required: true }, { key: 'supplier_id', label: 'Mã định danh nhà cung cấp', type: 'number', required: false }, { key: 'notes', label: 'Ghi chú', type: 'textarea', required: false }, { key: 'lines', label: 'Dòng vật tư', type: 'lines', required: true, fields: [{ key: 'stock_item_id', label: 'Mã vật tư', type: 'number' }, { key: 'warehouse_location_id', label: 'Mã vị trí kho', type: 'number' }, { key: 'quantity', label: 'Số lượng', type: 'number' }] }],
  issues: [{ key: 'issue_code', label: 'Mã phiếu', required: true }, { key: 'warehouse_id', label: 'Mã định danh kho', type: 'number', required: true }, { key: 'reason_code', label: 'Mã lý do', required: false }, { key: 'notes', label: 'Ghi chú', type: 'textarea', required: false }, { key: 'lines', label: 'Dòng vật tư', type: 'lines', required: true, fields: [{ key: 'stock_item_id', label: 'Mã vật tư', type: 'number' }, { key: 'warehouse_location_id', label: 'Mã vị trí kho', type: 'number' }, { key: 'quantity', label: 'Số lượng', type: 'number' }] }],
  transfers: [{ key: 'transfer_code', label: 'Mã điều chuyển', required: true }, { key: 'source_warehouse_id', label: 'Mã kho xuất', type: 'number', required: true }, { key: 'destination_warehouse_id', label: 'Mã kho nhận', type: 'number', required: true }, { key: 'notes', label: 'Ghi chú', type: 'textarea', required: false }, { key: 'lines', label: 'Dòng vật tư', type: 'lines', required: true, fields: [{ key: 'stock_item_id', label: 'Mã vật tư', type: 'number' }, { key: 'source_location_id', label: 'Vị trí xuất', type: 'number' }, { key: 'destination_location_id', label: 'Vị trí nhận', type: 'number' }, { key: 'quantity', label: 'Số lượng', type: 'number' }] }],
  stocktakes: [{ key: 'stocktake_code', label: 'Mã phiên kiểm kê', required: true }, { key: 'warehouse_id', label: 'Mã định danh kho', type: 'number', required: true }, { key: 'notes', label: 'Ghi chú', type: 'textarea', required: false }],
  materials: [{ key: 'bom_code', label: 'Mã định mức', required: true }, { key: 'stock_item_id', label: 'Mã thành phẩm', type: 'number', required: true }, { key: 'version_number', label: 'Phiên bản', type: 'number', required: true, default_value: 1 }, { key: 'base_quantity', label: 'Số lượng cơ sở', type: 'number', required: true }, { key: 'valid_from', label: 'Hiệu lực từ', type: 'date', required: true }, { key: 'valid_to', label: 'Hiệu lực đến', type: 'date', required: false }, { key: 'notes', label: 'Ghi chú', type: 'textarea', required: false }, { key: 'lines', label: 'Dòng định mức', type: 'lines', required: true, fields: [{ key: 'material_stock_item_id', label: 'Mã nguyên vật liệu', type: 'number' }, { key: 'quantity_per_base', label: 'Định lượng', type: 'number' }, { key: 'scrap_percent', label: 'Tỷ lệ hao hụt (%)', type: 'number', required: false }] }],
  orders: [{ key: 'order_code', label: 'Mã lệnh', required: true }, { key: 'production_plan_line_id', label: 'Mã dòng kế hoạch', type: 'number', required: true }, { key: 'bom_id', label: 'Mã định mức', type: 'number', required: true }, { key: 'target_quantity', label: 'Số lượng kế hoạch', type: 'number', required: true }, { key: 'planned_starts_on', label: 'Ngày bắt đầu', type: 'date', required: true }, { key: 'planned_ends_on', label: 'Ngày kết thúc', type: 'date', required: true }, { key: 'production_line_name', label: 'Dây chuyền', required: false }, { key: 'notes', label: 'Ghi chú', type: 'textarea', required: false }],
  assignments: [{ key: 'production_order_id', label: 'Mã lệnh sản xuất', type: 'number', required: true }, { key: 'employee_id', label: 'Mã định danh nhân viên', type: 'number', required: true }, { key: 'work_shift_id', label: 'Mã ca làm việc', type: 'number', required: false }, { key: 'assignment_name', label: 'Nhiệm vụ', required: false }, { key: 'starts_at', label: 'Bắt đầu (ISO)', required: true }, { key: 'ends_at', label: 'Kết thúc (ISO)', required: true }, { key: 'notes', label: 'Ghi chú', type: 'textarea', required: false }],
};

Object.entries(create_schemas).forEach(([feature_key, schema]) => {
  if (feature_catalog[feature_key]) feature_catalog[feature_key].create_schema = schema;
});

function format_cell(value, column_key) {
  if (value === null || value === undefined || value === '') return '—';
  if (column_key === 'amount' || column_key === 'total_net_amount' || column_key === 'target_quantity' || column_key === 'standard_working_days') {
    const number = Number(value);
    if (Number.isFinite(number)) return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 }).format(number);
  }
  if (typeof value === 'string' && value.includes('T')) return value.replace('T', ' ').replace('Z', '').slice(0, 19);
  return String(value);
}

function display_status(value) {
  return <Tag color={status_colors[value]}>{status_labels[value] || value || 'Chưa xác định'}</Tag>;
}

function render_create_field(field, field_props = {}) {
  if (field.type === 'number') return <InputNumber {...field_props} min={field.min ?? 0} style={{ width: '100%' }} />;
  if (field.type === 'date') return <Input {...field_props} type="date" />;
  if (field.type === 'select') return <Select {...field_props} options={field.options} />;
  if (field.type === 'textarea') return <Input.TextArea {...field_props} rows={3} />;
  return <Input {...field_props} />;
}

function CreateRecordModal({ config, open, on_close, on_created }) {
  const [form] = Form.useForm();
  const { message } = AntdApp.useApp();
  const schema = useMemo(() => config.create_schema || [], [config.create_schema]);
  const default_values = useMemo(() => Object.fromEntries(schema.map((field) => [field.key, field.type === 'lines' ? [{}] : field.default_value])), [schema]);

  useEffect(() => {
    if (open) {
      form.resetFields();
      form.setFieldsValue(default_values);
    }
  }, [default_values, form, open]);

  const on_finish = async (values) => {
    try {
      await request_api(config.endpoint, { method: 'POST', body: JSON.stringify(values) });
      message.success('Record created successfully.');
      on_close();
      on_created();
    } catch (error) {
      message.error(error.message || 'The record could not be created.');
      if (error.field_errors) form.setFields(Object.entries(error.field_errors).map(([name, errors]) => ({ name, errors: [errors] })));
    }
  };

  return <Modal open={open} title={`Tạo ${config.title.toLowerCase()}`} onCancel={on_close} footer={null} width={760} destroyOnClose>
    <Form form={form} layout="vertical" requiredMark={false} onFinish={on_finish} className="workflow_create_form">
      {schema.map((field) => {
        if (field.type === 'lines') return <div key={field.key} className="workflow_form_lines"><Typography.Text strong>{field.label}</Typography.Text><Form.List name={field.key} rules={[{ validator: async (_, values) => values?.length ? Promise.resolve() : Promise.reject(new Error('At least one line is required.')) }]}>{(fields, { add, remove }) => <><div className="workflow_form_line_list">{fields.map(({ key, name, ...rest_field }) => <div className="workflow_form_line" key={key}>{field.fields.map((line_field) => <Form.Item {...rest_field} key={line_field.key} name={[name, line_field.key]} label={line_field.label} rules={line_field.required === false ? [] : [{ required: true, message: `${line_field.label} is required.` }]}>{render_create_field(line_field)}</Form.Item>)}<Button type="text" danger icon={<StopOutlined />} aria-label="Xóa dòng" onClick={() => remove(name)} /></div>)}</div><Button type="dashed" block onClick={() => add({})} icon={<PlusOutlined />}>Thêm dòng</Button></>}</Form.List></div>;
        if (field.type === 'checkbox') return <Form.Item key={field.key} name={field.key} valuePropName="checked"><Checkbox>{field.label}</Checkbox></Form.Item>;
        return <Form.Item key={field.key} label={field.label} name={field.key} rules={field.required === false ? [] : [{ required: true, message: `${field.label} is required.` }]}>{render_create_field(field)}</Form.Item>;
      })}
      <Space><Button onClick={on_close}>Hủy</Button><Button type="primary" htmlType="submit">Lưu dữ liệu</Button></Space>
    </Form>
  </Modal>;
}

function ModuleDataPage({ feature_key }) {
  const config = feature_catalog[feature_key] || feature_catalog.absences;
  const module_visual = module_visuals[config.module_key] || module_visuals.human_resources;
  const { current_user } = use_auth();
  const { message } = AntdApp.useApp();
  const [search_params, set_search_params] = useSearchParams();
  const [records, set_records] = useState([]);
  const [filters, set_filters] = useState(() => ({ search: search_params.get('search') || '', status: search_params.get('status') || '' }));
  const [search_input, set_search_input] = useState(() => search_params.get('search') || '');
  const [pagination, set_pagination] = useState(() => ({ current: Math.max(Number(search_params.get('page')) || 1, 1), page_size: 50, total: 0 }));
  const current_page = pagination.current;
  const current_page_size = pagination.page_size;
  const [is_loading, set_is_loading] = useState(true);
  const [error_message, set_error_message] = useState('');
  const request_sequence = useRef(0);
  const [create_open, set_create_open] = useState(false);
  const can_create = Boolean(config.create_schema && has_permission(current_user, config.create_permission));
  const can_write = config.actions.some((action) => has_permission(current_user, action.permission));

  const sync_query = useCallback((next_filters, next_page = null) => {
    set_search_params((current) => {
      const params = new URLSearchParams(current);
      for (const [key, value] of Object.entries(next_filters)) {
        if (value) params.set(key, value);
        else params.delete(key);
      }
      if (next_page) params.set('page', String(next_page));
      else params.delete('page');
      return params;
    }, { replace: true });
  }, [set_search_params]);

  const load_records = useCallback(async (next_filters, next_page, next_page_size) => {
    const sequence = request_sequence.current + 1;
    request_sequence.current = sequence;
    set_is_loading(true);
    set_error_message('');
    const params = new URLSearchParams({ page: String(next_page - 1), page_size: String(next_page_size) });
    if (config.search_param && next_filters.search.trim()) params.set(config.search_param, next_filters.search.trim());
    if (config.status_param && next_filters.status) params.set(config.status_param, next_filters.status);
    try {
      const response = await request_api(`${config.endpoint}?${params.toString()}`);
      if (sequence !== request_sequence.current) return;
      const page_data = response.data || {};
      const items = Array.isArray(page_data.items) ? page_data.items : [];
      set_records(items);
      set_pagination({ current: Number(page_data.page || 0) + 1, page_size: Number(page_data.page_size || next_page_size), total: Number(page_data.total_items || 0) });
    } catch (error) {
      if (sequence !== request_sequence.current) return;
      set_records([]);
      set_error_message(error.message || 'The data could not be loaded.');
    } finally {
      if (sequence === request_sequence.current) set_is_loading(false);
    }
  }, [config]);

  useEffect(() => {
    load_records(filters, current_page, current_page_size);
  }, [current_page, current_page_size, filters, load_records]);

  const run_action = useCallback((action, record) => {
    Modal.confirm({
      title: `${action.label} bản ghi?`,
      content: `Thao tác này sẽ chuyển trạng thái sang “${status_labels[action.target_status] || action.target_status}”.`,
      okText: 'Xác nhận',
      cancelText: 'Hủy',
      okButtonProps: { type: action.key === 'cancel' || action.key === 'reject' ? 'default' : 'primary', danger: action.key === 'cancel' || action.key === 'reject' },
      onOk: async () => {
        try {
          await request_api(action.endpoint(record), { method: 'POST', ...(action.body ? { body: JSON.stringify(action.body) } : {}) });
          message.success('Workflow action completed successfully.');
          await load_records(filters, pagination.current, pagination.page_size);
        } catch (error) {
          message.error(error.message || 'The workflow action could not be completed.');
        }
      },
    });
  }, [filters, load_records, message, pagination]);

  const columns = useMemo(() => {
    const base_columns = config.columns.map(([key, title, width]) => ({
      title, dataIndex: key, key, width, ellipsis: true,
      render: (value) => key === 'status' ? display_status(value) : format_cell(value, key),
    }));
    const actions = config.actions.filter((action) => has_permission(current_user, action.permission));
    return [...base_columns, {
      title: 'Thao tác', key: 'actions', fixed: 'right', width: actions.length ? 220 : 100,
      render: (_, record) => <Space size={2}>
        <Button type="text" icon={<EyeOutlined />} aria-label="Xem chi tiết" onClick={() => document.dispatchEvent(new CustomEvent('vinamik:open_record', { detail: record }))} />
        {actions.filter((action) => action.statuses.includes(record.status)).map((action) => <Button key={action.key} type="text" icon={action.key === 'cancel' || action.key === 'reject' ? <StopOutlined /> : action.key === 'approve' || action.key === 'complete' ? <CheckOutlined /> : <SendOutlined />} danger={action.key === 'cancel' || action.key === 'reject'} onClick={() => run_action(action, record)}>{action.label}</Button>)}
      </Space>,
    }];
  }, [config, current_user, run_action]);

  const open_record_event = useCallback((event) => {
    const record = event.detail;
    if (!record) return;
    const labels = config.columns.map(([key, title]) => ({ key, title, value: format_cell(record[key], key) }));
    Modal.info({ title: `Chi tiết ${record[config.columns[0][0]] || 'bản ghi'}`, width: 700, content: <div className="workflow_detail_grid">{labels.map((item) => <div key={item.key}><span>{item.title}</span><strong>{item.key === 'status' ? display_status(record[item.key]) : item.value}</strong></div>)}</div>, okText: 'Đóng' });
  }, [config]);

  useEffect(() => {
    document.addEventListener('vinamik:open_record', open_record_event);
    return () => document.removeEventListener('vinamik:open_record', open_record_event);
  }, [open_record_event]);

  const column_presets = useMemo(() => ({
    overview: [config.columns[0][0], config.columns[1]?.[0], config.columns[2]?.[0], 'status', 'actions'],
    detail: [...config.columns.map(([key]) => key), 'actions'],
    audit: [config.columns[0][0], ...config.columns.slice(-3).map(([key]) => key), 'status'],
  }), [config]);

  const on_search = () => {
    const next_filters = { ...filters, search: search_input };
    set_filters(next_filters);
    set_pagination((current) => ({ ...current, current: 1 }));
    sync_query(next_filters, 1);
  };
  const on_status_change = (status) => {
    const next_filters = { ...filters, status: status || '' };
    set_filters(next_filters);
    set_pagination((current) => ({ ...current, current: 1 }));
    sync_query(next_filters, 1);
  };

  return <AntdApp>
    <div className={`module_data_page module_data_page_${config.module_key}`}>
    <div className="page_heading module_surface_header"><div className="module_heading_identity"><span className="module_heading_mark">{module_visual.icon}</span><div><Typography.Title level={2}>{config.title}</Typography.Title><Typography.Paragraph>{config.description}</Typography.Paragraph></div></div><Tag color={can_write || can_create ? 'blue' : 'gold'} icon={can_write || can_create ? <CheckOutlined /> : <LockOutlined />}>{can_write || can_create ? 'Có thể thao tác theo quyền' : 'Chỉ xem'}</Tag></div>
    <div className={'module_context_band module_context_band_' + config.module_key}><span>{module_visual.context}</span>{config.module_key === 'human_resources' && <strong>Nhận diện người trước khi thao tác</strong>}{config.module_key === 'inventory' && <strong>Chứng từ là nguồn sự thật của tồn kho</strong>}{config.module_key === 'production' && <strong>Tiến độ đi theo trạng thái lệnh</strong>}</div>
    {config.module_key === 'production' && <div className="production_stage_strip" aria-label="Trạng thái lệnh sản xuất"><span>Đã lập kế hoạch</span><i /> <span>Đã phát hành</span><i /> <span>Đang thực hiện</span><i /> <span>Đã hoàn thành</span></div>}
    <DataWorkspace
      module_key={config.module_key}
      title={`Danh sách ${config.title.toLowerCase()}`}
      description="Dữ liệu tải theo trang từ máy chủ trung tâm. Nhấp đúp hoặc dùng nút xem để mở chi tiết."
      toolbar={<Space wrap className="list_toolbar control_row"><Input.Search allowClear placeholder={config.search_placeholder} value={search_input} onChange={(event) => set_search_input(event.target.value)} onSearch={on_search} style={{ width: 310 }} disabled={!config.search_param} /><Select allowClear placeholder="Tất cả trạng thái" value={filters.status || undefined} options={config.status_options.map((value) => ({ value, label: status_labels[value] || value }))} onChange={on_status_change} style={{ width: 180 }} />{config.create_schema && <Button type="primary" icon={<PlusOutlined />} disabled={!can_create} onClick={() => set_create_open(true)}>{can_create ? 'Tạo mới' : 'Tạo mới (không đủ quyền)'}</Button>}</Space>}
      columns={columns}
      data_source={records}
      row_key={config.row_key}
      loading={is_loading}
      pagination={pagination}
      on_change={(next_pagination) => { set_pagination((current) => ({ ...current, current: next_pagination.current, page_size: next_pagination.pageSize })); sync_query(filters, next_pagination.current); }}
      empty_text={error_message ? 'Không thể tải dữ liệu' : 'Chưa có dữ liệu phù hợp'}
      total_label="bản ghi"
      read_only={!can_write && !can_create}
      column_presets={column_presets}
      storage_key={`data_workspace_${config.module_key}_${feature_key}_${current_user?.username || 'account'}`}
    />
    {error_message && <Typography.Paragraph type="danger" className="workflow_error">{error_message}</Typography.Paragraph>}
    </div>
    {config.create_schema && <CreateRecordModal config={config} open={create_open} on_close={() => set_create_open(false)} on_created={() => load_records(filters, current_page, current_page_size)} />}
  </AntdApp>;
}

export { feature_catalog };
export default ModuleDataPage;


