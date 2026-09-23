import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Alert, App as antd_app, Button, Checkbox, Drawer, Form, Input, InputNumber, Modal, Select, Space, Tag, Typography } from 'antd';
import { FilterOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { has_permission } from '../../platform/common/permission_utils';
import { request_api } from '../../platform/common/api_client';
import { use_auth } from '../../platform/identity/auth_context';
import DataWorkspace from '../../platform/layout/data_workspace';
import RecordActionBar from '../../platform/layout/record_action_bar';
import ModuleMasthead from '../../platform/layout/module_masthead';
import DebouncedSearchInput from '../../platform/layout/debounced_search_input';

const AntdApp = antd_app;
const status_options = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'active', label: 'Đang sử dụng' },
  { value: 'inactive', label: 'Ngừng sử dụng' },
];
const status_labels = { active: 'Đang sử dụng', inactive: 'Ngừng sử dụng' };
const item_type_options = [
  { value: 'raw_material', label: 'Nguyên vật liệu' },
  { value: 'finished_product', label: 'Thành phẩm' },
];

function InventoryBalanceView({ current_user }) {
  const [balances, set_balances] = useState([]);
  const [warehouses, set_warehouses] = useState([]);
  const [filters, set_filters] = useState({ search: '', warehouse_id: '' });
  const [search_input, set_search_input] = useState('');
  const [pagination, set_pagination] = useState({ current: 1, page_size: 50, total: 0 });
  const [is_loading, set_is_loading] = useState(false);
  const [error_message, set_error_message] = useState('');
  const balances_request_ref = useRef(null);

  const load_balances = async (next_filters = filters, next_page = pagination.current, next_page_size = pagination.page_size) => {
    balances_request_ref.current?.abort();
    const controller = new AbortController();
    balances_request_ref.current = controller;
    set_is_loading(true);
    set_error_message('');
    const params = new URLSearchParams({ page: String(next_page - 1), page_size: String(next_page_size) });
    if (next_filters.search.trim()) params.set('search', next_filters.search.trim());
    if (next_filters.warehouse_id) params.set('warehouse_id', next_filters.warehouse_id);
    try {
      const response = await request_api('/api/v1/inventory/balances?' + params.toString(), { signal: controller.signal });
      if (controller.signal.aborted) return;
      const page_data = response.data || {};
      set_balances(page_data.items || []);
      set_pagination({ current: Number(page_data.page || 0) + 1, page_size: Number(page_data.page_size || next_page_size), total: Number(page_data.total_items || 0) });
    } catch (error) {
      if (controller.signal.aborted || error?.name === 'AbortError') return;
      set_balances([]);
      set_error_message(error.message || 'Stock balances could not be loaded.');
    } finally {
      if (balances_request_ref.current === controller) {
        balances_request_ref.current = null;
        set_is_loading(false);
      }
    }
  };

  useEffect(() => {
    request_api('/api/v1/inventory/master_data/warehouses')
      .then((response) => set_warehouses(response.data || []))
      .catch(() => set_warehouses([]));
    load_balances();
    // Initial data load intentionally runs once with the initial filter state.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => () => {
    balances_request_ref.current?.abort();
  }, []);

  const columns = [
    { title: 'Mã vật tư', dataIndex: 'item_code', key: 'item_code', fixed: 'left', width: 150 },
    { title: 'Tên vật tư', dataIndex: 'item_name', key: 'item_name', width: 260, ellipsis: true },
    { title: 'Kho', dataIndex: 'warehouse_code', key: 'warehouse_code', width: 150 },
    { title: 'Vị trí', dataIndex: 'location_code', key: 'location_code', width: 150 },
    { title: 'Mã lô', dataIndex: 'lot_code', key: 'lot_code', width: 150, render: (value) => value || 'Không theo dõi lô' },
    { title: 'Số dư', dataIndex: 'on_hand_quantity', key: 'on_hand_quantity', width: 130, render: (value) => Number(value || 0).toLocaleString('vi-VN', { maximumFractionDigits: 6 }) },
    { title: 'Đơn vị', dataIndex: 'unit_code', key: 'unit_code', width: 100 },
  ];

  const on_search = (next_search = search_input) => {
    const next_filters = { ...filters, search: next_search };
    set_search_input(next_search);
    set_filters(next_filters);
    load_balances(next_filters, 1, pagination.page_size);
  };
  const on_warehouse_change = (warehouse_id) => {
    const next_filters = { ...filters, warehouse_id: warehouse_id || '' };
    set_filters(next_filters);
    load_balances(next_filters, 1, pagination.page_size);
  };

  return <DataWorkspace
    module_key="inventory"
    title="Số dư tồn kho"
    description="Số dư được tổng hợp từ sổ giao dịch và có thể lọc theo kho, vật tư, vị trí hoặc lô."
    toolbar={<Space wrap className="list_toolbar control_row">
      <DebouncedSearchInput placeholder="Tìm mã hoặc tên vật tư" value={search_input} on_commit={on_search} style={{ width: 300 }} />
      <Select allowClear placeholder="Tất cả kho" value={filters.warehouse_id || undefined} options={warehouses.map((item) => ({ value: item.warehouse_id, label: item.warehouse_code + ' — ' + item.warehouse_name }))} onChange={on_warehouse_change} style={{ width: 220 }} />
    </Space>}
    columns={columns}
    data_source={balances}
    row_key={(item) => item.stock_item_id + '-' + item.warehouse_location_id + '-' + (item.stock_lot_id || 'none')}
    loading={is_loading}
    pagination={pagination}
    on_change={(next_pagination) => load_balances(filters, next_pagination.current, next_pagination.pageSize)}
    empty_text={error_message ? 'Không thể tải số dư' : 'Chưa có số dư tồn kho'}
    total_label="dòng số dư"
    read_only
    column_presets={{ overview: ['item_code', 'item_name', 'warehouse_code', 'location_code', 'on_hand_quantity', 'unit_code'], detail: columns.map((item) => item.key), audit: ['item_code', 'warehouse_code', 'location_code', 'lot_code', 'on_hand_quantity'] }}
    storage_key={'data_workspace_inventory_balances_' + (current_user?.username || 'account')}
  />;
}

