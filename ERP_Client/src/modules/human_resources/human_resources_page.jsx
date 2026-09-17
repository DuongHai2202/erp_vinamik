import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { App as antd_app, Button, Form, Input, Modal, Select, Space, Tag, Typography } from 'antd';
import { EditOutlined, PlusOutlined, StopOutlined } from '@ant-design/icons';
import { has_permission } from '../../platform/common/permission_utils';
import { request_api } from '../../platform/common/api_client';
import { use_auth } from '../../platform/identity/auth_context';
import DataWorkspace from '../../platform/layout/data_workspace';

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
  const { message } = AntdApp.useApp();
  const [search_params, set_search_params] = useSearchParams();
  const [employees, set_employees] = useState([]);
  const [is_loading, set_is_loading] = useState(false);
  const [error_message, set_error_message] = useState('');
  const [filters, set_filters] = useState(() => ({ search: search_params.get('search') || '', status: search_params.get('status') || '' }));
  const [pagination, set_pagination] = useState(() => ({ current: Math.max(Number(search_params.get('page')) || 1, 1), page_size: 50, total: 0 }));
  const [is_modal_open, set_is_modal_open] = useState(false);
  const [editing_employee, set_editing_employee] = useState(null);
  const [form] = Form.useForm();

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
    set_is_loading(true);
    set_error_message('');
    const params = new URLSearchParams({ page: String(next_page - 1), page_size: String(next_page_size) });
    if (next_filters.search.trim()) params.set('search', next_filters.search.trim());
    if (next_filters.status) params.set('status', next_filters.status);
    try {
      const response = await request_api(`/api/v1/human_resources/employees?${params.toString()}`);
      set_employees(response.data.items);
      set_pagination({ current: response.data.page + 1, page_size: response.data.page_size, total: response.data.total_items });
    } catch (error) {
      set_error_message(error.message || 'Employees could not be loaded.');
    } finally {
      set_is_loading(false);
    }
  };

  useEffect(() => {
    load_employees();
    // The initial request intentionally runs once with the initial filter state.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const open_create = () => {
    set_editing_employee(null);
    form.resetFields();
    form.setFieldsValue({ employment_status: 'active' });
    set_is_modal_open(true);
  };

  const open_edit = (employee) => {
    set_editing_employee(employee);
    form.setFieldsValue(employee);
    set_is_modal_open(true);
  };

  const close_modal = () => {
    if (!form.isFieldsTouched()) {
      set_is_modal_open(false);
      return;
    }
    Modal.confirm({ title: 'Đóng biểu mẫu?', content: 'Các thay đổi chưa lưu sẽ bị mất.', okText: 'Đóng', cancelText: 'Tiếp tục chỉnh sửa', onOk: () => set_is_modal_open(false) });
  };

  const on_finish = async (values) => {
    const payload = Object.fromEntries(Object.entries(values).map(([key, value]) => [key, value === '' ? null : value]));
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
    Modal.confirm({
      title: 'Vô hiệu hóa hồ sơ?',
      content: `Hồ sơ ${employee.employee_code} sẽ chuyển sang ngừng hoạt động.`,
      okText: 'Vô hiệu hóa',
      cancelText: 'Hủy',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await request_api(`/api/v1/human_resources/employees/${employee.employee_id}`, { method: 'DELETE' });
          message.success('Employee deactivated successfully.');
          await load_employees(filters, pagination.current, pagination.page_size);
        } catch (error) {
          message.error(error.message || 'Employee could not be deactivated.');
        }
      },
    });
  };

  const columns = [
    { title: 'Mã nhân viên', dataIndex: 'employee_code', key: 'employee_code', fixed: 'left', width: 140 },
    { title: 'Họ và tên', dataIndex: 'full_name', key: 'full_name', width: 220 },
    { title: 'Phòng ban', dataIndex: 'department_name', key: 'department_name', render: (value) => value || '—' },
    { title: 'Chức danh', dataIndex: 'job_title_name', key: 'job_title_name', render: (value) => value || '—' },
    { title: 'Trạng thái', dataIndex: 'employment_status', key: 'employment_status', render: (value) => <Tag color={value === 'active' ? 'green' : 'default'}>{status_labels[value] || value}</Tag> },
    { title: 'Ngày vào làm', dataIndex: 'hired_on', key: 'hired_on', render: (value) => value || '—' },
    { title: 'Thao tác', key: 'actions', fixed: 'right', width: 150, render: (_, employee) => <Space>
      {can_update && <Button type="text" icon={<EditOutlined />} aria-label="Sửa hồ sơ" onClick={() => open_edit(employee)} />}
      {can_deactivate && employee.employment_status !== 'inactive' && employee.employment_status !== 'terminated' && <Button type="text" danger icon={<StopOutlined />} aria-label="Vô hiệu hóa hồ sơ" onClick={() => deactivate_employee(employee)} />}
    </Space> },
  ];

  const on_table_change = (next_pagination) => {
    sync_query(filters, next_pagination.current);
    load_employees(filters, next_pagination.current, next_pagination.pageSize);
  };

  return <AntdApp>
    <div className="page_heading"><Typography.Title level={2}>Quản lý nhân sự</Typography.Title><Typography.Paragraph>Quản lý hồ sơ nhân viên dùng chung cho các phân hệ có nhu cầu truy nguyên người thực hiện.</Typography.Paragraph></div>
    <DataWorkspace
      title="Danh sách hồ sơ nhân viên"
      description="Tìm kiếm và phân trang trực tiếp trên dữ liệu trung tâm. Nhấp đúp một dòng để xem các trường hồ sơ."
      toolbar={<Space wrap className="list_toolbar">
        <Input.Search allowClear placeholder="Tìm theo mã hoặc họ tên" value={filters.search} onChange={(event) => set_filters({ ...filters, search: event.target.value })} onSearch={() => { sync_query(filters); load_employees(filters, 1, pagination.page_size); }} style={{ width: 280 }} />
        <Select value={filters.status} options={status_options} onChange={(status) => { const next_filters = { ...filters, status }; set_filters(next_filters); sync_query(next_filters); load_employees(next_filters, 1, pagination.page_size); }} style={{ width: 180 }} />
        {can_create && <Button type="primary" icon={<PlusOutlined />} onClick={open_create}>Thêm nhân viên</Button>}
      </Space>}
      columns={columns}
      data_source={employees}
      row_key="employee_id"
      loading={is_loading}
      pagination={pagination}
      on_change={on_table_change}
      empty_text="Chưa có hồ sơ nhân viên"
      total_label="hồ sơ"
      read_only={!can_create && !can_update && !can_deactivate}
      column_presets={{ overview: ['employee_code', 'full_name', 'department_name', 'employment_status', 'actions'], detail: ['employee_code', 'full_name', 'department_name', 'job_title_name', 'employment_status', 'hired_on', 'actions'], audit: ['employee_code', 'full_name', 'department_name', 'job_title_name', 'employment_status', 'hired_on'] }}
      storage_key={`data_workspace_hr_${current_user?.username || 'account'}`}
    />
    {error_message && <Typography.Paragraph type="danger">{error_message}</Typography.Paragraph>}
    <Modal open={is_modal_open} title={editing_employee ? 'Sửa hồ sơ nhân viên' : 'Thêm nhân viên'} onCancel={close_modal} footer={null} destroyOnClose>
      <Form form={form} layout="vertical" onFinish={on_finish} requiredMark={false}>
        <Form.Item label="Mã nhân viên" name="employee_code" rules={[{ required: true, message: 'Employee code is required.' }]}><Input maxLength={40} /></Form.Item>
        <Form.Item label="Họ và tên" name="full_name" rules={[{ required: true, message: 'Full name is required.' }]}><Input maxLength={160} /></Form.Item>
        <Form.Item label="Số điện thoại" name="phone_number"><Input maxLength={30} /></Form.Item>
        <Form.Item label="Email" name="email" rules={[{ type: 'email', message: 'Email format is invalid.' }]}><Input maxLength={254} /></Form.Item>
        <Form.Item label="Mã phòng ban" name="department_id"><Input type="number" /></Form.Item>
        <Form.Item label="Mã chức danh" name="job_title_id"><Input type="number" /></Form.Item>
        <Form.Item label="Mã người quản lý" name="manager_employee_id"><Input type="number" /></Form.Item>
        <Form.Item label="Trạng thái" name="employment_status"><Select options={status_options.filter((item) => item.value)} /></Form.Item>
        <Form.Item label="Ngày vào làm" name="hired_on"><Input type="date" /></Form.Item>
        <Form.Item label="Ngày nghỉ việc" name="terminated_on"><Input type="date" /></Form.Item>
        <Form.Item label="Ghi chú" name="notes"><Input.TextArea maxLength={2000} rows={3} /></Form.Item>
        <Space><Button onClick={close_modal}>Hủy</Button><Button type="primary" htmlType="submit">Lưu hồ sơ</Button></Space>
      </Form>
    </Modal>
  </AntdApp>;
}

export default HumanResourcesPage;
