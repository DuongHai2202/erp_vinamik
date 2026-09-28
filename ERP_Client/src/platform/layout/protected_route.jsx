import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { Spin } from 'antd';
import { use_auth } from '../identity/auth_context';

function ProtectedRoute() {
  const { current_user, is_loading } = use_auth();
  const location = useLocation();
  if (is_loading) {
    return <div className="full_page_center" role="status" aria-live="polite"><Spin size="large" /><span>Đang kiểm tra phiên đăng nhập...</span></div>;
  }
  if (!current_user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <Outlet />;
}

export default ProtectedRoute;
