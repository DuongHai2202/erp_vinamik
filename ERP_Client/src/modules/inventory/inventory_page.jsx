import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { App as antd_app, Button, Checkbox, Form, Input, InputNumber, Modal, Select, Space, Tag, Typography } from 'antd';
import { EditOutlined, PlusOutlined, StopOutlined } from '@ant-design/icons';
import { has_permission } from '../../platform/common/permission_utils';
import { request_api } from '../../platform/common/api_client';
import { use_auth } from '../../platform/identity/auth_context';
import DataWorkspace from '../../platform/layout/data_workspace';

const AntdApp = antd_app;
const status_options = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'active', label: 'Đang sử dụng' },
  { value: 'inactive', label: 'Ngừng sử dụng' },
];
const status_labels = { active: 'Đang sử dụng', inactive: 'Ngừng sử dụng' };

function InventoryPage() {
  const { current_user } = use_auth();
  const { message } = AntdApp.useApp();
  const [search_params, set_search_params] = useSearchParams();
  const [materials, set_materials] = useState([]);
  const [units, set_units] = useState([]);
  const [categories, set_categories] = useState([]);
  const [is_loading, set_is_loading] = useState(false);
  const [error_message, set_error_message] = useState('');
  const [filters, set_filters] = useState(() => ({ search: search_params.get('search') || '', status: search_params.get('status') || '' }));
  const [pagination, set_pagination] = useState(() => ({ current: Math.max(Number(search_params.get('page')) || 1, 1), page_size: 50, total: 0 }));
  const [is_modal_open, set_is_modal_open] = useState(false);
  const [editing_material, set_editing_material] = useState(null);
  const [form] = Form.useForm();

  const can_create = has_permission(current_user, 'inventory_material_create');
  const can_update = has_permission(current_user, 'inventory_material_update');
  const can_deactivate = has_permission(current_user, 'inventory_material_deactivate');

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

  const load_materials = async (next_filters = filters, next_page = pagination.current, next_page_size = pagination.page_size) => {
    set_is_loading(true);
    set_error_message('');
    const params = new URLSearchParams({ page: String(next_page - 1), page_size: String(next_page_size) });
    if (next_filters.search.trim()) params.set('search', next_filters.search.trim());
    if (next_filters.status) params.set('status', next_filters.status);
    try {
      const response = await request_api(`/api/v1/inventory/materials?${params.toString()}`);
      set_materials(response.data.items);
      set_pagination({ current: response.data.page + 1, page_size: response.data.page_size, total: response.data.total_items });
    } catch (error) {
      set_error_message(error.message || 'Materials could not be loaded.');
    } finally {
      set_is_loading(false);
    }
  };

  useEffect(() => {
    Promise.all([
      request_api('/api/v1/inventory/master_data/units'),
      request_api('/api/v1/inventory/master_data/categories'),
    ]).then(([unit_response, category_response]) => {
      set_units(unit_response.data);
      set_categories(category_response.data);
    }).catch((error) => set_error_message(error.message || 'Master data could not be loaded.'));
    load_materials();
    // The initial requests intentionally run once with the initial filter state.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const open_create = () => {
    set_editing_material(null);
    form.resetFields();
    form.setFieldsValue({ status: 'active', lot_controlled: false });
    set_is_modal_open(true);
  };

  const open_edit = (material) => {
    set_editing_material(material);
    form.setFieldsValue({ ...material, base_unit_of_measure_id: material.base_unit_of_measure_id, item_category_id: material.item_category_id });
    set_is_modal_open(true);
  };

  const on_finish = async (values) => {
    const payload = { ...values, item_category_id: values.item_category_id || null, minimum_stock_quantity: values.minimum_stock_quantity || null };
    try {
      if (editing_material) {
        await request_api(`/api/v1/inventory/materials/${editing_material.stock_item_id}`, { method: 'PUT', body: JSON.stringify(payload) });
        message.success('Material updated successfully.');
      } else {
        await request_api('/api/v1/inventory/materials', { method: 'POST', body: JSON.stringify(payload) });
        message.success('Material created successfully.');
      }
      set_is_modal_open(false);
      form.resetFields();
      await load_materials(filters, pagination.current, pagination.page_size);
    } catch (error) {
      message.error(error.message || 'Material could not be saved.');
      if (error.field_errors) {
        form.setFields(Object.entries(error.field_errors).map(([name, errors]) => ({ name, errors: [errors] })));
      }
    }
  };

  const deactivate_material = (material) => {
    Modal.confirm({
      title: 'Vô hiệu hóa mặt hàng?',
      content: `Mặt hàng ${material.item_code} sẽ chuyển sang ngừng sử dụng.`,
      okText: 'Vô hiệu hóa',
      cancelText: 'Hủy',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await request_api(`/api/v1/inventory/materials/${material.stock_item_id}`, { method: 'DELETE' });
          message.success('Material deactivated successfully.');
          await load_materials(filters, pagination.current, pagination.page_size);
        } catch (error) {
          message.error(error.message || 'Material could not be deactivated.');
        }
      },
    });
  };

  const columns = [
    { title: 'Mã vật tư', dataIndex: 'item_code', key: 'item_code', fixed: 'left', width: 140 },
    { title: 'Tên vật tư', dataIndex: 'item_name', key: 'item_name', width: 240 },
    { title: 'Nhóm', dataIndex: 'category_name', key: 'category_name', render: (value) => value || '—' },
    { title: 'Đơn vị tính', dataIndex: 'unit_name', key: 'unit_name', render: (value, material) => value ? `${value} (${material.unit_code})` : '—' },
    { title: 'Theo dõi lô', dataIndex: 'lot_controlled', key: 'lot_controlled', render: (value) => value ? 'Có' : 'Không' },
    { title: 'Tồn tối thiểu', dataIndex: 'minimum_stock_quantity', key: 'minimum_stock_quantity', render: (value) => value ?? '—' },
    { title: 'Trạng thái', dataIndex: 'status', key: 'status', render: (value) => <Tag color={value === 'active' ? 'green' : 'default'}>{status_labels[value] || value}</Tag> },
    { title: 'Thao tác', key: 'actions', fixed: 'right', width: 140, render: (_, material) => <Space>
      {can_update && <Button type="text" icon={<EditOutlined />} aria-label="Sửa vật tư" onClick={() => open_edit(material)} />}
      {can_deactivate && material.status === 'active' && <Button type="text" danger icon={<StopOutlined />} aria-label="Vô hiệu hóa vật tư" onClick={() => deactivate_material(material)} />}
    </Space> },
  ];

  return <AntdApp>
    <div className="page_heading"><Typography.Title level={2}>Kho và nguyên vật liệu</Typography.Title><Typography.Paragraph>Danh mục nguyên vật liệu là nguồn dùng chung; số dư chỉ được lấy từ sổ giao dịch kho.</Typography.Paragraph></div>
    <DataWorkspace
      title="Danh sách nguyên vật liệu"
      description="Danh mục dùng chung cho Kho và Sản xuất. Dữ liệu được phân trang từ máy chủ để giữ thao tác mượt khi có nhiều bản ghi."
      toolbar={<Space wrap className="list_toolbar">
        <Input.Search allowClear placeholder="Tìm theo mã hoặc tên vật tư" value={filters.search} onChange={(event) => set_filters({ ...filters, search: event.target.value })} onSearch={() => { sync_query(filters); load_materials(filters, 1, pagination.page_size); }} style={{ width: 300 }} />
        <Select value={filters.status} options={status_options} onChange={(status) => { const next_filters = { ...filters, status }; set_filters(next_filters); sync_query(next_filters); load_materials(next_filters, 1, pagination.page_size); }} style={{ width: 170 }} />
        {can_create && <Button type="primary" icon={<PlusOutlined />} onClick={open_create}>Thêm nguyên vật liệu</Button>}
      </Space>}
      columns={columns}
      data_source={materials}
      row_key="stock_item_id"
      loading={is_loading}
      pagination={pagination}
      on_change={(next_pagination) => { sync_query(filters, next_pagination.current); load_materials(filters, next_pagination.current, next_pagination.pageSize); }}
      empty_text="Chưa có nguyên vật liệu"
      total_label="mặt hàng"
      read_only={!can_create && !can_update && !can_deactivate}
      column_presets={{ overview: ['item_code', 'item_name', 'category_name', 'unit_name', 'status', 'actions'], detail: ['item_code', 'item_name', 'category_name', 'unit_name', 'lot_controlled', 'minimum_stock_quantity', 'status', 'actions'], audit: ['item_code', 'item_name', 'category_name', 'lot_controlled', 'minimum_stock_quantity', 'status'] }}
      storage_key={`data_workspace_inventory_${current_user?.username || 'account'}`}
    />
    {error_message && <Typography.Paragraph type="danger">{error_message}</Typography.Paragraph>}
    <Modal open={is_modal_open} title={editing_material ? 'Sửa nguyên vật liệu' : 'Thêm nguyên vật liệu'} onCancel={() => set_is_modal_open(false)} footer={null} destroyOnClose>
      <Form form={form} layout="vertical" onFinish={on_finish} requiredMark={false}>
        <Form.Item label="Mã vật tư" name="item_code" rules={[{ required: true, message: 'Item code is required.' }]}><Input maxLength={60} /></Form.Item>
        <Form.Item label="Tên vật tư" name="item_name" rules={[{ required: true, message: 'Item name is required.' }]}><Input maxLength={180} /></Form.Item>
        <Form.Item label="Nhóm vật tư" name="item_category_id"><Select allowClear options={categories.map((item) => ({ value: item.category_id, label: item.category_name }))} /></Form.Item>
        <Form.Item label="Đơn vị tính cơ sở" name="base_unit_of_measure_id" rules={[{ required: true, message: 'Base unit is required.' }]}><Select options={units.map((item) => ({ value: item.unit_id, label: `${item.unit_name} (${item.unit_code})` }))} /></Form.Item>
        <Form.Item label="Theo dõi theo lô" name="lot_controlled" valuePropName="checked"><Checkbox>Có theo dõi lô và hạn dùng</Checkbox></Form.Item>
        <Form.Item label="Tồn tối thiểu" name="minimum_stock_quantity"><InputNumber min={0} precision={6} style={{ width: '100%' }} /></Form.Item>
        <Form.Item label="Trạng thái" name="status"><Select options={status_options.filter((item) => item.value)} /></Form.Item>
        <Form.Item label="Mô tả" name="description"><Input.TextArea maxLength={2000} rows={3} /></Form.Item>
        <Space><Button onClick={() => set_is_modal_open(false)}>Hủy</Button><Button type="primary" htmlType="submit">Lưu mặt hàng</Button></Space>
      </Form>
    </Modal>
  </AntdApp>;
}

export default InventoryPage;

