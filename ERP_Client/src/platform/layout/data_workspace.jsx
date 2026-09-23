import { useEffect, useMemo, useState } from 'react';
import { Button, Card, Checkbox, Descriptions, Drawer, Empty, Input, Pagination, Select, Space, Table, Typography } from 'antd';
import { CheckOutlined, LockOutlined, ReloadOutlined, SlidersOutlined, TableOutlined } from '@ant-design/icons';
import { format_datetime_vn, format_field_value } from '../common/formatters';

const preset_labels = {
  overview: 'Tổng quan',
  detail: 'Chi tiết',
  audit: 'Kiểm tra',
};

const preset_storage_version = '3';

function load_preset(storage_key, available) {
  const default_preset = available.includes('detail') ? 'detail' : available[0];
  try {
    const stored = window.localStorage.getItem(storage_key);
    // Reset preferences written by earlier builds once; Detail is the default after this migration.
    const storage_version = window.localStorage.getItem(`${storage_key}:version`);
    if (storage_version !== preset_storage_version) return default_preset;
    const explicitly_selected = window.localStorage.getItem(`${storage_key}:preset_explicit`) === '1';
    return explicitly_selected && available.includes(stored) ? stored : default_preset;
  } catch {
    return default_preset;
  }
}

function load_column_keys(storage_key, available) {
  try {
    // Custom columns are only a user preference when the current storage
    // record explicitly says so. Older builds persisted columns without that
    // marker, which could silently override the new default Detail preset.
    const storage_version = window.localStorage.getItem(`${storage_key}:version`);
    if (storage_version !== preset_storage_version) return null;
    const explicitly_selected = window.localStorage.getItem(`${storage_key}:preset_explicit`) === '1';
    const stored_preset = window.localStorage.getItem(storage_key);
    if (!explicitly_selected || stored_preset !== 'custom') return null;
    const stored = JSON.parse(window.localStorage.getItem(`${storage_key}:columns`) || 'null');
    if (!Array.isArray(stored)) return null;
    const filtered = stored.filter((key) => available.includes(key));
    return filtered.length ? filtered : null;
  } catch {
    return null;
  }
}

function read_field(record, data_index) {
  if (Array.isArray(data_index)) return data_index.reduce((value, key) => value?.[key], record);
  return record?.[data_index];
}

const empty_field_labels = {
  posted_at: 'Chưa ghi sổ',
  submitted_at: 'Chưa gửi duyệt',
  approved_at: 'Chưa duyệt',
  effective_to: 'Không thời hạn',
  valid_to: 'Không thời hạn',
};

function empty_field_value(field_key) {
  return empty_field_labels[field_key] || 'Chưa cập nhật';
}

function display_value(value, field_key) {
  if (value === null || value === undefined || value === '') return empty_field_value(field_key);
  if (typeof value === 'boolean') return value ? 'Có' : 'Không';
  if (typeof value === 'object') return JSON.stringify(value);
  const formatted = format_field_value(value, field_key);
  if (formatted !== value) return formatted;
  if (typeof value === 'string' && value.includes('T')) return format_datetime_vn(value);
  return String(value);
}

