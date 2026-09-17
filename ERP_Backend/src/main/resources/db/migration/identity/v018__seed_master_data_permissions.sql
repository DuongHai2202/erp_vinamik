INSERT INTO identity.permission (permission_code, module_code, action_code, description)
VALUES
    ('hr_master_read', 'hr', 'read', 'Xem danh mục phòng ban, chức danh và ca làm việc.'),
    ('hr_master_create', 'hr', 'create', 'Tạo danh mục phòng ban, chức danh và ca làm việc.'),
    ('hr_master_update', 'hr', 'update', 'Cập nhật danh mục phòng ban, chức danh và ca làm việc.'),
    ('hr_master_deactivate', 'hr', 'deactivate', 'Vô hiệu hóa danh mục phòng ban, chức danh và ca làm việc.'),
    ('inventory_master_read', 'inventory', 'read', 'Xem đơn vị, nhóm hàng, nhà cung cấp, kho và vị trí kho.'),
    ('inventory_master_create', 'inventory', 'create', 'Tạo đơn vị, nhóm hàng, nhà cung cấp, kho và vị trí kho.'),
    ('inventory_master_update', 'inventory', 'update', 'Cập nhật đơn vị, nhóm hàng, nhà cung cấp, kho và vị trí kho.'),
    ('inventory_master_deactivate', 'inventory', 'deactivate', 'Vô hiệu hóa đơn vị, nhóm hàng, nhà cung cấp, kho và vị trí kho.')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'system_admin'
  AND permission.permission_code IN (
      'hr_master_read', 'hr_master_create', 'hr_master_update', 'hr_master_deactivate',
      'inventory_master_read', 'inventory_master_create', 'inventory_master_update',
      'inventory_master_deactivate')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'read_only'
  AND permission.permission_code IN ('hr_master_read', 'inventory_master_read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code IN ('hr_staff', 'hr_manager')
  AND permission.permission_code LIKE 'hr_master_%'
  AND (role.role_code = 'hr_manager' OR permission.action_code IN ('read', 'create', 'update'))
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code IN ('inventory_staff', 'inventory_manager')
  AND permission.permission_code LIKE 'inventory_master_%'
  AND (role.role_code = 'inventory_manager' OR permission.action_code IN ('read', 'create'))
ON CONFLICT (role_id, permission_id) DO NOTHING;
