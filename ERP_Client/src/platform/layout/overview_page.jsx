import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ArrowRightOutlined,
  CheckCircleFilled,
  ClockCircleOutlined,
  GlobalOutlined,
  LockOutlined,
  ReloadOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import { Button, Spin, Tag, Typography } from 'antd';
import { Link } from 'react-router-dom';
import { has_permission } from '../common/permission_utils';
import { format_datetime_vn } from '../common/formatters';
import { request_api } from '../common/api_client';
import { use_system_status } from '../common/system_status_context';
import { use_auth } from '../identity/auth_context';
import { get_visible_navigation, navigation_registry } from './navigation_registry';

const queue_definitions = [
  {
    key: 'hr_absence',
    module_key: 'human_resources',
    module_label: 'Nhân sự',
    label: 'Yêu cầu nghỉ chờ duyệt',
    permission: 'hr_absence_read',
    path: '/human_resources/absences',
    load: async () => {
      const response = await request_api('/api/v1/human_resources/absences?status=pending&page=0&page_size=1');
      return response.data.total_items;
    },
  },
  {
    key: 'inventory_unposted',
    module_key: 'inventory',
    module_label: 'Kho',
    label: 'Phiếu kho chưa ghi sổ',
    permissions: ['inventory_receipt_read', 'inventory_issue_read'],
    path: '/inventory/receipts',
    load: async (user) => {
      const paths = [];
      if (has_permission(user, 'inventory_receipt_read')) {
        paths.push('/api/v1/inventory/receipts?status=draft&page=0&page_size=1');
        paths.push('/api/v1/inventory/receipts?status=pending&page=0&page_size=1');
      }
      if (has_permission(user, 'inventory_issue_read')) {
        paths.push('/api/v1/inventory/issues?status=draft&page=0&page_size=1');
        paths.push('/api/v1/inventory/issues?status=pending&page=0&page_size=1');
      }
      const responses = await Promise.all(paths.map((path) => request_api(path)));
      return responses.reduce((total, result) => total + Number(result.data.total_items || 0), 0);
    },
  },
  {
    key: 'production_overdue',
    module_key: 'production',
    module_label: 'Sản xuất',
    label: 'Lệnh sản xuất quá hạn',
    permission: 'production_order_read',
    path: '/production/orders',
    load: async () => {
      const response = await request_api('/api/v1/production/orders/overdue_count');
      return Number(response.data || 0);
    },
  },
  {
    key: 'payroll_open',
    module_key: 'human_resources',
    module_label: 'Tiền lương',
    label: 'Kỳ lương đang mở',
    permission: 'hr_payroll_read',
    path: '/human_resources/payroll',
    load: async () => {
      // A payroll period remains open until it is locked. Count every
      // reviewable lifecycle state instead of only draft periods.
      const open_statuses = ['draft', 'calculated', 'approved', 'rejected'];
      const responses = await Promise.all(open_statuses.map((status) => request_api(
        `/api/v1/human_resources/payroll/periods?status=${status}&page=0&page_size=1`,
      )));
      return responses.reduce((total, response) => total + Number(response.data.total_items || 0), 0);
    },
  },
];

const activity_labels = {
  create: 'Tạo mới',
  update: 'Cập nhật',
  post: 'Ghi sổ',
  approve: 'Duyệt',
  reject: 'Từ chối',
  lock: 'Khóa',
  status_change: 'Đổi trạng thái',
  login_success: 'Đăng nhập',
  logout: 'Đăng xuất',
};

function activity_label(action_code) {
  const suffix = action_code?.split('_').at(-1);
  return activity_labels[suffix] || activity_labels[action_code] || 'Cập nhật dữ liệu';
}

function format_activity_time(value) {
  if (!value) return 'Vừa cập nhật';
  const text = String(value);
  const date = value instanceof Date || text.includes('T') ? new Date(value) : null;
  if (!date || Number.isNaN(date.getTime())) return format_datetime_vn(value);
  const parts = Object.fromEntries(new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
  }).formatToParts(date).map((part) => [part.type, part.value]));
  return `${parts.day}/${parts.month}/${parts.year} ${parts.hour}:${parts.minute}:${parts.second}`;
}

function can_read_module(current_user, module) {
  return module.planned || module.features.some((feature) => feature.permission && has_permission(current_user, feature.permission));
}