function DataWorkspace({
  module_key = 'shared',
  title,
  description,
  toolbar,
  columns,
  data_source,
  row_key,
  loading,
  pagination,
  on_change,
  empty_text = 'Chưa có dữ liệu',
  total_label = 'bản ghi',
  read_only = false,
  column_presets,
  storage_key = 'vinamik_data_workspace_preset',
  on_open_record,
}) {
  const all_columns = useMemo(() => columns || [], [columns]);
  const available_presets = Object.keys(column_presets || {});
  const available_preset_signature = available_presets.join('|');
  const column_keys = useMemo(() => all_columns.map((column) => column.key).filter(Boolean), [all_columns]);
  const column_key_signature = column_keys.join('|');
  const [preset, set_preset] = useState(() => load_preset(storage_key, available_presets));
  const [custom_column_keys, set_custom_column_keys] = useState(() => load_column_keys(storage_key, column_keys));
  const has_detail_preset = available_presets.includes('detail');
  useEffect(() => {
    // DataWorkspace is reused while navigating between features. Re-read the
    // feature-specific preference so a previous table cannot leak its layout
    // into the next one; unversioned/old records resolve to Detail.
    set_preset(load_preset(storage_key, available_presets));
    set_custom_column_keys(load_column_keys(storage_key, column_keys));
    // Signatures are stable strings, unlike the freshly-created arrays above.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [storage_key, available_preset_signature, column_key_signature]);
  useEffect(() => {
    if (!has_detail_preset || custom_column_keys?.length || preset === 'detail') return;
    try {
      const explicitly_selected = window.localStorage.getItem(`${storage_key}:preset_explicit`) === '1';
      if (!explicitly_selected) set_preset('detail');
    } catch {
      set_preset('detail');
    }
  }, [custom_column_keys, has_detail_preset, preset, storage_key]);
  const [column_drawer_open, set_column_drawer_open] = useState(false);
  const [column_search, set_column_search] = useState('');
  const [draft_column_keys, set_draft_column_keys] = useState([]);
  const [selected_record, set_selected_record] = useState(null);
  const [table_scroll_y, set_table_scroll_y] = useState(() => (
    typeof window === 'undefined' ? 560 : Math.max(300, Math.min(720, window.innerHeight - 430))
  ));

  useEffect(() => {
    const open_workspace_record = (event) => {
      const payload = event.detail;
      if (!payload?.record || (payload.row_key && payload.row_key !== row_key)) return;
      set_selected_record(payload.record);
    };
    document.addEventListener('vinamik:data_workspace_open_record', open_workspace_record);
    return () => document.removeEventListener('vinamik:data_workspace_open_record', open_workspace_record);
  }, [row_key]);

  useEffect(() => {
    const update_table_height = () => set_table_scroll_y(Math.max(300, Math.min(720, window.innerHeight - 430)));
    window.addEventListener('resize', update_table_height, { passive: true });
    return () => window.removeEventListener('resize', update_table_height);
  }, []);

  const selected_keys = useMemo(() => {
    if (custom_column_keys?.length) return custom_column_keys;
    if (!available_presets.length) return column_keys;
    return (column_presets[preset] || column_keys).filter(Boolean);
  }, [available_presets.length, column_keys, column_presets, custom_column_keys, preset]);

  const visible_columns = useMemo(() => all_columns.filter((column) => (
    column.key === 'actions' || selected_keys.includes(column.key)
  )), [all_columns, selected_keys]);

  const column_candidates = useMemo(() => {
    const query = column_search.trim().toLowerCase();
    return all_columns.filter((column) => column.key !== 'actions' && (!query || String(column.title || column.key).toLowerCase().includes(query)));
  }, [all_columns, column_search]);
  const table_width = useMemo(() => visible_columns.reduce((total, column) => total + (Number(column.width) || 180), 0), [visible_columns]);
  const open_column_drawer = () => { set_draft_column_keys(selected_keys.filter((key) => key !== 'actions')); set_column_search(''); set_column_drawer_open(true); };
  const apply_column_selection = () => {
    const next_keys = draft_column_keys.length ? draft_column_keys : selected_keys.filter((key) => key !== 'actions').slice(0, 1);
    set_custom_column_keys(next_keys);
    set_preset('custom');
    try {
      window.localStorage.setItem(`${storage_key}:columns`, JSON.stringify(next_keys));
      window.localStorage.setItem(storage_key, 'custom');
      window.localStorage.setItem(`${storage_key}:preset_explicit`, '1');
      window.localStorage.setItem(`${storage_key}:version`, preset_storage_version);
    } catch { /* Keep the current-session preference when storage is unavailable. */ }
    set_column_drawer_open(false);
  };
  const reset_column_selection = () => {
    const default_preset = available_presets.includes('detail') ? 'detail' : available_presets[0];
    set_custom_column_keys(null);
    set_preset(default_preset);
    try {
      window.localStorage.removeItem(`${storage_key}:columns`);
      if (default_preset) {
        window.localStorage.setItem(storage_key, default_preset);
        window.localStorage.setItem(`${storage_key}:preset_explicit`, '1');
        window.localStorage.setItem(`${storage_key}:version`, preset_storage_version);
      }
    } catch { /* Keep the current-session preference when storage is unavailable. */ }
    set_column_drawer_open(false);
  };
  const detail_columns = useMemo(() => columns.filter((column) => (
    column.dataIndex && column.key !== 'actions' && read_field(selected_record, column.dataIndex) !== undefined
  )), [columns, selected_record]);

  const update_preset = (value) => {
    set_preset(value);
    set_custom_column_keys(null);
    try {
      window.localStorage.setItem(storage_key, value);
      window.localStorage.setItem(`${storage_key}:preset_explicit`, '1');
      window.localStorage.setItem(`${storage_key}:version`, preset_storage_version);
      window.localStorage.removeItem(`${storage_key}:columns`);
    } catch {
      // Keep the current-session preference when browser storage is unavailable.
    }
  };

  const total_items = Number(pagination?.total || 0);
  const current_page = Number(pagination?.current || 1);
  const current_page_size = Number(pagination?.page_size || pagination?.pageSize || 10);
  const range_start = total_items === 0 ? 0 : ((current_page - 1) * current_page_size) + 1;
  const range_end = total_items === 0 ? 0 : Math.min(current_page * current_page_size, total_items);
  const handle_page_change = (next_page, next_page_size) => {
    on_change?.({ current: next_page, pageSize: next_page_size || current_page_size });
  };

  return <Card className={`data_workspace data_workspace_${module_key}`} bordered={false}>
    <div className="data_workspace_heading">
      <div>
        <Typography.Title level={4}>{title}</Typography.Title>
        {description && <Typography.Text>{description}</Typography.Text>}
      </div>
      <Space wrap className="data_workspace_controls">
        {read_only && <span className="data_workspace_permission"><LockOutlined /> Chỉ xem</span>}
        {available_presets.length > 0 && <Select
          aria-label="Chọn bố cục cột"
          value={custom_column_keys ? 'custom' : preset}
          onChange={(value) => value === 'custom' ? open_column_drawer() : update_preset(value)}
          options={[...available_presets.map((value) => ({ value, label: preset_labels[value] || value })), ...(custom_column_keys ? [{ value: 'custom', label: 'Tùy chỉnh' }] : [])]}
          suffixIcon={<SlidersOutlined />}
          className="data_workspace_preset"
        />}
        <Button className="data_workspace_columns_button" icon={<TableOutlined />} onClick={open_column_drawer}>Cột</Button>
      </Space>
    </div>
    {toolbar && <div className="data_workspace_toolbar">{toolbar}</div>}
    {read_only && <div className="data_workspace_read_only"><LockOutlined /> Bạn có quyền xem dữ liệu; thao tác ghi đang bị khóa theo tài khoản.</div>}
    <Table
      className="data_workspace_table"
      rowKey={row_key}
      columns={visible_columns}
      dataSource={data_source}
      loading={loading}
      virtual
      scroll={{ x: table_width, y: table_scroll_y }}
      pagination={false}
      onChange={on_change}
      locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={empty_text} /> }}
      onRow={(record) => ({
        onDoubleClick: () => (on_open_record ? on_open_record(record) : set_selected_record(record)),
      })}
    />
    {pagination && <div className="data_workspace_pagination">
      <span className="data_workspace_total">{range_start}–{range_end} / {total_items} {total_label}</span>
      <Pagination
        current={current_page}
        pageSize={current_page_size}
        total={total_items}
        pageSizeOptions={[10, 20, 50, 100]}
        showSizeChanger
        showLessItems
        responsive
        onChange={handle_page_change}
        onShowSizeChange={handle_page_change}
      />
    </div>}
    <Drawer
      title={selected_record ? `Chi tiết ${display_value(read_field(selected_record, row_key))}` : 'Chi tiết bản ghi'}
      open={Boolean(selected_record)}
      onClose={() => set_selected_record(null)}
      width={Math.min(760, window.innerWidth - 32)}
      destroyOnClose
    >
      {selected_record && <Descriptions bordered size="small" column={1} className="data_workspace_detail">
        {detail_columns.map((column) => <Descriptions.Item key={column.key} label={column.title}>
          {display_value(read_field(selected_record, column.dataIndex), column.key)}
        </Descriptions.Item>)}
      </Descriptions>}
    </Drawer>
    <Drawer
      title={<div className="column_drawer_title"><TableOutlined /><div><strong>Chọn cột hiển thị</strong><span>Chỉ tải các trường cần cho công việc hiện tại</span></div></div>}
      open={column_drawer_open}
      onClose={() => set_column_drawer_open(false)}
      width={Math.min(380, typeof window === 'undefined' ? 380 : window.innerWidth - 24)}
      className="column_drawer"
      footer={<Space><Button icon={<ReloadOutlined />} onClick={reset_column_selection}>Theo bố cục</Button><Button type="primary" icon={<CheckOutlined />} onClick={apply_column_selection}>Áp dụng</Button></Space>}
    >
      <Input.Search allowClear value={column_search} onChange={(event) => set_column_search(event.target.value)} placeholder="Tìm tên cột" />
      <div className="column_drawer_count">{draft_column_keys.length} cột đang chọn</div>
      <div className="column_drawer_list" role="group" aria-label="Các cột có thể hiển thị">
        {column_candidates.map((column) => <label key={column.key} className="column_drawer_item"><Checkbox checked={draft_column_keys.includes(column.key)} onChange={(event) => set_draft_column_keys((current) => event.target.checked ? [...current, column.key] : current.filter((key) => key !== column.key))} /><span>{column.title || column.key}</span></label>)}
        {!column_candidates.length && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Không tìm thấy cột" />}
      </div>
    </Drawer>
  </Card>;
}

export default DataWorkspace;
