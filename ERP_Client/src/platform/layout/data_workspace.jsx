import { useMemo, useState } from 'react';
import { Card, Descriptions, Drawer, Empty, Select, Space, Table, Tag, Typography } from 'antd';
import { LockOutlined, SlidersOutlined } from '@ant-design/icons';

const preset_labels = {
  overview: 'Tổng quan',
  detail: 'Chi tiết',
  audit: 'Kiểm tra',
};

function load_preset(storage_key, available) {
  try {
    const stored = window.localStorage.getItem(storage_key);
    return available.includes(stored) ? stored : available[0];
  } catch {
    return available[0];
  }
}

function read_field(record, data_index) {
  if (Array.isArray(data_index)) return data_index.reduce((value, key) => value?.[key], record);
  return record?.[data_index];
}

function display_value(value) {
  if (value === null || value === undefined || value === '') return '—';
  if (typeof value === 'boolean') return value ? 'Có' : 'Không';
  if (typeof value === 'object') return JSON.stringify(value);
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
}) {
  const available_presets = Object.keys(column_presets || {});
  const [preset, set_preset] = useState(() => load_preset(storage_key, available_presets));
  const [selected_record, set_selected_record] = useState(null);
  const visible_columns = useMemo(() => {
    if (!available_presets.length) return columns;
    const keys = column_presets[preset] || [];
    return columns.filter((column) => keys.includes(column.key));
  }, [available_presets, columns, column_presets, preset]);
  const table_width = useMemo(() => visible_columns.reduce((total, column) => total + (Number(column.width) || 180), 0), [visible_columns]);
  const detail_columns = useMemo(() => columns.filter((column) => (
    column.dataIndex && column.key !== 'actions' && read_field(selected_record, column.dataIndex) !== undefined
  )), [columns, selected_record]);

  const update_preset = (value) => {
    set_preset(value);
    try {
      window.localStorage.setItem(storage_key, value);
    } catch {
      // Keep the current-session preference when browser storage is unavailable.
    }
  };

  const pagination_config = pagination ? {
    ...pagination,
    showSizeChanger: true,
    showTotal: (value, range) => `${range[0]}–${range[1]} / ${value} ${total_label}`,
  } : false;

  return <Card className={`data_workspace data_workspace_${module_key}`} bordered={false}>
    <div className="data_workspace_heading">
      <div>
        <Typography.Title level={4}>{title}</Typography.Title>
        {description && <Typography.Text>{description}</Typography.Text>}
      </div>
      <Space wrap className="data_workspace_controls">
        {read_only && <Tag color="gold" icon={<LockOutlined />}>Chỉ xem</Tag>}
        {available_presets.length > 0 && <Select
          aria-label="Chọn bố cục cột"
          value={preset}
          onChange={update_preset}
          options={available_presets.map((value) => ({ value, label: preset_labels[value] || value }))}
          suffixIcon={<SlidersOutlined />}
          className="data_workspace_preset"
        />}
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
      scroll={{ x: table_width, y: 560 }}
      pagination={pagination_config}
      onChange={on_change}
      locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={empty_text} /> }}
      onRow={(record) => ({
        onDoubleClick: () => set_selected_record(record),
      })}
    />
    {pagination?.total === 0 && !loading && <span className="data_workspace_total_hint">Tổng 0 {total_label}</span>}
    <Drawer
      title={selected_record ? `Chi tiết ${display_value(read_field(selected_record, row_key))}` : 'Chi tiết bản ghi'}
      open={Boolean(selected_record)}
      onClose={() => set_selected_record(null)}
      width={Math.min(760, window.innerWidth - 32)}
      destroyOnClose
    >
      {selected_record && <Descriptions bordered size="small" column={1} className="data_workspace_detail">
        {detail_columns.map((column) => <Descriptions.Item key={column.key} label={column.title}>
          {display_value(read_field(selected_record, column.dataIndex))}
        </Descriptions.Item>)}
      </Descriptions>}
    </Drawer>
  </Card>;
}

export default DataWorkspace;
