import { useEffect, useRef, useState } from 'react';
import { Alert, App as antd_app, Button, Form, Input, InputNumber, Modal, Select, Space, Spin, Tag } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { has_permission } from '../../platform/common/permission_utils';
import { request_api } from '../../platform/common/api_client';
import { use_auth } from '../../platform/identity/auth_context';
import DataWorkspace from '../../platform/layout/data_workspace';
import { LookupField } from '../../platform/layout/workflow_fields';
import RecordActionBar from '../../platform/layout/record_action_bar';
import ModuleMasthead from '../../platform/layout/module_masthead';
import DebouncedSearchInput from '../../platform/layout/debounced_search_input';
import { format_date_vn, format_number_vn, format_vnd, to_iso_date, VietnameseDateInput } from '../../platform/common/formatters';

const AntdApp = antd_app;
const status_options = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'draft', label: 'Bản nháp' },
  { value: 'active', label: 'Đang hiệu lực' },
  { value: 'expired', label: 'Đã hết hạn' },
  { value: 'terminated', label: 'Đã chấm dứt' },
  { value: 'cancelled', label: 'Đã hủy' },
];
const status_labels = Object.fromEntries(status_options.filter((item) => item.value).map((item) => [item.value, item.label]));

function ContractsPage() {
  const { current_user } = use_auth();
  const { message, modal } = AntdApp.useApp();
  const [contracts, set_contracts] = useState([]);
  const [is_loading, set_is_loading] = useState(false);
  const [error_message, set_error_message] = useState('');
  const [filters, set_filters] = useState({ search: '', status: '', expiring_only: false });
  const [search_input, set_search_input] = useState('');
  const [pagination, set_pagination] = useState({ current: 1, page_size: 50, total: 0 });
  const [is_modal_open, set_is_modal_open] = useState(false);
  const [selected_contract, set_selected_contract] = useState(null);
  const [contract_detail_loading, set_contract_detail_loading] = useState(false);
  const [editing_contract, set_editing_contract] = useState(null);
  const [active_action_key, set_active_action_key] = useState('');
  const [form] = Form.useForm();
  const contracts_request_ref = useRef(null);
  const contract_detail_request_ref = useRef(0);

  const can_create = has_permission(current_user, 'hr_contract_create');
  const can_update = has_permission(current_user, 'hr_contract_update');
  const can_approve = has_permission(current_user, 'hr_contract_approve');
  const can_delete = has_permission(current_user, 'hr_contract_delete');

  const load_contracts = async (next_filters = filters, next_page = pagination.current, next_page_size = pagination.page_size) => {
    contracts_request_ref.current?.abort();
    const controller = new AbortController();
    contracts_request_ref.current = controller;
    set_is_loading(true);
    set_error_message('');
    const params = new URLSearchParams({ page: String(next_page - 1), page_size: String(next_page_size) });
    if (next_filters.search.trim()) params.set('search', next_filters.search.trim());
    if (next_filters.status) params.set('status', next_filters.status);
    if (next_filters.expiring_only) params.set('expiring_only', 'true');
    try {
      const response = await request_api('/api/v1/human_resources/contracts?' + params.toString(), { signal: controller.signal });
      if (controller.signal.aborted) return;
      set_contracts(response.data.items);
      set_pagination({ current: response.data.page + 1, page_size: response.data.page_size, total: response.data.total_items });
    } catch (error) {
      if (controller.signal.aborted || error?.name === 'AbortError') return;
      set_contracts([]);
      set_error_message(error.message || 'Employment contracts could not be loaded.');
    } finally {
      if (contracts_request_ref.current === controller) {
        contracts_request_ref.current = null;
        set_is_loading(false);
      }
    }
  };

  useEffect(() => {
    load_contracts();
    // Initial data load intentionally runs once with initial filters.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => () => {
    contracts_request_ref.current?.abort();
  }, []);

  const open_create = () => {
    set_editing_contract(null);
    form.resetFields();
    form.setFieldsValue({ currency_code: 'VND', status: 'draft' });
    set_is_modal_open(true);
  };

  const open_edit = async (contract) => {
    try {
      const response = await request_api('/api/v1/human_resources/contracts/' + contract.employment_contract_id);
      const detail = response.data;
      set_editing_contract(detail);
      form.resetFields();
      form.setFieldsValue({
        ...detail,
        effective_from: detail.effective_from ? format_date_vn(detail.effective_from) : undefined,
        effective_to: detail.effective_to ? format_date_vn(detail.effective_to) : undefined,
      });
      set_is_modal_open(true);
    } catch (error) {
      message.error(error.message || 'Employment contract could not be loaded for editing.');
    }
  };

  const on_finish = async (values) => {
    const payload = Object.fromEntries(Object.entries(values).map(([key, value]) => [key, value === '' ? null : value]));
    payload.effective_from = to_iso_date(payload.effective_from);
    payload.effective_to = to_iso_date(payload.effective_to);
    try {
      if (editing_contract) {
        await request_api('/api/v1/human_resources/contracts/' + editing_contract.employment_contract_id, { method: 'PUT', body: JSON.stringify(payload) });
        message.success('Employment contract updated successfully.');
      } else {
        await request_api('/api/v1/human_resources/contracts', { method: 'POST', body: JSON.stringify(payload) });
        message.success('Employment contract created successfully.');
      }
      set_is_modal_open(false);
      form.resetFields();
      await load_contracts(filters, pagination.current, pagination.page_size);
    } catch (error) {
      message.error(error.message || 'Employment contract could not be saved.');
      if (error.field_errors) {
        form.setFields(Object.entries(error.field_errors).map(([name, errors]) => ({ name, errors: [errors] })));
      }
    }
  };

  const delete_contract = (contract) => {
    modal.confirm({
      title: 'Xóa hợp đồng?',
      content: 'Hợp đồng ' + contract.contract_code + ' sẽ bị xóa khỏi dữ liệu bản nháp.',
      okText: 'Xác nhận',
      cancelText: 'Hủy',
      okButtonProps: { danger: true },
      onOk: async () => {
        const action_key = 'delete:' + contract.employment_contract_id;
        set_active_action_key(action_key);
        try {
          await request_api('/api/v1/human_resources/contracts/' + contract.employment_contract_id, { method: 'DELETE' });
          message.success('Employment contract deleted successfully.');
          await load_contracts(filters, pagination.current, pagination.page_size);
        } catch (error) {
          message.error(error.message || 'Employment contract could not be deleted.');
        } finally {
          set_active_action_key((current) => current === action_key ? '' : current);
        }
      },
    });
  };

  const change_status = (contract, status) => {
    const action_label = status === 'active' ? 'Duyệt' : status === 'cancelled' ? 'Hủy' : 'Đóng';
    modal.confirm({
      title: action_label + ' hợp đồng?',
      content: 'Hợp đồng ' + contract.contract_code + ' sẽ chuyển sang ' + (status_labels[status] || status) + '.',
      okText: 'Xác nhận',
      cancelText: 'Hủy',
      okButtonProps: status === 'terminated' || status === 'cancelled' ? { danger: true } : undefined,
      onOk: async () => {
        const action_key = status + ':' + contract.employment_contract_id;
        set_active_action_key(action_key);
        try {
          await request_api('/api/v1/human_resources/contracts/' + contract.employment_contract_id + '/status', { method: 'POST', body: JSON.stringify({ status }) });
          message.success('Employment contract status updated successfully.');
          await load_contracts(filters, pagination.current, pagination.page_size);
        } catch (error) {
          message.error(error.message || 'Employment contract status could not be updated.');
        } finally {
          set_active_action_key((current) => current === action_key ? '' : current);
        }
      },
    });
  };

  const open_contract_detail = async (item) => {
    const sequence = contract_detail_request_ref.current + 1;
    contract_detail_request_ref.current = sequence;
    // The table row already contains enough summary data to paint the modal. Show it
    // immediately and replace it with the authoritative detail response in the background.
    set_selected_contract(item);
    set_contract_detail_loading(true);
    try {
      const response = await request_api('/api/v1/human_resources/contracts/' + item.employment_contract_id);
      if (sequence === contract_detail_request_ref.current) set_selected_contract(response.data || item);
    } catch (error) {
      if (sequence === contract_detail_request_ref.current) message.error(error.message || 'Employment contract details could not be loaded.');
    } finally {
      if (sequence === contract_detail_request_ref.current) set_contract_detail_loading(false);
    }
  };

  const on_search = (next_search = search_input) => {
    const next_filters = { ...filters, search: next_search };
    set_search_input(next_search);
    set_filters(next_filters);
    load_contracts(next_filters, 1, pagination.page_size);
  };
  const columns = [
    { title: 'Số hợp đồng', dataIndex: 'contract_code', key: 'contract_code', fixed: 'left', width: 150 },
    { title: 'Nhân viên', key: 'employee', width: 230, render: (_, item) => (item.employee_code || 'Chưa có mã') + ' — ' + (item.employee_name || 'Chưa liên kết') },
    { title: 'Loại hợp đồng', dataIndex: 'contract_type', key: 'contract_type', width: 150 },
    { title: 'Hiệu lực từ', dataIndex: 'effective_from', key: 'effective_from', render: (value) => value ? format_date_vn(value) : 'Chưa cập nhật', width: 130 },
    { title: 'Hiệu lực đến', dataIndex: 'effective_to', key: 'effective_to', render: (value) => value ? format_date_vn(value) : 'Không thời hạn', width: 150 },
    { title: 'Lương cơ bản', key: 'base_salary', render: (_, item) => (item.currency_code || 'VND') === 'VND' ? format_vnd(item.base_salary) : format_number_vn(item.base_salary) + ' ' + item.currency_code, width: 160 },
    { title: 'Trạng thái', dataIndex: 'status', key: 'status', render: (value, item) => <Space direction="vertical" size={2}><Tag color={value === 'active' ? 'green' : value === 'draft' ? 'blue' : 'default'}>{status_labels[value] || value}</Tag>{item.expiring_soon && <Tag color="orange">Còn {item.days_until_expiry} ngày</Tag>}</Space>, width: 140 },
    { title: 'Thao tác', key: 'actions', fixed: 'right', width: 220, render: (_, item) => <RecordActionBar
      on_open={() => open_contract_detail(item)}
      on_edit={() => open_edit(item)}
      can_edit={can_update}
      edit_status_allowed={item.status === 'draft'}
      workflow_actions={[
        ...(can_delete ? [{ key: 'delete', label: item.status === 'draft' ? 'Xóa hợp đồng' : 'Xóa hợp đồng (chỉ bản nháp)', disabled: item.status !== 'draft', disabled_reason: 'Chỉ xóa được hợp đồng ở trạng thái bản nháp.', loading: active_action_key === 'delete:' + item.employment_contract_id, on_click: () => delete_contract(item) }] : []),
        ...(can_approve && item.status === 'draft' ? [{ key: 'approve', label: 'Duyệt hợp đồng', loading: active_action_key === 'active:' + item.employment_contract_id, on_click: () => change_status(item, 'active') }] : []),
        ...(can_approve && item.status === 'active' ? [{ key: 'terminate', label: 'Đóng hợp đồng', loading: active_action_key === 'terminated:' + item.employment_contract_id, on_click: () => change_status(item, 'terminated') }] : []),
        ...(can_approve && item.status === 'draft' ? [{ key: 'cancel', label: 'Hủy hợp đồng', loading: active_action_key === 'cancelled:' + item.employment_contract_id, on_click: () => change_status(item, 'cancelled') }] : []),
      ]}
    /> },
  ];

  return <AntdApp>
    <div className="module_page module_page_human_resources">
      <ModuleMasthead
        module_key="human_resources"
        description="Quản lý thời hạn, mức lương cơ bản và cảnh báo hợp đồng sắp hết hạn."
        current_feature_label="Hợp đồng lao động"
      />
      {filters.expiring_only && <Alert type="warning" showIcon message="Danh sách đang lọc các hợp đồng còn hiệu lực và hết hạn trong 15 ngày tới." className="contract_expiry_alert" />}
      {error_message && <Alert className="workspace_error" type="error" showIcon message={error_message} />}
      <DataWorkspace
        title="Danh sách hợp đồng lao động"
        description="Tìm kiếm, lọc trạng thái và theo dõi thời hạn trên dữ liệu trung tâm. Nhấp đúp một dòng để xem chi tiết. Hợp đồng đã duyệt chỉ chuyển trạng thái để giữ lịch sử."
        toolbar={<Space wrap className="list_toolbar">
          <DebouncedSearchInput placeholder="Tìm số hợp đồng hoặc nhân viên" value={search_input} on_commit={on_search} style={{ width: 280 }} />
          <Select value={filters.status} options={status_options} onChange={(status) => { const next_filters = { ...filters, status }; set_filters(next_filters); load_contracts(next_filters, 1, pagination.page_size); }} style={{ width: 170 }} />
          <Button type={filters.expiring_only ? 'primary' : 'default'} onClick={() => { const next_filters = { ...filters, expiring_only: !filters.expiring_only }; set_filters(next_filters); load_contracts(next_filters, 1, pagination.page_size); }}>Cảnh báo 15 ngày</Button>
          {can_create && <Button type="primary" icon={<PlusOutlined />} onClick={open_create}>Thêm hợp đồng</Button>}
        </Space>}
        columns={columns}
        data_source={contracts}
        row_key="employment_contract_id"
        loading={is_loading}
        pagination={pagination}
        on_change={(next_pagination) => load_contracts(filters, next_pagination.current, next_pagination.pageSize)}
        empty_text={error_message ? 'Employment contracts could not be loaded.' : 'Chưa có hợp đồng'}
        total_label="hợp đồng"
        read_only={!can_create && !can_update && !can_approve && !can_delete}
        column_presets={{ overview: ['contract_code', 'employee', 'effective_from', 'effective_to', 'status', 'actions'], detail: ['contract_code', 'employee', 'contract_type', 'effective_from', 'effective_to', 'base_salary', 'status', 'actions'], audit: ['contract_code', 'employee', 'contract_type', 'effective_from', 'effective_to', 'base_salary', 'status'] }}
        storage_key={'data_workspace_hr_contracts_' + (current_user?.username || 'account')}
        on_open_record={open_contract_detail}
      />

      <Modal
        className="entity_detail_modal entity_detail_modal_contract"
        width={680}
        open={Boolean(selected_contract)}
        title={selected_contract ? 'Chi tiết hợp đồng ' + selected_contract.contract_code : 'Chi tiết hợp đồng'}
        onCancel={() => set_selected_contract(null)}
        footer={<Button onClick={() => set_selected_contract(null)}>Đóng</Button>}
        destroyOnClose
      >
        {selected_contract && <>
          {contract_detail_loading && <div className="workflow_detail_refreshing"><Spin size="small" /><span>Đang cập nhật chi tiết…</span></div>}
          <div className="workflow_detail_grid">
          <div><span>Nhân viên</span><strong>{(selected_contract.employee_code || 'Chưa có mã') + ' — ' + (selected_contract.employee_name || 'Chưa liên kết')}</strong></div>
          <div><span>Trạng thái</span><strong>{status_labels[selected_contract.status] || selected_contract.status}</strong></div>
          <div><span>Loại hợp đồng</span><strong>{selected_contract.contract_type || 'Chưa cập nhật'}</strong></div>
          <div><span>Hiệu lực</span><strong>{selected_contract.effective_from ? format_date_vn(selected_contract.effective_from) : 'Chưa cập nhật'} — {selected_contract.effective_to ? format_date_vn(selected_contract.effective_to) : 'Không thời hạn'}</strong></div>
          <div><span>Lương cơ bản</span><strong>{(selected_contract.currency_code || 'VND') === 'VND' ? format_vnd(selected_contract.base_salary) : format_number_vn(selected_contract.base_salary) + ' ' + selected_contract.currency_code}</strong></div>
          <div><span>Ghi chú</span><strong>{selected_contract.notes || 'Không có ghi chú'}</strong></div>
          </div>
        </>}
      </Modal>

      <Modal className="entity_form_modal entity_form_modal_contract" width={720} open={is_modal_open} title={<div className="modal_title_block"><span>HỒ SƠ NHÂN SỰ</span><strong>{editing_contract ? 'Sửa hợp đồng lao động' : 'Thêm hợp đồng lao động'}</strong><small>Hiệu lực, lương cơ bản và trạng thái hợp đồng.</small></div>} onCancel={() => set_is_modal_open(false)} footer={<div className="modal_footer_actions"><Button onClick={() => set_is_modal_open(false)}>Hủy</Button><Button type="primary" htmlType="submit" form="hr_contract_form">Lưu hợp đồng</Button></div>} destroyOnClose>
        <Form id="hr_contract_form" form={form} layout="vertical" onFinish={on_finish} requiredMark={false}>
          <Form.Item label="Số hợp đồng" name="contract_code" rules={[{ required: true, message: 'Contract code is required.' }]}><Input maxLength={60} /></Form.Item>
          <Form.Item label="Nhân viên" name="employee_id" rules={[{ required: true, message: 'Employee is required.' }]}><LookupField field={{ lookup: 'employees', name: 'employee_id', required: true, placeholder: 'Chọn nhân viên' }} selected_option={editing_contract?.employee_id ? { value: editing_contract.employee_id, label: [editing_contract.employee_code, editing_contract.employee_name].filter(Boolean).join(' — ') || String(editing_contract.employee_id) } : undefined} form={form} /></Form.Item>
          <Form.Item label="Loại hợp đồng" name="contract_type" rules={[{ required: true, message: 'Contract type is required.' }]}><Input maxLength={40} placeholder="Ví dụ: indefinite" /></Form.Item>
          <Space style={{ width: '100%' }} size="middle"><Form.Item label="Hiệu lực từ" name="effective_from" rules={[{ required: true, message: 'Effective start date is required.' }]}><VietnameseDateInput /></Form.Item><Form.Item label="Hiệu lực đến" name="effective_to"><VietnameseDateInput /></Form.Item></Space>
          <Space style={{ width: '100%' }} size="middle"><Form.Item label="Lương cơ bản" name="base_salary" rules={[{ required: true, message: 'Base salary is required.' }]}><InputNumber min={0} precision={2} style={{ width: '100%' }} formatter={(value) => value === undefined || value === null || value === '' ? '' : format_number_vn(value)} parser={(value) => String(value || '').replace(/\./g, '')} /></Form.Item><Form.Item label="Tiền tệ" name="currency_code"><Input maxLength={3} /></Form.Item></Space>
          <Form.Item label="Trạng thái" name="status"><Select options={[{ value: 'draft', label: 'Bản nháp' }]} /></Form.Item>
          <Form.Item label="Ghi chú" name="notes"><Input.TextArea maxLength={2000} rows={3} /></Form.Item>

        </Form>
      </Modal>
    </div>
  </AntdApp>;
}

export default ContractsPage;
