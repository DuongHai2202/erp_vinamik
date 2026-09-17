import { Suspense, useEffect, useMemo, useState } from 'react';
import {
  App as antd_app,
  Avatar,
  Badge,
  Breadcrumb,
  Button,
  Divider,
  Drawer,
  Dropdown,
  Input,
  Layout,
  Menu,
  Segmented,
  Select,
  Tooltip,
  Typography,
} from 'antd';
import {
  AppstoreOutlined,
  BgColorsOutlined,
  CheckOutlined,
  DownOutlined,
  GlobalOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MoonOutlined,
  SettingOutlined,
  SlidersOutlined,
  SunOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { has_permission } from '../common/permission_utils';
import { use_system_status } from '../common/system_status_context';
import { use_auth } from '../identity/auth_context';
import { use_theme } from './theme_context';
import { get_visible_navigation } from './navigation_registry';

const { Header, Sider, Content } = Layout;
const AntdApp = antd_app;

const route_labels = {
  '/': 'Tổng quan',
  '/human_resources': 'Hồ sơ nhân viên',
  '/human_resources/contracts': 'Hợp đồng lao động',
  '/human_resources/absences': 'Nghỉ phép và vắng mặt',
  '/human_resources/rewards_discipline': 'Khen thưởng và kỷ luật',
  '/human_resources/payroll': 'Tính lương',
  '/inventory': 'Danh mục nguyên vật liệu',
  '/inventory/receipts': 'Phiếu nhập kho',
  '/inventory/issues': 'Phiếu xuất kho',
  '/inventory/stocktakes': 'Kiểm kê kho',
  '/inventory/transfers': 'Điều chuyển kho',
  '/production': 'Kế hoạch sản xuất',
  '/production/materials': 'Định mức và vật tư',
  '/production/orders': 'Lệnh sản xuất',
  '/production/assignments': 'Phân công nhân sự',
  '/production/finished_products': 'Sản lượng thành phẩm',
  '/settings/users': 'Tài khoản và phân quyền',
  '/settings/registration_requests': 'Yêu cầu cấp tài khoản',
};

function AppShell() {
  const { current_user, logout } = use_auth();
  const { state: system_state, latency_ms } = use_system_status();
  const { preferences, accent_options, font_options, update_preference, reset_preferences } = use_theme();
  const [is_collapsed, set_is_collapsed] = useState(false);
  const [is_mobile, set_is_mobile] = useState(false);
  const [open_keys, set_open_keys] = useState(['group_human_resources']);
  const [customizer_open, set_customizer_open] = useState(false);
  const [command_open, set_command_open] = useState(false);
  const [command_search, set_command_search] = useState('');
  const [selected_command_index, set_selected_command_index] = useState(0);
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    document.querySelector('.app_content')?.scrollTo({ top: 0, left: 0, behavior: 'auto' });
  }, [location.pathname]);

  const navigation_groups = useMemo(() => get_visible_navigation(current_user), [current_user]);

  const menu_items = useMemo(() => [
    {
      key: 'workspace',
      type: 'group',
      label: 'Không gian làm việc',
      children: [{ key: '/', icon: <AppstoreOutlined />, label: <Link to="/">Tổng quan</Link> }],
    },
    ...navigation_groups.map((group) => ({
      key: `group_${group.key}`,
      icon: <group.icon />,
      label: group.title,
      children: group.features.map((feature) => ({ key: feature.path, label: <Link to={feature.path}>{feature.label}</Link>, icon: <feature.icon /> })),
    })),
    ...((has_permission(current_user, 'identity_user_read') || has_permission(current_user, 'identity_registration_read')) ? [{
      key: 'group_settings',
      icon: <SettingOutlined />,
      label: 'Thiết lập hệ thống',
      children: [
        ...(has_permission(current_user, 'identity_user_read') ? [{ key: '/settings/users', icon: <UserOutlined />, label: <Link to="/settings/users">Tài khoản và phân quyền</Link> }] : []),
        ...(has_permission(current_user, 'identity_registration_read') ? [{ key: '/settings/registration_requests', icon: <UserOutlined />, label: <Link to="/settings/registration_requests">Yêu cầu cấp tài khoản</Link> }] : []),
      ],
    }] : []),

  ], [current_user, navigation_groups]);

  const command_items = useMemo(() => [
    { key: '/', label: 'Tổng quan', group: 'Không gian làm việc', icon: <AppstoreOutlined /> },
    ...navigation_groups.flatMap((group) => group.features.map((feature) => ({
      key: feature.path,
      label: feature.label,
      group: group.title,
      icon: <feature.icon />,
    }))),
    ...(has_permission(current_user, 'identity_user_read') ? [{ key: '/settings/users', label: route_labels['/settings/users'], group: 'Thiết lập hệ thống', icon: <UserOutlined /> }] : []),
    ...(has_permission(current_user, 'identity_registration_read') ? [{ key: '/settings/registration_requests', label: route_labels['/settings/registration_requests'], group: 'Thiết lập hệ thống', icon: <UserOutlined /> }] : []),
  ], [current_user, navigation_groups]);

  const filtered_commands = useMemo(() => {
    const query = command_search.trim().toLowerCase();
    return command_items.filter((item) => !query || item.label.toLowerCase().includes(query) || item.group.toLowerCase().includes(query));
  }, [command_items, command_search]);

  useEffect(() => {
    set_selected_command_index(0);
  }, [command_search, command_open]);

  useEffect(() => {
    const on_key_down = (event) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        set_command_open(true);
        return;
      }
      if (!command_open) return;
      if (event.key === 'Escape') {
        event.preventDefault();
        set_command_open(false);
      } else if (event.key === 'ArrowDown') {
        event.preventDefault();
        set_selected_command_index((index) => filtered_commands.length ? (index + 1) % filtered_commands.length : 0);
      } else if (event.key === 'ArrowUp') {
        event.preventDefault();
        set_selected_command_index((index) => filtered_commands.length ? (index - 1 + filtered_commands.length) % filtered_commands.length : 0);
      } else if (event.key === 'Enter' && filtered_commands[selected_command_index]) {
        event.preventDefault();
        navigate(filtered_commands[selected_command_index].key);
        set_command_open(false);
        set_command_search('');
      }
    };
    window.addEventListener('keydown', on_key_down);
    return () => window.removeEventListener('keydown', on_key_down);
  }, [command_open, filtered_commands, navigate, selected_command_index]);

  const selected_key = route_labels[location.pathname] ? location.pathname : '/';
  const active_group = navigation_groups.find((group) => group.features.some((item) => item.path === selected_key));
  const page_label = route_labels[location.pathname] || 'Tổng quan';
  const account_menu = [
    { key: 'profile', disabled: true, label: <span className="account_menu_identity"><strong>{current_user?.username}</strong><small>{current_user?.super_admin ? 'Quản trị tối cao' : 'Tài khoản nghiệp vụ'}</small></span> },
    { type: 'divider' },
    { key: 'theme', icon: <BgColorsOutlined />, label: 'Tùy chỉnh giao diện' },
    { key: 'logout', icon: <LogoutOutlined />, label: 'Đăng xuất' },
  ];

  useEffect(() => {
    if (active_group && !open_keys.includes(`group_${active_group.key}`)) {
      set_open_keys((current_keys) => [...current_keys, `group_${active_group.key}`]);
    }
  }, [active_group, open_keys]);

  const on_account_action = async ({ key }) => {
    if (key === 'theme') set_customizer_open(true);
    if (key === 'logout') {
      await logout();
      navigate('/login', { replace: true });
    }
  };

  const open_command = (key) => {
    navigate(key);
    set_command_open(false);
    set_command_search('');
    if (is_mobile) set_is_collapsed(true);
  };

  const on_breakpoint = (broken) => {
    set_is_mobile(broken);
    if (broken) set_is_collapsed(true);
  };

  const system_label = system_state === 'online' ? 'Trực tuyến' : system_state === 'offline' ? 'Mất kết nối' : system_state === 'degraded' ? 'Có cảnh báo' : 'Đang kiểm tra';

  return <AntdApp>
    <Layout className="app_layout">
      <Sider collapsible collapsed={is_collapsed} trigger={null} width={286} collapsedWidth={is_mobile ? 0 : 84} breakpoint="lg" onBreakpoint={on_breakpoint} className="app_sider">
        <div className="brand_lockup">
          <div className="brand_mark"><span>V</span><i /></div>
          {!is_collapsed && <div className="brand_copy"><strong>Vinamik</strong><span>ĐIỀU PHỐI VẬN HÀNH</span></div>}
        </div>
        {!is_collapsed && <div className={`workspace_pill workspace_pill_${system_state}`}><span className="live_dot" /> Nguồn dữ liệu <small>{system_label}</small></div>}
        {is_collapsed ? <CollapsedNavigation navigation_groups={navigation_groups} selected_key={selected_key} has_settings={has_permission(current_user, 'identity_user_read') || has_permission(current_user, 'identity_registration_read')} has_user_settings={has_permission(current_user, 'identity_user_read')} has_registration_settings={has_permission(current_user, 'identity_registration_read')} navigate={navigate} /> : <Menu theme="dark" mode="inline" selectedKeys={[selected_key]} openKeys={open_keys} onOpenChange={set_open_keys} items={menu_items} onClick={() => { if (is_mobile) set_is_collapsed(true); }} className="app_menu" />}
        {!is_collapsed && <div className="sider_footer">
          <div className="sider_footer_icon"><GlobalOutlined /></div>
          <div><strong>{system_label}</strong><span>{latency_ms === null ? 'Dữ liệu trung tâm' : `${latency_ms} ms phản hồi`}</span></div>
        </div>}
      </Sider>
      {is_mobile && !is_collapsed && <button type="button" className="sider_scrim" aria-label="Đóng thanh điều hướng" onClick={() => set_is_collapsed(true)} />}
      <Layout>
        <Header className="app_header">
          <div className="app_header_inner">
            <div className="header_left">
            <Tooltip title={is_collapsed ? 'Mở thanh điều hướng' : 'Thu gọn thanh điều hướng'}>
              <Button type="text" className="icon_button header_toggle" icon={is_collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />} onClick={() => set_is_collapsed(!is_collapsed)} aria-label="Thu gọn thanh điều hướng" />
            </Tooltip>
            <Breadcrumb items={[{ title: <Link to="/">Tổng quan</Link> }, ...(selected_key !== '/' ? [{ title: <span>{active_group?.title || 'Phân hệ'}</span> }, { title: page_label }] : [])]} />
          </div>
          <div className="header_right">
            <button type="button" className="command_trigger" onClick={() => set_command_open(true)} aria-label="Mở trung tâm lệnh">
              <span><span className="command_search_icon">⌕</span> Tìm chức năng</span><kbd>Ctrl K</kbd>
            </button>
            <Tooltip title="Tùy chỉnh giao diện"><Button type="text" className="icon_button" icon={<SlidersOutlined />} onClick={() => set_customizer_open(true)} aria-label="Tùy chỉnh giao diện" /></Tooltip>
            <Dropdown menu={{ items: account_menu, onClick: on_account_action }} trigger={['click']} placement="bottomRight">
              <button type="button" className="profile_button">
                <Badge dot color={system_state === 'online' ? '#20c997' : '#e4a33b'} offset={[-2, 28]}><Avatar icon={<UserOutlined />} /></Badge>
                <span className="profile_copy"><strong>{current_user?.username}</strong><small>{current_user?.super_admin ? 'Quản trị tối cao' : 'Người dùng'}</small></span>
                <DownOutlined className="profile_chevron" />
              </button>
            </Dropdown>
          </div>
          </div>
        </Header>
        <Content className="app_content"><div className="app_content_inner"><Suspense fallback={<div className="route_loading" role="status"><span className="loading_orbit" />Đang tải màn hình...</div>}><Outlet /></Suspense></div></Content>
      </Layout>
    </Layout>

    <Drawer title={<div className="drawer_title"><BgColorsOutlined /><div><strong>Tùy chỉnh không gian</strong><span>Thiết lập riêng cho tài khoản này</span></div></div>} placement="right" width={360} open={customizer_open} onClose={() => set_customizer_open(false)} className="customizer_drawer" footer={<Button block onClick={reset_preferences}>Khôi phục mặc định</Button>}>
      <div className="customizer_section">
        <Typography.Text className="customizer_label">Chế độ hiển thị</Typography.Text>
        <Segmented block value={preferences.appearance} onChange={(value) => update_preference('appearance', value)} options={[{ value: 'light', label: <span><SunOutlined /> Sáng</span> }, { value: 'dark', label: <span><MoonOutlined /> Tối</span> }]} />
      </div>
      <div className="customizer_section">
        <Typography.Text className="customizer_label">Màu nhận diện</Typography.Text>
        <div className="accent_grid">{Object.entries(accent_options).map(([key, option]) => <button type="button" key={key} className={`accent_option ${preferences.accent === key ? 'is_selected' : ''}`} onClick={() => update_preference('accent', key)} aria-label={option.label} aria-pressed={preferences.accent === key}><span style={{ backgroundColor: option.primary }} />{option.label}{preferences.accent === key && <CheckOutlined />}</button>)}</div>
      </div>
      <div className="customizer_section">
        <Typography.Text className="customizer_label">Kiểu chữ tiếng Việt</Typography.Text>
        <Select block value={preferences.font_family} onChange={(value) => update_preference('font_family', value)} options={Object.entries(font_options).map(([value, option]) => ({ value, label: option.label }))} />
      </div>
      <div className="customizer_section">
        <Typography.Text className="customizer_label">Cỡ chữ</Typography.Text>
        <Segmented block value={preferences.font_size} onChange={(value) => update_preference('font_size', value)} options={[14, 15, 16, 17].map((value) => ({ value, label: value + ' px' }))} />
      </div>
      <div className="customizer_section">
        <Typography.Text className="customizer_label">Mật độ bảng và biểu mẫu</Typography.Text>
        <Segmented block value={preferences.density} onChange={(value) => update_preference('density', value)} options={[{ value: 'compact', label: 'Gọn' }, { value: 'comfortable', label: 'Thoáng' }]} />
      </div>
      <div className="customizer_section">
        <Typography.Text className="customizer_label">Độ bo góc</Typography.Text>
        <Segmented block value={preferences.radius} onChange={(value) => update_preference('radius', value)} options={[{ value: 10, label: 'Gọn' }, { value: 14, label: 'Cân bằng' }, { value: 20, label: 'Mềm' }]} />
      </div>
      <Divider />
      <div className="customizer_hint"><GlobalOutlined /><span>Tùy chỉnh này chỉ ảnh hưởng cách hiển thị. Dữ liệu và quyền luôn dùng chung từ backend trung tâm.</span></div>
    </Drawer>

    <ModalCommand
      open={command_open}
      on_close={() => { set_command_open(false); set_command_search(''); }}
      search={command_search}
      on_search={set_command_search}
      items={filtered_commands}
      selected_index={selected_command_index}
      on_select={open_command}
    />
  </AntdApp>;
}