function useWorkQueue(current_user) {
  const [queue, set_queue] = useState([]);
  const [is_loading, set_is_loading] = useState(false);
  const [has_error, set_has_error] = useState(false);
  const [last_loaded_at, set_last_loaded_at] = useState(null);

  const load_queue = useCallback(async () => {
    const available = queue_definitions.filter((item) => item.permissions ? item.permissions.some((permission) => has_permission(current_user, permission)) : has_permission(current_user, item.permission));
    if (!available.length) {
      set_queue([]);
      return;
    }
    set_is_loading(true);
    set_has_error(false);
    const results = await Promise.allSettled(available.map((item) => item.load(current_user)));
    set_queue(results.map((result, index) => ({
      ...available[index],
      count: result.status === 'fulfilled' ? result.value : null,
      state: result.status === 'fulfilled' ? 'ready' : 'error',
    })));
    set_has_error(results.some((result) => result.status === 'rejected'));
    set_last_loaded_at(new Date());
    set_is_loading(false);
  }, [current_user]);

  useEffect(() => {
    load_queue();
  }, [load_queue]);

  return { queue, is_loading, has_error, last_loaded_at, load_queue };
}

function OverviewPage() {
  const { current_user } = use_auth();
  const { state: system_state, checked_at, latency_ms, check_status } = use_system_status();
  const { queue, is_loading: queue_loading, has_error: queue_error, last_loaded_at, load_queue } = useWorkQueue(current_user);
  const [activities, set_activities] = useState([]);
  const [activity_loading, set_activity_loading] = useState(false);

  const visible_navigation = useMemo(() => get_visible_navigation(current_user), [current_user]);
  const available_features = visible_navigation.reduce((total, module) => total + module.features.length, 0);
  const read_only = navigation_registry.some((module) => !module.planned
    && can_read_module(current_user, module)
    && module.features.some((feature) => feature.permission && !has_permission(current_user, feature.permission)));

  const load_activities = useCallback(async () => {
    if (!has_permission(current_user, 'identity_audit_read')) {
      set_activities([]);
      return;
    }
    set_activity_loading(true);
    try {
      const response = await request_api('/api/v1/identity/audit_events?page=0&page_size=6');
      set_activities(response.data.items || []);
    } catch {
      set_activities([]);
    } finally {
      set_activity_loading(false);
    }
  }, [current_user]);

  useEffect(() => {
    load_activities();
  }, [load_activities]);

  const status_meta = {
    checking: { label: 'Đang kiểm tra', color: 'processing' },
    online: { label: 'Trực tuyến', color: 'success' },
    degraded: { label: 'Có cảnh báo', color: 'warning' },
    offline: { label: 'Mất kết nối', color: 'error' },
  }[system_state] || { label: 'Chưa xác định', color: 'default' };

  return <div className="overview_page">


    <section className="overview_section attention_section">
      <div className="overview_section_heading">
        <div>
          <Typography.Title level={3}>Cần xử lý</Typography.Title>
          <Typography.Text>Các hàng đợi được lấy trực tiếp từ dữ liệu hiện tại.{last_loaded_at && ` Cập nhật ${format_activity_time(last_loaded_at)}.`}</Typography.Text>
        </div>
        <Button type="text" icon={<ReloadOutlined />} onClick={() => { load_queue(); load_activities(); }} loading={queue_loading || activity_loading}>Làm mới</Button>
      </div>
      <div className="attention_grid">
        {queue.map((item, index) => <Link to={item.path} className={`attention_card attention_card_${item.module_key} attention_card_${item.state}`} key={item.key}>
          <span className="attention_card_index">{String(index + 1).padStart(2, '0')}</span>
          <span className="attention_card_top"><span>{item.module_label}</span><ArrowRightOutlined /></span>
          <strong>{item.count === null ? 'Chưa cập nhật' : item.count}</strong>
          <span>{item.label}</span>
          {item.state === 'error' && <small>Không tải được dữ liệu</small>}
        </Link>)}
        {!queue_loading && !queue.length && <div className="attention_empty"><CheckCircleFilled /> Không có hàng đợi cần xử lý.</div>}
        {queue_loading && <div className="attention_empty"><Spin size="small" /> Đang tải hàng đợi...</div>}
      </div>
      {queue_error && <Typography.Text className="overview_inline_warning"><WarningOutlined /> Một số hàng đợi chưa phản hồi. Bạn có thể thử làm mới.</Typography.Text>}
    </section>

    <section className="overview_section">
      <div className="overview_section_heading">
        <div>
          <Typography.Title level={3}>Bảng điều phối</Typography.Title>
          <Typography.Text>{visible_navigation.length} phân hệ, {available_features} chức năng theo quyền hiện tại.</Typography.Text>
        </div>
        <div className="sync_marker"><span className="live_dot" /> DỮ LIỆU TRUNG TÂM</div>
      </div>
      <div className="module_matrix">
        {navigation_registry.map((module) => {
          const ModuleIcon = module.icon;
          const module_readable = can_read_module(current_user, module);
          return <article className={`module_row ${module_readable ? '' : 'module_row_locked'}`} style={{ '--module-accent': module.color }} key={module.key}>
            <div className="module_row_identity">
              <div className="module_row_icon"><ModuleIcon /></div>
              <div className="module_row_copy">
                <Typography.Title level={4}>{module.title}</Typography.Title>
                <Typography.Paragraph>{module.description}</Typography.Paragraph>
              </div>
              {module.planned && <Tag color="gold" icon={<ClockCircleOutlined />}>Đang chuẩn bị</Tag>}
              {!module.planned && !module_readable && <Tag color="gold" icon={<LockOutlined />}>Chưa cấp quyền</Tag>}
              {module_readable && <Link to={module.features[0].path} className="module_open" aria-label={`Mở ${module.title}`}><ArrowRightOutlined /></Link>}
            </div>
            <div className="module_row_features">
              {module.features.map((feature, index) => {
                const FeatureIcon = feature.icon;
                const can_read = module.planned || has_permission(current_user, feature.permission);
                return can_read ? <Link to={feature.path} className="module_feature" key={feature.path}>
                  <span className="module_feature_index">{String(index + 1).padStart(2, '0')}</span>
                  <span className="module_feature_icon"><FeatureIcon /></span>
                  <span className="module_feature_label">{feature.label}</span>
                  <ArrowRightOutlined className="module_feature_arrow" />
                </Link> : <button type="button" className="module_feature module_feature_locked" key={feature.path} title="Bạn chưa được cấp quyền đọc" disabled>
                  <span className="module_feature_index">{String(index + 1).padStart(2, '0')}</span>
                  <span className="module_feature_icon"><LockOutlined /></span>
                  <span className="module_feature_label">{feature.label}</span>
                  <small>Chưa cấp quyền đọc</small>
                </button>;
              })}
            </div>
          </article>;
        })}
      </div>
    </section>

    <section className="overview_followup">
      <div className="quick_access_panel">
        <div className="followup_heading"><div><Typography.Title level={4}>Hoạt động gần đây</Typography.Title><Typography.Text>Những thay đổi gần nhất mà tài khoản được phép xem.</Typography.Text></div><ClockCircleOutlined /></div>
        {activity_loading && <div className="activity_empty"><Spin size="small" /> Đang tải hoạt động...</div>}
        {!activity_loading && !activities.length && <div className="activity_empty"><GlobalOutlined /> Chưa có hoạt động để hiển thị.</div>}
        {!activity_loading && activities.map((activity) => <div className="activity_item" key={activity.audit_id}>
          <span className="activity_dot" />
          <div><strong>{activity_label(activity.action_code)}</strong><small>{activity.actor_username || 'Hệ thống'} · {format_activity_time(activity.occurred_at)}</small></div>
          <Tag>{activity.module_code === 'hr' ? 'Nhân sự' : activity.module_code === 'inventory' ? 'Kho' : activity.module_code === 'production' ? 'Sản xuất' : 'Hệ thống'}</Tag>
        </div>)}
      </div>
      <div className="connection_panel">
        <div className="connection_panel_head"><GlobalOutlined /><span>TRẠNG THÁI HỆ THỐNG</span></div>
        <Typography.Title level={4}>Nguồn dữ liệu trung tâm.</Typography.Title>
        <Typography.Paragraph>Dữ liệu và quyền được phục vụ từ cùng một backend, nên các máy trong hệ thống nhìn thấy cùng một trạng thái sau khi đồng bộ.</Typography.Paragraph>
        <div className={`connection_status connection_status_${system_state}`}>
          <span className="live_dot" /> <strong>{status_meta.label}</strong>
          {latency_ms !== null && <small>{latency_ms} ms</small>}
          <Button type="text" icon={<ReloadOutlined />} onClick={check_status} aria-label="Kiểm tra kết nối lại" />
        </div>
        {checked_at && <small className="connection_checked_at">Kiểm tra lúc {format_activity_time(checked_at)}</small>}
        {read_only && <div className="read_only_notice"><LockOutlined /> Một số chức năng đang ở chế độ chỉ xem theo quyền tài khoản.</div>}
      </div>
    </section>
  </div>;
}

export default OverviewPage;

