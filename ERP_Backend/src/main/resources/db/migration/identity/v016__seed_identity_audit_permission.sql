INSERT INTO identity.permission (permission_code, module_code, action_code, description)
VALUES ('identity_audit_read', 'identity', 'read', 'Read append-only audit events for operational investigation.')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'system_admin'
  AND permission.permission_code = 'identity_audit_read'
ON CONFLICT (role_id, permission_id) DO NOTHING;
