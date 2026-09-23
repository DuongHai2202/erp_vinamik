
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Alert, App as antd_app, Button, Descriptions, Divider, Drawer, Form, Input, InputNumber, Modal, Space, Spin, Table, Tag, Tooltip, Typography } from 'antd';
import { DatabaseOutlined, DeleteOutlined, EditOutlined, PlusOutlined, SendOutlined, StopOutlined } from '@ant-design/icons';
import { has_permission } from '../common/permission_utils';
import { request_api } from '../common/api_client';
import { LookupField } from './workflow_fields';
import { format_date_vn, format_datetime_vn, format_field_value, format_vnd, to_iso_date, VietnameseDateInput } from '../common/formatters';

const AntdApp = antd_app;
const large_table_threshold = 40;

function table_render_props(row_count, horizontal_scroll, vertical_scroll = 360) {
  const is_large = Number(row_count || 0) > large_table_threshold;
  return {
    virtual: is_large,
    scroll: { x: horizontal_scroll, ...(is_large ? { y: vertical_scroll } : {}) },
  };
}

const status_labels = {
  draft: 'Bản nháp',
  pending: 'Chờ duyệt',
  submitted: 'Đã gửi',
  calculated: 'Đã tính',
  approved: 'Đã duyệt',
  released: 'Đã phát hành',
  in_progress: 'Đang thực hiện',
  completed: 'Đã hoàn thành',
  posted: 'Đã ghi sổ',
  rejected: 'Từ chối',
  cancelled: 'Đã hủy',
  active: 'Đang hoạt động',
  inactive: 'Ngừng hoạt động',
  counted: 'Đã kiểm đếm',
  locked: 'Đã khóa',
  paused: 'Tạm dừng',
  pending_receipt: 'Chờ nhập kho',
  failed: 'Không đạt',
};

const status_colors = {
  pending: 'gold',
  submitted: 'gold',
  calculated: 'blue',
  approved: 'green',
  released: 'blue',
  in_progress: 'processing',
  completed: 'green',
  posted: 'green',
  rejected: 'red',
  cancelled: 'default',
  active: 'green',
  inactive: 'default',
  counted: 'cyan',
  locked: 'purple',
  paused: 'orange',
  pending_receipt: 'gold',
  failed: 'red',
  draft: 'default',
};

function display_status(value) {
  return <Tag color={status_colors[value]}>{status_labels[value] || value || 'Chưa xác định'}</Tag>;
}

const empty_detail_labels = {
  posted_at: 'Chưa ghi sổ',
  submitted_at: 'Chưa gửi duyệt',
  approved_at: 'Chưa duyệt',
  effective_to: 'Không thời hạn',
  valid_to: 'Không thời hạn',
};

function display_value(value, field_key) {
  if (value === null || value === undefined || value === '') return empty_detail_labels[field_key] || 'Chưa cập nhật';
  if (typeof value === 'boolean') return value ? 'Có' : 'Không';
  if (typeof value === 'object') return JSON.stringify(value);
  const formatted = format_field_value(value, field_key);
  if (formatted !== value) return formatted;
  return typeof value === 'string' && value.includes('T') ? format_datetime_vn(value) : String(value);
}

const business_value_labels = {
  raw_material: 'Nguyên vật liệu',
  finished_product: 'Thành phẩm',
  kg: 'Kilôgam',
  litre: 'Lít',
  piece: 'Cái',
  reward: 'Khen thưởng',
  discipline: 'Kỷ luật',
  annual_leave: 'Nghỉ phép năm',
  sick_leave: 'Nghỉ ốm',
  personal_unpaid: 'Nghỉ không lương',
  maternity_leave: 'Nghỉ thai sản',
  family_leave: 'Nghỉ việc riêng',
  monthly_mon_sat_v1: 'Theo tháng, thứ Hai–thứ Bảy',
  weighted_average: 'Bình quân gia quyền',
  actual_hours: 'Giờ thực tế',
  good_quantity: 'Sản lượng đạt',
  exclude_from_good: 'Loại khỏi sản lượng đạt',
  appearance: 'Ngoại quan',
  seal_integrity: 'Độ kín bao bì',
  net_weight: 'Khối lượng tịnh',
  microbiology: 'Vi sinh',
  label_information: 'Thông tin nhãn',
  hold: 'Tạm giữ',
  rework: 'Gia công lại',
  scrap: 'Loại bỏ',
  return: 'Trả về',
};

function display_business_value(value, field_key) {
  if (typeof value === 'string' && business_value_labels[value]) return business_value_labels[value];
  return display_value(value, field_key);
}

