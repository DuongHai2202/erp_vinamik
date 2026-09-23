import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Alert, App as antd_app, Button, Checkbox, Form, Input, InputNumber, Modal, Select, Space, Table, Tag, Typography } from 'antd';
import { PlusOutlined, StopOutlined } from '@ant-design/icons';
import { useSearchParams } from 'react-router-dom';
import { has_permission } from '../common/permission_utils';
import { request_api } from '../common/api_client';
import { use_auth } from '../identity/auth_context';
import DataWorkspace from './data_workspace';
import WorkflowDetailDrawer from './workflow_detail_drawer';
import { LookupField, PlanLineField } from './workflow_fields';
import RecordActionBar from './record_action_bar';
import ModuleMasthead from './module_masthead';
import DebouncedSearchInput from './debounced_search_input';
import { format_datetime_vn, format_field_value, format_number_vn, money_keys, to_iso_date, VietnameseDateInput } from '../common/formatters';

const AntdApp = antd_app;

const status_labels = {
  draft: 'Bản nháp', pending: 'Chờ duyệt', submitted: 'Đã gửi', calculated: 'Đã tính', approved: 'Đã duyệt',
  released: 'Đã phát hành', in_progress: 'Đang thực hiện', completed: 'Đã hoàn thành', posted: 'Đã ghi sổ',
  rejected: 'Từ chối', cancelled: 'Đã hủy', active: 'Đang hoạt động', inactive: 'Ngừng hoạt động',
  planned: 'Đã lập kế hoạch', paused: 'Tạm dừng', counted: 'Đã kiểm đếm', locked: 'Đã khóa',
  passed: 'Đạt', failed: 'Không đạt', held: 'Tạm giữ', open: 'Mở', resolved: 'Đã xử lý', published: 'Đã công bố', counting: 'Đang kiểm kê', pending_receipt: 'Chờ nhập kho', expired: 'Đã hết hạn', terminated: 'Đã chấm dứt', received: 'Đã nhập kho', quarantined: 'Cách ly', calculating: 'Đang tính',
};



const field_value_labels = {
  raw_material: 'Nguyên vật liệu',
  indefinite: 'Không xác định thời hạn',
  fixed_term_12_months: 'Xác định thời hạn 12 tháng',
  fixed_term_36_months: 'Xác định thời hạn 36 tháng',
  finished_product: 'Thành phẩm',
  kg: 'Kilôgam',
  litre: 'Lít',
  piece: 'Cái',
  reward: 'Khen thưởng',
  discipline: 'Kỷ luật',
  annual_leave: 'Nghỉ phép năm',
  sick_leave: 'Nghỉ ốm',
  personal_unpaid: 'Nghỉ không lương',
  maternity_leave: 'Nghỉ thai sản',
  family_leave: 'Nghỉ việc riêng',
  monthly_mon_sat_v1: 'Theo tháng, thứ Hai–thứ Bảy',
  weighted_average: 'Bình quân gia quyền',
  actual_hours: 'Giờ thực tế',
  good_quantity: 'Sản lượng đạt',
  exclude_from_good: 'Loại khỏi sản lượng đạt',
  appearance: 'Ngoại quan',
  seal_integrity: 'Độ kín bao bì',
  net_weight: 'Khối lượng tịnh',
  microbiology: 'Vi sinh',
  label_information: 'Thông tin nhãn',
  hold: 'Tạm giữ',
  rework: 'Gia công lại',
  scrap: 'Loại bỏ',
  return: 'Trả về',
};

const status_colors = {
  pending: 'gold', submitted: 'gold', calculated: 'blue', approved: 'green', released: 'blue',
  in_progress: 'processing', paused: 'orange', completed: 'green', posted: 'green', rejected: 'red', cancelled: 'default',
  active: 'green', inactive: 'default', planned: 'blue', counted: 'cyan', locked: 'purple', draft: 'default',
  passed: 'green', failed: 'red', held: 'orange', open: 'gold', resolved: 'green', published: 'blue', counting: 'processing', pending_receipt: 'gold', expired: 'orange', terminated: 'default', received: 'green', quarantined: 'red', calculating: 'processing',
};

function display_status(value) {
  return <Tag color={status_colors[value]}>{status_labels[value] || field_value_labels[value] || value || 'Chưa xác định'}</Tag>;
}

