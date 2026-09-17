import {
  CheckCircleFilled,
  DatabaseOutlined,
  FileDoneOutlined,
  FileSearchOutlined,
  FieldTimeOutlined,
  GlobalOutlined,
  ShopOutlined,
  TeamOutlined,
  ToolOutlined,
  UserOutlined,
} from '@ant-design/icons';

const navigation_registry = [
  {
    key: 'human_resources',
    title: 'Quản lý nhân sự',
    short_title: 'Nhân sự',
    description: 'Hồ sơ, hợp đồng và các quyết định liên quan đến người lao động.',
    color: 'var(--erp-module-human-resources)',
    icon: TeamOutlined,
    permission: 'hr_employee_read',
    features: [
      { path: '/human_resources', label: 'Hồ sơ nhân viên', icon: UserOutlined, permission: 'hr_employee_read' },
      { path: '/human_resources/contracts', label: 'Hợp đồng lao động', icon: FileDoneOutlined, permission: 'hr_contract_read' },
      { path: '/human_resources/absences', label: 'Nghỉ phép và vắng mặt', icon: FieldTimeOutlined, permission: 'hr_absence_read' },
      { path: '/human_resources/rewards_discipline', label: 'Khen thưởng và kỷ luật', icon: CheckCircleFilled, permission: 'hr_reward_read' },
      { path: '/human_resources/payroll', label: 'Tính lương', icon: FileSearchOutlined, permission: 'hr_payroll_read' },
    ],
  },
  {
    key: 'inventory',
    title: 'Kho và nguyên vật liệu',
    short_title: 'Kho',
    description: 'Số dư, lô hàng và giao dịch được kiểm soát từ một sổ kho duy nhất.',
    color: 'var(--erp-module-inventory)',
    icon: DatabaseOutlined,
    permission: 'inventory_material_read',
    features: [
      { path: '/inventory', label: 'Danh mục nguyên vật liệu', icon: ShopOutlined, permission: 'inventory_material_read' },
      { path: '/inventory/receipts', label: 'Phiếu nhập kho', icon: FileDoneOutlined, permission: 'inventory_receipt_read' },
      { path: '/inventory/issues', label: 'Phiếu xuất kho', icon: FileSearchOutlined, permission: 'inventory_issue_read' },
      { path: '/inventory/stocktakes', label: 'Kiểm kê kho', icon: CheckCircleFilled, permission: 'inventory_stocktake_read' },
      { path: '/inventory/transfers', label: 'Điều chuyển kho', icon: GlobalOutlined, permission: 'inventory_transfer_read' },
    ],
  },
  {
    key: 'production',
    title: 'Quản lý sản xuất',
    short_title: 'Sản xuất',
    description: 'Từ kế hoạch, định mức đến lệnh sản xuất và thành phẩm bàn giao cho kho.',
    color: 'var(--erp-module-production)',
    icon: ToolOutlined,
    permission: 'production_plan_read',
    features: [
      { path: '/production', label: 'Kế hoạch sản xuất', icon: FileDoneOutlined, permission: 'production_plan_read' },
      { path: '/production/materials', label: 'Định mức và vật tư', icon: DatabaseOutlined, permission: 'production_bom_read' },
      { path: '/production/orders', label: 'Lệnh sản xuất', icon: ToolOutlined, permission: 'production_order_read' },
      { path: '/production/assignments', label: 'Phân công nhân sự', icon: TeamOutlined, permission: 'production_assignment_read' },
      { path: '/production/finished_products', label: 'Sản lượng thành phẩm', icon: ShopOutlined, permission: 'production_output_read' },
    ],
  },
];

function get_visible_navigation(current_user) {
  return navigation_registry
    .map((module) => ({
      ...module,
      features: module.features.filter((feature) => current_user?.permission_codes?.includes(feature.permission)),
    }))
    .filter((module) => current_user?.permission_codes?.includes(module.permission) && module.features.length > 0);
}

export { get_visible_navigation, navigation_registry };