function response_data(response) {
  return response?.data || null;
}

function DetailFields({ config, detail }) {
  const items = config.columns.filter(([key]) => detail?.[key] !== undefined);
  return <Descriptions bordered size="small" column={{ xs: 1, sm: 2 }} className="workflow_detail_descriptions">
    {items.map(([key, title]) => <Descriptions.Item key={key} label={title}>
      {key === 'status' ? display_status(detail[key]) : display_business_value(detail[key], key)}
    </Descriptions.Item>)}
  </Descriptions>;
}

function LinesTable({ lines, kind }) {
  if (!Array.isArray(lines) || !lines.length) return <Typography.Text type="secondary">Chưa có dòng chi tiết.</Typography.Text>;
  let columns;
  if (kind === 'stocktake') {
    columns = [
      { title: 'Mã vật tư', dataIndex: 'item_code', width: 140 },
      { title: 'Vị trí', dataIndex: 'location_code', width: 120 },
      { title: 'Theo sổ', dataIndex: 'system_quantity', width: 110 },
      { title: 'Đã đếm', dataIndex: 'counted_quantity', width: 110 },
      { title: 'Chênh lệch', dataIndex: 'difference_quantity', width: 110 },
    ];
  } else {
    const keys = Object.keys(lines[0]).filter((key) => !key.endsWith('_id') && key !== 'notes');
    columns = keys.slice(0, 8).map((key) => ({ title: key.replaceAll('_', ' '), dataIndex: key, render: (value) => display_business_value(value, key) }));
  }
  return <Table size="small" rowKey={(item, index) => String(item.stocktake_line_id || item.receipt_line_id || item.issue_line_id || item.transfer_line_id || item.bom_line_id || index)} dataSource={lines} columns={columns} pagination={false} {...table_render_props(lines.length, 620)} />;
}

function StocktakeLines({ detail, on_changed, current_user }) {
  const [count_values, set_count_values] = useState({});
  const [saving_id, set_saving_id] = useState(null);
  const { message } = AntdApp.useApp();
  const can_count = has_permission(current_user, 'inventory_stocktake_create') && detail?.status === 'counting';
  const save_count = async (line) => {
    const value = count_values[line.stocktake_line_id];
    if (value === undefined || value === null) return;
    set_saving_id(line.stocktake_line_id);
    try {
      await request_api('/api/v1/inventory/stocktakes/' + detail.stocktake_id + '/lines/' + line.stocktake_line_id + '/count', {
        method: 'POST',
        body: JSON.stringify({ counted_quantity: value }),
      });
      message.success('Stock count saved successfully.');
      on_changed();
    } catch (error) {
      message.error(error.message || 'The count could not be saved.');
    } finally {
      set_saving_id(null);
    }
  };
  const columns = [
    { title: 'Mã vật tư', dataIndex: 'item_code', width: 130 },
    { title: 'Tên vật tư', dataIndex: 'item_name', width: 180 },
    { title: 'Vị trí', dataIndex: 'location_code', width: 120 },
    { title: 'Theo sổ', dataIndex: 'system_quantity', width: 100 },
    { title: 'Số đếm', dataIndex: 'counted_quantity', width: 190, render: (value, line) => <div className="stocktake_count_editor"><InputNumber min={0} value={count_values[line.stocktake_line_id] ?? value ?? undefined} onChange={(next_value) => set_count_values((current) => ({ ...current, [line.stocktake_line_id]: next_value }))} disabled={!can_count} aria-label={'Số đếm ' + line.item_code} /><Button type="primary" size="small" loading={saving_id === line.stocktake_line_id} disabled={!can_count || count_values[line.stocktake_line_id] === undefined} onClick={() => save_count(line)}>Lưu</Button></div> },
    { title: 'Chênh lệch', dataIndex: 'difference_quantity', width: 110 },
  ];
  const lines = detail.lines || [];
  return <Table size="small" rowKey="stocktake_line_id" dataSource={lines} columns={columns} pagination={false} {...table_render_props(lines.length, 760)} />;
}

