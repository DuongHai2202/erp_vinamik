import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Alert, App as antd_app, Button, Form, Input, Modal, Select, Space, Tag } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { has_permission } from '../../platform/common/permission_utils';
import { request_api } from '../../platform/common/api_client';
import { use_auth } from '../../platform/identity/auth_context';
import DataWorkspace from '../../platform/layout/data_workspace';
import RecordActionBar from '../../platform/layout/record_action_bar';
import ModuleMasthead from '../../platform/layout/module_masthead';
import { LookupField } from '../../platform/layout/workflow_fields';
import DebouncedSearchInput from '../../platform/layout/debounced_search_input';
import { format_date_vn, to_iso_date, VietnameseDateInput } from '../../platform/common/formatters';

const AntdApp = antd_app;
const status_options = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'active', label: 'Đang làm việc' },
  { value: 'on_leave', label: 'Đang nghỉ' },
  { value: 'inactive', label: 'Ngừng hoạt động' },
  { value: 'terminated', label: 'Đã nghỉ việc' },
];
const status_labels = Object.fromEntries(status_options.filter((item) => item.value).map((item) => [item.value, item.label]));

function HumanResourcesPage() {
  const { current_user } = use_auth();
  const { message, modal } = AntdApp.useApp();
  const [search_params, set_search_params] = useSearchParams();
  const [employees, set_employees] = useState([]);
  const [is_loading, set_is_loading] = useState(false);
  const [error_message, set_error_message] = useState('');
  const [filters, set_filters] = useState(() => ({ search: search_params.get('search') || '', status: search_params.get('status') || '' }));
  const [search_input, set_search_input] = useState(() => search_params.get('search') || '');
  const [pagination, set_pagination] = useState(() => ({ current: Math.max(Number(search_params.get('page')) || 1, 1), page_size: 50, total: 0 }));
  const [is_modal_open, set_is_modal_open] = useState(false);
  const [editing_employee, set_editing_employee] = useState(null);
  const [form] = Form.useForm();
  const employees_request_ref = useRef(null);
  const [active_action_key, set_active_action_key] = useState('');

  const can_create = has_permission(current_user, 'hr_employee_create');
  const can_update = has_permission(current_user, 'hr_employee_update');
  const can_deactivate = has_permission(current_user, 'hr_employee_deactivate');

  const sync_query = (next_filters, next_page = null) => {
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
  };

  const load_employees = async (next_filters = filters, next_page = pagination.current, next_page_size = pagination.page_size) => {
    employees_request_ref.current?.abort();
    const controller = new AbortController();
    employees_request_ref.current = controller;
    set_is_loading(true);
    set_error_message('');
    const params = new URLSearchParams({ page: String(next_page - 1), page_size: String(next_page_size) });
    if (next_filters.search.trim()) params.set('search', next_filters.search.trim());
    if (next_filters.status) params.set('status', next_filters.status);
    try {
      const response = await request_api(`/api/v1/human_resources/employees?${params.toString()}`, { signal: controller.signal });
      if (controller.signal.aborted) return;
      set_employees(response.data.items);
      set_pagination({ current: response.data.page + 1, page_size: response.data.page_size, total: response.data.total_items });
    } catch (error) {
      if (controller.signal.aborted || error?.name === 'AbortError') return;
      set_employees([]);
      set_error_message(error.message || 'Employees could not be loaded.');
    } finally {
      if (employees_request_ref.current === controller) {
        employees_request_ref.current = null;
        set_is_loading(false);
      }
    }
  };

  useEffect(() => {
    load_employees();
    // The initial request intentionally runs once with the initial filter state.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => () => {
    employees_request_ref.current?.abort();
  }, []);

  const open_create = () => {
    set_editing_employee(null);
    form.resetFields();
    form.setFieldsValue({ employment_status: 'active' });
    set_is_modal_open(true);
  };

  const open_edit = async (employee) => {
    try {
      const response = await request_api('/api/v1/human_resources/employees/' + employee.employee_id);
      const detail = response.data;
      set_editing_employee(detail);
      form.resetFields();
      form.setFieldsValue({
        ...detail,
        date_of_birth: detail.date_of_birth ? format_date_vn(detail.date_of_birth) : undefined,
        hired_on: detail.hired_on ? format_date_vn(detail.hired_on) : undefined,
        terminated_on: detail.terminated_on ? format_date_vn(detail.terminated_on) : undefined,
      });
      set_is_modal_open(true);
    } catch (error) {
      message.error(error.message || 'Employee could not be loaded for editing.');
    }
  };

  const open_detail = async (employee) => {
    try {
      const response = await request_api('/api/v1/human_resources/employees/' + employee.employee_id);
      document.dispatchEvent(new CustomEvent('vinamik:data_workspace_open_record', { detail: { row_key: 'employee_id', record: response.data } }));
    } catch (error) {
      message.error(error.message || 'Employee details could not be loaded.');
    }
  };

  const close_modal = () => {
    if (!form.isFieldsTouched()) {
      set_is_modal_open(false);
      return;
    }
    modal.confirm({ title: 'Đóng biểu mẫu?', content: 'Các thay đổi chưa lưu sẽ bị mất.', okText: 'Đóng', cancelText: 'Tiếp tục chỉnh sửa', onOk: () => set_is_modal_open(false) });
  };

  const on_finish = async (values) => {
    const payload = Object.fromEntries(Object.entries(values).map(([key, value]) => [key, value === '' ? null : value]));
    for (const key of ['date_of_birth', 'hired_on', 'terminated_on']) payload[key] = to_iso_date(payload[key]);
    try {
      if (editing_employee) {
        await request_api(`/api/v1/human_resources/employees/${editing_employee.employee_id}`, { method: 'PUT', body: JSON.stringify(payload) });
        message.success('Employee updated successfully.');
      } else {
        await request_api('/api/v1/human_resources/employees', { method: 'POST', body: JSON.stringify(payload) });
        message.success('Employee created successfully.');
      }
      set_is_modal_open(false);
      form.resetFields();
      await load_employees(filters, pagination.current, pagination.page_size);
    } catch (error) {
      message.error(error.message || 'Employee could not be saved.');
      if (error.field_errors) {
        form.setFields(Object.entries(error.field_errors).map(([name, errors]) => ({ name, errors: [errors] })));
      }
    }
  };

  const deactivate_employee = (employee) => {
    modal.confirm({
      title: 'Vô hiệu hóa hồ sơ?',
      content: `Hồ sơ ${employee.employee_code} sẽ chuyển sang ngừng hoạt động.`,
      okText: 'Vô hiệu hóa',
      cancelText: 'Hủy',
      okButtonProps: { danger: true },
      onOk: async () => {
        const action_key = 'deactivate:' + employee.employee_id;
        set_active_action_key(action_key);
        try {
          await request_api(`/api/v1/human_resources/employees/${employee.employee_id}`, { method: 'DELETE' });
          message.success('Employee deactivated successfully.');
          await load_employees(filters, pagination.current, pagination.page_size);
        } catch (error) {
          message.error(error.message || 'Employee could not be deactivated.');
        } finally {
          set_active_action_key((current) => current === action_key ? '' : current);
        }
      },
    });
  };

  const on_search = (next_search = search_input) => {
    const next_filters = { ...filters, search: next_search };
    set_search_input(next_search);
    set_filters(next_filters);
    sync_query(next_filters, 1);
    load_employees(next_filters, 1, pagination.page_size);
  };
  const columns = [
    { title: 'Mã nhân viên', dataIndex: 'employee_code', key: 'employee_code', fixed: 'left', width: 140 },
    { title: 'Họ và tên', dataIndex: 'full_name', key: 'full_name', width: 220 },
    { title: 'Ngày sinh', dataIndex: 'date_of_birth', key: 'date_of_birth', width: 120, render: (value) => value ? format_date_vn(value) : 'Chưa cập nhật' },
    { title: 'Phòng ban', dataIndex: 'department_name', key: 'department_name', render: (value) => value || 'Chưa cập nhật' },
    { title: 'Chức danh', dataIndex: 'job_title_name', key: 'job_title_name', render: (value) => value || 'Chưa cập nhật' },
    { title: 'Trạng thái', dataIndex: 'employment_status', key: 'employment_status', render: (value) => <Tag color={value === 'active' ? 'green' : 'default'}>{status_labels[value] || value}</Tag> },
    { title: 'Ngày vào làm', dataIndex: 'hired_on', key: 'hired_on', render: (value) => value ? format_date_vn(value) : 'Chưa cập nhật' },
    { title: 'Thao tác', key: 'actions', fixed: 'right', width: 220, render: (_, employee) => <RecordActionBar
      on_open={() => open_detail(employee)}
      on_edit={() => open_edit(employee)}
      can_edit={can_update}

      workflow_actions={can_deactivate && employee.employment_status !== 'inactive' && employee.employment_status !== 'terminated' ? [{ key: 'deactivate', label: 'Ngừng sử dụng', loading: active_action_key === 'deactivate:' + employee.employee_id, on_click: () => deactivate_employee(employee) }] : []}
    /> },
  ];

  const on_table_change = (next_pagination) => {
    sync_query(filters, next_pagination.current);
    load_employees(filters, next_pagination.current, next_pagination.pageSize);
  };

  return <AntdApp>
    <div className="module_page module_page_human_resources">
      <ModuleMasthead
        module_key="human_resources"
        description="Quản lý hồ sơ nhân viên dùng chung cho các phân hệ có nhu cầu truy nguyên người thực hiện."
      />
    {error_message && <Alert className="workspace_error" type="error" showIcon message={error_message} />}
    <DataWorkspace
      title="Danh sách hồ sơ nhân viên"
      description="Tìm kiếm và phân trang trực tiếp trên dữ liệu trung tâm. Nhấp đúp một dòng để xem các trường hồ sơ."
      toolbar={<Space wrap className="list_toolbar">
        <DebouncedSearchInput placeholder="Tìm theo mã hoặc họ tên" value={search_input} on_commit={on_search} style={{ width: 280 }} />
        <Select value={filters.status} options={status_options} onChange={(status) => { const next_filters = { ...filters, status }; set_filters(next_filters); sync_query(next_filters); load_employees(next_filters, 1, pagination.page_size); }} style={{ width: 180 }} />
        {can_create && <Button type="primary" icon={<PlusOutlined />} onClick={open_create}>Thêm nhân viên</Button>}
      </Space>}
      columns={columns}
      data_source={employees}
      row_key="employee_id"
      loading={is_loading}
      pagination={pagination}
      on_change={on_table_change}
      empty_text={error_message ? 'Employee records could not be loaded.' : 'Chưa có hồ sơ nhân viên'}
      total_label="hồ sơ"
      read_only={!can_create && !can_update && !can_deactivate}
      column_presets={{ overview: ['employee_code', 'full_name', 'department_name', 'employment_status', 'actions'], detail: ['employee_code', 'full_name', 'date_of_birth', 'department_name', 'job_title_name', 'employment_status', 'hired_on', 'actions'], audit: ['employee_code', 'full_name', 'department_name', 'job_title_name', 'employment_status', 'hired_on'] }}
      storage_key={`data_workspace_hr_${current_user?.username || 'account'}`} on_open_record={open_detail}
    />

    <Modal className="entity_form_modal entity_form_modal_hr" width={760} open={is_modal_open} title={<div className="modal_title_block"><span>HỒ SƠ NHÂN VIÊN</span><strong>{editing_employee ? 'Sửa hồ sơ nhân viên' : 'Thêm nhân viên'}</strong><small>Thông tin dùng chung cho các phân hệ cần truy nguyên người thực hiện.</small></div>} onCancel={close_modal} footer={<div className="modal_footer_actions"><Button onClick={close_modal}>Hủy</Button><Button type="primary" htmlType="submit" form="employee_form">Lưu hồ sơ</Button></div>} destroyOnClose>
      <Form id="employee_form" form={form} layout="vertical" onFinish={on_finish} requiredMark={false}>
        <Form.Item label="Mã nhân viên" name="employee_code" rules={[{ required: true, message: 'Employee code is required.' }]}><Input maxLength={40} /></Form.Item>
        <Form.Item label="Họ và tên" name="full_name" rules={[{ required: true, message: 'Full name is required.' }]}><Input maxLength={160} /></Form.Item>
        <Form.Item label="Số điện thoại" name="phone_number"><Input maxLength={30} /></Form.Item>
        <Form.Item label="Email" name="email" rules={[{ type: 'email', message: 'Email format is invalid.' }]}><Input maxLength={254} /></Form.Item>
        <Form.Item label="Phòng ban" name="department_id"><LookupField field={{ lookup: 'departments', name: 'department_id', placeholder: 'Chọn phòng ban' }} selected_option={editing_employee?.department_id ? { value: editing_employee.department_id, label: editing_employee.department_name || String(editing_employee.department_id) } : undefined} form={form} /></Form.Item>
        <Form.Item label="Chức danh" name="job_title_id"><LookupField field={{ lookup: 'job_titles', name: 'job_title_id', placeholder: 'Chọn chức danh' }} selected_option={editing_employee?.job_title_id ? { value: editing_employee.job_title_id, label: editing_employee.job_title_name || String(editing_employee.job_title_id) } : undefined} form={form} /></Form.Item>
        <Form.Item label="Người quản lý" name="manager_employee_id"><LookupField field={{ lookup: 'employees', name: 'manager_employee_id', placeholder: 'Chọn người quản lý' }} form={form} /></Form.Item>
        <Form.Item label="Ngày sinh" name="date_of_birth"><VietnameseDateInput /></Form.Item>
        <Form.Item label="Trạng thái" name="employment_status"><Select options={status_options.filter((item) => item.value)} /></Form.Item>
        <Form.Item label="Ngày vào làm" name="hired_on"><VietnameseDateInput /></Form.Item>
        <Form.Item label="Ngày nghỉ việc" name="terminated_on"><VietnameseDateInput /></Form.Item>
        <Form.Item label="Ghi chú" name="notes"><Input.TextArea maxLength={2000} rows={3} /></Form.Item>

      </Form>
    </Modal>
    </div>
  </AntdApp>;
}

export default HumanResourcesPage;