function CollapsedNavigation({ navigation_groups, selected_key, has_settings, has_user_settings, has_registration_settings, navigate }) {
  const overview_active = selected_key === '/';
  const settings_active = selected_key === '/settings/users' || selected_key === '/settings/registration_requests';
  const to_menu_items = (group) => group.features.map((feature) => ({ key: feature.path, icon: <feature.icon />, label: feature.label }));

  return <nav className="collapsed_nav" aria-label="Điều hướng thu gọn">
    <Tooltip title="Tổng quan" placement="right">
      <Link to="/" className={`collapsed_nav_item ${overview_active ? 'is_active' : ''}`} aria-label="Tổng quan"><AppstoreOutlined /></Link>
    </Tooltip>
    {navigation_groups.map((group) => {
      const group_active = group.features.some((feature) => feature.path === selected_key);
      return <Dropdown key={group.key} placement="rightTop" trigger={['click']} menu={{ items: to_menu_items(group), onClick: ({ key }) => navigate(key) }}>
        <button type="button" className={`collapsed_nav_item ${group_active ? 'is_active' : ''}`} aria-label={group.title}><group.icon /></button>
      </Dropdown>;
    })}
    {has_settings && <Dropdown placement="rightTop" trigger={['click']} menu={{ items: [
      ...(has_user_settings ? [{ key: '/settings/users', icon: <UserOutlined />, label: 'Tài khoản và phân quyền' }] : []),
      ...(has_registration_settings ? [{ key: '/settings/registration_requests', icon: <UserOutlined />, label: 'Yêu cầu cấp tài khoản' }] : []),
    ], onClick: ({ key }) => navigate(key) }}>
      <button type="button" className={`collapsed_nav_item ${settings_active ? 'is_active' : ''}`} aria-label="Thiết lập hệ thống"><SettingOutlined /></button>
    </Dropdown>}
  </nav>;
}
function ModalCommand({ open, on_close, search, on_search, items, selected_index, on_select }) {
  return <Drawer title={<div className="command_drawer_title"><AppstoreOutlined /> Trung tâm lệnh</div>} placement="top" height={470} open={open} onClose={on_close} className="command_drawer">
    <Input autoFocus value={search} onChange={(event) => on_search(event.target.value)} placeholder="Tìm chức năng, phân hệ..." prefix={<span className="command_input_icon">⌕</span>} size="large" />
    <div className="command_hint">Dùng phím ↑ ↓ để chọn · Enter để mở · Esc để đóng</div>
    <div className="command_results" role="listbox" aria-label="Kết quả điều hướng">
      {items.length === 0 && <div className="command_empty">Không tìm thấy chức năng phù hợp.</div>}
      {items.map((item, index) => <button type="button" className={`command_result ${index === selected_index ? 'is_selected' : ''}`} key={item.key} onMouseEnter={() => {}} onClick={() => on_select(item.key)} role="option" aria-selected={index === selected_index}>
        <span className="command_result_icon">{item.icon}</span><span><strong>{item.label}</strong><small>{item.group}</small></span><span className="command_result_arrow">↵</span>
      </button>)}
    </div>
  </Drawer>;
}

export default AppShell;