const feature_catalog = {
  absences: {
    module_key: 'human_resources', title: 'Nghỉ phép và vắng mặt',
    description: 'Tiếp nhận, theo dõi và phê duyệt đơn nghỉ từ dữ liệu nhân sự trung tâm.',
    endpoint: '/api/v1/human_resources/absences', row_key: 'leave_request_id',
    search_placeholder: 'Tìm theo mã đơn hoặc nhân viên', search_param: 'search', status_param: 'status',
    read_permission: 'hr_absence_read', create_permission: 'hr_absence_create', update_permission: 'hr_absence_update',
    detail_kind: 'absence', edit_statuses: ['draft', 'pending'],
    status_options: ['draft', 'pending', 'approved', 'rejected', 'cancelled'],
    columns: [['request_code', 'Mã đơn', 165], ['employee_code', 'Mã nhân viên', 130], ['employee_name', 'Nhân viên', 190], ['leave_type_code', 'Loại nghỉ', 145], ['starts_on', 'Từ ngày', 120], ['ends_on', 'Đến ngày', 120], ['day_count', 'Số ngày', 90], ['status', 'Trạng thái', 125]],
    actions: [
      { key: 'approve', label: 'Duyệt', target_status: 'approved', permission: 'hr_absence_approve', endpoint: (record) => '/api/v1/human_resources/absences/' + record.leave_request_id + '/decision', body: { status: 'approved', decision_note: null }, statuses: ['pending'] },
      { key: 'reject', label: 'Từ chối', target_status: 'rejected', permission: 'hr_absence_approve', endpoint: (record) => '/api/v1/human_resources/absences/' + record.leave_request_id + '/decision', body: { status: 'rejected', decision_note: null }, statuses: ['pending'] },
      { key: 'cancel', label: 'Hủy đơn', target_status: 'cancelled', permission: 'hr_absence_update', endpoint: (record) => '/api/v1/human_resources/absences/' + record.leave_request_id + '/cancel', statuses: ['draft', 'pending'] },
    ],
  },
  rewards_discipline: {
    module_key: 'human_resources', title: 'Khen thưởng và kỷ luật',
    description: 'Quản lý các quyết định thưởng, phạt và đầu vào đã duyệt cho kỳ lương.',
    endpoint: '/api/v1/human_resources/rewards_discipline', row_key: 'employee_reward_discipline_id',
    search_placeholder: 'Tìm theo mã hoặc nhân viên', search_param: 'search', status_param: 'status',
    read_permission: 'hr_reward_read', create_permission: 'hr_reward_create', update_permission: 'hr_reward_update',
    detail_kind: 'reward', edit_statuses: ['draft'],
    status_options: ['draft', 'pending', 'approved', 'rejected', 'cancelled'],
    columns: [['record_code', 'Mã quyết định', 165], ['employee_code', 'Mã nhân viên', 130], ['employee_name', 'Nhân viên', 190], ['event_type', 'Loại sự kiện', 135], ['effective_on', 'Ngày hiệu lực', 130], ['amount', 'Số tiền', 125], ['currency_code', 'Tiền tệ', 95], ['status', 'Trạng thái', 125]],
    actions: [
      { key: 'submit', label: 'Gửi duyệt', target_status: 'pending', permission: 'hr_reward_update', endpoint: (record) => '/api/v1/human_resources/rewards_discipline/' + record.employee_reward_discipline_id + '/submit', statuses: ['draft'] },
      { key: 'approve', label: 'Duyệt', target_status: 'approved', permission: 'hr_reward_approve', endpoint: (record) => '/api/v1/human_resources/rewards_discipline/' + record.employee_reward_discipline_id + '/decision', body: { status: 'approved', decision_note: null }, statuses: ['pending'] },
      { key: 'reject', label: 'Từ chối', target_status: 'rejected', permission: 'hr_reward_approve', endpoint: (record) => '/api/v1/human_resources/rewards_discipline/' + record.employee_reward_discipline_id + '/decision', body: { status: 'rejected', decision_note: null }, statuses: ['pending'] },
      { key: 'cancel', label: 'Hủy quyết định', target_status: 'cancelled', permission: 'hr_reward_update', endpoint: (record) => '/api/v1/human_resources/rewards_discipline/' + record.employee_reward_discipline_id + '/cancel', statuses: ['draft', 'pending'] },
      { key: 'delete', label: 'Xóa bản nháp', permission: 'hr_reward_delete', method: 'DELETE', endpoint: (record) => '/api/v1/human_resources/rewards_discipline/' + record.employee_reward_discipline_id, statuses: ['draft'] },
    ],
  },
  payroll: {
    module_key: 'human_resources', title: 'Tính lương',
    description: 'Theo dõi kỳ lương, snapshot kết quả và trạng thái duyệt khóa.',
    endpoint: '/api/v1/human_resources/payroll/periods', row_key: 'payroll_period_id',
    search_placeholder: 'Lọc theo mã kỳ lương', search_param: null, status_param: 'status',
    read_permission: 'hr_payroll_read', create_permission: 'hr_payroll_create', delete_permission: 'hr_payroll_delete', detail_kind: 'payroll',
    status_options: ['draft', 'calculated', 'approved', 'rejected', 'locked'],
    columns: [['period_code', 'Mã kỳ lương', 170], ['starts_on', 'Từ ngày', 120], ['ends_on', 'Đến ngày', 120], ['standard_working_days', 'Ngày công chuẩn', 135], ['calculation_version', 'Công thức', 210], ['record_count', 'Số nhân sự', 110], ['total_net_amount', 'Thực lĩnh', 135], ['status', 'Trạng thái', 125]],
    actions: [
      { key: 'calculate', label: 'Tính lương', target_status: 'calculated', permission: 'hr_payroll_update', endpoint: (record) => '/api/v1/human_resources/payroll/periods/' + record.payroll_period_id + '/calculate', statuses: ['draft', 'rejected'] },
      { key: 'approve', label: 'Duyệt', target_status: 'approved', permission: 'hr_payroll_approve', endpoint: (record) => '/api/v1/human_resources/payroll/periods/' + record.payroll_period_id + '/approve', statuses: ['calculated'] },
      { key: 'reject', label: 'Trả lại', target_status: 'rejected', permission: 'hr_payroll_approve', endpoint: (record) => '/api/v1/human_resources/payroll/periods/' + record.payroll_period_id + '/reject', body: { decision_note: null }, statuses: ['calculated'] },
      { key: 'lock', label: 'Khóa kỳ', target_status: 'locked', permission: 'hr_payroll_approve', endpoint: (record) => '/api/v1/human_resources/payroll/periods/' + record.payroll_period_id + '/lock', statuses: ['approved'] },
      { key: 'delete', label: 'Xóa kỳ nháp', permission: 'hr_payroll_delete', method: 'DELETE', endpoint: (record) => '/api/v1/human_resources/payroll/periods/' + record.payroll_period_id, statuses: ['draft'] },
    ],
  },
  receipts: {
    module_key: 'inventory', title: 'Phiếu nhập kho',
    description: 'Kiểm tra chứng từ nhập và ghi sổ tập trung theo kho.',
    endpoint: '/api/v1/inventory/receipts', row_key: 'receipt_id',
    search_placeholder: 'Tìm theo mã phiếu hoặc kho', search_param: 'search', status_param: 'status',
    read_permission: 'inventory_receipt_read', create_permission: 'inventory_receipt_create', update_permission: 'inventory_receipt_update', delete_permission: 'inventory_receipt_delete', detail_kind: 'receipt',
    status_options: ['draft', 'pending', 'posted', 'cancelled'], edit_statuses: ['draft'],
    columns: [['receipt_code', 'Mã phiếu', 190], ['warehouse_code', 'Kho', 180], ['line_count', 'Số dòng', 100], ['created_at', 'Ngày lập', 190], ['posted_at', 'Ngày ghi sổ', 190], ['status', 'Trạng thái', 125]],
    actions: [
      { key: 'submit', label: 'Gửi duyệt', target_status: 'pending', permission: 'inventory_receipt_update', endpoint: (record) => '/api/v1/inventory/receipts/' + record.receipt_id + '/submit', statuses: ['draft'] },
      { key: 'post', label: 'Ghi sổ', target_status: 'posted', permission: 'inventory_receipt_post', endpoint: (record) => '/api/v1/inventory/receipts/' + record.receipt_id + '/post', statuses: ['draft', 'pending'] },
      { key: 'cancel', label: 'Hủy phiếu', target_status: 'cancelled', permission: 'inventory_receipt_update', endpoint: (record) => '/api/v1/inventory/receipts/' + record.receipt_id + '/cancel', statuses: ['draft', 'pending'] },
      { key: 'delete', label: 'Xóa bản nháp', permission: 'inventory_receipt_delete', method: 'DELETE', endpoint: (record) => '/api/v1/inventory/receipts/' + record.receipt_id, statuses: ['draft'] },
    ],
  },
  issues: {
    module_key: 'inventory', title: 'Phiếu xuất kho',
    description: 'Xuất vật tư theo chứng từ, kiểm tra tồn và ghi sổ không âm kho.',
    endpoint: '/api/v1/inventory/issues', row_key: 'issue_id',
    search_placeholder: 'Tìm theo mã phiếu hoặc kho', search_param: 'search', status_param: 'status',
    read_permission: 'inventory_issue_read', create_permission: 'inventory_issue_create', update_permission: 'inventory_issue_update', delete_permission: 'inventory_issue_delete', detail_kind: 'issue',
    status_options: ['draft', 'pending', 'posted', 'cancelled'], edit_statuses: ['draft'],
    columns: [['issue_code', 'Mã phiếu', 205], ['warehouse_code', 'Kho', 180], ['line_count', 'Số dòng', 100], ['created_at', 'Ngày lập', 190], ['posted_at', 'Ngày ghi sổ', 190], ['status', 'Trạng thái', 125]],
    actions: [
      { key: 'submit', label: 'Gửi duyệt', target_status: 'pending', permission: 'inventory_issue_update', endpoint: (record) => '/api/v1/inventory/issues/' + record.issue_id + '/submit', statuses: ['draft'] },
      { key: 'post', label: 'Ghi sổ', target_status: 'posted', permission: 'inventory_issue_post', endpoint: (record) => '/api/v1/inventory/issues/' + record.issue_id + '/post', statuses: ['draft', 'pending'] },
      { key: 'cancel', label: 'Hủy phiếu', target_status: 'cancelled', permission: 'inventory_issue_update', endpoint: (record) => '/api/v1/inventory/issues/' + record.issue_id + '/cancel', statuses: ['draft', 'pending'] },
      { key: 'delete', label: 'Xóa bản nháp', permission: 'inventory_issue_delete', method: 'DELETE', endpoint: (record) => '/api/v1/inventory/issues/' + record.issue_id, statuses: ['draft'] },
    ],
  },
  stocktakes: {
    module_key: 'inventory', title: 'Kiểm kê kho',
    description: 'Mở phiên kiểm kê, theo dõi tiến độ đếm và ghi nhận chênh lệch có kiểm soát.',
    endpoint: '/api/v1/inventory/stocktakes', row_key: 'stocktake_id',
    search_placeholder: 'Tìm theo mã phiên hoặc kho', search_param: 'search', status_param: 'status',
    read_permission: 'inventory_stocktake_read', create_permission: 'inventory_stocktake_create', detail_kind: 'stocktake',
    status_options: ['draft', 'counting', 'submitted', 'approved', 'posted', 'cancelled'],
    columns: [['stocktake_code', 'Mã phiên', 280], ['warehouse_code', 'Kho', 180], ['line_count', 'Số dòng', 100], ['counted_line_count', 'Đã đếm', 135], ['started_at', 'Bắt đầu', 190], ['posted_at', 'Ngày ghi sổ', 190], ['status', 'Trạng thái', 125]],
    actions: [
      { key: 'submit', label: 'Gửi duyệt', target_status: 'submitted', permission: 'inventory_stocktake_create', endpoint: (record) => '/api/v1/inventory/stocktakes/' + record.stocktake_id + '/submit', statuses: ['counting'] },
      { key: 'approve', label: 'Duyệt', target_status: 'approved', permission: 'inventory_stocktake_adjust', endpoint: (record) => '/api/v1/inventory/stocktakes/' + record.stocktake_id + '/approve', statuses: ['submitted'] },
      { key: 'post', label: 'Ghi sổ', target_status: 'posted', permission: 'inventory_stocktake_adjust', endpoint: (record) => '/api/v1/inventory/stocktakes/' + record.stocktake_id + '/post', statuses: ['approved'] },
      { key: 'cancel', label: 'Hủy phiên', target_status: 'cancelled', permission: 'inventory_stocktake_create', endpoint: (record) => '/api/v1/inventory/stocktakes/' + record.stocktake_id + '/cancel', statuses: ['draft', 'counting', 'submitted'] },
    ],
  },
  transfers: {
    module_key: 'inventory', title: 'Điều chuyển kho',
    description: 'Điều chuyển vật tư giữa các kho trong một giao dịch có thể truy vết.',
    endpoint: '/api/v1/inventory/transfers', row_key: 'transfer_id',
    search_placeholder: 'Tìm theo mã điều chuyển hoặc kho', search_param: 'search', status_param: 'status',
    read_permission: 'inventory_transfer_read', create_permission: 'inventory_transfer_create', update_permission: 'inventory_transfer_update', delete_permission: 'inventory_transfer_delete', detail_kind: 'transfer',
    status_options: ['draft', 'pending', 'posted', 'cancelled'], edit_statuses: ['draft'],
    columns: [['transfer_code', 'Mã điều chuyển', 190], ['source_warehouse_code', 'Kho xuất', 160], ['destination_warehouse_code', 'Kho nhận', 160], ['line_count', 'Số dòng', 100], ['created_at', 'Ngày lập', 190], ['status', 'Trạng thái', 125]],
    actions: [
      { key: 'submit', label: 'Gửi duyệt', target_status: 'pending', permission: 'inventory_transfer_update', endpoint: (record) => '/api/v1/inventory/transfers/' + record.transfer_id + '/submit', statuses: ['draft'] },
      { key: 'post', label: 'Ghi sổ', target_status: 'posted', permission: 'inventory_transfer_post', endpoint: (record) => '/api/v1/inventory/transfers/' + record.transfer_id + '/post', statuses: ['draft', 'pending'] },
      { key: 'cancel', label: 'Hủy phiếu', target_status: 'cancelled', permission: 'inventory_transfer_update', endpoint: (record) => '/api/v1/inventory/transfers/' + record.transfer_id + '/cancel', statuses: ['draft', 'pending'] },
      { key: 'delete', label: 'Xóa bản nháp', permission: 'inventory_transfer_delete', method: 'DELETE', endpoint: (record) => '/api/v1/inventory/transfers/' + record.transfer_id, statuses: ['draft'] },
    ],
  },
  materials: {
    module_key: 'production', title: 'Định mức và vật tư',
    description: 'Quản lý phiên bản BOM, định lượng cơ sở và trạng thái hiệu lực cho sản phẩm.',
    endpoint: '/api/v1/production/boms', row_key: 'bom_id',
    search_placeholder: 'Tìm theo mã BOM hoặc thành phẩm', search_param: 'search', status_param: 'status',
    read_permission: 'production_bom_read', create_permission: 'production_bom_create', update_permission: 'production_bom_update', delete_permission: 'production_bom_delete',
    detail_kind: 'bom', edit_statuses: ['draft'], status_options: ['draft', 'active', 'inactive'],
    columns: [['bom_code', 'Mã định mức', 165], ['product_item_code', 'Mã thành phẩm', 150], ['product_item_name', 'Tên thành phẩm', 270], ['version_number', 'Phiên bản', 100], ['valid_from', 'Hiệu lực từ', 130], ['valid_to', 'Hiệu lực đến', 130], ['line_count', 'Số dòng', 100], ['status', 'Trạng thái', 125]],
    actions: [
      { key: 'activate', label: 'Kích hoạt', target_status: 'active', permission: 'production_bom_approve', endpoint: (record) => '/api/v1/production/boms/' + record.bom_id + '/status', body: { status: 'active' }, statuses: ['draft', 'inactive'] },
      { key: 'deactivate', label: 'Ngừng hiệu lực', target_status: 'inactive', permission: 'production_bom_approve', endpoint: (record) => '/api/v1/production/boms/' + record.bom_id + '/status', body: { status: 'inactive' }, statuses: ['active'] },
      { key: 'delete', label: 'Xóa bản nháp', permission: 'production_bom_delete', method: 'DELETE', endpoint: (record) => '/api/v1/production/boms/' + record.bom_id, statuses: ['draft'] },
    ],
  },
  orders: {
    module_key: 'production', title: 'Lệnh sản xuất',
    description: 'Phát hành và theo dõi tiến độ lệnh theo kế hoạch và định mức đã duyệt.',
    endpoint: '/api/v1/production/orders', row_key: 'production_order_id',
    search_placeholder: 'Tìm theo mã lệnh hoặc thành phẩm', search_param: 'search', status_param: 'status',
    read_permission: 'production_order_read', create_permission: 'production_order_create', update_permission: 'production_order_update',
    detail_kind: 'production_order', edit_statuses: ['draft', 'planned'], status_options: ['draft', 'planned', 'released', 'in_progress', 'paused', 'completed', 'cancelled'],
    columns: [['order_code', 'Mã lệnh', 205], ['stock_item_code', 'Mã thành phẩm', 145], ['stock_item_name', 'Tên thành phẩm', 270], ['target_quantity', 'Số lượng kế hoạch', 145], ['planned_starts_on', 'Bắt đầu', 125], ['planned_ends_on', 'Kết thúc', 125], ['status', 'Trạng thái', 125]],
    actions: [
      { key: 'release', label: 'Phát hành', target_status: 'released', permission: 'production_order_release', endpoint: (record) => '/api/v1/production/orders/' + record.production_order_id + '/release', statuses: ['planned'] },
      { key: 'pause', label: 'Tạm dừng', target_status: 'paused', permission: 'production_order_update', endpoint: (record) => '/api/v1/production/orders/' + record.production_order_id + '/status', body: { status: 'paused' }, statuses: ['in_progress'] },
      { key: 'resume', label: 'Tiếp tục', target_status: 'in_progress', permission: 'production_order_update', endpoint: (record) => '/api/v1/production/orders/' + record.production_order_id + '/status', body: { status: 'in_progress' }, statuses: ['paused'] },
      { key: 'complete', label: 'Hoàn tất', target_status: 'completed', permission: 'production_order_complete', endpoint: (record) => '/api/v1/production/orders/' + record.production_order_id + '/complete', statuses: ['in_progress', 'paused'] },
      { key: 'cancel', label: 'Hủy lệnh', target_status: 'cancelled', permission: 'production_order_update', endpoint: (record) => '/api/v1/production/orders/' + record.production_order_id + '/cancel', statuses: ['planned', 'released'] },
    ],
  },
  assignments: {
    module_key: 'production', title: 'Phân công nhân sự',
    description: 'Xếp nhân viên vào ca và lệnh sản xuất, tránh trùng lịch làm việc.',
    endpoint: '/api/v1/production/assignments', row_key: 'production_assignment_id',
    search_placeholder: 'Lọc theo mã lệnh hoặc nhân viên', search_param: null, status_param: 'status',
    read_permission: 'production_assignment_read', create_permission: 'production_assignment_create', update_permission: 'production_assignment_update',
    detail_kind: 'assignment', edit_statuses: ['planned'], status_options: ['planned', 'active', 'completed', 'cancelled'],
    columns: [['order_code', 'Mã lệnh', 205], ['employee_code', 'Mã nhân viên', 130], ['employee_name', 'Nhân viên', 190], ['shift_name', 'Ca làm việc', 145], ['assignment_name', 'Nhiệm vụ', 210], ['starts_at', 'Bắt đầu', 190], ['ends_at', 'Kết thúc', 190], ['status', 'Trạng thái', 125]],
    actions: [
      { key: 'start', label: 'Bắt đầu', target_status: 'active', permission: 'production_assignment_update', endpoint: (record) => '/api/v1/production/assignments/' + record.production_assignment_id + '/status', body: { status: 'active' }, statuses: ['planned'] },
      { key: 'complete', label: 'Hoàn tất', target_status: 'completed', permission: 'production_assignment_update', endpoint: (record) => '/api/v1/production/assignments/' + record.production_assignment_id + '/status', body: { status: 'completed' }, statuses: ['active'] },
      { key: 'cancel', label: 'Hủy phân công', target_status: 'cancelled', permission: 'production_assignment_update', endpoint: (record) => '/api/v1/production/assignments/' + record.production_assignment_id + '/status', body: { status: 'cancelled' }, statuses: ['planned', 'active'] },
    ],
  },
  finished_products: {
    module_key: 'production', title: 'Sản lượng thành phẩm',
    description: 'Theo dõi các lệnh sản xuất đã phát hành để ghi nhận đạt/lỗi và bàn giao sang Kho.',
    endpoint: '/api/v1/production/orders', row_key: 'production_order_id',
    search_placeholder: 'Tìm theo mã lệnh hoặc thành phẩm', search_param: 'search', status_param: 'status',
    read_permission: 'production_order_read', create_permission: 'production_output_create', detail_create_permission: 'production_output_create',
    detail_kind: 'production_order', status_options: ['released', 'in_progress', 'paused', 'completed'],
    columns: [['order_code', 'Mã lệnh', 205], ['stock_item_code', 'Mã thành phẩm', 145], ['stock_item_name', 'Tên thành phẩm', 270], ['target_quantity', 'Sản lượng kế hoạch', 150], ['planned_ends_on', 'Hạn hoàn thành', 135], ['status', 'Trạng thái', 125]],
    actions: [],
  },
  quality_inspections: {
    module_key: 'quality_cost', title: 'Kiểm tra chất lượng',
    description: 'Ghi nhận kết quả kiểm tra theo lệnh, sản lượng và lô thành phẩm.',
    endpoint: '/api/v1/quality_cost/inspections', row_key: 'id', delete_permission: 'quality_inspection_delete',
    search_placeholder: 'Tìm mã kiểm tra hoặc mã lô', search_param: 'search', status_param: 'status',
    read_permission: 'quality_inspection_read', create_permission: 'quality_inspection_create', update_permission: 'quality_inspection_update',
    detail_kind: 'quality_inspection', edit_statuses: ['draft'], status_options: ['draft', 'submitted', 'passed', 'failed', 'held', 'released', 'cancelled'],
    columns: [['code', 'Mã kiểm tra', 170], ['stock_item_id', 'Mã thành phẩm', 135], ['lot_code', 'Mã lô', 160], ['inspected_quantity', 'Số lượng kiểm', 130], ['good_quantity', 'Đạt', 105], ['defective_quantity', 'Lỗi', 105], ['inspected_on', 'Ngày kiểm', 125], ['status', 'Trạng thái', 125]],
    actions: [
      { key: 'submit', label: 'Gửi duyệt', target_status: 'submitted', permission: 'quality_inspection_approve', endpoint: (record) => '/api/v1/quality_cost/inspections/' + record.id + '/status', body: { status: 'submitted' }, statuses: ['draft'] },
      { key: 'pass', label: 'Đạt', target_status: 'passed', permission: 'quality_inspection_approve', endpoint: (record) => '/api/v1/quality_cost/inspections/' + record.id + '/status', body: { status: 'passed' }, statuses: ['submitted'] },
      { key: 'fail', label: 'Không đạt', target_status: 'failed', permission: 'quality_inspection_approve', endpoint: (record) => '/api/v1/quality_cost/inspections/' + record.id + '/status', body: { status: 'failed' }, statuses: ['submitted'] },
      { key: 'hold', label: 'Giữ lại', target_status: 'held', permission: 'quality_inspection_approve', endpoint: (record) => '/api/v1/quality_cost/inspections/' + record.id + '/status', body: { status: 'held' }, statuses: ['submitted'] },
      { key: 'release', label: 'Giải phóng', target_status: 'released', permission: 'quality_inspection_approve', endpoint: (record) => '/api/v1/quality_cost/inspections/' + record.id + '/status', body: { status: 'released' }, statuses: ['held'] },
      { key: 'cancel', label: 'Hủy', target_status: 'cancelled', permission: 'quality_inspection_approve', endpoint: (record) => '/api/v1/quality_cost/inspections/' + record.id + '/status', body: { status: 'cancelled' }, statuses: ['draft', 'submitted', 'held'] },
      { key: 'delete', label: 'Xóa bản nháp', permission: 'quality_inspection_delete', method: 'DELETE', endpoint: (record) => '/api/v1/quality_cost/inspections/' + record.id, statuses: ['draft'] },
    ],
  },
  quality_nonconformances: {
    module_key: 'quality_cost', title: 'Sản phẩm không phù hợp',
    description: 'Theo dõi nguyên nhân, biện pháp xử lý và kết quả đóng hồ sơ lỗi.',
    endpoint: '/api/v1/quality_cost/nonconformances', row_key: 'id', delete_permission: 'quality_nonconformance_delete',
    search_placeholder: 'Tìm mã hồ sơ hoặc mã lỗi', search_param: 'search', status_param: 'status',
    read_permission: 'quality_nonconformance_read', create_permission: 'quality_nonconformance_create', update_permission: 'quality_nonconformance_update',
    detail_kind: 'nonconformance', edit_statuses: ['open', 'in_progress'], status_options: ['open', 'in_progress', 'resolved', 'cancelled'],
    columns: [['code', 'Mã hồ sơ', 170], ['inspection_id', 'Mã kiểm tra', 125], ['defect_code', 'Mã lỗi', 150], ['quantity', 'Số lượng', 115], ['disposition', 'Hướng xử lý', 140], ['resolved_on', 'Ngày đóng', 125], ['status', 'Trạng thái', 125]],
    actions: [
      { key: 'start', label: 'Bắt đầu xử lý', target_status: 'in_progress', permission: 'quality_nonconformance_approve', endpoint: (record) => '/api/v1/quality_cost/nonconformances/' + record.id + '/status', body: { status: 'in_progress' }, statuses: ['open'] },
      { key: 'resolve', label: 'Đóng hồ sơ', target_status: 'resolved', permission: 'quality_nonconformance_approve', endpoint: (record) => '/api/v1/quality_cost/nonconformances/' + record.id + '/status', body: { status: 'resolved' }, statuses: ['open', 'in_progress'] },
      { key: 'cancel', label: 'Hủy', target_status: 'cancelled', permission: 'quality_nonconformance_approve', endpoint: (record) => '/api/v1/quality_cost/nonconformances/' + record.id + '/status', body: { status: 'cancelled' }, statuses: ['open', 'in_progress'] },
      { key: 'delete', label: 'Xóa hồ sơ mở', permission: 'quality_nonconformance_delete', method: 'DELETE', endpoint: (record) => '/api/v1/quality_cost/nonconformances/' + record.id, statuses: ['open'] },
    ],
  },
  cost_periods: {
    module_key: 'quality_cost', title: 'Kỳ và chính sách giá thành',
    description: 'Mở kỳ, gắn phiên bản công thức, duyệt và khóa kết quả tính giá thành.',
    endpoint: '/api/v1/quality_cost/periods', row_key: 'id',
    search_placeholder: 'Tìm mã kỳ giá thành', search_param: 'search', status_param: 'status',
    read_permission: 'cost_period_read', create_permission: 'cost_period_create', update_permission: 'cost_period_update', delete_permission: 'cost_period_delete',
    detail_kind: 'cost_period', edit_statuses: ['draft'], status_options: ['draft', 'open', 'calculating', 'calculated', 'approved', 'locked', 'cancelled'],
    columns: [['code', 'Mã kỳ', 170], ['starts_on', 'Từ ngày', 125], ['ends_on', 'Đến ngày', 125], ['rule_code', 'Phiên bản công thức', 180], ['status', 'Trạng thái', 125]],
    actions: [
      { key: 'open', label: 'Mở kỳ', target_status: 'open', permission: 'cost_period_approve', endpoint: (record) => '/api/v1/quality_cost/periods/' + record.id + '/status', body: { status: 'open' }, statuses: ['draft'] },
      { key: 'calculate', label: 'Bắt đầu tính', target_status: 'calculating', permission: 'cost_period_approve', endpoint: (record) => '/api/v1/quality_cost/periods/' + record.id + '/status', body: { status: 'calculating' }, statuses: ['open'] },
      { key: 'mark_calculated', label: 'Hoàn tất tính', target_status: 'calculated', permission: 'cost_period_approve', endpoint: (record) => '/api/v1/quality_cost/periods/' + record.id + '/status', body: { status: 'calculated' }, statuses: ['calculating'] },
      { key: 'approve', label: 'Duyệt kỳ', target_status: 'approved', permission: 'cost_period_approve', endpoint: (record) => '/api/v1/quality_cost/periods/' + record.id + '/status', body: { status: 'approved' }, statuses: ['calculated'] },
      { key: 'lock', label: 'Khóa kỳ', target_status: 'locked', permission: 'cost_period_approve', endpoint: (record) => '/api/v1/quality_cost/periods/' + record.id + '/status', body: { status: 'locked' }, statuses: ['approved'] },
      { key: 'cancel', label: 'Hủy kỳ', target_status: 'cancelled', permission: 'cost_period_approve', endpoint: (record) => '/api/v1/quality_cost/periods/' + record.id + '/status', body: { status: 'cancelled' }, statuses: ['draft', 'open', 'calculating', 'calculated', 'approved'] },
      { key: 'delete', label: 'Xóa kỳ nháp', permission: 'cost_period_delete', method: 'DELETE', endpoint: (record) => '/api/v1/quality_cost/periods/' + record.id, statuses: ['draft'] },
    ],
  },
  cost_calculations: {
    module_key: 'quality_cost', title: 'Tính giá thành',
    description: 'Tập hợp vật tư, nhân công và chi phí chung để tính giá thành đơn vị.',
    endpoint: '/api/v1/quality_cost/calculations', row_key: 'id',
    search_placeholder: 'Tìm mã vật tư hoặc tên thành phẩm', search_param: 'search', status_param: 'status',
    read_permission: 'cost_calculation_read', create_permission: 'cost_calculation_create', update_permission: 'cost_calculation_update', delete_permission: 'cost_calculation_delete',
    detail_kind: 'cost_calculation', edit_statuses: ['calculated'], status_options: ['calculated', 'approved', 'locked', 'cancelled'],
    columns: [['id', 'Mã kết quả', 110], ['cost_period_id', 'Kỳ giá thành', 120], ['stock_item_code', 'Mã thành phẩm', 145], ['good_quantity', 'Sản lượng đạt', 130], ['material_cost', 'Vật tư', 135], ['direct_labor_cost', 'Nhân công', 135], ['overhead_cost', 'Chi phí chung', 135], ['unit_cost', 'Giá thành đơn vị', 145], ['status', 'Trạng thái', 125]],
    actions: [
      { key: 'approve', label: 'Duyệt', target_status: 'approved', permission: 'cost_calculation_approve', endpoint: (record) => '/api/v1/quality_cost/calculations/' + record.id + '/status', body: { status: 'approved' }, statuses: ['calculated'] },
      { key: 'lock', label: 'Khóa kết quả', target_status: 'locked', permission: 'cost_calculation_approve', endpoint: (record) => '/api/v1/quality_cost/calculations/' + record.id + '/status', body: { status: 'locked' }, statuses: ['approved'] },
      { key: 'cancel', label: 'Hủy', target_status: 'cancelled', permission: 'cost_calculation_approve', endpoint: (record) => '/api/v1/quality_cost/calculations/' + record.id + '/status', body: { status: 'cancelled' }, statuses: ['calculated'] },
      { key: 'delete', label: 'Xóa kết quả chưa duyệt', permission: 'cost_calculation_delete', method: 'DELETE', endpoint: (record) => '/api/v1/quality_cost/calculations/' + record.id, statuses: ['calculated'] },
    ],
  },
  price_proposals: {
    module_key: 'quality_cost', title: 'Đề xuất giá bán',
    description: 'Lập giá bán đề xuất từ giá thành và biên lợi nhuận mong muốn.',
    endpoint: '/api/v1/quality_cost/price_proposals', row_key: 'id', delete_permission: 'price_proposal_delete',
    search_placeholder: 'Tìm mã đề xuất hoặc mã thành phẩm', search_param: 'search', status_param: 'status',
    read_permission: 'price_proposal_read', create_permission: 'price_proposal_create', update_permission: 'price_proposal_update',
    detail_kind: 'price_proposal', edit_statuses: ['draft', 'rejected'], status_options: ['draft', 'pending', 'approved', 'rejected', 'published', 'cancelled'],
    columns: [['code', 'Mã đề xuất', 165], ['stock_item_code', 'Mã thành phẩm', 145], ['unit_cost', 'Giá thành', 130], ['margin_percent', 'Biên lợi nhuận (%)', 145], ['proposed_price', 'Giá đề xuất', 145], ['effective_on', 'Hiệu lực từ', 125], ['status', 'Trạng thái', 125]],
    actions: [
      { key: 'submit', label: 'Gửi duyệt', target_status: 'pending', permission: 'price_proposal_approve', endpoint: (record) => '/api/v1/quality_cost/price_proposals/' + record.id + '/status', body: { status: 'pending' }, statuses: ['draft', 'rejected'] },
      { key: 'approve', label: 'Duyệt', target_status: 'approved', permission: 'price_proposal_approve', endpoint: (record) => '/api/v1/quality_cost/price_proposals/' + record.id + '/status', body: { status: 'approved' }, statuses: ['pending'] },
      { key: 'publish', label: 'Công bố', target_status: 'published', permission: 'price_proposal_approve', endpoint: (record) => '/api/v1/quality_cost/price_proposals/' + record.id + '/status', body: { status: 'published' }, statuses: ['approved'] },
      { key: 'reject', label: 'Từ chối', target_status: 'rejected', permission: 'price_proposal_approve', endpoint: (record) => '/api/v1/quality_cost/price_proposals/' + record.id + '/status', body: { status: 'rejected' }, statuses: ['pending'] },
      { key: 'cancel', label: 'Hủy', target_status: 'cancelled', permission: 'price_proposal_approve', endpoint: (record) => '/api/v1/quality_cost/price_proposals/' + record.id + '/status', body: { status: 'cancelled' }, statuses: ['draft', 'pending', 'rejected', 'approved'] },
      { key: 'delete', label: 'Xóa đề xuất nháp', permission: 'price_proposal_delete', method: 'DELETE', endpoint: (record) => '/api/v1/quality_cost/price_proposals/' + record.id, statuses: ['draft', 'rejected'] },
    ],
  },
  price_approvals: {
    module_key: 'quality_cost', title: 'Phê duyệt bảng giá',
    description: 'Duyệt, từ chối và công bố các đề xuất giá bán đã tính.',
    endpoint: '/api/v1/quality_cost/price_approvals', row_key: 'id',
    search_placeholder: 'Tìm mã đề xuất hoặc mã thành phẩm', search_param: 'search', status_param: 'status',
    read_permission: 'price_proposal_read', update_permission: 'price_proposal_update',
    detail_kind: 'price_proposal', status_options: ['pending', 'approved', 'rejected', 'published'],
    columns: [['code', 'Mã đề xuất', 165], ['stock_item_code', 'Mã thành phẩm', 145], ['unit_cost', 'Giá thành', 130], ['margin_percent', 'Biên lợi nhuận (%)', 145], ['proposed_price', 'Giá đề xuất', 145], ['effective_on', 'Hiệu lực từ', 125], ['status', 'Trạng thái', 125]],
    actions: [
      { key: 'approve', label: 'Duyệt', target_status: 'approved', permission: 'price_proposal_approve', endpoint: (record) => '/api/v1/quality_cost/price_approvals/' + record.id + '/status', body: { status: 'approved' }, statuses: ['pending'] },
      { key: 'reject', label: 'Từ chối', target_status: 'rejected', permission: 'price_proposal_approve', endpoint: (record) => '/api/v1/quality_cost/price_approvals/' + record.id + '/status', body: { status: 'rejected' }, statuses: ['pending'] },
      { key: 'publish', label: 'Công bố', target_status: 'published', permission: 'price_proposal_approve', endpoint: (record) => '/api/v1/quality_cost/price_approvals/' + record.id + '/status', body: { status: 'published' }, statuses: ['approved'] },
    ],
  },

};