function InventoryPage() {
  const { current_user } = use_auth();
  const { message, modal } = AntdApp.useApp();
  const [search_params, set_search_params] = useSearchParams();
  const [materials, set_materials] = useState([]);
  const [units, set_units] = useState([]);
  const [categories, set_categories] = useState([]);
  const [is_loading, set_is_loading] = useState(false);
  const [error_message, set_error_message] = useState('');
  const [filters, set_filters] = useState(() => ({ search: search_params.get('search') || '', status: search_params.get('status') || '', item_type: search_params.get('item_type') || 'raw_material' }));
  const [search_input, set_search_input] = useState(() => search_params.get('search') || '');
  const [pagination, set_pagination] = useState(() => ({ current: Math.max(Number(search_params.get('page')) || 1, 1), page_size: 50, total: 0 }));
  const [is_modal_open, set_is_modal_open] = useState(false);
  const [editing_material, set_editing_material] = useState(null);
  const [form] = Form.useForm();
  const [active_view, set_active_view] = useState('materials');
  const [filter_drawer_open, set_filter_drawer_open] = useState(false);
  const [draft_filters, set_draft_filters] = useState({ item_type: search_params.get('item_type') || 'raw_material' });
  const materials_request_ref = useRef(null);
  const [active_action_key, set_active_action_key] = useState('');

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
    materials_request_ref.current?.abort();
    const controller = new AbortController();
    materials_request_ref.current = controller;
    set_is_loading(true);
    set_error_message('');
    const params = new URLSearchParams({ page: String(next_page - 1), page_size: String(next_page_size) });
    if (next_filters.search.trim()) params.set('search', next_filters.search.trim());
    if (next_filters.status) params.set('status', next_filters.status);
    params.set('item_type', next_filters.item_type || 'raw_material');
    try {
      const response = await request_api('/api/v1/inventory/materials?' + params.toString(), { signal: controller.signal });
      if (controller.signal.aborted) return;
      set_materials(response.data.items);
      set_pagination({ current: response.data.page + 1, page_size: response.data.page_size, total: response.data.total_items });
    } catch (error) {
      if (controller.signal.aborted || error?.name === 'AbortError') return;
      set_materials([]);
      set_error_message(error.message || 'Materials could not be loaded.');
    } finally {
      if (materials_request_ref.current === controller) {
        materials_request_ref.current = null;
        set_is_loading(false);
      }
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

  useEffect(() => () => {
    materials_request_ref.current?.abort();
  }, []);

  const on_material_search = (next_search = search_input) => {
    const next_filters = { ...filters, search: next_search };
    set_search_input(next_search);
    set_filters(next_filters);
    sync_query(next_filters, 1);
    load_materials(next_filters, 1, pagination.page_size);
  };
  const open_advanced_filters = () => {
    set_draft_filters({ item_type: filters.item_type || 'raw_material' });
    set_filter_drawer_open(true);
  };

  const apply_advanced_filters = () => {
    const next_filters = { ...filters, ...draft_filters };
    set_filters(next_filters);
    sync_query(next_filters, 1);
    load_materials(next_filters, 1, pagination.page_size);
    set_filter_drawer_open(false);
  };

  const reset_advanced_filters = () => {
    const next_filters = { ...filters, item_type: 'raw_material' };
    set_draft_filters({ item_type: 'raw_material' });
    set_filters(next_filters);
    sync_query(next_filters, 1);
    load_materials(next_filters, 1, pagination.page_size);
    set_filter_drawer_open(false);
  };

  const open_create = () => {
    set_editing_material(null);
    form.resetFields();
    form.setFieldsValue({ status: 'active', lot_controlled: false });
    set_is_modal_open(true);
  };

  const open_edit = async (material) => {
    try {
      const response = await request_api('/api/v1/inventory/materials/' + material.stock_item_id);
      const detail = response.data;
      set_editing_material(detail);
      form.resetFields();
      form.setFieldsValue({
        ...detail,
        base_unit_of_measure_id: detail.base_unit_of_measure_id,
        item_category_id: detail.item_category_id,
      });
      set_is_modal_open(true);
    } catch (error) {
      message.error(error.message || 'Material could not be loaded for editing.');
    }
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
    modal.confirm({
      title: 'Vô hiệu hóa mặt hàng?',
      content: `Mặt hàng ${material.item_code} sẽ chuyển sang ngừng sử dụng.`,
      okText: 'Vô hiệu hóa',
      cancelText: 'Hủy',
      okButtonProps: { danger: true },
      onOk: async () => {
        const action_key = 'deactivate:' + material.stock_item_id;
        set_active_action_key(action_key);
        try {
          await request_api(`/api/v1/inventory/materials/${material.stock_item_id}`, { method: 'DELETE' });
          message.success('Material deactivated successfully.');
          await load_materials(filters, pagination.current, pagination.page_size);
        } catch (error) {
          message.error(error.message || 'Material could not be deactivated.');
        } finally {
          set_active_action_key((current) => current === action_key ? '' : current);
        }
      },
    });
  };

  const columns = [
    { title: 'Mã vật tư', dataIndex: 'item_code', key: 'item_code', fixed: 'left', width: 140 },
    { title: 'Tên vật tư', dataIndex: 'item_name', key: 'item_name', width: 240 },
    { title: 'Nhóm', dataIndex: 'category_name', key: 'category_name', render: (value) => value || 'Chưa phân nhóm' },
    { title: 'Đơn vị tính', dataIndex: 'unit_name', key: 'unit_name', render: (value, material) => value ? `${value} (${material.unit_code})` : 'Chưa có đơn vị' },
    { title: 'Theo dõi lô', dataIndex: 'lot_controlled', key: 'lot_controlled', render: (value) => value ? 'Có' : 'Không' },
    { title: 'Tồn tối thiểu', dataIndex: 'minimum_stock_quantity', key: 'minimum_stock_quantity', render: (value) => value ?? 'Chưa thiết lập' },
    { title: 'Trạng thái', dataIndex: 'status', key: 'status', render: (value) => <Tag color={value === 'active' ? 'green' : 'default'}>{status_labels[value] || value}</Tag> },
    { title: 'Thao tác', key: 'actions', fixed: 'right', width: 220, render: (_, material) => <RecordActionBar
      on_open={() => document.dispatchEvent(new CustomEvent('vinamik:data_workspace_open_record', { detail: { row_key: 'stock_item_id', record: material } }))}
      on_edit={() => open_edit(material)}
      can_edit={can_update}

      workflow_actions={can_deactivate && material.status === 'active' ? [{ key: 'deactivate', label: 'Ngừng sử dụng', loading: active_action_key === 'deactivate:' + material.stock_item_id, on_click: () => deactivate_material(material) }] : []}
    /> },
  ];

  return <AntdApp>
    <div className="module_page module_page_inventory">
      <ModuleMasthead
        module_key="inventory"
        description="Danh mục nguyên vật liệu là nguồn dùng chung; số dư chỉ được lấy từ sổ giao dịch kho."
        current_feature_label={active_view === 'materials' ? 'Danh mục nguyên vật liệu' : 'Số dư tồn kho'}
      />
    <Space className="inventory_view_switch" size="small">
      <Button type={active_view === 'materials' ? 'primary' : 'default'} onClick={() => set_active_view('materials')}>Danh mục vật tư</Button>
      <Button type={active_view === 'balances' ? 'primary' : 'default'} onClick={() => set_active_view('balances')}>Số dư tồn kho</Button>
    </Space>
    {error_message && <Alert className="workspace_error" type="error" showIcon message={error_message} />}
    {active_view === 'balances' ? <InventoryBalanceView current_user={current_user} /> : <DataWorkspace
      title="Danh sách nguyên vật liệu"
      description="Danh mục dùng chung cho Kho và Sản xuất. Dữ liệu được phân trang từ máy chủ để giữ thao tác mượt khi có nhiều bản ghi."
      toolbar={<Space wrap className="list_toolbar">
        <DebouncedSearchInput placeholder="Tìm theo mã hoặc tên vật tư" value={search_input} on_commit={on_material_search} style={{ width: 300 }} />
        <Select value={filters.status} options={status_options} onChange={(status) => { const next_filters = { ...filters, status }; set_filters(next_filters); sync_query(next_filters); load_materials(next_filters, 1, pagination.page_size); }} style={{ width: 170 }} />
        <Button icon={<FilterOutlined />} onClick={open_advanced_filters}>Bộ lọc</Button>
        {can_create && <Button type="primary" icon={<PlusOutlined />} onClick={open_create}>Thêm nguyên vật liệu</Button>}
      </Space>}
      columns={columns}
      data_source={materials}
      row_key="stock_item_id"
      loading={is_loading}
      pagination={pagination}
      on_change={(next_pagination) => { sync_query(filters, next_pagination.current); load_materials(filters, next_pagination.current, next_pagination.pageSize); }}
      empty_text={error_message ? 'Materials could not be loaded.' : 'Chưa có nguyên vật liệu'}
      total_label="mặt hàng"
      read_only={!can_create && !can_update && !can_deactivate}
      column_presets={{ overview: ['item_code', 'item_name', 'category_name', 'unit_name', 'status', 'actions'], detail: ['item_code', 'item_name', 'category_name', 'unit_name', 'lot_controlled', 'minimum_stock_quantity', 'status', 'actions'], audit: ['item_code', 'item_name', 'category_name', 'lot_controlled', 'minimum_stock_quantity', 'status'] }}
      storage_key={`data_workspace_inventory_${current_user?.username || 'account'}`}
    />}
    <Drawer
      title="Bộ lọc danh mục vật tư"
      open={filter_drawer_open}
      onClose={() => set_filter_drawer_open(false)}
      width={360}
      footer={<Space style={{ display: 'flex', justifyContent: 'flex-end' }}><Button onClick={reset_advanced_filters} icon={<ReloadOutlined />}>Đặt lại</Button><Button type="primary" onClick={apply_advanced_filters}>Áp dụng</Button></Space>}
    >
      <Typography.Paragraph type="secondary">Bộ lọc được gửi trực tiếp tới máy chủ, phù hợp với danh mục lớn.</Typography.Paragraph>
      <Typography.Text strong>Loại danh mục</Typography.Text>
      <Select
        style={{ width: '100%', marginTop: 8 }}
        value={draft_filters.item_type}
        options={item_type_options}
        onChange={(item_type) => set_draft_filters({ ...draft_filters, item_type })}
      />
    </Drawer>

    {active_view === 'materials' && <Modal className="entity_form_modal entity_form_modal_inventory" width={720} open={is_modal_open} title={<div className="modal_title_block"><span>DANH MỤC VẬT TƯ</span><strong>{editing_material ? 'Sửa nguyên vật liệu' : 'Thêm nguyên vật liệu'}</strong><small>Danh mục dùng chung cho tồn kho và sản xuất.</small></div>} onCancel={() => set_is_modal_open(false)} footer={<div className="modal_footer_actions"><Button onClick={() => set_is_modal_open(false)}>Hủy</Button><Button type="primary" htmlType="submit" form="inventory_material_form">Lưu mặt hàng</Button></div>} destroyOnClose>
      <Form id="inventory_material_form" form={form} layout="vertical" onFinish={on_finish} requiredMark={false}>
        <Form.Item label="Mã vật tư" name="item_code" rules={[{ required: true, message: 'Item code is required.' }]}><Input maxLength={60} /></Form.Item>
        <Form.Item label="Tên vật tư" name="item_name" rules={[{ required: true, message: 'Item name is required.' }]}><Input maxLength={180} /></Form.Item>
        <Form.Item label="Nhóm vật tư" name="item_category_id"><Select allowClear options={categories.map((item) => ({ value: item.category_id, label: item.category_name }))} /></Form.Item>
        <Form.Item label="Đơn vị tính cơ sở" name="base_unit_of_measure_id" rules={[{ required: true, message: 'Base unit is required.' }]}><Select options={units.map((item) => ({ value: item.unit_id, label: `${item.unit_name} (${item.unit_code})` }))} /></Form.Item>
        <Form.Item label="Theo dõi theo lô" name="lot_controlled" valuePropName="checked"><Checkbox>Có theo dõi lô và hạn dùng</Checkbox></Form.Item>
        <Form.Item label="Tồn tối thiểu" name="minimum_stock_quantity"><InputNumber min={0} precision={6} style={{ width: '100%' }} /></Form.Item>
        <Form.Item label="Trạng thái" name="status"><Select options={status_options.filter((item) => item.value)} /></Form.Item>
        <Form.Item label="Mô tả" name="description"><Input.TextArea maxLength={2000} rows={3} /></Form.Item>

      </Form>
    </Modal>}
    </div>
  </AntdApp>;
}

export default InventoryPage;
