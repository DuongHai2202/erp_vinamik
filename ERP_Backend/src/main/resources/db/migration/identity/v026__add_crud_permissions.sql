INSERT INTO identity.permission (permission_code, module_code, action_code, description)
VALUES
    ('hr_reward_delete', 'hr', 'delete', 'Xóa bản nháp quyết định thưởng và kỷ luật.'),
    ('hr_payroll_delete', 'hr', 'delete', 'Xóa kỳ lương bản nháp.'),
    ('inventory_receipt_update', 'inventory', 'update', 'Cập nhật hoặc hủy chứng từ nhập kho chưa ghi sổ.'),
    ('inventory_receipt_delete', 'inventory', 'delete', 'Xóa bản nháp chứng từ nhập kho.'),
    ('inventory_issue_update', 'inventory', 'update', 'Cập nhật hoặc hủy chứng từ xuất kho chưa ghi sổ.'),
    ('inventory_issue_delete', 'inventory', 'delete', 'Xóa bản nháp chứng từ xuất kho.'),
    ('inventory_transfer_update', 'inventory', 'update', 'Cập nhật hoặc hủy chứng từ điều chuyển chưa ghi sổ.'),
    ('inventory_transfer_delete', 'inventory', 'delete', 'Xóa bản nháp chứng từ điều chuyển.'),
    ('production_bom_delete', 'production', 'delete', 'Xóa bản nháp định mức nguyên vật liệu.'),
    ('production_output_update', 'production', 'update', 'Cập nhật hoặc hủy sản lượng chưa nhập kho.'),
    ('production_output_delete', 'production', 'delete', 'Xóa bản nháp sản lượng chưa nhập kho.')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'hr_manager'
  AND permission.permission_code = 'hr_reward_delete'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'inventory_staff'
  AND permission.permission_code IN ('inventory_receipt_update', 'inventory_issue_update', 'inventory_transfer_update')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'inventory_manager'
  AND permission.permission_code IN ('inventory_receipt_update', 'inventory_receipt_delete', 'inventory_issue_update', 'inventory_issue_delete', 'inventory_transfer_update', 'inventory_transfer_delete')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'production_staff'
  AND permission.permission_code = 'production_output_update'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'production_manager'
  AND permission.permission_code IN ('production_output_update', 'production_output_delete')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'hr_manager'
  AND permission.permission_code = 'hr_payroll_delete'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'production_manager'
  AND permission.permission_code = 'production_bom_delete'
ON CONFLICT (role_id, permission_id) DO NOTHING;
