import { Result } from 'antd';
import { Link } from 'react-router-dom';

function NotFoundPage() {
  return <Result
    status="404"
    title="Page not found."
    subTitle="The page you requested does not exist or is no longer available."
    extra={<Link className="ant-btn ant-btn-primary" to="/">Về tổng quan</Link>}
  />;
}

export default NotFoundPage;
