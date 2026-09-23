INSERT INTO identity.permission (permission_code, module_code, action_code, description)
VALUES ('hr_contract_delete', 'hr', 'delete', 'Xóa hợp đồng lao động bản nháp.')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code IN ('system_admin', 'hr_manager')
  AND permission.permission_code = 'hr_contract_delete'
ON CONFLICT (role_id, permission_id) DO NOTHING;