const create_schemas = {
  absences: [
    { key: 'request_code', label: 'Mã đơn', required: true }, { key: 'employee_id', label: 'Nhân viên', lookup: 'employees', required: true },
    { key: 'leave_type_code', label: 'Loại nghỉ', type: 'select', options: [{ value: 'annual_leave', label: 'Nghỉ phép năm' }, { value: 'sick_leave', label: 'Nghỉ ốm' }, { value: 'family_leave', label: 'Nghỉ việc gia đình' }, { value: 'unpaid_leave', label: 'Nghỉ không lương' }], required: true },
    { key: 'starts_on', label: 'Từ ngày', type: 'date', required: true }, { key: 'ends_on', label: 'Đến ngày', type: 'date', required: true },
    { key: 'is_paid', label: 'Có hưởng lương', type: 'checkbox', default_value: true }, { key: 'reason', label: 'Lý do', type: 'textarea', required: false },
    { key: 'status', label: 'Trạng thái khởi tạo', type: 'select', options: [{ value: 'draft', label: 'Bản nháp' }, { value: 'pending', label: 'Gửi duyệt ngay' }], default_value: 'pending', required: true },
  ],
  rewards_discipline: [
    { key: 'record_code', label: 'Mã quyết định', required: true }, { key: 'employee_id', label: 'Nhân viên', lookup: 'employees', required: true },
    { key: 'event_type', label: 'Loại sự kiện', type: 'select', options: [{ value: 'reward', label: 'Khen thưởng' }, { value: 'discipline', label: 'Kỷ luật' }], required: true },
    { key: 'effective_on', label: 'Ngày hiệu lực', type: 'date', required: true }, { key: 'reason', label: 'Lý do', type: 'textarea', required: true },
    { key: 'amount', label: 'Số tiền', type: 'number', required: false, default_value: 0 }, { key: 'currency_code', label: 'Mã tiền tệ', default_value: 'VND', required: true },
    { key: 'status', label: 'Trạng thái khởi tạo', type: 'select', options: [{ value: 'draft', label: 'Bản nháp' }, { value: 'pending', label: 'Gửi duyệt ngay' }], default_value: 'draft', required: true },
  ],
  payroll: [{ key: 'year', label: 'Năm', type: 'number', min: 2000, max: 2100, required: true }, { key: 'month', label: 'Tháng', type: 'number', min: 1, max: 12, required: true }],
  receipts: [
    { key: 'receipt_code', label: 'Mã phiếu', required: true }, { key: 'warehouse_id', label: 'Kho nhập', lookup: 'warehouses', required: true }, { key: 'supplier_id', label: 'Nhà cung cấp', lookup: 'suppliers', required: false },
    { key: 'source_module', label: 'Nguồn chứng từ', required: false }, { key: 'source_document_id', label: 'Mã chứng từ nguồn', type: 'number', required: false }, { key: 'reference_number', label: 'Số tham chiếu', required: false }, { key: 'idempotency_key', label: 'Mã chống gửi trùng', required: false }, { key: 'notes', label: 'Ghi chú', type: 'textarea', required: false },
    { key: 'lines', label: 'Dòng vật tư', type: 'lines', required: true, fields: [{ key: 'stock_item_id', label: 'Vật tư', lookup: 'raw_materials', required: true }, { key: 'warehouse_location_id', label: 'Vị trí kho', lookup: 'warehouse_locations', parent_field: 'warehouse_id', required: true }, { key: 'stock_lot_id', label: 'Mã lô (nếu có)', type: 'number', required: false }, { key: 'quantity', label: 'Số lượng', type: 'number', min: 0.000001, required: true }] },
  ],
  issues: [
    { key: 'issue_code', label: 'Mã phiếu', required: true }, { key: 'warehouse_id', label: 'Kho xuất', lookup: 'warehouses', required: true }, { key: 'reason_code', label: 'Mã lý do', required: false },
    { key: 'source_module', label: 'Nguồn chứng từ', required: false }, { key: 'source_document_id', label: 'Mã chứng từ nguồn', type: 'number', required: false }, { key: 'idempotency_key', label: 'Mã chống gửi trùng', required: false }, { key: 'notes', label: 'Ghi chú', type: 'textarea', required: false },
    { key: 'lines', label: 'Dòng vật tư', type: 'lines', required: true, fields: [{ key: 'stock_item_id', label: 'Vật tư', lookup: 'raw_materials', required: true }, { key: 'warehouse_location_id', label: 'Vị trí kho', lookup: 'warehouse_locations', parent_field: 'warehouse_id', required: true }, { key: 'stock_lot_id', label: 'Mã lô (nếu có)', type: 'number', required: false }, { key: 'quantity', label: 'Số lượng', type: 'number', min: 0.000001, required: true }] },
  ],
  transfers: [
    { key: 'transfer_code', label: 'Mã điều chuyển', required: true }, { key: 'source_warehouse_id', label: 'Kho xuất', lookup: 'warehouses', required: true }, { key: 'destination_warehouse_id', label: 'Kho nhận', lookup: 'warehouses', required: true },
    { key: 'idempotency_key', label: 'Mã chống gửi trùng', required: false }, { key: 'notes', label: 'Ghi chú', type: 'textarea', required: false },
    { key: 'lines', label: 'Dòng vật tư', type: 'lines', required: true, fields: [{ key: 'stock_item_id', label: 'Vật tư', lookup: 'raw_materials', required: true }, { key: 'stock_lot_id', label: 'Mã lô (nếu có)', type: 'number', required: false }, { key: 'source_location_id', label: 'Vị trí xuất', lookup: 'warehouse_locations', parent_field: 'source_warehouse_id', required: true }, { key: 'destination_location_id', label: 'Vị trí nhận', lookup: 'warehouse_locations', parent_field: 'destination_warehouse_id', required: true }, { key: 'quantity', label: 'Số lượng', type: 'number', min: 0.000001, required: true }] },
  ],
  stocktakes: [{ key: 'stocktake_code', label: 'Mã phiên kiểm kê', required: true }, { key: 'warehouse_id', label: 'Kho kiểm kê', lookup: 'warehouses', required: true }, { key: 'notes', label: 'Ghi chú', type: 'textarea', required: false }],
  materials: [
    { key: 'bom_code', label: 'Mã định mức', required: true }, { key: 'stock_item_id', label: 'Thành phẩm', lookup: 'finished_products', required: true }, { key: 'version_number', label: 'Phiên bản', type: 'number', min: 1, required: true, default_value: 1 },
    { key: 'base_quantity', label: 'Số lượng cơ sở', type: 'number', min: 0.000001, required: true }, { key: 'valid_from', label: 'Hiệu lực từ', type: 'date', required: true }, { key: 'valid_to', label: 'Hiệu lực đến', type: 'date', required: false }, { key: 'notes', label: 'Ghi chú', type: 'textarea', required: false },
    { key: 'lines', label: 'Dòng định mức', type: 'lines', required: true, fields: [{ key: 'material_stock_item_id', label: 'Nguyên vật liệu', lookup: 'raw_materials', required: true }, { key: 'quantity_per_base', label: 'Định lượng', type: 'number', min: 0.000001, required: true }, { key: 'scrap_percent', label: 'Tỷ lệ hao hụt (%)', type: 'number', min: 0, max: 100, required: false }] },
  ],
  orders: [
    { key: 'order_code', label: 'Mã lệnh', required: true }, { key: 'production_plan_id', label: 'Kế hoạch sản xuất', lookup: 'production_plans', required: true, virtual: true }, { key: 'production_plan_line_id', label: 'Dòng kế hoạch', type: 'plan_line', min: 1, required: true }, { key: 'bom_id', label: 'Định mức BOM', lookup: 'boms', required: true },
    { key: 'target_quantity', label: 'Số lượng kế hoạch', type: 'number', min: 0.000001, required: true }, { key: 'planned_starts_on', label: 'Ngày bắt đầu', type: 'date', required: true }, { key: 'planned_ends_on', label: 'Ngày kết thúc', type: 'date', required: true }, { key: 'production_line_name', label: 'Dây chuyền', required: false }, { key: 'notes', label: 'Ghi chú', type: 'textarea', required: false },
  ],
  assignments: [
    { key: 'production_order_id', label: 'Lệnh sản xuất', lookup: 'production_orders', required: true }, { key: 'employee_id', label: 'Nhân viên', lookup: 'employees', required: true }, { key: 'work_shift_id', label: 'Ca làm việc', lookup: 'work_shifts', required: false }, { key: 'assignment_name', label: 'Nhiệm vụ', type: 'select', options: [{ value: 'Vận hành thiết bị', label: 'Vận hành thiết bị' }, { value: 'Chuẩn bị nguyên vật liệu', label: 'Chuẩn bị nguyên vật liệu' }, { value: 'Kiểm soát chất lượng', label: 'Kiểm soát chất lượng' }, { value: 'Đóng gói sản phẩm', label: 'Đóng gói sản phẩm' }, { value: 'Kiểm soát đóng gói', label: 'Kiểm soát đóng gói' }, { value: 'Ghi nhận sản lượng', label: 'Ghi nhận sản lượng' }, { value: 'Vệ sinh dây chuyền', label: 'Vệ sinh dây chuyền' }, { value: 'Bảo trì thiết bị', label: 'Bảo trì thiết bị' }, { value: 'Bốc xếp và bàn giao', label: 'Bốc xếp và bàn giao' }, { value: 'Khác', label: 'Khác' }], required: false },
    { key: 'starts_at', label: 'Bắt đầu', type: 'datetime', required: true }, { key: 'ends_at', label: 'Kết thúc', type: 'datetime', required: true }, { key: 'notes', label: 'Ghi chú', type: 'textarea', required: false },
  ],
  quality_inspections: [
    { key: 'inspection_code', detail_key: 'code', label: 'Mã kiểm tra', required: true },
    { key: 'production_order_id', label: 'Mã lệnh sản xuất', type: 'number', required: false },
    { key: 'production_output_id', label: 'Mã sản lượng', type: 'number', required: false },
    { key: 'stock_item_id', label: 'Mã thành phẩm', type: 'number', required: true },
    { key: 'lot_code', label: 'Mã lô', required: false },
    { key: 'inspected_quantity', label: 'Số lượng kiểm', type: 'number', min: 0.000001, required: true },
    { key: 'good_quantity', label: 'Số lượng đạt', type: 'number', min: 0, required: true, default_value: 0 },
    { key: 'defective_quantity', label: 'Số lượng lỗi', type: 'number', min: 0, required: true, default_value: 0 },
    { key: 'inspected_on', label: 'Ngày kiểm', type: 'date', required: true },
    { key: 'notes', label: 'Ghi chú', type: 'textarea', required: false },
  ],
  quality_nonconformances: [
    { key: 'nonconformance_code', detail_key: 'code', label: 'Mã hồ sơ', required: true },
    { key: 'inspection_id', label: 'Mã kiểm tra', type: 'number', required: true },
    { key: 'defect_code', label: 'Mã lỗi', required: true },
    { key: 'quantity', label: 'Số lượng lỗi', type: 'number', min: 0.000001, required: true },
    { key: 'disposition', label: 'Hướng xử lý', type: 'select', options: [{ value: 'hold', label: 'Giữ lại' }, { value: 'rework', label: 'Tái chế' }, { value: 'scrap', label: 'Loại bỏ' }, { value: 'return', label: 'Trả lại' }, { value: 'release', label: 'Cho phép xuất' }], required: true, default_value: 'hold' },
    { key: 'notes', label: 'Ghi chú', type: 'textarea', required: false },
  ],
  cost_periods: [
    { key: 'period_code', detail_key: 'code', label: 'Mã kỳ giá thành', required: true },
    { key: 'starts_on', label: 'Từ ngày', type: 'date', required: true },
    { key: 'ends_on', label: 'Đến ngày', type: 'date', required: true },
    { key: 'rule_version_id', label: 'Mã phiên bản công thức', type: 'number', min: 1, required: true },
    { key: 'notes', label: 'Ghi chú', type: 'textarea', required: false },
  ],
  cost_calculations: [
    { key: 'cost_period_id', label: 'Mã kỳ giá thành', type: 'number', min: 1, required: true },
    { key: 'production_order_id', label: 'Mã lệnh sản xuất', type: 'number', required: false },
    { key: 'stock_item_id', label: 'Mã thành phẩm', type: 'number', min: 1, required: true },
    { key: 'stock_item_code', label: 'Mã hàng', required: false },
    { key: 'stock_item_name', label: 'Tên thành phẩm', required: false },
    { key: 'good_quantity', label: 'Sản lượng đạt', type: 'number', min: 0.000001, required: true },
    { key: 'material_cost', label: 'Chi phí nguyên vật liệu', type: 'number', min: 0, required: true, default_value: 0 },
    { key: 'direct_labor_cost', label: 'Chi phí nhân công', type: 'number', min: 0, required: true, default_value: 0 },
    { key: 'overhead_cost', label: 'Chi phí sản xuất chung', type: 'number', min: 0, required: true, default_value: 0 },
    { key: 'adjustment_amount', label: 'Điều chỉnh', type: 'number', min: 0, required: false, default_value: 0 },
    { key: 'notes', label: 'Ghi chú', type: 'textarea', required: false },
  ],
  price_proposals: [
    { key: 'proposal_code', detail_key: 'code', label: 'Mã đề xuất', required: true },
    { key: 'cost_period_id', label: 'Mã kỳ giá thành', type: 'number', min: 1, required: true },
    { key: 'stock_item_id', label: 'Mã thành phẩm', type: 'number', min: 1, required: true },
    { key: 'stock_item_code', label: 'Mã hàng', required: false },
    { key: 'stock_item_name', label: 'Tên thành phẩm', required: false },
    { key: 'unit_cost', label: 'Giá thành đơn vị', type: 'number', min: 0, required: true },
    { key: 'margin_percent', label: 'Biên lợi nhuận (%)', type: 'number', min: 0, required: true, default_value: 0 },
    { key: 'proposed_price', label: 'Giá bán đề xuất', type: 'number', min: 0, required: true },
    { key: 'effective_on', label: 'Ngày hiệu lực', type: 'date', required: true },
    { key: 'currency_code', label: 'Mã tiền tệ', required: true, default_value: 'VND' },
    { key: 'notes', label: 'Ghi chú', type: 'textarea', required: false },
  ],

};