function PayrollRecords({ records, on_open_record }) {
  const columns = [
    { title: 'Mã nhân viên', dataIndex: 'employee_code', width: 120 },
    { title: 'Họ và tên', dataIndex: 'full_name', width: 190 },
    { title: 'Lương cơ bản', dataIndex: 'base_salary_snapshot', render: (value) => format_vnd(value) },
    { title: 'Nghỉ không lương', dataIndex: 'unpaid_leave_days', render: (value) => display_value(value) },
    { title: 'Thưởng', dataIndex: 'reward_amount', render: (value) => format_vnd(value) },
    { title: 'Kỷ luật', dataIndex: 'discipline_amount', render: (value) => format_vnd(value) },
    { title: 'Thực lĩnh', dataIndex: 'net_amount', render: (value) => format_vnd(value) },
    { title: 'Chi tiết', key: 'detail', fixed: 'right', render: (_, record) => <Button type="link" onClick={() => on_open_record(record)}>Xem dòng lương</Button> },
  ];
  return <Table size="small" rowKey="payroll_record_id" dataSource={records} columns={columns} pagination={{ pageSize: 20, showSizeChanger: false }} {...table_render_props(records.length, 980)} />;
}

const payroll_line_type_labels = {
  base_salary: 'Lương cơ bản',
  unpaid_leave: 'Nghỉ không lương',
  reward: 'Thưởng',
  discipline: 'Kỷ luật',
};

const payroll_source_type_labels = {
  employment_contract: 'Hợp đồng lao động',
  leave_request: 'Đơn nghỉ không lương',
  employee_reward_discipline: 'Khen thưởng / kỷ luật',
};

function payroll_line_label(line) {
  return payroll_line_type_labels[line?.line_type] || line?.line_label || display_value(line?.line_type);
}

function payroll_source_label(source_type) {
  return payroll_source_type_labels[source_type] || display_value(source_type);
}

function PayrollRecordModal({ record, open, on_close }) {
  const [detail, set_detail] = useState(null);
  const [loading, set_loading] = useState(false);
  useEffect(() => {
    let mounted = true;
    if (!open || !record) return () => { mounted = false; };
    set_loading(true);
    request_api('/api/v1/human_resources/payroll/records/' + record.payroll_record_id).then((response) => {
      if (mounted) set_detail(response_data(response));
    }).catch(() => {
      if (mounted) set_detail(null);
    }).finally(() => {
      if (mounted) set_loading(false);
    });
    return () => { mounted = false; };
  }, [open, record]);
  return <Modal className="workflow_detail_modal" open={open} title={record ? 'Chi tiết lương ' + record.employee_code : 'Chi tiết lương'} onCancel={on_close} footer={null} width={860} destroyOnClose>
    {loading && <div className="workflow_detail_loading"><Spin /></div>}
    {!loading && detail && <><Descriptions bordered size="small" column={{ xs: 1, sm: 2 }}>
      {Object.entries(detail.record || {}).filter(([key]) => !key.endsWith('_id')).map(([key, value]) => <Descriptions.Item key={key} label={key.replaceAll('_', ' ')}>{key === 'status' ? display_status(value) : display_value(value, key)}</Descriptions.Item>)}
    </Descriptions><Divider /><Typography.Title level={5}>Các dòng cấu thành lương</Typography.Title><Table size="small" rowKey="payroll_line_id" dataSource={detail.lines || []} pagination={false} columns={[
      { title: 'Loại', dataIndex: 'line_type', render: (value) => payroll_line_type_labels[value] || display_value(value) },
      { title: 'Nội dung', dataIndex: 'line_label', render: (_, line) => payroll_line_label(line) },
      { title: 'Số lượng', dataIndex: 'quantity', render: (value) => display_value(value) },
      { title: 'Đơn giá', dataIndex: 'unit_amount', render: (value) => format_vnd(value) },
      { title: 'Số tiền', dataIndex: 'amount', render: (value) => format_vnd(value) },
      { title: 'Nguồn', dataIndex: 'source_type', render: (value) => payroll_source_label(value) },
    ]} /></>}
  </Modal>;
}

