import { useEffect, useState } from 'react';
import { Alert, App as antd_app, Button, Card, Form, Input, InputNumber, Modal, Select, Space, Table, Tag, Typography } from 'antd';
import { CheckOutlined, CloseOutlined, EditOutlined, PlusOutlined, StopOutlined } from '@ant-design/icons';
import { has_permission } from '../../platform/common/permission_utils';
import { request_api } from '../../platform/common/api_client';
import { use_auth } from '../../platform/identity/auth_context';

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
  const [employees, set_employees] = useState([]);
  const [is_loading, set_is_loading] = useState(false);
  const [error_message, set_error_message] = useState('');
  const [filters, set_filters] = useState({ search: '', status: '', expiring_only: false });
  const [pagination, set_pagination] = useState({ current: 1, page_size: 50, total: 0 });
  const [is_modal_open, set_is_modal_open] = useState(false);
  const [editing_contract, set_editing_contract] = useState(null);
  const [form] = Form.useForm();

  const can_create = has_permission(current_user, 'hr_contract_create');
  const can_update = has_permission(current_user, 'hr_contract_update');
  const can_approve = has_permission(current_user, 'hr_contract_approve');

  const load_contracts = async (next_filters = filters, next_page = pagination.current, next_page_size = pagination.page_size) => {
    set_is_loading(true);
    set_error_message('');
    const params = new URLSearchParams({ page: String(next_page - 1), page_size: String(next_page_size) });
    if (next_filters.search.trim()) params.set('search', next_filters.search.trim());
    if (next_filters.status) params.set('status', next_filters.status);
    if (next_filters.expiring_only) params.set('expiring_only', 'true');
    try {
      const response = await request_api(`/api/v1/human_resources/contracts?${params.toString()}`);
      set_contracts(response.data.items);
      set_pagination({ current: response.data.page + 1, page_size: response.data.page_size, total: response.data.total_items });
    } catch (error) {
      set_error_message(error.message || 'Employment contracts could not be loaded.');
    } finally {
      set_is_loading(false);
    }
  };

  const load_employees = async () => {
    try {
      const response = await request_api('/api/v1/human_resources/employees?page=0&page_size=100&status=active');
      set_employees(response.data.items);
    } catch (error) {
      set_error_message(error.message || 'Employees could not be loaded.');
    }
  };

  useEffect(() => {
    load_contracts();
    load_employees();
    // Initial data load intentionally runs once with initial filters.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const open_create = () => {
    set_editing_contract(null);
    form.resetFields();
    form.setFieldsValue({ currency_code: 'VND', status: 'draft' });
    set_is_modal_open(true);
  };

  const open_edit = (contract) => {
    set_editing_contract(contract);
    form.setFieldsValue(contract);
    set_is_modal_open(true);
  };

  const on_finish = async (values) => {
    const payload = Object.fromEntries(Object.entries(values).map(([key, value]) => [key, value === '' ? null : value]));
    try {
      if (editing_contract) {
        await request_api(`/api/v1/human_resources/contracts/${editing_contract.employment_contract_id}`, { method: 'PUT', body: JSON.stringify(payload) });
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

  const change_status = (contract, status) => {
    const action_label = status === 'active' ? 'Duyệt' : status === 'cancelled' ? 'Hủy' : 'Đóng';
    modal.confirm({
      title: `${action_label} hợp đồng?`,
      content: `Hợp đồng ${contract.contract_code} sẽ chuyển sang ${status_labels[status]}.`,
      okText: 'Xác nhận',
      cancelText: 'Hủy',
      okButtonProps: status === 'terminated' || status === 'cancelled' ? { danger: true } : undefined,
      onOk: async () => {
        try {
          await request_api(`/api/v1/human_resources/contracts/${contract.employment_contract_id}/status`, { method: 'POST', body: JSON.stringify({ status }) });
          message.success('Employment contract status updated successfully.');
          await load_contracts(filters, pagination.current, pagination.page_size);
        } catch (error) {
          message.error(error.message || 'Employment contract status could not be updated.');
        }
      },
    });
  };

  const columns = [
    { title: 'Số hợp đồng', dataIndex: 'contract_code', key: 'contract_code', fixed: 'left', width: 150 },
    { title: 'Nhân viên', key: 'employee', width: 230, render: (_, item) => `${item.employee_code} — ${item.employee_name}` },
    { title: 'Loại hợp đồng', dataIndex: 'contract_type', key: 'contract_type', width: 150 },
    { title: 'Hiệu lực từ', dataIndex: 'effective_from', key: 'effective_from', width: 130 },
    { title: 'Hiệu lực đến', dataIndex: 'effective_to', key: 'effective_to', render: (value) => value || 'Không thời hạn', width: 150 },
    { title: 'Lương cơ bản', key: 'base_salary', render: (_, item) => `${Number(item.base_salary).toLocaleString('vi-VN')} ${item.currency_code}`, width: 160 },
    { title: 'Trạng thái', dataIndex: 'status', key: 'status', render: (value, item) => <Space direction="vertical" size={2}><Tag color={value === 'active' ? 'green' : value === 'draft' ? 'blue' : 'default'}>{status_labels[value] || value}</Tag>{item.expiring_soon && <Tag color="orange">Còn {item.days_until_expiry} ngày</Tag>}</Space>, width: 140 },
    { title: 'Thao tác', key: 'actions', fixed: 'right', width: 170, render: (_, item) => <Space>
      {can_update && item.status === 'draft' && <Button type="text" icon={<EditOutlined />} aria-label="Sửa hợp đồng" onClick={() => open_edit(item)} />}
      {can_approve && item.status === 'draft' && <Button type="text" icon={<CheckOutlined />} aria-label="Duyệt hợp đồng" onClick={() => change_status(item, 'active')} />}
      {can_approve && item.status === 'active' && <Button type="text" danger icon={<StopOutlined />} aria-label="Đóng hợp đồng" onClick={() => change_status(item, 'terminated')} />}
      {can_approve && item.status === 'draft' && <Button type="text" danger icon={<CloseOutlined />} aria-label="Hủy hợp đồng" onClick={() => change_status(item, 'cancelled')} />}
    </Space> },
  ];

  return <AntdApp>
    <div className="page_heading"><Typography.Title level={2}>Hợp đồng lao động</Typography.Title><Typography.Paragraph>Quản lý thời hạn, mức lương cơ bản và cảnh báo hợp đồng sắp hết hạn.</Typography.Paragraph></div>
    {filters.expiring_only && <Alert type="warning" showIcon message="Danh sách đang lọc các hợp đồng còn hiệu lực và hết hạn trong 15 ngày tới." style={{ marginBottom: 16 }} />}
    <Card>
      <Space wrap className="list_toolbar">
        <Input.Search allowClear placeholder="Tìm số hợp đồng hoặc nhân viên" value={filters.search} onChange={(event) => set_filters({ ...filters, search: event.target.value })} onSearch={() => load_contracts(filters, 1, pagination.page_size)} style={{ width: 280 }} />
        <Select value={filters.status} options={status_options} onChange={(status) => { const next_filters = { ...filters, status }; set_filters(next_filters); load_contracts(next_filters, 1, pagination.page_size); }} style={{ width: 170 }} />
        <Button type={filters.expiring_only ? 'primary' : 'default'} onClick={() => { const next_filters = { ...filters, expiring_only: !filters.expiring_only }; set_filters(next_filters); load_contracts(next_filters, 1, pagination.page_size); }}>Cảnh báo 15 ngày</Button>
        {can_create && <Button type="primary" icon={<PlusOutlined />} onClick={open_create}>Thêm hợp đồng</Button>}
      </Space>
      {error_message && <Typography.Paragraph type="danger">{error_message}</Typography.Paragraph>}
      <Table rowKey="employment_contract_id" columns={columns} dataSource={contracts} loading={is_loading} scroll={{ x: 1350 }} pagination={{ ...pagination, showSizeChanger: true, showTotal: (total) => `Tổng ${total} hợp đồng` }} onChange={(next_pagination) => load_contracts(filters, next_pagination.current, next_pagination.pageSize)} locale={{ emptyText: 'Chưa có hợp đồng' }} />
    </Card>
    <Modal open={is_modal_open} title={editing_contract ? 'Sửa hợp đồng lao động' : 'Thêm hợp đồng lao động'} onCancel={() => set_is_modal_open(false)} footer={null} destroyOnClose>
      <Form form={form} layout="vertical" onFinish={on_finish} requiredMark={false}>
        <Form.Item label="Số hợp đồng" name="contract_code" rules={[{ required: true, message: 'Contract code is required.' }]}><Input maxLength={60} /></Form.Item>
        <Form.Item label="Nhân viên" name="employee_id" rules={[{ required: true, message: 'Employee is required.' }]}><Select showSearch optionFilterProp="label" options={employees.map((employee) => ({ value: employee.employee_id, label: `${employee.employee_code} — ${employee.full_name}` }))} /></Form.Item>
        <Form.Item label="Loại hợp đồng" name="contract_type" rules={[{ required: true, message: 'Contract type is required.' }]}><Input maxLength={40} placeholder="Ví dụ: indefinite" /></Form.Item>
        <Space style={{ width: '100%' }} size="middle"><Form.Item label="Hiệu lực từ" name="effective_from" rules={[{ required: true, message: 'Effective start date is required.' }]}><Input type="date" /></Form.Item><Form.Item label="Hiệu lực đến" name="effective_to"><Input type="date" /></Form.Item></Space>
        <Space style={{ width: '100%' }} size="middle"><Form.Item label="Lương cơ bản" name="base_salary" rules={[{ required: true, message: 'Base salary is required.' }]}><InputNumber min={0} precision={2} style={{ width: '100%' }} /></Form.Item><Form.Item label="Tiền tệ" name="currency_code"><Input maxLength={3} /></Form.Item></Space>
        <Form.Item label="Trạng thái" name="status"><Select options={[{ value: 'draft', label: 'Bản nháp' }]} /></Form.Item>
        <Form.Item label="Ghi chú" name="notes"><Input.TextArea maxLength={2000} rows={3} /></Form.Item>
        <Space><Button onClick={() => set_is_modal_open(false)}>Hủy</Button><Button type="primary" htmlType="submit">Lưu hợp đồng</Button></Space>
      </Form>
    </Modal>
  </AntdApp>;
}

export default ContractsPage;
