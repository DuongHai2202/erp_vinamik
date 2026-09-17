import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { App as antd_app, Button, Form, Input, InputNumber, Modal, Select, Space, Table, Tag, Typography } from 'antd';
import { DeleteOutlined, EditOutlined, MinusCircleOutlined, PlusOutlined, SendOutlined } from '@ant-design/icons';
import { has_permission } from '../../platform/common/permission_utils';
import { request_api } from '../../platform/common/api_client';
import { use_auth } from '../../platform/identity/auth_context';
import DataWorkspace from '../../platform/layout/data_workspace';

const AntdApp = antd_app;
const plan_status_options = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'draft', label: 'Bản nháp' },
  { value: 'approved', label: 'Đã duyệt' },
  { value: 'released', label: 'Đã phát hành' },
  { value: 'completed', label: 'Đã hoàn thành' },
  { value: 'cancelled', label: 'Đã hủy' },
];
const plan_status_labels = Object.fromEntries(plan_status_options.filter((item) => item.value).map((item) => [item.value, item.label]));
const next_status = { draft: 'approved', approved: 'released', released: 'completed' };

function ProductionPage() {
  const { current_user } = use_auth();
  const { message, modal } = AntdApp.useApp();
  const [search_params, set_search_params] = useSearchParams();
  const [plans, set_plans] = useState([]);
  const [stock_items, set_stock_items] = useState([]);
  const [is_loading, set_is_loading] = useState(false);
  const [error_message, set_error_message] = useState('');
  const [filters, set_filters] = useState(() => ({ search: search_params.get('search') || '', status: search_params.get('status') || '' }));
  const [pagination, set_pagination] = useState(() => ({ current: Math.max(Number(search_params.get('page')) || 1, 1), page_size: 50, total: 0 }));
  const [is_form_open, set_is_form_open] = useState(false);
  const [is_detail_open, set_is_detail_open] = useState(false);
  const [selected_plan, set_selected_plan] = useState(null);
  const [editing_plan, set_editing_plan] = useState(null);
  const [form] = Form.useForm();

  const can_create = has_permission(current_user, 'production_plan_create');
  const can_update = has_permission(current_user, 'production_plan_update');
  const can_approve = has_permission(current_user, 'production_plan_approve');
  const can_delete = has_permission(current_user, 'production_plan_delete');

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

  const load_plans = async (next_filters = filters, next_page = pagination.current, next_page_size = pagination.page_size) => {
    set_is_loading(true);
    set_error_message('');
    const params = new URLSearchParams({ page: String(next_page - 1), page_size: String(next_page_size) });
    if (next_filters.search.trim()) params.set('search', next_filters.search.trim());
    if (next_filters.status) params.set('status', next_filters.status);
    try {
      const response = await request_api(`/api/v1/production/plans?${params.toString()}`);
      set_plans(response.data.items);
      set_pagination({ current: response.data.page + 1, page_size: response.data.page_size, total: response.data.total_items });
    } catch (error) {
      set_error_message(error.message || 'Production plans could not be loaded.');
    } finally {
      set_is_loading(false);
    }
  };

  useEffect(() => {
    request_api('/api/v1/inventory/materials?item_type=finished_product&page_size=100')
      .then((response) => set_stock_items(response.data.items))
      .catch((error) => set_error_message(error.message || 'Finished products could not be loaded.'));
    load_plans();
    // The initial requests intentionally run once with the initial filter state.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const open_create = () => {
    set_editing_plan(null);
    form.resetFields();
    form.setFieldsValue({ lines: [{ stock_item_id: undefined, target_quantity: undefined }] });
    set_is_form_open(true);
  };

  const open_edit = async (plan) => {
    try {
      const response = await request_api(`/api/v1/production/plans/${plan.production_plan_id}`);
      set_editing_plan(response.data);
      form.setFieldsValue({ ...response.data, lines: response.data.lines.map((line) => ({ stock_item_id: line.stock_item_id, target_quantity: line.target_quantity, required_on: line.required_on, notes: line.notes })) });
      set_is_form_open(true);
    } catch (error) {
      message.error(error.message || 'Production plan could not be loaded.');
    }
  };

  const open_detail = async (plan) => {
    try {
      const response = await request_api(`/api/v1/production/plans/${plan.production_plan_id}`);
      set_selected_plan(response.data);
      set_is_detail_open(true);
    } catch (error) {
      message.error(error.message || 'Production plan could not be loaded.');
    }
  };

  const on_finish = async (values) => {
    const payload = { ...values, lines: values.lines.map((line) => ({ ...line, required_on: line.required_on || null, notes: line.notes || null })) };
    try {
      if (editing_plan) {
        await request_api(`/api/v1/production/plans/${editing_plan.production_plan_id}`, { method: 'PUT', body: JSON.stringify(payload) });
        message.success('Production plan updated successfully.');
      } else {
        await request_api('/api/v1/production/plans', { method: 'POST', body: JSON.stringify(payload) });
        message.success('Production plan created successfully.');
      }
      set_is_form_open(false);
      form.resetFields();
      await load_plans(filters, pagination.current, pagination.page_size);
    } catch (error) {
      message.error(error.message || 'Production plan could not be saved.');
      if (error.field_errors) form.setFields(Object.entries(error.field_errors).map(([name, errors]) => ({ name, errors: [errors] })));
    }
  };

  const change_plan_status = (plan) => {
    const target = next_status[plan.status];
    if (!target) return;
    modal.confirm({
      title: 'Chuyển trạng thái kế hoạch?',
      content: `Kế hoạch ${plan.plan_code} sẽ chuyển sang ${plan_status_labels[target]}.`,
      okText: 'Xác nhận',
      cancelText: 'Hủy',
      onOk: async () => {
        try {
          await request_api(`/api/v1/production/plans/${plan.production_plan_id}/status`, { method: 'POST', body: JSON.stringify({ status: target }) });
          message.success('Production plan status changed successfully.');
          await load_plans(filters, pagination.current, pagination.page_size);
        } catch (error) {
          message.error(error.message || 'Production plan status could not be changed.');
        }
      },
    });
  };

  const delete_plan = (plan) => {
    modal.confirm({
      title: 'Xóa kế hoạch nháp?',
      content: `Kế hoạch ${plan.plan_code} sẽ bị xóa khỏi hệ thống.`,
      okText: 'Xóa kế hoạch',
      cancelText: 'Hủy',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await request_api(`/api/v1/production/plans/${plan.production_plan_id}`, { method: 'DELETE' });
          message.success('Production plan deleted successfully.');
          await load_plans(filters, pagination.current, pagination.page_size);
        } catch (error) {
          message.error(error.message || 'Production plan could not be deleted.');
        }
      },
    });
  };

  const columns = [
    { title: 'Mã kế hoạch', dataIndex: 'plan_code', key: 'plan_code', fixed: 'left', width: 150 },
    { title: 'Tên kế hoạch', dataIndex: 'plan_name', key: 'plan_name', width: 230 },
    { title: 'Ngày bắt đầu', dataIndex: 'starts_on', key: 'starts_on' },
    { title: 'Ngày kết thúc', dataIndex: 'ends_on', key: 'ends_on' },
    { title: 'Số dòng', dataIndex: 'line_count', key: 'line_count' },
    { title: 'Trạng thái', dataIndex: 'status', key: 'status', render: (value) => <Tag color={value === 'released' ? 'blue' : value === 'completed' ? 'green' : 'default'}>{plan_status_labels[value] || value}</Tag> },
    { title: 'Thao tác', key: 'actions', fixed: 'right', width: 210, render: (_, plan) => <Space>
      <Button type="link" onClick={() => open_detail(plan)}>Chi tiết</Button>
      {can_update && plan.status === 'draft' && <Button type="text" icon={<EditOutlined />} aria-label="Sửa kế hoạch" onClick={() => open_edit(plan)} />}
      {can_approve && next_status[plan.status] && <Button type="text" icon={<SendOutlined />} aria-label="Chuyển trạng thái" onClick={() => change_plan_status(plan)} />}
      {can_delete && plan.status === 'draft' && <Button type="text" danger icon={<DeleteOutlined />} aria-label="Xóa kế hoạch" onClick={() => delete_plan(plan)} />}
    </Space> },
  ];

  return <AntdApp>
    <div className="page_heading"><Typography.Title level={2}>Quản lý sản xuất</Typography.Title><Typography.Paragraph>Lập kế hoạch nhiều dòng theo dữ liệu thành phẩm do Kho cung cấp; kế hoạch là căn cứ phát hành lệnh sản xuất.</Typography.Paragraph></div>
    <DataWorkspace
      title="Danh sách kế hoạch sản xuất"
      description="Theo dõi kế hoạch theo trạng thái, thời gian và số dòng sản phẩm. Nhấp đúp để xem nhanh dữ liệu kế hoạch."
      toolbar={<Space wrap className="list_toolbar">
        <Input.Search allowClear placeholder="Tìm theo mã hoặc tên kế hoạch" value={filters.search} onChange={(event) => set_filters({ ...filters, search: event.target.value })} onSearch={() => { sync_query(filters); load_plans(filters, 1, pagination.page_size); }} style={{ width: 300 }} />
        <Select value={filters.status} options={plan_status_options} onChange={(status) => { const next_filters = { ...filters, status }; set_filters(next_filters); sync_query(next_filters); load_plans(next_filters, 1, pagination.page_size); }} style={{ width: 170 }} />
        {can_create && <Button type="primary" icon={<PlusOutlined />} onClick={open_create}>Thêm kế hoạch</Button>}
      </Space>}
      columns={columns}
      data_source={plans}
      row_key="production_plan_id"
      loading={is_loading}
      pagination={pagination}
      on_change={(next_pagination) => { sync_query(filters, next_pagination.current); load_plans(filters, next_pagination.current, next_pagination.pageSize); }}
      empty_text="Chưa có kế hoạch sản xuất"
      total_label="kế hoạch"
      read_only={!can_create && !can_update && !can_approve && !can_delete}
      column_presets={{ overview: ['plan_code', 'plan_name', 'starts_on', 'ends_on', 'status', 'actions'], detail: ['plan_code', 'plan_name', 'starts_on', 'ends_on', 'line_count', 'status', 'actions'], audit: ['plan_code', 'plan_name', 'planned_on', 'starts_on', 'ends_on', 'status'] }}
      storage_key={`data_workspace_production_${current_user?.username || 'account'}`}
    />
    {error_message && <Typography.Paragraph type="danger">{error_message}</Typography.Paragraph>}
    <Modal open={is_form_open} title={editing_plan ? 'Sửa kế hoạch sản xuất' : 'Thêm kế hoạch sản xuất'} onCancel={() => set_is_form_open(false)} footer={null} width={760} destroyOnClose>
      <Form form={form} layout="vertical" onFinish={on_finish} requiredMark={false}>
        <Space size="middle" style={{ display: 'flex' }}>
          <Form.Item label="Mã kế hoạch" name="plan_code" rules={[{ required: true, message: 'Plan code is required.' }]} style={{ flex: 1 }}><Input maxLength={60} /></Form.Item>
          <Form.Item label="Tên kế hoạch" name="plan_name" rules={[{ required: true, message: 'Plan name is required.' }]} style={{ flex: 2 }}><Input maxLength={180} /></Form.Item>
        </Space>
        <Space size="middle" style={{ display: 'flex' }}>
          <Form.Item label="Ngày lập" name="planned_on" rules={[{ required: true, message: 'Planned date is required.' }]} style={{ flex: 1 }}><Input type="date" /></Form.Item>
          <Form.Item label="Ngày bắt đầu" name="starts_on" rules={[{ required: true, message: 'Start date is required.' }]} style={{ flex: 1 }}><Input type="date" /></Form.Item>
          <Form.Item label="Ngày kết thúc" name="ends_on" rules={[{ required: true, message: 'End date is required.' }]} style={{ flex: 1 }}><Input type="date" /></Form.Item>
        </Space>
        <Typography.Title level={5}>Sản phẩm kế hoạch</Typography.Title>
        <Form.List name="lines">
          {(fields, { add, remove }) => <>
            {fields.map(({ key, name, ...rest_field }) => <Space key={key} align="baseline" style={{ display: 'flex' }}>
              <Form.Item {...rest_field} name={[name, 'stock_item_id']} rules={[{ required: true, message: 'Stock item is required.' }]}><Select placeholder="Chọn thành phẩm" options={stock_items.map((item) => ({ value: item.stock_item_id, label: `${item.item_code} — ${item.item_name}` }))} style={{ width: 320 }} /></Form.Item>
              <Form.Item {...rest_field} name={[name, 'target_quantity']} rules={[{ required: true, message: 'Target quantity is required.' }]}><InputNumber min={0.000001} placeholder="Số lượng" style={{ width: 150 }} /></Form.Item>
              <Form.Item {...rest_field} name={[name, 'required_on']}><Input type="date" placeholder="Hạn cần" /></Form.Item>
              <Button type="text" danger icon={<MinusCircleOutlined />} aria-label="Xóa dòng" onClick={() => remove(name)} />
            </Space>)}
            <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>Thêm dòng sản phẩm</Button>
          </>}
        </Form.List>
        <Form.Item label="Ghi chú" name="notes"><Input.TextArea maxLength={2000} rows={3} /></Form.Item>
        <Space><Button onClick={() => set_is_form_open(false)}>Hủy</Button><Button type="primary" htmlType="submit">Lưu kế hoạch</Button></Space>
      </Form>
    </Modal>
    <Modal open={is_detail_open} title={selected_plan ? `Chi tiết ${selected_plan.plan_code}` : 'Chi tiết kế hoạch'} onCancel={() => set_is_detail_open(false)} footer={null} width={820}>
      {selected_plan && <Space direction="vertical" style={{ width: '100%' }}>
        <Typography.Paragraph>{selected_plan.plan_name} · {plan_status_labels[selected_plan.status] || selected_plan.status}</Typography.Paragraph>
        <Table rowKey="production_plan_line_id" size="small" pagination={false} dataSource={selected_plan.lines} columns={[{ title: 'Mã vật tư/thành phẩm', dataIndex: 'stock_item_code' }, { title: 'Tên', dataIndex: 'stock_item_name' }, { title: 'Đơn vị', dataIndex: 'unit_code' }, { title: 'Số lượng', dataIndex: 'target_quantity' }, { title: 'Hạn cần', dataIndex: 'required_on' }]} />
      </Space>}
    </Modal>
  </AntdApp>;
}

export default ProductionPage;

