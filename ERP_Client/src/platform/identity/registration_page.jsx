import { Alert, Button, Card, Form, Input, Typography } from 'antd';
import { LockOutlined, MailOutlined, IdcardOutlined, UserOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import { useState } from 'react';
import { request_api } from '../common/api_client';

function RegistrationPage() {
  const [form] = Form.useForm();
  const [is_submitting, set_is_submitting] = useState(false);
  const [error_message, set_error_message] = useState('');
  const [is_submitted, set_is_submitted] = useState(false);

  const on_finish = async (values) => {
    set_is_submitting(true);
    set_error_message('');
    try {
      await request_api('/api/v1/auth/registration-requests', {
        method: 'POST',
        body: JSON.stringify({
          full_name: values.full_name,
          work_email: values.work_email,
          employee_code: values.employee_code || null,
          username: values.username,
          password: values.password,
        }),
      });
      set_is_submitted(true);
      form.resetFields();
    } catch (error) {
      set_error_message(error.message || 'Registration request could not be submitted.');
    } finally {
      set_is_submitting(false);
    }
  };

  return <main className="identity_page registration_page"><div className="identity_backdrop" /><Card className="registration_card" bordered={false}>
    <div className="login_brand"><div className="brand_mark large">V</div><div><Typography.Title level={2}>Vinamik</Typography.Title><Typography.Text>Điều phối vận hành doanh nghiệp</Typography.Text></div></div>
    <div className="login_heading"><Typography.Title level={3}>Đăng ký cấp tài khoản</Typography.Title><Typography.Paragraph>Gửi thông tin để quản trị viên xác minh nhân sự và cấp quyền truy cập phù hợp.</Typography.Paragraph></div>
    {is_submitted && <Alert type="success" showIcon message="Registration request submitted for review." description="Quản trị viên sẽ kiểm tra thông tin trước khi kích hoạt tài khoản." className="login_alert" />}
    {error_message && <Alert type="error" showIcon message={error_message} className="login_alert" />}
    {!is_submitted && <Form form={form} layout="vertical" requiredMark={false} onFinish={on_finish}>
      <Form.Item label="Họ và tên" name="full_name" rules={[{ required: true, message: 'Full name is required.' }, { max: 160, message: 'Full name is too long.' }]}><Input size="large" prefix={<IdcardOutlined />} placeholder="Nhập họ và tên" autoComplete="name" /></Form.Item>
      <Form.Item label="Email công ty" name="work_email" rules={[{ required: true, message: 'Work email is required.' }, { type: 'email', message: 'Work email is invalid.' }]}><Input size="large" prefix={<MailOutlined />} placeholder="name@company.com" autoComplete="email" /></Form.Item>
      <Form.Item label="Mã nhân viên (nếu có)" name="employee_code"><Input size="large" prefix={<IdcardOutlined />} placeholder="Ví dụ: vmk0001" autoComplete="organization-title" /></Form.Item>
      <Form.Item label="Tên đăng nhập" name="username" rules={[{ required: true, message: 'Username is required.' }, { pattern: /^[a-zA-Z0-9][a-zA-Z0-9._-]{2,79}$/, message: 'Username format is invalid.' }]}><Input size="large" prefix={<UserOutlined />} placeholder="Nhập tên đăng nhập" autoComplete="username" /></Form.Item>
      <Form.Item label="Mật khẩu" name="password" rules={[{ required: true, message: 'Password is required.' }, { min: 15, message: 'Password must contain at least 15 characters.' }]}><Input.Password size="large" prefix={<LockOutlined />} placeholder="Tối thiểu 15 ký tự" autoComplete="new-password" /></Form.Item>
      <Form.Item label="Nhập lại mật khẩu" name="confirm_password" dependencies={['password']} rules={[{ required: true, message: 'Password confirmation is required.' }, ({ getFieldValue }) => ({ validator(_, value) { return !value || getFieldValue('password') === value ? Promise.resolve() : Promise.reject(new Error('Passwords do not match.')); } })]}><Input.Password size="large" prefix={<LockOutlined />} placeholder="Nhập lại mật khẩu" autoComplete="new-password" /></Form.Item>
      <Button type="primary" htmlType="submit" size="large" block loading={is_submitting}>Gửi yêu cầu</Button>
    </Form>}
    <div className="identity_page_footer"><Link to="/login">Quay lại đăng nhập</Link></div>
  </Card></main>;
}

export default RegistrationPage;