Object.entries(create_schemas).forEach(([feature_key, schema]) => { if (feature_catalog[feature_key]) feature_catalog[feature_key].create_schema = schema; });

const empty_cell_labels = {
  posted_at: 'Chưa ghi sổ',
  submitted_at: 'Chưa gửi duyệt',
  approved_at: 'Chưa duyệt',
  effective_to: 'Không thời hạn',
  valid_to: 'Không thời hạn',
};

function format_cell(value, column_key, record) {
  if (value === null || value === undefined || value === '') return empty_cell_labels[column_key] || 'Chưa cập nhật';
  if (column_key === 'counted_line_count' && record?.line_count !== undefined) {
    return `${format_number_vn(value, 0)} / ${format_number_vn(record.line_count, 0)} dòng`;
  }
  if (['target_quantity', 'standard_working_days', 'inspected_quantity', 'good_quantity', 'defective_quantity', 'quantity', 'day_count', 'line_count', 'record_count', 'counted_line_count', 'margin_percent'].includes(column_key)) {
    const number = Number(value);
    if (Number.isFinite(number)) return format_number_vn(number, column_key === 'margin_percent' ? 6 : 2);
  }
  const formatted = format_field_value(value, column_key);
  if (formatted !== value) return formatted;
  if (typeof value === 'string' && value.includes('T')) return format_datetime_vn(value);
  if (typeof value === 'string' && field_value_labels[value]) return field_value_labels[value];
  return String(value);
}

