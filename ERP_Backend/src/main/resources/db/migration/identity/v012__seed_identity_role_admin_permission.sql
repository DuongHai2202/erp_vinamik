INSERT INTO identity.permission (permission_code, module_code, action_code, description)
VALUES ('identity_role_admin', 'identity', 'admin', 'Manage system administrator role assignments.')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'system_admin'
  AND permission.permission_code = 'identity_role_admin'
ON CONFLICT (role_id, permission_id) DO NOTHING;
