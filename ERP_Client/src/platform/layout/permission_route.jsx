import { Result } from 'antd';
import { has_permission } from '../common/permission_utils';
import { use_auth } from '../identity/auth_context';

function PermissionRoute({ permission_code, children }) {
  const { current_user } = use_auth();
  if (!has_permission(current_user, permission_code)) {
    return <Result status="403" title="Access denied." subTitle="Your account does not have permission to view this page." />;
  }
  return children;
}

export default PermissionRoute;