function OutputFormModal({ order, output, open, on_close, on_saved }) {
  const [form] = Form.useForm();
  const { message } = AntdApp.useApp();
  const [saving, set_saving] = useState(false);
  const is_editing = Boolean(output);
  useEffect(() => {
    if (!open) return;
    form.resetFields();
    form.setFieldsValue(output ? {
      warehouse_id: output.warehouse_id,
      warehouse_location_id: output.warehouse_location_id,
      lot_code: output.lot_code,
      manufactured_on: output.manufactured_on ? format_date_vn(output.manufactured_on) : undefined,
      expires_on: output.expires_on ? format_date_vn(output.expires_on) : undefined,
      good_quantity: Number(output.good_quantity || 0),
      defective_quantity: Number(output.defective_quantity || 0),
      idempotency_key: output.idempotency_key,
      notes: output.notes,
    } : {
      manufactured_on: format_date_vn(new Date().toISOString().slice(0, 10)),
      good_quantity: 0,
      defective_quantity: 0,
      idempotency_key: order?.order_code + '-' + Date.now(),
    });
  }, [form, open, order, output]);
  const on_finish = async (values) => {
    const payload = { ...values, manufactured_on: to_iso_date(values.manufactured_on), expires_on: to_iso_date(values.expires_on) };
    set_saving(true);
    try {
      const path = '/api/v1/production/orders/' + order.production_order_id + '/outputs' + (is_editing ? '/' + output.production_output_id : '');
      await request_api(path, { method: is_editing ? 'PUT' : 'POST', body: JSON.stringify(payload) });
      message.success(is_editing ? 'Production output updated successfully.' : 'Production output created successfully.');
      on_saved();
      on_close();
    } catch (error) {
      message.error(error.message || 'The production output could not be saved.');
    } finally {
      set_saving(false);
    }
  };
  return <Modal className="entity_form_modal entity_form_modal_production_output" open={open} title={<div className="modal_title_block"><span>THÀNH PHẨM</span><strong>{is_editing ? 'Chỉnh sửa sản lượng thành phẩm' : 'Ghi nhận sản lượng thành phẩm'}</strong><small>Số lượng đạt sẽ được bàn giao sang Kho sau khi ghi sổ.</small></div>} onCancel={on_close} footer={<div className="modal_footer_actions"><Button onClick={on_close}>Hủy</Button><Button type="primary" htmlType="submit" form="production_output_form" loading={saving}>{is_editing ? 'Lưu thay đổi' : 'Lưu sản lượng'}</Button></div>} width={720} destroyOnClose>
    <Form id="production_output_form" form={form} layout="vertical" onFinish={on_finish} requiredMark={false}>
      <Form.Item label="Kho nhận" name="warehouse_id" rules={[{ required: true, message: 'Warehouse is required.' }]}><LookupField field={{ lookup: 'warehouses', required: true, placeholder: 'Chọn kho nhận' }} form={form} /></Form.Item>
      <Form.Item label="Vị trí nhận" name="warehouse_location_id" rules={[{ required: true, message: 'Warehouse location is required.' }]}><LookupField field={{ lookup: 'warehouse_locations', parent_field: 'warehouse_id', required: true, placeholder: 'Chọn vị trí nhận' }} form={form} /></Form.Item>
      <Space style={{ width: '100%' }} size="middle"><Form.Item label="Mã lô" name="lot_code" rules={[{ required: true, message: 'Lot code is required.' }]}><Input maxLength={80} /></Form.Item><Form.Item label="Ngày sản xuất" name="manufactured_on" rules={[{ required: true, message: 'Manufactured date is required.' }]}><VietnameseDateInput /></Form.Item><Form.Item label="Hạn dùng" name="expires_on"><VietnameseDateInput /></Form.Item></Space>
      <Space style={{ width: '100%' }} size="middle"><Form.Item label="Số lượng đạt" name="good_quantity" rules={[{ required: true, message: 'Good quantity is required.' }]}><InputNumber min={0} style={{ width: '100%' }} /></Form.Item><Form.Item label="Số lượng lỗi" name="defective_quantity" rules={[{ required: true, message: 'Defective quantity is required.' }]}><InputNumber min={0} style={{ width: '100%' }} /></Form.Item></Space>
      <Form.Item label="Mã chống gửi trùng" name="idempotency_key" rules={[{ required: true, message: 'Idempotency key is required.' }]}><Input maxLength={120} disabled={is_editing} /></Form.Item>
      <Form.Item label="Ghi chú" name="notes"><Input.TextArea rows={3} maxLength={2000} /></Form.Item>
    </Form>
  </Modal>;
}

