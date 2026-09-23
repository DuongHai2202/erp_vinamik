import { useEffect, useState } from 'react';
import { App as antd_app, Button, Card, Form, Input, Modal, Result, Select, Space, Table, Tag, Typography } from 'antd';
import { KeyOutlined, SafetyOutlined, UserAddOutlined } from '@ant-design/icons';
import { has_permission } from '../common/permission_utils';
import { request_api } from '../common/api_client';
import { use_auth } from './auth_context';
import RecordActionBar from '../layout/record_action_bar';
import DebouncedSearchInput from '../layout/debounced_search_input';
import { LookupField } from '../layout/workflow_fields';
import { format_datetime_vn } from '../common/formatters';

const AntdApp = antd_app;
const status_options = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'active', label: 'Đang hoạt động' },
  { value: 'locked', label: 'Đã khóa' },
  { value: 'disabled', label: 'Đã vô hiệu hóa' },
];
const status_labels = Object.fromEntries(status_options.filter((item) => item.value).map((item) => [item.value, item.label]));
const role_labels = {
  system_admin: 'Quản trị hệ thống',
  read_only: 'Chỉ xem dữ liệu',
  hr_staff: 'Nhân viên nhân sự',
  hr_manager: 'Quản lý nhân sự',
  inventory_staff: 'Nhân viên kho',
  inventory_manager: 'Quản lý kho',
  production_staff: 'Nhân viên sản xuất',
  production_manager: 'Quản lý sản xuất',
};

function role_label(role_code) {
  return role_labels[role_code] || role_code;
}