function render_create_field(field, form, selected_option) {
  if (field.type === 'plan_line') return <PlanLineField form={form} />;
  if (field.lookup) return <LookupField field={{ ...field, name: field.key }} form={form} selected_option={selected_option} />;
  if (field.type === 'number') return <InputNumber min={field.min ?? 0} max={field.max} style={{ width: '100%' }} formatter={(value) => money_keys.has(field.key) && value !== undefined && value !== null && value !== '' ? format_number_vn(value) : value} parser={(value) => money_keys.has(field.key) ? String(value || '').replace(/\./g, '').replace(',', '.') : value} />;
  if (field.type === 'date') return <VietnameseDateInput />;
  if (field.type === 'datetime') return <Input type="datetime-local" />;
  if (field.type === 'select') return <Select options={field.options} style={{ width: '100%' }} />;
  if (field.type === 'textarea') return <Input.TextArea rows={3} />;
  return <Input />;
}

function normalize_form_values(values, schema) {
  const payload = {};
  schema.forEach((field) => {
    if (field.virtual) return;
    const value = values[field.key];
    if (field.type === 'lines') {
      payload[field.key] = (value || []).map((line) => Object.fromEntries(field.fields.map((line_field) => {
        let line_value = line?.[line_field.key];
        if (line_field.type === 'date' && line_value) line_value = to_iso_date(line_value);
        if (line_field.type === 'datetime' && line_value) line_value = new Date(line_value).toISOString();
        return [line_field.key, line_value === '' ? null : line_value];
      })));
    } else if (field.type === 'date') payload[field.key] = value ? to_iso_date(value) : null;
    else if (field.type === 'datetime' && value) payload[field.key] = new Date(value).toISOString();
    else payload[field.key] = value === '' ? null : value;
  });
  return payload;
}

