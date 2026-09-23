import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Alert, App as antd_app, Button, Drawer, Form, Input, InputNumber, Modal, Select, Space, Spin, Table, Tag, Typography } from 'antd';
import { FilterOutlined, MinusCircleOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { has_permission } from '../../platform/common/permission_utils';
import { request_api } from '../../platform/common/api_client';
import { use_auth } from '../../platform/identity/auth_context';
import DataWorkspace from '../../platform/layout/data_workspace';
import RecordActionBar from '../../platform/layout/record_action_bar';
import ModuleMasthead from '../../platform/layout/module_masthead';
import DebouncedSearchInput from '../../platform/layout/debounced_search_input';
import { format_date_vn, to_iso_date, VietnameseDateInput } from '../../platform/common/formatters';

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
  const [search_input, set_search_input] = useState(() => search_params.get('search') || '');
  const [pagination, set_pagination] = useState(() => ({ current: Math.max(Number(search_params.get('page')) || 1, 1), page_size: 50, total: 0 }));
  const [is_form_open, set_is_form_open] = useState(false);
  const [is_detail_open, set_is_detail_open] = useState(false);
  const [is_detail_loading, set_is_detail_loading] = useState(false);
  const [selected_plan, set_selected_plan] = useState(null);
  const [editing_plan, set_editing_plan] = useState(null);
  const [filter_drawer_open, set_filter_drawer_open] = useState(false);
  const [draft_filters, set_draft_filters] = useState({ status: search_params.get('status') || '' });
  const [form] = Form.useForm();
  const plans_request_ref = useRef(null);
  const stock_items_request_ref = useRef(null);
  const detail_request_ref = useRef(null);
  const detail_request_sequence = useRef(0);
  const [active_action_key, set_active_action_key] = useState('');

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

  const on_plan_search = (next_search = search_input) => {
    const next_filters = { ...filters, search: next_search };
    set_search_input(next_search);
    set_filters(next_filters);
    sync_query(next_filters, 1);
    load_plans(next_filters, 1, pagination.page_size);
  };
  const open_advanced_filters = () => {
    set_draft_filters({ status: filters.status || '' });
    set_filter_drawer_open(true);
  };

  const apply_advanced_filters = () => {
    const next_filters = { ...filters, ...draft_filters };
    set_filters(next_filters);
    sync_query(next_filters, 1);
    load_plans(next_filters, 1, pagination.page_size);
    set_filter_drawer_open(false);
  };

  const reset_advanced_filters = () => {
    const next_filters = { ...filters, status: '' };
    set_draft_filters({ status: '' });
    set_filters(next_filters);
    sync_query(next_filters, 1);
    load_plans(next_filters, 1, pagination.page_size);
    set_filter_drawer_open(false);
  };

  const load_plans = async (next_filters = filters, next_page = pagination.current, next_page_size = pagination.page_size) => {
    plans_request_ref.current?.abort();
    const controller = new AbortController();
    plans_request_ref.current = controller;
    set_is_loading(true);
    set_error_message('');
    const params = new URLSearchParams({ page: String(next_page - 1), page_size: String(next_page_size) });
    if (next_filters.search.trim()) params.set('search', next_filters.search.trim());
    if (next_filters.status) params.set('status', next_filters.status);
    try {
      const response = await request_api('/api/v1/production/plans?' + params.toString(), { signal: controller.signal });
      if (controller.signal.aborted) return;
      set_plans(response.data.items);
      set_pagination({ current: response.data.page + 1, page_size: response.data.page_size, total: response.data.total_items });
    } catch (error) {
      if (controller.signal.aborted || error?.name === 'AbortError') return;
      set_plans([]);
      set_error_message(error.message || 'Production plans could not be loaded.');
    } finally {
      if (plans_request_ref.current === controller) {
        plans_request_ref.current = null;
        set_is_loading(false);
      }
    }
  };

  useEffect(() => {
    const controller = new AbortController();
    stock_items_request_ref.current = controller;
    request_api('/api/v1/inventory/materials?item_type=finished_product&page_size=100', { signal: controller.signal })
      .then((response) => { if (!controller.signal.aborted) set_stock_items(response.data.items); })
      .catch((error) => { if (!controller.signal.aborted && error?.name !== 'AbortError') { set_stock_items([]); set_error_message(error.message || 'Finished products could not be loaded.'); } });
    load_plans();
    // The initial requests intentionally run once with the initial filter state.
    return () => {
      controller.abort();
      plans_request_ref.current?.abort();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => () => {
    plans_request_ref.current?.abort();
    stock_items_request_ref.current?.abort();
    detail_request_sequence.current += 1;
    detail_request_ref.current?.abort();
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
      form.setFieldsValue({ ...response.data, planned_on: response.data.planned_on ? format_date_vn(response.data.planned_on) : undefined, starts_on: response.data.starts_on ? format_date_vn(response.data.starts_on) : undefined, ends_on: response.data.ends_on ? format_date_vn(response.data.ends_on) : undefined, lines: response.data.lines.map((line) => ({ stock_item_id: line.stock_item_id, target_quantity: line.target_quantity, required_on: line.required_on ? format_date_vn(line.required_on) : undefined, notes: line.notes })) });
      set_is_form_open(true);
    } catch (error) {
      message.error(error.message || 'Production plan could not be loaded.');
    }
  };

  const close_detail = () => {
    detail_request_sequence.current += 1;
    detail_request_ref.current?.abort();
    detail_request_ref.current = null;
    set_is_detail_loading(false);
    set_is_detail_open(false);
    set_selected_plan(null);
  };

  const open_detail = async (plan) => {
    detail_request_ref.current?.abort();
    const controller = new AbortController();
    const sequence = detail_request_sequence.current + 1;
    detail_request_sequence.current = sequence;
    detail_request_ref.current = controller;
    set_selected_plan(plan);
    set_is_detail_loading(true);
    set_is_detail_open(true);
    try {
      const response = await request_api('/api/v1/production/plans/' + plan.production_plan_id, { signal: controller.signal });
      if (controller.signal.aborted || sequence !== detail_request_sequence.current) return;
      set_selected_plan(response.data);
    } catch (error) {
      if (controller.signal.aborted || error?.name === 'AbortError' || sequence !== detail_request_sequence.current) return;
      message.error(error.message || 'Production plan could not be loaded.');
      close_detail();
    } finally {
      if (detail_request_ref.current === controller) {
        detail_request_ref.current = null;
        set_is_detail_loading(false);
      }
    }
  };

  const on_finish = async (values) => {
    const payload = { ...values, planned_on: to_iso_date(values.planned_on), starts_on: to_iso_date(values.starts_on), ends_on: to_iso_date(values.ends_on), lines: values.lines.map((line) => ({ ...line, required_on: to_iso_date(line.required_on), notes: line.notes || null })) };
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
        const action_key = 'transition:' + plan.production_plan_id;
        set_active_action_key(action_key);
        try {
          await request_api(`/api/v1/production/plans/${plan.production_plan_id}/status`, { method: 'POST', body: JSON.stringify({ status: target }) });
          message.success('Production plan status changed successfully.');
          await load_plans(filters, pagination.current, pagination.page_size);
        } catch (error) {
          message.error(error.message || 'Production plan status could not be changed.');
        } finally {
          set_active_action_key((current) => current === action_key ? '' : current);
        }
      },
    });
  };

  const cancel_plan = (plan) => {
    modal.confirm({
      title: 'Hủy kế hoạch?',
      content: `Kế hoạch ${plan.plan_code} sẽ chuyển sang trạng thái đã hủy.`,
      okText: 'Hủy kế hoạch',
      cancelText: 'Đóng',
      okButtonProps: { danger: true },
      onOk: async () => {
        const action_key = 'cancel:' + plan.production_plan_id;
        set_active_action_key(action_key);
        try {
          await request_api(`/api/v1/production/plans/${plan.production_plan_id}/status`, { method: 'POST', body: JSON.stringify({ status: 'cancelled' }) });
          message.success('Production plan cancelled successfully.');
          await load_plans(filters, pagination.current, pagination.page_size);
        } catch (error) {
          message.error(error.message || 'Production plan could not be cancelled.');
        } finally {
          set_active_action_key((current) => current === action_key ? '' : current);
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
        const action_key = 'delete:' + plan.production_plan_id;
        set_active_action_key(action_key);
        try {
          await request_api(`/api/v1/production/plans/${plan.production_plan_id}`, { method: 'DELETE' });
          message.success('Production plan deleted successfully.');
          await load_plans(filters, pagination.current, pagination.page_size);
        } catch (error) {
          message.error(error.message || 'Production plan could not be deleted.');
        } finally {
          set_active_action_key((current) => current === action_key ? '' : current);
        }
      },
    });
  };

  const columns = [
    { title: 'Mã kế hoạch', dataIndex: 'plan_code', key: 'plan_code', fixed: 'left', width: 150 },
    { title: 'Tên kế hoạch', dataIndex: 'plan_name', key: 'plan_name', width: 230 },
    { title: 'Ngày bắt đầu', dataIndex: 'starts_on', key: 'starts_on', render: (value) => value ? format_date_vn(value) : 'Chưa lập' },
    { title: 'Ngày kết thúc', dataIndex: 'ends_on', key: 'ends_on', render: (value) => value ? format_date_vn(value) : 'Chưa lập' },
    { title: 'Số dòng', dataIndex: 'line_count', key: 'line_count' },
    { title: 'Trạng thái', dataIndex: 'status', key: 'status', render: (value) => <Tag color={value === 'released' ? 'blue' : value === 'completed' ? 'green' : 'default'}>{plan_status_labels[value] || value}</Tag> },
    { title: 'Thao tác', key: 'actions', fixed: 'right', width: 220, render: (_, plan) => {
      const row_actions = [];
      if (can_approve && next_status[plan.status]) row_actions.push({ key: 'transition', label: 'Chuyển trạng thái', loading: active_action_key === 'transition:' + plan.production_plan_id, on_click: () => change_plan_status(plan) });
      if (can_approve && ['draft', 'approved', 'released'].includes(plan.status)) row_actions.push({ key: 'cancel', label: 'Hủy kế hoạch', loading: active_action_key === 'cancel:' + plan.production_plan_id, on_click: () => cancel_plan(plan) });
      if (can_delete && plan.status === 'draft') row_actions.push({ key: 'delete', label: 'Xóa kế hoạch', loading: active_action_key === 'delete:' + plan.production_plan_id, on_click: () => delete_plan(plan) });
      return <RecordActionBar
        on_open={() => open_detail(plan)}
        on_edit={() => open_edit(plan)}
        can_edit={can_update}
        edit_status_allowed={plan.status === 'draft'}
        workflow_actions={row_actions}
      />;
    } },
  ];

  return <AntdApp>
    <div className="module_page module_page_production">
      <ModuleMasthead
        module_key="production"
        description="Lập kế hoạch nhiều dòng theo dữ liệu thành phẩm do Kho cung cấp; kế hoạch là căn cứ phát hành lệnh sản xuất."
        current_feature_label="Kế hoạch sản xuất"
      />
      <div className="production_stage_rail" aria-label="Các giai đoạn sản xuất">
        <span className="is_active">Lập kế hoạch</span><i /><span>Phát hành lệnh</span><i /><span>Thực hiện</span><i /><span>Hoàn tất</span>
      </div>
    {error_message && <Alert className="workspace_error" type="error" showIcon message={error_message} />}
    <DataWorkspace
      title="Danh sách kế hoạch sản xuất"
      description="Theo dõi kế hoạch theo trạng thái, thời gian và số dòng sản phẩm. Nhấp đúp để xem nhanh dữ liệu kế hoạch."
      toolbar={<Space wrap className="list_toolbar">
        <DebouncedSearchInput placeholder="Tìm theo mã hoặc tên kế hoạch" value={search_input} on_commit={on_plan_search} style={{ width: 300 }} />
        <Button icon={<FilterOutlined />} onClick={open_advanced_filters}>Bộ lọc</Button>
        {can_create && <Button type="primary" icon={<PlusOutlined />} onClick={open_create}>Thêm kế hoạch</Button>}
      </Space>}
      columns={columns}
      data_source={plans}
      row_key="production_plan_id"
      loading={is_loading}
      pagination={pagination}
      on_change={(next_pagination) => { sync_query(filters, next_pagination.current); load_plans(filters, next_pagination.current, next_pagination.pageSize); }}
      empty_text={error_message ? 'Production plans could not be loaded.' : 'Chưa có kế hoạch sản xuất'}
      total_label="kế hoạch"
      read_only={!can_create && !can_update && !can_approve && !can_delete}
      column_presets={{ overview: ['plan_code', 'plan_name', 'starts_on', 'ends_on', 'status', 'actions'], detail: ['plan_code', 'plan_name', 'starts_on', 'ends_on', 'line_count', 'status', 'actions'], audit: ['plan_code', 'plan_name', 'planned_on', 'starts_on', 'ends_on', 'status'] }}
      storage_key={`data_workspace_production_${current_user?.username || 'account'}`}
    />
    <Drawer
      title="Bộ lọc kế hoạch sản xuất"
      open={filter_drawer_open}
      onClose={() => set_filter_drawer_open(false)}
      width={360}
      footer={<Space style={{ display: 'flex', justifyContent: 'flex-end' }}><Button onClick={reset_advanced_filters} icon={<ReloadOutlined />}>Đặt lại</Button><Button type="primary" onClick={apply_advanced_filters}>Áp dụng</Button></Space>}
    >
      <Typography.Paragraph type="secondary">Lọc theo trạng thái nghiệp vụ của kế hoạch; truy vấn được gửi trực tiếp tới máy chủ.</Typography.Paragraph>
      <Typography.Text strong>Trạng thái kế hoạch</Typography.Text>
      <Select
        style={{ width: '100%', marginTop: 8 }}
        value={draft_filters.status || undefined}
        allowClear
        placeholder="Tất cả trạng thái"
        options={plan_status_options.filter((item) => item.value)}
        onChange={(status) => set_draft_filters({ ...draft_filters, status: status || '' })}
      />
    </Drawer>

    <Modal className="entity_form_modal entity_form_modal_production" open={is_form_open} title={<div className="modal_title_block"><span>ĐIỀU ĐỘ SẢN XUẤT</span><strong>{editing_plan ? 'Sửa kế hoạch sản xuất' : 'Thêm kế hoạch sản xuất'}</strong><small>Kế hoạch là đầu vào cho lệnh sản xuất và nhu cầu vật tư.</small></div>} onCancel={() => set_is_form_open(false)} footer={<div className="modal_footer_actions"><Button onClick={() => set_is_form_open(false)}>Hủy</Button><Button type="primary" htmlType="submit" form="production_plan_form">Lưu kế hoạch</Button></div>} width={760} destroyOnClose>
      <Form id="production_plan_form" form={form} layout="vertical" onFinish={on_finish} requiredMark={false}>
        <Space size="middle" style={{ display: 'flex' }}>
          <Form.Item label="Mã kế hoạch" name="plan_code" rules={[{ required: true, message: 'Plan code is required.' }]} style={{ flex: 1 }}><Input maxLength={60} /></Form.Item>
          <Form.Item label="Tên kế hoạch" name="plan_name" rules={[{ required: true, message: 'Plan name is required.' }]} style={{ flex: 2 }}><Input maxLength={180} /></Form.Item>
        </Space>
        <Space size="middle" style={{ display: 'flex' }}>
          <Form.Item label="Ngày lập" name="planned_on" rules={[{ required: true, message: 'Planned date is required.' }]} style={{ flex: 1 }}><VietnameseDateInput /></Form.Item>
          <Form.Item label="Ngày bắt đầu" name="starts_on" rules={[{ required: true, message: 'Start date is required.' }]} style={{ flex: 1 }}><VietnameseDateInput /></Form.Item>
          <Form.Item label="Ngày kết thúc" name="ends_on" rules={[{ required: true, message: 'End date is required.' }]} style={{ flex: 1 }}><VietnameseDateInput /></Form.Item>
        </Space>
        <Typography.Title level={5}>Sản phẩm kế hoạch</Typography.Title>
        <Form.List name="lines">
          {(fields, { add, remove }) => <>
            {fields.map(({ key, name, ...rest_field }) => <Space key={key} align="baseline" style={{ display: 'flex' }}>
              <Form.Item {...rest_field} name={[name, 'stock_item_id']} rules={[{ required: true, message: 'Stock item is required.' }]}><Select placeholder="Chọn thành phẩm" options={stock_items.map((item) => ({ value: item.stock_item_id, label: `${item.item_code} — ${item.item_name}` }))} style={{ width: 320 }} /></Form.Item>
              <Form.Item {...rest_field} name={[name, 'target_quantity']} rules={[{ required: true, message: 'Target quantity is required.' }]}><InputNumber min={0.000001} placeholder="Số lượng" style={{ width: 150 }} /></Form.Item>
              <Form.Item {...rest_field} name={[name, 'required_on']}><VietnameseDateInput placeholder="dd/MM/yyyy" /></Form.Item>
              <Button type="text" danger icon={<MinusCircleOutlined />} aria-label="Xóa dòng" onClick={() => remove(name)} />
            </Space>)}
            <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>Thêm dòng sản phẩm</Button>
          </>}
        </Form.List>
        <Form.Item label="Ghi chú" name="notes"><Input.TextArea maxLength={2000} rows={3} /></Form.Item>

      </Form>
    </Modal>
    <Modal open={is_detail_open} title={selected_plan ? 'Chi tiết ' + selected_plan.plan_code : 'Chi tiết kế hoạch'} onCancel={close_detail} footer={null} width={820} destroyOnClose>
      {is_detail_loading && <div className="workflow_detail_loading"><Spin tip="Đang tải chi tiết kế hoạch..." /></div>}
      {!is_detail_loading && selected_plan && <Space direction="vertical" style={{ width: '100%' }}>
        <Typography.Paragraph>{selected_plan.plan_name} · {plan_status_labels[selected_plan.status] || selected_plan.status}</Typography.Paragraph>
        <Table rowKey="production_plan_line_id" size="small" pagination={false} dataSource={selected_plan.lines || []} columns={[{ title: 'Mã vật tư/thành phẩm', dataIndex: 'stock_item_code' }, { title: 'Tên', dataIndex: 'stock_item_name' }, { title: 'Đơn vị', dataIndex: 'unit_code' }, { title: 'Số lượng', dataIndex: 'target_quantity' }, { title: 'Hạn cần', dataIndex: 'required_on', render: (value) => value ? format_date_vn(value) : 'Chưa lập' }]} />
      </Space>}
    </Modal>
    </div>
  </AntdApp>;
}

export default ProductionPage;