function UsersPage() {
  const { current_user } = use_auth();
  const { message, modal } = AntdApp.useApp();
  const [users, set_users] = useState([]);
  const [roles, set_roles] = useState([]);
  const [is_loading, set_is_loading] = useState(false);
  const [error_message, set_error_message] = useState('');
  const [filters, set_filters] = useState({ search: '', status: '' });
  const [search_input, set_search_input] = useState('');
  const [pagination, set_pagination] = useState({ current: 1, page_size: 50, total: 0 });
  const [user_modal_open, set_user_modal_open] = useState(false);
  const [role_modal_open, set_role_modal_open] = useState(false);
  const [password_modal_open, set_password_modal_open] = useState(false);
  const [editing_user, set_editing_user] = useState(null);
  const [user_form] = Form.useForm();
  const [role_form] = Form.useForm();
  const [password_form] = Form.useForm();

  const can_create = has_permission(current_user, 'identity_user_create');
  const can_update = has_permission(current_user, 'identity_user_update');
  const can_manage_roles = has_permission(current_user, 'identity_role_update');

  const load_roles = async () => {
    try {
      const response = await request_api('/api/v1/identity/roles');
      set_roles(response.data || []);
    } catch (error) {
      set_error_message(error.message || 'Roles could not be loaded.');
    }
  };

  const load_users = async (next_filters = filters, next_page = pagination.current, next_page_size = pagination.page_size) => {
    set_is_loading(true);
    set_error_message('');
    const params = new URLSearchParams({ page: String(next_page - 1), page_size: String(next_page_size) });
    if (next_filters.search.trim()) params.set('search', next_filters.search.trim());
    if (next_filters.status) params.set('status', next_filters.status);
    try {
      const response = await request_api(`/api/v1/identity/users?${params.toString()}`);
      set_users(response.data.items);
      set_pagination({ current: response.data.page + 1, page_size: response.data.page_size, total: response.data.total_items });
    } catch (error) {
      set_error_message(error.message || 'Users could not be loaded.');
    } finally {
      set_is_loading(false);
    }
  };

  useEffect(() => {
    load_roles();
    load_users();
    // Initial data load intentionally uses the initial filter state.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const on_search = (next_search = search_input) => {
    const next_filters = { ...filters, search: next_search };
    set_search_input(next_search);
    set_filters(next_filters);
    load_users(next_filters, 1, pagination.page_size);
  };
  const open_create = () => {
    set_editing_user(null);
    user_form.resetFields();
    user_form.setFieldsValue({ status: 'active', role_codes: ['read_only'] });
    set_user_modal_open(true);
  };

  const open_edit = (user) => {
    set_editing_user(user);
    user_form.setFieldsValue({ status: user.status, employee_id: user.employee_id });
    set_user_modal_open(true);
  };

  const open_roles = (user) => {
    set_editing_user(user);
    role_form.setFieldsValue({ role_codes: user.role_codes });
    set_role_modal_open(true);
  };

  const open_password = (user) => {
    set_editing_user(user);
    password_form.resetFields();
    set_password_modal_open(true);
  };

  const on_user_finish = async (values) => {
    try {
      if (editing_user) {
        await request_api(`/api/v1/identity/users/${editing_user.user_id}`, { method: 'PUT', body: JSON.stringify(values) });
        message.success('User updated successfully.');
      } else {
        await request_api('/api/v1/identity/users', { method: 'POST', body: JSON.stringify(values) });
        message.success('User created successfully.');
      }
      set_user_modal_open(false);
      await load_users(filters, pagination.current, pagination.page_size);
    } catch (error) {
      message.error(error.message || 'User could not be saved.');
    }
  };

  const on_roles_finish = async (values) => {
    try {
      await request_api(`/api/v1/identity/users/${editing_user.user_id}/roles`, { method: 'PUT', body: JSON.stringify(values) });
      message.success('User roles updated successfully.');
      set_role_modal_open(false);
      await load_users(filters, pagination.current, pagination.page_size);
    } catch (error) {
      message.error(error.message || 'User roles could not be updated.');
    }
  };

  const on_password_finish = async (values) => {
    try {
      await request_api(`/api/v1/identity/users/${editing_user.user_id}/reset-password`, { method: 'POST', body: JSON.stringify(values) });
      message.success('Password reset successfully.');
      set_password_modal_open(false);
      password_form.resetFields();
    } catch (error) {
      message.error(error.message || 'Password could not be reset.');
    }
  };

  const columns = [
    { title: 'Tên đăng nhập', dataIndex: 'username', key: 'username', fixed: 'left', width: 180 },
    { title: 'Trạng thái', dataIndex: 'status', key: 'status', width: 160, render: (value) => <Tag color={value === 'active' ? 'green' : 'default'}>{status_labels[value] || value}</Tag> },
    { title: 'Vai trò', dataIndex: 'role_codes', key: 'role_codes', render: (values) => <Space wrap>{(values || []).map((value) => <Tag key={value}>{role_label(value)}</Tag>)}</Space> },
    { title: 'Nhân viên liên kết', dataIndex: 'employee_id', key: 'employee_id', width: 220, render: (value, user) => user.employee_code ? `${user.employee_code} — ${user.employee_name || 'Chưa có tên'}` : (value ? `Mã nội bộ #${value}` : 'Chưa liên kết') },
    { title: 'Đăng nhập gần nhất', dataIndex: 'last_login_at', key: 'last_login_at', width: 190, render: (value) => value ? format_datetime_vn(value) : 'Chưa đăng nhập' },
    { title: 'Thao tác', key: 'actions', fixed: 'right', width: 220, render: (_, user) => <RecordActionBar
      on_open={() => modal.info({
        title: `Chi tiết tài khoản ${user.username}`,
        content: <div className="workflow_detail_grid"><div><span>Trạng thái</span><strong>{status_labels[user.status] || user.status}</strong></div><div><span>Vai trò</span><strong>{(user.role_codes || []).map(role_label).join(', ') || 'Chưa gán'}</strong></div><div><span>Nhân viên liên kết</span><strong>{user.employee_code ? `${user.employee_code} — ${user.employee_name || 'Chưa có tên'}` : (user.employee_id ? `Mã nội bộ #${user.employee_id}` : 'Chưa liên kết')}</strong></div></div>,
        okText: 'Đóng',
      })}
      on_edit={() => open_edit(user)}
      can_edit={can_update}
      workflow_actions={[
        ...(can_manage_roles ? [{ key: 'roles', label: 'Gán vai trò', icon: <SafetyOutlined />, on_click: () => open_roles(user) }] : []),
        ...(can_update ? [{ key: 'password', label: 'Đặt lại mật khẩu', icon: <KeyOutlined />, on_click: () => open_password(user) }] : []),
      ]}
    /> },
  ];

  if (!has_permission(current_user, 'identity_user_read')) {
    return <Result status="403" title="Access denied." subTitle="Your account does not have permission to view this page." />;
  }

  return <AntdApp>
    <div className="page_heading"><Typography.Title level={2}>Tài khoản và phân quyền</Typography.Title><Typography.Paragraph>Quản trị tài khoản tập trung; quyền thực tế luôn được kiểm tra ở backend.</Typography.Paragraph></div>
    <Card>
      <Space wrap className="list_toolbar">
        <DebouncedSearchInput placeholder="Tìm tên đăng nhập" value={search_input} on_commit={on_search} style={{ width: 260 }} />
        <Select value={filters.status} options={status_options} onChange={(status) => { const next_filters = { ...filters, status }; set_filters(next_filters); load_users(next_filters, 1, pagination.page_size); }} style={{ width: 180 }} />
        {can_create && <Button type="primary" icon={<UserAddOutlined />} onClick={open_create}>Thêm tài khoản</Button>}
      </Space>
      {error_message && <Typography.Paragraph type="danger">{error_message}</Typography.Paragraph>}
      <Table rowKey="user_id" columns={columns} dataSource={users} loading={is_loading} scroll={{ x: 1100 }} pagination={{ ...pagination, showSizeChanger: true }} onChange={(next_pagination) => load_users(filters, next_pagination.current, next_pagination.pageSize)} locale={{ emptyText: 'Chưa có tài khoản' }} />
    </Card>

    <Modal open={user_modal_open} title={editing_user ? 'Cập nhật tài khoản' : 'Thêm tài khoản'} onCancel={() => set_user_modal_open(false)} footer={null} destroyOnClose>
      <Form form={user_form} layout="vertical" onFinish={on_user_finish} requiredMark={false}>
        {!editing_user && <>
          <Form.Item label="Tên đăng nhập" name="username" rules={[{ required: true, message: 'Username is required.' }]}><Input maxLength={80} /></Form.Item>
          <Form.Item label="Mật khẩu tạm thời" name="password" rules={[{ required: true, min: 12, message: 'Password must contain at least 12 characters.' }]}><Input.Password maxLength={128} /></Form.Item>
          <Form.Item label="Vai trò ban đầu" name="role_codes"><Select mode="multiple" options={roles.map((role) => ({ value: role.role_code, label: role.display_name || role_label(role.role_code) }))} /></Form.Item>
        </>}
        <Form.Item
          label="Nhân viên liên kết"
          name="employee_id"
          extra="Tìm theo mã HR (ví dụ vmk0001) hoặc họ tên. Hệ thống tự lưu mã nội bộ để liên kết dữ liệu."
        >
          <LookupField
            field={{ lookup: 'employees', name: 'employee_id', placeholder: 'Tìm mã nhân viên hoặc họ tên' }}
            form={user_form}
            selected_option={editing_user?.employee_id ? {
              value: editing_user.employee_id,
              label: [editing_user.employee_code, editing_user.employee_name].filter(Boolean).join(' — ') || `Mã nội bộ #${editing_user.employee_id}`,
            } : undefined}
          />
        </Form.Item>
        {editing_user && <Form.Item label="Trạng thái" name="status" rules={[{ required: true, message: 'Status is required.' }]}><Select options={status_options.filter((item) => item.value)} /></Form.Item>}
        <Space><Button onClick={() => set_user_modal_open(false)}>Hủy</Button><Button type="primary" htmlType="submit">Lưu tài khoản</Button></Space>
      </Form>
    </Modal>

    <Modal open={role_modal_open} title={`Gán vai trò: ${editing_user?.username || ''}`} onCancel={() => set_role_modal_open(false)} footer={null} destroyOnClose>
      <Form form={role_form} layout="vertical" onFinish={on_roles_finish} requiredMark={false}>
        <Form.Item label="Vai trò" name="role_codes" rules={[{ required: true, message: 'Select at least one role.' }]}><Select mode="multiple" options={roles.map((role) => ({ value: role.role_code, label: role.display_name || role_label(role.role_code) }))} /></Form.Item>
        <Space><Button onClick={() => set_role_modal_open(false)}>Hủy</Button><Button type="primary" htmlType="submit">Lưu vai trò</Button></Space>
      </Form>
    </Modal>

    <Modal open={password_modal_open} title={`Đặt lại mật khẩu: ${editing_user?.username || ''}`} onCancel={() => set_password_modal_open(false)} footer={null} destroyOnClose>
      <Form form={password_form} layout="vertical" onFinish={on_password_finish} requiredMark={false}>
        <Form.Item label="Mật khẩu mới" name="password" rules={[{ required: true, min: 12, message: 'Password must contain at least 12 characters.' }]}><Input.Password maxLength={128} /></Form.Item>
        <Space><Button onClick={() => set_password_modal_open(false)}>Hủy</Button><Button type="primary" htmlType="submit">Đặt lại mật khẩu</Button></Space>
      </Form>
    </Modal>
  </AntdApp>;
}

export default UsersPage;