function datetime_local_value(value) {
  if (!value) return value;
  const raw = String(value);
  // Values without an offset already represent local wall-clock time.
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(raw) && !/[zZ]|[+-]\d{2}:\d{2}$/.test(raw)) return raw.slice(0, 16);
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return raw.slice(0, 16);
  const pad = (part) => String(part).padStart(2, '0');
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + 'T' + pad(date.getHours()) + ':' + pad(date.getMinutes());
}

function input_value_for_field(field, value) {
  if (field.type === 'datetime' && value) return datetime_local_value(value);
  if (field.type === 'date' && value) return format_field_value(value, field.key);
  return value;
}

function detail_value_for_field(detail, field) {
  const detail_key = field.detail_key || field.key;
  if (detail?.[detail_key] !== undefined) return detail[detail_key];
  if (detail?.[field.key] !== undefined) return detail[field.key];
  return undefined;
}

function edit_values_from_detail(detail, schema) {
  const values = {};
  schema.forEach((field) => {
    if (field.type === 'lines') values[field.key] = (detail?.[field.key] || []).map((line) => Object.fromEntries(field.fields.map((line_field) => [line_field.key, input_value_for_field(line_field, detail_value_for_field(line, line_field))] )));
    else {
      const value = detail_value_for_field(detail, field);
      if (value !== undefined) values[field.key] = input_value_for_field(field, value);
    }
  });
  return values;
}