function ConsumptionFormModal({ order, open, on_close, on_saved }) {
  const [form] = Form.useForm();
  const { message } = AntdApp.useApp();
  const [saving, set_saving] = useState(false);
  useEffect(() => {
    if (open) {
      form.resetFields();
      form.setFieldsValue({ idempotency_key: order?.order_code + '-consume-' + Date.now(), lines: [{}] });
    }
  }, [form, open, order]);
  const on_finish = async (values) => {
    set_saving(true);
    try {
      await request_api('/api/v1/production/orders/' + order.production_order_id + '/material-consumption', { method: 'POST', body: JSON.stringify(values) });
      message.success('Material consumption created successfully.');
      on_saved();
      on_close();
    } catch (error) {
      message.error(error.message || 'Material consumption could not be saved.');
    } finally {
      set_saving(false);
    }
  };
  return <Modal className="entity_form_modal entity_form_modal_production_consumption" open={open} title={<div className="modal_title_block"><span>TIÊU HAO VẬT TƯ</span><strong>Ghi nhận tiêu hao vật tư</strong><small>Kiểm tra kho và số lượng trước khi ghi nhận giao dịch.</small></div>} onCancel={on_close} footer={<div className="modal_footer_actions"><Button onClick={on_close}>Hủy</Button><Button type="primary" htmlType="submit" form="production_consumption_form" loading={saving}>Lưu tiêu hao</Button></div>} width={820} destroyOnClose>
    <Form id="production_consumption_form" form={form} layout="vertical" onFinish={on_finish} requiredMark={false}>
      <Form.Item label="Kho xuất" name="warehouse_id" rules={[{ required: true, message: 'Warehouse is required.' }]}><LookupField field={{ lookup: 'warehouses', required: true, placeholder: 'Chọn kho xuất' }} form={form} /></Form.Item>
      <Form.Item label="Mã chống gửi trùng" name="idempotency_key" rules={[{ required: true, message: 'Idempotency key is required.' }]}><Input maxLength={120} /></Form.Item>
      <Form.List name="lines" rules={[{ validator: async (_, values) => values?.length ? Promise.resolve() : Promise.reject(new Error('At least one line is required.')) }]}>
        {(fields, { add, remove }) => <div className="workflow_form_lines"><Typography.Text strong>Vật tư đã tiêu hao</Typography.Text>{fields.map(({ key, name, ...rest_field }) => <div className="workflow_form_line" key={key}>
          <Form.Item {...rest_field} name={[name, 'material_stock_item_id']} label="Nguyên vật liệu" rules={[{ required: true, message: 'Material is required.' }]}><LookupField field={{ lookup: 'raw_materials', required: true, placeholder: 'Chọn nguyên vật liệu' }} form={form} /></Form.Item>
          <Form.Item {...rest_field} name={[name, 'warehouse_location_id']} label="Vị trí kho" rules={[{ required: true, message: 'Warehouse location is required.' }]}><LookupField field={{ lookup: 'warehouse_locations', parent_field: 'warehouse_id', required: true, placeholder: 'Chọn vị trí' }} form={form} /></Form.Item>
          <Form.Item {...rest_field} name={[name, 'consumed_quantity']} label="Số lượng" rules={[{ required: true, message: 'Consumed quantity is required.' }]}><InputNumber min={0.000001} style={{ width: '100%' }} /></Form.Item>
          <Button type="text" danger icon={<StopOutlined />} aria-label="Xóa dòng" onClick={() => remove(name)} />
        </div>)}<Button type="dashed" block icon={<PlusOutlined />} onClick={() => add({})}>Thêm dòng vật tư</Button></div>}
      </Form.List>
      <Form.Item label="Ghi chú" name="notes"><Input.TextArea rows={3} maxLength={2000} /></Form.Item>

    </Form>
  </Modal>;
}

