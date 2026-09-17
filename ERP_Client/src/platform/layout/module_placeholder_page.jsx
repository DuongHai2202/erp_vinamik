import { ArrowLeftOutlined, CheckCircleFilled, DatabaseOutlined, FieldTimeOutlined, LockOutlined, RocketOutlined, TeamOutlined, ToolOutlined } from '@ant-design/icons';
import { Button, Card, Col, Divider, Result, Row, Space, Tag, Typography } from 'antd';
import { Link } from 'react-router-dom';
import { has_permission } from '../common/permission_utils';
import { use_auth } from '../identity/auth_context';

const feature_catalog = {
  human_resources: {
    title: 'Quản lý nhân sự',
    icon: <TeamOutlined />,
    color: '#3868ff',
    features: {
      absences: { label: 'Nghỉ phép và vắng mặt', description: 'Tiếp nhận, duyệt và theo dõi yêu cầu nghỉ của nhân viên.', permission: 'hr_absence_read' },
      rewards_discipline: { label: 'Khen thưởng và kỷ luật', description: 'Quản lý đề xuất, quyết định và đầu vào ảnh hưởng đến payroll.', permission: 'hr_reward_read' },
      payroll: { label: 'Tính lương', description: 'Tính, duyệt và khóa kỳ lương theo snapshot bất biến.', permission: 'hr_payroll_read' },
    },
  },
  inventory: {
    title: 'Kho và nguyên vật liệu',
    icon: <DatabaseOutlined />,
    color: '#0f9f89',
    features: {
      receipts: { label: 'Phiếu nhập kho', description: 'Tạo, kiểm tra và ghi sổ các lô hàng nhập vào kho.', permission: 'inventory_receipt_read' },
      issues: { label: 'Phiếu xuất kho', description: 'Xuất vật tư theo chứng từ, khóa tồn và bảo vệ không âm kho.', permission: 'inventory_issue_read' },
      stocktakes: { label: 'Kiểm kê', description: 'Mở phiên kiểm kê, nhập số đếm và ghi nhận điều chỉnh có audit.', permission: 'inventory_stocktake_read' },
      transfers: { label: 'Điều chuyển', description: 'Điều chuyển vật tư giữa các kho và vị trí theo một giao dịch.', permission: 'inventory_transfer_read' },
    },
  },
  production: {
    title: 'Quản lý sản xuất',
    icon: <ToolOutlined />,
    color: '#7657e8',
    features: {
      materials: { label: 'Định mức và vật tư', description: 'Quản lý BOM, phiên bản và nhu cầu nguyên vật liệu.', permission: 'production_bom_read' },
      orders: { label: 'Lệnh sản xuất', description: 'Phát hành, theo dõi tiến độ và hoàn tất lệnh sản xuất.', permission: 'production_order_read' },
      assignments: { label: 'Phân công nhân sự', description: 'Xếp nhân viên vào ca và lịch làm việc, có kiểm tra trùng lịch.', permission: 'production_assignment_read' },
      finished_products: { label: 'Sản lượng thành phẩm', description: 'Ghi nhận đạt/lỗi và bàn giao thành phẩm đạt sang Kho.', permission: 'production_output_read' },
    },
  },
};

function ModulePlaceholderPage({ module_key, feature_key }) {
  const { current_user } = use_auth();
  const module = feature_catalog[module_key] || feature_catalog.human_resources;
  const feature = module.features[feature_key] || Object.values(module.features)[0];

  if (!has_permission(current_user, feature.permission)) {
    return <Result status="403" title="Access denied." subTitle="Your account does not have permission to view this page." />;
  }

  return <div className="placeholder_page">
    <div className="placeholder_breadcrumb">
      <Button type="text" icon={<ArrowLeftOutlined />}><Link to={'/' + module_key}>Về tổng quan {module.title.toLowerCase()}</Link></Button>
      <Tag color="gold" icon={<FieldTimeOutlined />}>Đang hoàn thiện giao diện</Tag>
    </div>
    <Card className="placeholder_hero" bordered={false} style={{ '--module-accent': module.color }}>
      <div className="placeholder_hero_icon">{module.icon}</div>
      <div>
        <Typography.Text className="eyebrow">PHÂN HỆ {module.title.toUpperCase()}</Typography.Text>
        <Typography.Title level={1}>{feature.label}</Typography.Title>
        <Typography.Paragraph>{feature.description}</Typography.Paragraph>
        <Space wrap>
          <Tag color="green" icon={<CheckCircleFilled />}>Backend API sẵn sàng</Tag>
          <Tag icon={<LockOutlined />}>Kiểm tra quyền ở backend</Tag>
        </Space>
      </div>
    </Card>
    <Row gutter={[18, 18]} className="placeholder_grid">
      <Col xs={24} lg={15}>
        <Card className="placeholder_workflow" title="Luồng nghiệp vụ dự kiến" bordered={false}>
          <div className="workflow_step">
            <span className="workflow_index">01</span>
            <div><Typography.Text strong>Chọn dữ liệu và bộ lọc</Typography.Text><Typography.Paragraph>Danh sách sẽ phân trang trên server, hỗ trợ tìm kiếm và lọc theo quyền.</Typography.Paragraph></div>
          </div>
          <div className="workflow_step">
            <span className="workflow_index">02</span>
            <div><Typography.Text strong>Kiểm tra trước khi ghi</Typography.Text><Typography.Paragraph>Biểu mẫu hiển thị lỗi theo trường và trạng thái xử lý rõ ràng.</Typography.Paragraph></div>
          </div>
          <div className="workflow_step">
            <span className="workflow_index">03</span>
            <div><Typography.Text strong>Ghi nhận có dấu vết</Typography.Text><Typography.Paragraph>Thao tác quan trọng được backend kiểm tra quyền, transaction và audit.</Typography.Paragraph></div>
          </div>
        </Card>
      </Col>
      <Col xs={24} lg={9}>
        <Card className="placeholder_contract" bordered={false}>
          <RocketOutlined className="placeholder_contract_icon" />
          <Typography.Title level={4}>Sẵn sàng để triển khai UI</Typography.Title>
          <Typography.Paragraph>Dữ liệu của chức năng sẽ được tải từ nguồn trung tâm khi giao diện hoàn thiện. Bạn có thể quay lại tổng quan để mở một nghiệp vụ khác.</Typography.Paragraph>
          <Divider />
          <Typography.Text type="secondary">Trạng thái triển khai</Typography.Text>
          <Typography.Paragraph strong>Đang chuẩn bị màn hình nghiệp vụ</Typography.Paragraph>
          <Button type="primary" block><Link to={'/' + module_key}>Mở chức năng đã có</Link></Button>
        </Card>
      </Col>
    </Row>
  </div>;
}

export default ModulePlaceholderPage;