function StockAvailabilityHint({ form, feature_key }) {
  const is_inventory_issue = feature_key === 'issues';
  const is_inventory_transfer = feature_key === 'transfers';
  const warehouse_id = Form.useWatch(is_inventory_transfer ? 'source_warehouse_id' : 'warehouse_id', form);
  const lines = Form.useWatch('lines', form) || [];
  const [balances, set_balances] = useState([]);
  const [loading, set_loading] = useState(false);

  useEffect(() => {
    let mounted = true;
    if ((!is_inventory_issue && !is_inventory_transfer) || !warehouse_id) {
      set_balances([]);
      return () => { mounted = false; };
    }
    set_loading(true);
    request_api('/api/v1/inventory/balances?warehouse_id=' + warehouse_id + '&page=0&page_size=100')
      .then((response) => { if (mounted) set_balances(response.data?.items || []); })
      .catch(() => { if (mounted) set_balances([]); })
      .finally(() => { if (mounted) set_loading(false); });
    return () => { mounted = false; };
  }, [is_inventory_issue, is_inventory_transfer, warehouse_id]);

  if (!is_inventory_issue && !is_inventory_transfer) return null;
  const location_key = is_inventory_transfer ? 'source_location_id' : 'warehouse_location_id';
  const rows = lines.filter((line) => line?.stock_item_id && line?.[location_key]).map((line, index) => {
    const matching = balances.filter((balance) => balance.stock_item_id === line.stock_item_id
      && balance.warehouse_location_id === line[location_key]
      && (!line.stock_lot_id || balance.stock_lot_id === line.stock_lot_id));
    const available = matching.reduce((total, balance) => total + Number(balance.on_hand_quantity || 0), 0);
    return { key: index, item_code: matching[0]?.item_code || String(line.stock_item_id), location_code: matching[0]?.location_code || String(line[location_key]), requested: Number(line.quantity || 0), available };
  });
  if (!warehouse_id || !rows.length) return null;
  return <div className="stock_availability_hint"><Typography.Text strong>Số lượng khả dụng trước khi ghi sổ</Typography.Text><Table size="small" loading={loading} pagination={false} dataSource={rows} columns={[{ title: 'Vật tư', dataIndex: 'item_code' }, { title: 'Vị trí', dataIndex: 'location_code' }, { title: 'Yêu cầu', dataIndex: 'requested' }, { title: 'Khả dụng', dataIndex: 'available', render: (value, row) => <Tag color={value >= row.requested ? 'green' : 'red'}>{value}</Tag> }]} /></div>;
}
function lookup_option_for_field(field, record) {
  if (!field?.lookup || !record) return null;
  const value = record[field.detail_key || field.key] ?? record[field.key];
  if (value === undefined || value === null || value === '') return null;
  const base = field.key.replace(/_id$/, '');
  const first_value = (...keys) => keys.map((key) => record[key]).find((item) => item !== undefined && item !== null && item !== '');
  const pairs = {
    employees: [first_value('employee_code', base + '_code'), first_value('employee_name', 'full_name', base + '_name')],
    production_orders: [first_value('order_code', base + '_code'), first_value('stock_item_name', base + '_name')],
    work_shifts: [first_value('shift_code', base + '_code'), first_value('shift_name', base + '_name')],
    production_plans: [first_value('plan_code', base + '_code'), first_value('plan_name', base + '_name')],
    boms: [first_value('bom_code', base + '_code'), first_value('product_item_name', base + '_name')],
    departments: [first_value('department_code', base + '_code'), first_value('department_name', base + '_name')],
    job_titles: [first_value('job_title_code', base + '_code'), first_value('job_title_name', base + '_name')],
    warehouses: [first_value('warehouse_code', base + '_code'), first_value('warehouse_name', base + '_name')],
    suppliers: [first_value('supplier_code', base + '_code'), first_value('supplier_name', base + '_name')],
    raw_materials: [first_value('item_code', 'material_item_code', base + '_code', 'stock_item_code'), first_value('item_name', 'material_item_name', base + '_name', 'stock_item_name')],
    finished_products: [first_value('item_code', base + '_code', 'stock_item_code'), first_value('item_name', base + '_name', 'stock_item_name')],
    warehouse_locations: [first_value('location_code', base + '_code'), first_value('location_name', base + '_name')],
  }[field.lookup] || [first_value(base + '_code', field.key + '_code'), first_value(base + '_name', field.key + '_name')];
  const label = pairs.filter((item) => item !== undefined && item !== null && item !== '').join(' — ') || String(value);
  return { value, label };
}

function CreateRecordModal({ config, feature_key, open, editing_record, on_close, on_saved }) {
  const [form] = Form.useForm();
  const { message } = AntdApp.useApp();
  const schema = useMemo(() => config.create_schema || [], [config.create_schema]);
  const default_values = useMemo(() => Object.fromEntries(schema.map((field) => [field.key, field.type === 'lines' ? [{}] : field.default_value])), [schema]);
  const selected_lookup_options = useMemo(() => Object.fromEntries(schema.filter((field) => field.lookup).map((field) => [field.key, lookup_option_for_field(field, editing_record)]).filter(([, option]) => option)), [editing_record, schema]);
  const is_editing = Boolean(editing_record);
  const [saving, set_saving] = useState(false);

  useEffect(() => {
    if (!open) return;
    form.resetFields();
    form.setFieldsValue(editing_record ? edit_values_from_detail(editing_record, schema) : default_values);
  }, [default_values, editing_record, form, open, schema]);

  const on_finish = async (values) => {
    if (saving) return;
    set_saving(true);
    try {
      const payload = normalize_form_values(values, schema);
      const path = is_editing ? config.endpoint + '/' + editing_record[config.row_key] : config.endpoint;
      await request_api(path, { method: is_editing ? 'PUT' : 'POST', body: JSON.stringify(payload) });
      message.success(is_editing ? 'Record updated successfully.' : 'Record created successfully.');
      on_close();
      on_saved();
    } catch (error) {
      message.error(error.message || 'The record could not be saved.');
      if (error.field_errors) form.setFields(Object.entries(error.field_errors).map(([name, errors]) => ({ name, errors: [errors] })));
    } finally {
      set_saving(false);
    }
  };

  return <Modal open={open} title={(is_editing ? 'Sửa ' : 'Tạo ') + config.title.toLowerCase()} onCancel={on_close} footer={null} width={800} destroyOnClose>
    <Form form={form} layout="vertical" requiredMark={false} onFinish={on_finish} className="workflow_create_form">
      {schema.map((field) => {
        if (field.type === 'checkbox') return <Form.Item key={field.key} name={field.key} valuePropName="checked"><Checkbox>{field.label}</Checkbox></Form.Item>;
        if (field.type === 'lines') return <div key={field.key} className="workflow_form_lines"><Typography.Text strong>{field.label}</Typography.Text><Form.List name={field.key} rules={[{ validator: async (_, values) => values?.length ? Promise.resolve() : Promise.reject(new Error('At least one line is required.')) }]}>{(fields, { add, remove }) => <><div className="workflow_form_line_list">{fields.map(({ key, name, ...rest_field }) => <div className="workflow_form_line" key={key}>{field.fields.map((line_field, line_index) => <Form.Item {...rest_field} key={line_field.key} name={[name, line_field.key]} label={line_field.label} rules={line_field.required === false ? [] : [{ required: true, message: line_field.label + ' is required.' }]}>{render_create_field(line_field, form, lookup_option_for_field(line_field, editing_record?.[field.key]?.[line_index]))}</Form.Item>)}<Button type="text" danger icon={<StopOutlined />} aria-label="Xóa dòng" onClick={() => remove(name)} /></div>)}</div><Button type="dashed" block onClick={() => add({})} icon={<PlusOutlined />}>Thêm dòng</Button></>}</Form.List>{field.key === 'lines' && <StockAvailabilityHint form={form} feature_key={feature_key} />}</div>;
        return <Form.Item key={field.key} label={field.label} name={field.key} rules={field.required === false ? [] : [{ required: true, message: field.label + ' is required.' }]}>{render_create_field(field, form, selected_lookup_options[field.key])}</Form.Item>;
      })}
      <Space><Button onClick={on_close} disabled={saving}>Hủy</Button><Button type="primary" htmlType="submit" loading={saving}>{is_editing ? 'Lưu thay đổi' : 'Lưu dữ liệu'}</Button></Space>
    </Form>
  </Modal>;
}