function WorkflowDetailDrawer({ config, record, open, on_close, current_user, on_changed }) {
  const [detail, set_detail] = useState(record);
  const [related, set_related] = useState({});
  const [loading, set_loading] = useState(false);
  const [related_loading, set_related_loading] = useState(false);
  const [detail_refreshing, set_detail_refreshing] = useState(false);
  const [output_open, set_output_open] = useState(false);
  const [editing_output, set_editing_output] = useState(null);
  const [consumption_open, set_consumption_open] = useState(false);
  const [payroll_record, set_payroll_record] = useState(null);
  const [payroll_record_open, set_payroll_record_open] = useState(false);
  const { message, modal } = AntdApp.useApp();

  const detail_request_sequence = useRef(0);

  const load_detail = useCallback(async () => {
    if (!record || !open) return;
    const sequence = detail_request_sequence.current + 1;
    detail_request_sequence.current = sequence;
    // Paint the drawer with the row that is already in memory. The complete detail
    // and related data are refreshed in the background so opening a record never
    // waits for several requests before the user gets feedback.
    set_detail(record);
    set_related({});
    set_loading(false);
    set_related_loading(false);
    set_detail_refreshing(true);
    try {
      const response = await request_api(config.endpoint + '/' + record[config.row_key]);
      if (sequence !== detail_request_sequence.current) return;
      const loaded_detail = response_data(response) || record;
      set_detail(loaded_detail);
      // Let React commit the primary detail frame before related requests start. This
      // prevents a fast local API response from batching all updates into one janky paint.
      await new Promise((resolve) => setTimeout(resolve, 0));
      if (sequence !== detail_request_sequence.current) return;
      const requests = [];
      if (config.detail_kind === 'payroll') {
        requests.push(request_api(config.endpoint + '/' + record[config.row_key] + '/records?page=0&page_size=200')
          .then((value) => ['payroll_records', response_data(value)?.items || []]));
      }
      if (['issue', 'transfer'].includes(config.detail_kind)
          && has_permission(current_user, 'inventory_material_read')) {
        const warehouse_id = loaded_detail.warehouse_id || loaded_detail.source_warehouse_id;
        if (warehouse_id) {
          requests.push(request_api('/api/v1/inventory/balances?warehouse_id=' + warehouse_id + '&page=0&page_size=200')
            .then((value) => ['balances', response_data(value)?.items || []]));
        }
      }
      if (config.detail_kind === 'production_order') {
        const can_read_order = has_permission(current_user, 'production_order_read');
        const can_read_output = has_permission(current_user, 'production_output_read');
        if (can_read_order || can_read_output) {
          requests.push(request_api('/api/v1/production/orders/' + record.production_order_id + '/progress')
            .then((value) => ['progress', response_data(value)]));
        }
        if (can_read_order) {
          requests.push(request_api('/api/v1/production/orders/' + record.production_order_id + '/material-needs')
            .then((value) => ['material_needs', response_data(value)]));
          requests.push(request_api('/api/v1/production/orders/' + record.production_order_id + '/material-consumption')
            .then((value) => ['consumption', response_data(value) || []]));
        }
        if (can_read_output) {
          requests.push(request_api('/api/v1/production/orders/' + record.production_order_id + '/outputs')
            .then((value) => ['outputs', response_data(value) || []]));
        }
      }
      set_related_loading(requests.length > 0);
      const entries = await Promise.allSettled(requests);
      if (sequence !== detail_request_sequence.current) return;
      set_related(Object.fromEntries(entries.filter((item) => item.status === 'fulfilled').map((item) => item.value)));
      set_related_loading(false);
    } catch (error) {
      if (sequence !== detail_request_sequence.current) return;
      message.error(error.message || 'The record detail could not be loaded.');
      set_detail(record);
      set_related_loading(false);
    } finally {
      if (sequence === detail_request_sequence.current) {
        set_loading(false);
        set_detail_refreshing(false);
      }
    }
  }, [config, current_user, message, open, record]);

  useEffect(() => {
    load_detail();
  }, [load_detail]);

  const refresh = useCallback(async () => {
    await load_detail();
    on_changed?.();
  }, [load_detail, on_changed]);

  const cancel_output = useCallback((output) => {
    modal.confirm({
      title: 'Hủy sản lượng?',
      content: 'Sản lượng chưa nhập kho sẽ chuyển sang trạng thái đã hủy.',
      okText: 'Hủy sản lượng', cancelText: 'Đóng', okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await request_api('/api/v1/production/orders/' + output.production_order_id + '/outputs/' + output.production_output_id + '/cancel', { method: 'POST' });
          message.success('Production output cancelled successfully.');
          refresh();
        } catch (error) {
          message.error(error.message || 'The production output could not be cancelled.');
        }
      },
    });
  }, [message, modal, refresh]);

  const delete_output = useCallback((output) => {
    modal.confirm({
      title: 'Xóa bản nháp sản lượng?',
      content: 'Bản nháp và dữ liệu dòng sẽ bị xóa vĩnh viễn.',
      okText: 'Xóa bản nháp', cancelText: 'Đóng', okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await request_api('/api/v1/production/orders/' + output.production_order_id + '/outputs/' + output.production_output_id, { method: 'DELETE' });
          message.success('Production output deleted successfully.');
          refresh();
        } catch (error) {
          message.error(error.message || 'The production output could not be deleted.');
        }
      },
    });
  }, [message, modal, refresh]);

  const post_output = useCallback((output) => {
    modal.confirm({
      title: 'Ghi sổ sản lượng?',
      content: 'Sản lượng đạt sẽ được bàn giao sang Kho và cập nhật tồn trung tâm.',
      okText: 'Ghi sổ',
      cancelText: 'Hủy',
      onOk: async () => {
        try {
          await request_api('/api/v1/production/orders/' + output.production_order_id + '/outputs/' + output.production_output_id + '/post', { method: 'POST' });
          message.success('Production output posted successfully.');
          refresh();
        } catch (error) {
          message.error(error.message || 'The production output could not be posted.');
        }
      },
    });
  }, [message, modal, refresh]);

  const sections = useMemo(() => {
    if (!detail) return null;
    return <><DetailFields config={config} detail={detail} />
      {config.detail_kind === 'payroll' && <><Divider /><Typography.Title level={5}>Bảng lương nhân viên</Typography.Title><PayrollRecords records={related.payroll_records || []} on_open_record={(item) => { set_payroll_record(item); set_payroll_record_open(true); }} /></>}
      {config.detail_kind === 'stocktake' && <><Divider /><Typography.Title level={5}>Dòng kiểm kê</Typography.Title><StocktakeLines detail={detail} on_changed={refresh} current_user={current_user} /></>}
      {config.detail_kind !== 'payroll' && config.detail_kind !== 'production_order' && detail.lines && <><Divider /><Typography.Title level={5}>Dòng chi tiết</Typography.Title><LinesTable lines={detail.lines} kind={config.detail_kind} /></>}
      {related.balances && <><Divider /><Typography.Title level={5}>Tồn khả dụng tại kho</Typography.Title><Table size="small" rowKey={(item, index) => item.stock_item_id + '-' + item.warehouse_location_id + '-' + (item.stock_lot_id || 'none') + '-' + index} dataSource={related.balances} pagination={{ pageSize: 15, showSizeChanger: false }} {...table_render_props(related.balances.length, 760, 320)} columns={[{ title: 'Mã vật tư', dataIndex: 'item_code' }, { title: 'Tên vật tư', dataIndex: 'item_name' }, { title: 'Vị trí', dataIndex: 'location_code' }, { title: 'Lô', dataIndex: 'lot_code' }, { title: 'Số dư', dataIndex: 'on_hand_quantity' }, { title: 'Đơn vị', dataIndex: 'unit_code' }]} /></>}
      {config.detail_kind === 'production_order' && <><Divider /><Typography.Title level={5}>Tiến độ lệnh</Typography.Title>{related.progress ? <div className="production_progress_cards"><div><span>Đã đạt</span><strong>{display_value(related.progress.actual_good_quantity)}</strong></div><div><span>Hàng lỗi</span><strong>{display_value(related.progress.actual_defective_quantity)}</strong></div><div><span>Hoàn thành</span><strong>{display_value(related.progress.completion_percent)}%</strong></div><div><span>Còn lại</span><strong>{display_value(related.progress.remaining_quantity)}</strong></div></div> : <Typography.Text type="secondary">Chưa có dữ liệu tiến độ.</Typography.Text>}
        {related.material_needs && <><Typography.Title level={5}>Nhu cầu vật tư thực tế</Typography.Title><Table size="small" rowKey="material_stock_item_id" dataSource={related.material_needs.items || []} pagination={false} scroll={{ x: 700 }} columns={[{ title: 'Mã vật tư', dataIndex: 'material_item_code' }, { title: 'Tên vật tư', dataIndex: 'material_item_name' }, { title: 'Đơn vị', dataIndex: 'unit_code' }, { title: 'Cần dùng', dataIndex: 'required_quantity' }, { title: 'Khả dụng', dataIndex: 'available_quantity' }, { title: 'Thiếu', dataIndex: 'shortage_quantity', render: (value) => <Tag color={Number(value) > 0 ? 'red' : 'green'}>{display_value(value)}</Tag> }]} /></>}        {related.progress?.events?.length ? <><Typography.Title level={5}>Lịch sử tiến độ</Typography.Title><Table size="small" rowKey="production_order_event_id" dataSource={related.progress.events} pagination={false} columns={[{ title: 'Sự kiện', dataIndex: 'event_type' }, { title: 'Trạng thái trước', dataIndex: 'previous_status', render: (value) => status_labels[value] || value || 'Chưa cập nhật' }, { title: 'Trạng thái sau', dataIndex: 'new_status', render: (value) => status_labels[value] || value || 'Chưa cập nhật' }, { title: 'Thời điểm', dataIndex: 'occurred_at', render: (value) => format_datetime_vn(value) }]} /></> : null}        {detail.material_requirements && <><Typography.Title level={5}>Nhu cầu vật tư theo BOM</Typography.Title><LinesTable lines={detail.material_requirements} kind="material_requirements" /></>}
        <Space wrap className="workflow_detail_actions">{has_permission(current_user, 'production_output_create') && ['released', 'in_progress', 'paused', 'completed'].includes(detail.status) && <Button type="primary" icon={<PlusOutlined />} onClick={() => { set_editing_output(null); set_output_open(true); }}>Ghi nhận sản lượng</Button>}{has_permission(current_user, 'production_order_update') && ['released', 'in_progress', 'paused'].includes(detail.status) && <Button icon={<DatabaseOutlined />} onClick={() => set_consumption_open(true)}>Ghi nhận tiêu hao</Button>}</Space>
        {related.outputs && <><Typography.Title level={5}>Sản lượng đã ghi nhận</Typography.Title><Table size="small" rowKey="production_output_id" dataSource={related.outputs} pagination={false} {...table_render_props(related.outputs.length, 850, 320)} columns={[
          { title: 'Mã lô', dataIndex: 'lot_code' },
          { title: 'Ngày sản xuất', dataIndex: 'manufactured_on' },
          { title: 'Đạt', dataIndex: 'good_quantity' },
          { title: 'Lỗi', dataIndex: 'defective_quantity' },
          { title: 'Trạng thái', dataIndex: 'status', render: (value) => display_status(value) },
          { title: 'Thao tác', key: 'actions', render: (_, item) => <Space size="small" className="record_action_bar">{has_permission(current_user, 'production_output_update') && item.status === 'draft' && <Tooltip title="Chỉnh sửa"><span className="record_action_tooltip_target"><Button type="text" className="record_action_button record_action_icon" icon={<EditOutlined />} aria-label="Chỉnh sửa sản lượng" onClick={() => { set_editing_output(item); set_output_open(true); }} /></span></Tooltip>}{has_permission(current_user, 'production_output_delete') && item.status === 'draft' && <Tooltip title="Xóa"><span className="record_action_tooltip_target"><Button danger type="text" className="record_action_button record_action_icon" icon={<DeleteOutlined />} aria-label="Xóa sản lượng" onClick={() => delete_output(item)} /></span></Tooltip>}{has_permission(current_user, 'production_output_update') && ['draft', 'pending_receipt'].includes(item.status) && <Tooltip title="Hủy"><span className="record_action_tooltip_target"><Button danger type="text" className="record_action_button record_action_icon" icon={<StopOutlined />} aria-label="Hủy sản lượng" onClick={() => cancel_output(item)} /></span></Tooltip>}{has_permission(current_user, 'production_output_post') && ['draft', 'pending_receipt'].includes(item.status) && <Tooltip title="Ghi sổ"><span className="record_action_tooltip_target"><Button type="text" className="record_action_button record_action_icon" icon={<SendOutlined />} aria-label="Ghi sổ" onClick={() => post_output(item)} /></span></Tooltip>}</Space> },
        ]} /></>}
        {related.consumption && <><Typography.Title level={5}>Lịch sử tiêu hao vật tư</Typography.Title><Table size="small" rowKey="idempotency_key" dataSource={related.consumption} pagination={false} scroll={{ x: 780 }} columns={[
          { title: 'Mã giao dịch', dataIndex: 'idempotency_key' },
          { title: 'Số dòng', key: 'line_count', render: (_, item) => item.lines?.length || 0 },
          { title: 'Vật tư', key: 'items', render: (_, item) => item.lines?.map((line) => line.material_item_code).join(', ') || 'Chưa có vật tư' },
        ]} /></>}
      </>}
    </>;
  }, [cancel_output, config, current_user, delete_output, detail, post_output, related, refresh]);
  return <><Drawer className="workflow_detail_drawer" title={detail ? 'Chi tiết ' + display_value(detail[config.columns[0][0]]) : 'Chi tiết bản ghi'} open={open} onClose={on_close} width={Math.min(980, window.innerWidth - 24)} destroyOnClose>
    {loading && <div className="workflow_detail_loading"><Spin /></div>}
    {!loading && detail_refreshing && <div className="workflow_detail_refreshing"><Spin size="small" /><Typography.Text type="secondary">Đang cập nhật chi tiết…</Typography.Text></div>}
    {!loading && sections}
    {!loading && related_loading && <div className="workflow_detail_related_loading"><Spin size="small" /><Typography.Text type="secondary">Đang tải dữ liệu liên quan…</Typography.Text></div>}
    {!loading && !detail && <Alert type="warning" message="Không có dữ liệu chi tiết." />}
  </Drawer>
  <PayrollRecordModal record={payroll_record} open={payroll_record_open} on_close={() => set_payroll_record_open(false)} />
  {detail && <OutputFormModal order={detail} output={editing_output} open={output_open} on_close={() => { set_output_open(false); set_editing_output(null); }} on_saved={refresh} />}
  {detail && <ConsumptionFormModal order={detail} open={consumption_open} on_close={() => set_consumption_open(false)} on_saved={refresh} />}
  </>;
}

export default WorkflowDetailDrawer;
