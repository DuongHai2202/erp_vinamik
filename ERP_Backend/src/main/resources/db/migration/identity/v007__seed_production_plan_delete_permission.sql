INSERT INTO identity.permission (permission_code, module_code, action_code, description)
VALUES ('production_plan_delete', 'production', 'delete', 'Delete a draft production plan.')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code IN ('system_admin', 'production_manager')
  AND permission.permission_code = 'production_plan_delete'
ON CONFLICT (role_id, permission_id) DO NOTHING;