function ModuleDataPage({ feature_key }) {
  const config = feature_catalog[feature_key] || feature_catalog.absences;
  const { current_user } = use_auth();
  const { message, modal } = AntdApp.useApp();
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
  const request_controller = useRef(null);
  const [active_action_key, set_active_action_key] = useState('');
  const [create_open, set_create_open] = useState(false);
  const [editing_record, set_editing_record] = useState(null);
  const [selected_record, set_selected_record] = useState(null);
  const can_create = Boolean(config.create_schema && has_permission(current_user, config.create_permission));
  const can_detail_create = Boolean(config.detail_create_permission && has_permission(current_user, config.detail_create_permission));
  const can_write = config.actions.some((action) => has_permission(current_user, action.permission)) || Boolean(config.update_permission && has_permission(current_user, config.update_permission)) || can_detail_create;

  const sync_query = useCallback((next_filters, next_page = null) => {
    set_search_params((current) => {
      const params = new URLSearchParams(current);
      Object.entries(next_filters).forEach(([key, value]) => { if (value) params.set(key, value); else params.delete(key); });
      if (next_page) params.set('page', String(next_page)); else params.delete('page');
      return params;
    }, { replace: true });
  }, [set_search_params]);

  const load_records = useCallback(async (next_filters, next_page, next_page_size) => {
    request_controller.current?.abort();
    const controller = new AbortController();
    request_controller.current = controller;
    const sequence = request_sequence.current + 1;
    request_sequence.current = sequence;
    set_is_loading(true);
    set_error_message('');
    const params = new URLSearchParams({ page: String(next_page - 1), page_size: String(next_page_size) });
    if (config.search_param && next_filters.search.trim()) params.set(config.search_param, next_filters.search.trim());
    if (config.status_param && next_filters.status) params.set(config.status_param, next_filters.status);
    try {
      const response = await request_api(config.endpoint + '?' + params.toString(), { signal: controller.signal });
      if (controller.signal.aborted || sequence !== request_sequence.current) return;
      const page_data = response.data || {};
      set_records(Array.isArray(page_data.items) ? page_data.items : []);
      set_pagination({ current: Number(page_data.page || 0) + 1, page_size: Number(page_data.page_size || next_page_size), total: Number(page_data.total_items || 0) });
    } catch (error) {
      if (controller.signal.aborted || error?.name === 'AbortError' || sequence !== request_sequence.current) return;
      set_records([]);
      set_error_message(error.message || 'The data could not be loaded.');
    } finally {
      if (request_controller.current === controller) {
        request_controller.current = null;
        set_is_loading(false);
      }
    }
  }, [config]);

  useEffect(() => { load_records(filters, current_page, current_page_size); }, [current_page, current_page_size, filters, load_records]);

  useEffect(() => () => {
    request_controller.current?.abort();
  }, []);

  const run_action = useCallback((action, record) => {
    const row_action_key = action.key + ':' + record[config.row_key];
    modal.confirm({
      title: action.label + ' bản ghi?',
      content: action.key === 'delete'
        ? 'Thao tác này sẽ xóa vĩnh viễn bản nháp và các dòng chi tiết.'
        : 'Thao tác này sẽ chuyển trạng thái sang “' + (status_labels[action.target_status] || action.target_status) + '”.',
      okText: 'Xác nhận',
      cancelText: 'Hủy',
      okButtonProps: {
        type: ['cancel', 'reject'].includes(action.key) ? 'default' : 'primary',
        danger: ['cancel', 'reject', 'delete'].includes(action.key),
      },
      onOk: async () => {
        set_active_action_key(row_action_key);
        try {
          await request_api(action.endpoint(record), { method: action.method || 'POST', ...(action.body ? { body: JSON.stringify(action.body) } : {}) });
          message.success(action.key === 'delete' ? 'Record deleted successfully.' : action.key === 'cancel' ? 'Record cancelled successfully.' : 'Workflow action completed successfully.');
          await load_records(filters, pagination.current, pagination.page_size);
        } catch (error) {
          message.error(error.message || 'The workflow action could not be completed.');
        } finally {
          set_active_action_key((current) => current === row_action_key ? '' : current);
        }
      },
    });
  }, [config.row_key, filters, load_records, message, modal, pagination]);

  const open_detail = useCallback((record) => { set_selected_record(record); }, []);

  const open_edit = useCallback(async (record) => {
    try {
      const response = await request_api(config.endpoint + '/' + record[config.row_key]);
      const detail = response.data || {};
      const detail_values = Object.fromEntries(Object.entries(detail).filter(([, value]) => value !== undefined && value !== null && value !== ''));
      // Keep display fields from the list row as a fallback. Detail APIs
      // intentionally return ids plus labels only where the module contract
      // exposes them, while the edit form needs both to render a selection.
      set_editing_record({ ...record, ...detail_values });
      set_create_open(true);
    } catch (error) {
      message.error(error.message || 'The record could not be opened for editing.');
    }
  }, [config, message]);

  const close_editor = () => { set_create_open(false); set_editing_record(null); };

  const columns = useMemo(() => {
    const base_columns = config.columns.map(([key, title, width]) => ({ title, dataIndex: key, key, width, ellipsis: { showTitle: true }, render: (value, record) => key === 'status' ? display_status(value) : format_cell(value, key, record) }));
    const available_actions = config.actions.filter((action) => has_permission(current_user, action.permission));
    const can_edit = Boolean(config.update_permission && has_permission(current_user, config.update_permission));
    return [...base_columns, {
      title: 'Thao tác', key: 'actions', fixed: 'right', width: available_actions.length || can_edit ? 220 : 108,
      render: (_, record) => {
        const row_actions = available_actions
          .filter((action) => action.statuses.includes(record.status))
          .map((action) => ({
            ...action,
            loading: active_action_key === action.key + ':' + record[config.row_key],
            on_click: () => run_action(action, record),
          }));
        return <RecordActionBar
          on_open={() => open_detail(record)}
          on_edit={() => open_edit(record)}
          can_edit={can_edit}
          edit_status_allowed={!config.edit_statuses || config.edit_statuses.includes(record.status)}
          workflow_actions={row_actions}
        />;
      },
    }];
  }, [active_action_key, config, current_user, open_detail, open_edit, run_action]);

  const open_record_event = useCallback((event) => {
    const record = event.detail;
    if (!record) return;
    const labels = config.columns.map(([key, title]) => ({ key, title, value: format_cell(record[key], key) }));
    modal.info({ title: 'Chi tiết ' + (record[config.columns[0][0]] || 'bản ghi'), width: 700, content: <div className="workflow_detail_grid">{labels.map((item) => <div key={item.key}><span>{item.title}</span><strong>{item.key === 'status' ? display_status(record[item.key]) : item.value}</strong></div>)}</div>, okText: 'Đóng' });
  }, [config, modal]);

  useEffect(() => {
    document.addEventListener('vinamik:open_record', open_record_event);
    return () => document.removeEventListener('vinamik:open_record', open_record_event);
  }, [open_record_event]);

  const column_presets = useMemo(() => ({
    overview: [config.columns[0]?.[0], config.columns[1]?.[0], config.columns[2]?.[0], 'status', 'actions'],
    detail: config.columns.map(([key]) => key).concat('actions'),
    audit: [config.columns[0]?.[0], ...config.columns.slice(-3).map(([item]) => item), 'status'],
  }), [config]);

  const on_search = (next_search = search_input) => {
    const next_filters = { ...filters, search: next_search };
    set_search_input(next_search);
    set_filters(next_filters); set_pagination((current) => ({ ...current, current: 1 })); sync_query(next_filters, 1);
  };
  const on_status_change = (status) => {
    const next_filters = { ...filters, status: status || '' };
    set_filters(next_filters); set_pagination((current) => ({ ...current, current: 1 })); sync_query(next_filters, 1);
  };

  return <AntdApp>
    <div className={'module_data_page module_data_page_' + config.module_key}>
      <ModuleMasthead
        module_key={config.module_key}
        description={config.description}
        current_feature_label={config.title}
      />
      {config.module_key === 'production' && <div className="production_stage_strip" aria-label="Trạng thái lệnh sản xuất"><span>Đã lập kế hoạch</span><i /><span>Đã phát hành</span><i /><span>Đang thực hiện</span><i /><span>Đã hoàn thành</span></div>}
      {error_message && <Alert className="workspace_error" type="error" showIcon message={error_message} />}
      <DataWorkspace
        module_key={config.module_key}
        title={'Danh sách ' + config.title.toLowerCase()}
        description="Dữ liệu tải theo trang từ máy chủ trung tâm. Nhấp đúp hoặc dùng nút xem để mở chi tiết."
        toolbar={<Space wrap className="list_toolbar control_row"><DebouncedSearchInput placeholder={config.search_placeholder} value={search_input} on_commit={on_search} style={{ width: 310 }} disabled={!config.search_param} /><Select allowClear placeholder="Tất cả trạng thái" value={filters.status || undefined} options={config.status_options.map((value) => ({ value, label: status_labels[value] || value }))} onChange={on_status_change} style={{ width: 180 }} />{can_create && <Button type="primary" icon={<PlusOutlined />} onClick={() => { set_editing_record(null); set_create_open(true); }}>Tạo mới</Button>}</Space>}
        columns={columns} data_source={records} row_key={config.row_key} loading={is_loading} pagination={pagination}
        on_change={(next_pagination) => { set_pagination((current) => ({ ...current, current: next_pagination.current, page_size: next_pagination.pageSize })); sync_query(filters, next_pagination.current); }}
        empty_text={error_message ? 'Không thể tải dữ liệu' : 'Chưa có dữ liệu phù hợp'} total_label="bản ghi" read_only={!can_write && !can_create}
        column_presets={column_presets} storage_key={'data_workspace_' + config.module_key + '_' + feature_key + '_' + (current_user?.username || 'account')} on_open_record={open_detail}
      />

      <WorkflowDetailDrawer config={{ ...config, feature_key }} record={selected_record} open={Boolean(selected_record)} on_close={() => set_selected_record(null)} current_user={current_user} on_changed={() => load_records(filters, current_page, current_page_size)} />
    </div>
    {config.create_schema && <CreateRecordModal config={config} feature_key={feature_key} open={create_open} editing_record={editing_record} on_close={close_editor} on_saved={() => load_records(filters, current_page, current_page_size)} />}
  </AntdApp>;
}

export { feature_catalog };
export default ModuleDataPage;


