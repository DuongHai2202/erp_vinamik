import { Alert, Button, Card, Form, Input, Typography } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { use_auth } from './auth_context';

function LoginPage() {
  const { login } = use_auth();
  const [is_submitting, set_is_submitting] = useState(false);
  const [error_message, set_error_message] = useState('');
  const navigate = useNavigate();
  const location = useLocation();

  const on_finish = async (values) => {
    set_is_submitting(true);
    set_error_message('');
    try {
      await login(values.username, values.password);
      navigate(location.state?.from || '/', { replace: true });
    } catch (error) {
      set_error_message(error.message || 'Login failed.');
    } finally {
      set_is_submitting(false);
    }
  };

  return <main className="login_page"><div className="login_backdrop" /><Card className="login_card" bordered={false}>
    <div className="login_brand"><div className="brand_mark large">V</div><div><Typography.Title level={2}>Vinamik</Typography.Title><Typography.Text>Điều phối vận hành doanh nghiệp</Typography.Text></div></div>
    <div className="login_heading"><Typography.Title level={3}>Đăng nhập</Typography.Title><Typography.Paragraph>Đăng nhập để truy cập các phân hệ được cấp quyền.</Typography.Paragraph></div>
    {error_message && <Alert type="error" showIcon message={error_message} className="login_alert" />}
    <Form layout="vertical" requiredMark={false} onFinish={on_finish}>
      <Form.Item label="Tên đăng nhập" name="username" rules={[{ required: true, message: 'Username is required.' }]}><Input size="large" autoComplete="username" prefix={<UserOutlined />} placeholder="Nhập tên đăng nhập" /></Form.Item>
      <Form.Item label="Mật khẩu" name="password" rules={[{ required: true, message: 'Password is required.' }]}><Input.Password size="large" autoComplete="current-password" prefix={<LockOutlined />} placeholder="Nhập mật khẩu" /></Form.Item>
      <Button type="primary" htmlType="submit" size="large" block loading={is_submitting}>Đăng nhập</Button>
    </Form>
    <div className="identity_page_footer"><span>Chưa có tài khoản?</span><Link to="/register">Đăng ký cấp tài khoản</Link></div>
  </Card></main>;
}
export default LoginPage;
