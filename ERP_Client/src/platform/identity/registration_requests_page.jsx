import { App as antd_app, Button, Card, Form, Input, Modal, Select, Space, Table, Tag, Typography, message as antd_message } from 'antd';
import { CheckOutlined, CloseOutlined, LockOutlined } from '@ant-design/icons';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { has_permission } from '../common/permission_utils';
import { request_api } from '../common/api_client';
import { use_auth } from './auth_context';
import RecordActionBar from '../layout/record_action_bar';
import DebouncedSearchInput from '../layout/debounced_search_input';
import { LookupField } from '../layout/workflow_fields';
import { format_datetime_vn } from '../common/formatters';

const status_labels = { pending: 'Chờ duyệt', approved: 'Đã duyệt', rejected: 'Từ chối', expired: 'Đã hết hạn' };
const status_colors = { pending: 'gold', approved: 'green', rejected: 'red', expired: 'default' };

function format_datetime(value) {
  return value ? format_datetime_vn(value) : 'Chưa cập nhật';
}

function RegistrationRequestsPage() {
  const { current_user } = use_auth();
  const { modal } = antd_app.useApp();
  const [message, message_context] = antd_message.useMessage();
  const [items, set_items] = useState([]);
  const [roles, set_roles] = useState([]);
  const [status, set_status] = useState('pending');
  const [search, set_search] = useState('');
  const [search_query, set_search_query] = useState('');
  const [is_loading, set_is_loading] = useState(false);
  const [total, set_total] = useState(0);
  const [page, set_page] = useState(1);
  const [approve_record, set_approve_record] = useState(null);
  const [reject_record, set_reject_record] = useState(null);
  const [approve_form] = Form.useForm();
  const [reject_form] = Form.useForm();
  const can_read = has_permission(current_user, 'identity_registration_read');
  const can_approve = has_permission(current_user, 'identity_registration_approve');

  const load_data = useCallback(async (next_page) => {
    set_is_loading(true);
    try {
      const params = new URLSearchParams({ page: String(next_page - 1), page_size: '50' });
      if (status) params.set('status', status);
      if (search_query.trim()) params.set('search', search_query.trim());
      const response = await request_api('/api/v1/identity/registration_requests?' + params.toString());
      set_items(response.data?.items || []);
      set_total(Number(response.data?.total_items || 0));
      set_page(Number(response.data?.page || 0) + 1);
    } catch (error) {
      set_items([]);
      message.error(error.message || 'Registration requests could not be loaded.');
    } finally {
      set_is_loading(false);
    }
  }, [message, search_query, status]);

  useEffect(() => {
    if (!can_read) return;
    request_api('/api/v1/identity/roles').then((response) => set_roles((response.data || []).filter((role) => role.role_code !== 'system_admin'))).catch(() => set_roles([]));
  }, [can_read]);

  useEffect(() => { if (can_read) load_data(1); }, [can_read, load_data]);

  const open_approve = useCallback((record) => {
    set_approve_record(record);
    approve_form.resetFields();
    approve_form.setFieldsValue({ employee_id: null, role_codes: ['read_only'] });
  }, [approve_form]);

  const submit_approve = async (values) => {
    try {
      await request_api('/api/v1/identity/registration_requests/' + approve_record.registration_request_id + '/approve', { method: 'POST', body: JSON.stringify(values) });
      message.success('Registration request approved successfully.');
      set_approve_record(null);
      await load_data(page);
    } catch (error) {
      message.error(error.message || 'Registration request could not be approved.');
    }
  };

  const submit_reject = async (values) => {
    try {
      await request_api('/api/v1/identity/registration_requests/' + reject_record.registration_request_id + '/reject', { method: 'POST', body: JSON.stringify(values) });
      message.success('Registration request rejected successfully.');
      set_reject_record(null);
      await load_data(page);
    } catch (error) {
      message.error(error.message || 'Registration request could not be rejected.');
    }
  };

  const columns = useMemo(() => [
    { title: 'ỨNG VIÊN', dataIndex: 'full_name', key: 'full_name', width: 190, fixed: 'left', render: (value, record) => <div className="registration_applicant"><strong>{value}</strong><small>{record.username}</small></div> },
    { title: 'EMAIL CÔNG TY', dataIndex: 'work_email', key: 'work_email', width: 230, ellipsis: true },
    { title: 'MÃ NHÂN VIÊN', dataIndex: 'employee_code', key: 'employee_code', width: 140, render: (value) => value || 'Chưa cung cấp' },
    { title: 'GỬI LÚC', dataIndex: 'requested_at', key: 'requested_at', width: 175, render: format_datetime },
    { title: 'HẾT HẠN', dataIndex: 'expires_at', key: 'expires_at', width: 175, render: format_datetime },
    { title: 'TRẠNG THÁI', dataIndex: 'status', key: 'status', width: 120, render: (value) => <Tag color={status_colors[value]}>{status_labels[value] || value}</Tag> },
    { title: 'THAO TÁC', key: 'actions', width: 260, fixed: 'right', render: (_, record) => <RecordActionBar
      on_open={() => modal.info({
        title: `Chi tiết yêu cầu ${record.username}`,
        content: <div className="workflow_detail_grid"><div><span>Họ và tên</span><strong>{record.full_name}</strong></div><div><span>Email</span><strong>{record.work_email}</strong></div><div><span>Nhân viên</span><strong>{record.employee_code || 'Chưa cung cấp'}</strong></div><div><span>Trạng thái</span><strong>{status_labels[record.status] || record.status}</strong></div></div>,
        okText: 'Đóng',
      })}
      workflow_actions={record.status === 'pending' && can_approve ? [
        { key: 'approve', label: 'Duyệt', icon: <CheckOutlined />, on_click: () => open_approve(record) },
        { key: 'reject', label: 'Từ chối', icon: <CloseOutlined />, on_click: () => { set_reject_record(record); reject_form.resetFields(); } },
      ] : []}
    /> },
  ], [can_approve, modal, open_approve, reject_form]);

  if (!can_read) return <div className="identity_denied"><LockOutlined /><Typography.Title level={3}>Không có quyền truy cập</Typography.Title><Typography.Paragraph>Registration review permission is required.</Typography.Paragraph></div>;

  return <>
    {message_context}
    <div className="identity_admin_page registration_requests_page">
      <div className="page_heading"><div><Typography.Title level={2}>Yêu cầu cấp tài khoản</Typography.Title><Typography.Paragraph>Kiểm tra nhân sự, gán vai trò tối thiểu và duyệt tài khoản trước khi đăng nhập.</Typography.Paragraph></div><Tag color={can_approve ? 'blue' : 'gold'} icon={can_approve ? <CheckOutlined /> : <LockOutlined />}>{can_approve ? 'Có quyền duyệt' : 'Chỉ xem'}</Tag></div>
      <Card bordered={false} className="identity_admin_surface">
        <div className="control_row registration_filter_row"><DebouncedSearchInput placeholder="Tìm theo tên, email hoặc username" value={search} on_commit={(next_search) => { set_search(next_search); set_search_query(next_search); set_page(1); }} /><Select value={status || undefined} allowClear placeholder="Tất cả trạng thái" options={Object.entries(status_labels).map(([value, label]) => ({ value, label }))} onChange={(value) => { set_status(value || ''); set_page(1); }} /><Button onClick={() => load_data(1)}>Tải lại</Button></div>
        <Table rowKey="registration_request_id" loading={is_loading} columns={columns} dataSource={items} scroll={{ x: 1120, y: 560 }} pagination={{ current: page, pageSize: 50, total, showSizeChanger: false, showTotal: (value, range) => String(range[0]) + '–' + String(range[1]) + ' / ' + String(value) + ' yêu cầu' }} onChange={(pagination) => load_data(pagination.current)} />
      </Card>
      <Modal open={Boolean(approve_record)} title="Duyệt yêu cầu cấp tài khoản" onCancel={() => set_approve_record(null)} footer={null} destroyOnClose>
        <Typography.Paragraph>
          Gắn yêu cầu <strong>{approve_record?.username}</strong> với hồ sơ nhân viên trong HR. Tìm theo mã nhân viên
          <strong>{approve_record?.employee_code ? ` ${approve_record.employee_code}` : ''}</strong> hoặc họ tên; hệ thống tự gửi mã nội bộ cho backend.
        </Typography.Paragraph>
        <Form form={approve_form} layout="vertical" onFinish={submit_approve} requiredMark={false}>
          <Form.Item
            label="Nhân viên HR liên kết"
            name="employee_id"
            rules={[{ required: true, message: 'Employee is required.' }]}
            extra="Chọn đúng hồ sơ đang làm việc để cấp tài khoản."
          >
            <LookupField field={{ lookup: 'employees', name: 'employee_id', required: true, placeholder: 'Tìm theo mã hoặc họ tên nhân viên' }} form={approve_form} />
          </Form.Item>
          <Form.Item label="Vai trò được cấp" name="role_codes" rules={[{ required: true, message: 'At least one role is required.' }]}><Select mode="multiple" options={roles.map((role) => ({ value: role.role_code, label: role.display_name }))} placeholder="Chọn vai trò" /></Form.Item>
          <Space><Button onClick={() => set_approve_record(null)}>Hủy</Button><Button type="primary" htmlType="submit">Duyệt và tạo tài khoản</Button></Space>
        </Form>
      </Modal>
      <Modal open={Boolean(reject_record)} title="Từ chối yêu cầu cấp tài khoản" onCancel={() => set_reject_record(null)} footer={null} destroyOnClose>
        <Form form={reject_form} layout="vertical" onFinish={submit_reject} requiredMark={false}>
          <Form.Item label="Lý do xử lý" name="review_note"><Input.TextArea rows={4} maxLength={500} showCount placeholder="Ghi lý do để audit và trao đổi nội bộ" /></Form.Item>
          <Space><Button onClick={() => set_reject_record(null)}>Hủy</Button><Button danger htmlType="submit">Từ chối yêu cầu</Button></Space>
        </Form>
      </Modal>
    </div>
  </>;
}

export default RegistrationRequestsPage;
