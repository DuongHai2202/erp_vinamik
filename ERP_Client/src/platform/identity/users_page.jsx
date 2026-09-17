import { useEffect, useState } from 'react';
import { App as antd_app, Button, Card, Form, Input, InputNumber, Modal, Result, Select, Space, Table, Tag, Typography } from 'antd';
import { EditOutlined, KeyOutlined, SafetyOutlined, UserAddOutlined } from '@ant-design/icons';
import { has_permission } from '../common/permission_utils';
import { request_api } from '../common/api_client';
import { use_auth } from './auth_context';

const AntdApp = antd_app;
const status_options = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'active', label: 'Đang hoạt động' },
  { value: 'locked', label: 'Đã khóa' },
  { value: 'disabled', label: 'Đã vô hiệu hóa' },
];
const status_labels = Object.fromEntries(status_options.filter((item) => item.value).map((item) => [item.value, item.label]));

function UsersPage() {
  const { current_user } = use_auth();
  const { message } = AntdApp.useApp();
  const [users, set_users] = useState([]);
  const [roles, set_roles] = useState([]);
  const [is_loading, set_is_loading] = useState(false);
  const [error_message, set_error_message] = useState('');
  const [filters, set_filters] = useState({ search: '', status: '' });
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
    { title: 'Role', dataIndex: 'role_codes', key: 'role_codes', render: (values) => <Space wrap>{(values || []).map((value) => <Tag key={value}>{value}</Tag>)}</Space> },
    { title: 'Nhân viên liên kết', dataIndex: 'employee_id', key: 'employee_id', width: 150, render: (value) => value || '—' },
    { title: 'Đăng nhập gần nhất', dataIndex: 'last_login_at', key: 'last_login_at', width: 190, render: (value) => value ? new Date(value).toLocaleString('vi-VN') : 'Chưa đăng nhập' },
    { title: 'Thao tác', key: 'actions', fixed: 'right', width: 180, render: (_, user) => <Space>
      {can_update && <Button type="text" icon={<EditOutlined />} aria-label="Sửa tài khoản" onClick={() => open_edit(user)} />}
      {can_manage_roles && <Button type="text" icon={<SafetyOutlined />} aria-label="Gán role" onClick={() => open_roles(user)} />}
      {can_update && <Button type="text" icon={<KeyOutlined />} aria-label="Đặt lại mật khẩu" onClick={() => open_password(user)} />}
    </Space> },
  ];

  if (!has_permission(current_user, 'identity_user_read')) {
    return <Result status="403" title="Access denied." subTitle="Your account does not have permission to view this page." />;
  }

  return <AntdApp>
    <div className="page_heading"><Typography.Title level={2}>Tài khoản và phân quyền</Typography.Title><Typography.Paragraph>Quản trị tài khoản tập trung; quyền thực tế luôn được kiểm tra ở backend.</Typography.Paragraph></div>
    <Card>
      <Space wrap className="list_toolbar">
        <Input.Search allowClear placeholder="Tìm tên đăng nhập" value={filters.search} onChange={(event) => set_filters({ ...filters, search: event.target.value })} onSearch={() => load_users(filters, 1, pagination.page_size)} style={{ width: 260 }} />
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
          <Form.Item label="Role ban đầu" name="role_codes"><Select mode="multiple" options={roles.map((role) => ({ value: role.role_code, label: `${role.display_name} (${role.role_code})` }))} /></Form.Item>
        </>}
        <Form.Item label="Mã nhân viên liên kết" name="employee_id"><InputNumber min={1} style={{ width: '100%' }} /></Form.Item>
        {editing_user && <Form.Item label="Trạng thái" name="status" rules={[{ required: true, message: 'Status is required.' }]}><Select options={status_options.filter((item) => item.value)} /></Form.Item>}
        <Space><Button onClick={() => set_user_modal_open(false)}>Hủy</Button><Button type="primary" htmlType="submit">Lưu tài khoản</Button></Space>
      </Form>
    </Modal>

    <Modal open={role_modal_open} title={`Gán role: ${editing_user?.username || ''}`} onCancel={() => set_role_modal_open(false)} footer={null} destroyOnClose>
      <Form form={role_form} layout="vertical" onFinish={on_roles_finish} requiredMark={false}>
        <Form.Item label="Role" name="role_codes" rules={[{ required: true, message: 'Select at least one role.' }]}><Select mode="multiple" options={roles.map((role) => ({ value: role.role_code, label: `${role.display_name} (${role.role_code})` }))} /></Form.Item>
        <Space><Button onClick={() => set_role_modal_open(false)}>Hủy</Button><Button type="primary" htmlType="submit">Lưu role</Button></Space>
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
