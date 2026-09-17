INSERT INTO identity.role (role_code, display_name, description)
VALUES
    ('system_admin', 'Quản trị hệ thống', 'Toàn quyền cấu hình và vận hành các phân hệ đang triển khai.'),
    ('read_only', 'Chỉ xem dữ liệu', 'Chỉ xem dữ liệu thuộc phạm vi được cấp.'),
    ('hr_staff', 'Nhân viên nhân sự', 'Thực hiện nghiệp vụ nhân sự được cấp, không tự duyệt.'),
    ('hr_manager', 'Quản lý nhân sự', 'Thực hiện và duyệt nghiệp vụ nhân sự.'),
    ('inventory_staff', 'Nhân viên kho', 'Lập chứng từ kho được cấp, không tự điều chỉnh tồn.'),
    ('inventory_manager', 'Quản lý kho', 'Thực hiện nghiệp vụ kho và duyệt điều chỉnh tồn.'),
    ('production_staff', 'Nhân viên sản xuất', 'Lập và cập nhật kế hoạch, lệnh, phân công và kết quả.'),
    ('production_manager', 'Quản lý sản xuất', 'Thực hiện và duyệt nghiệp vụ sản xuất.')
ON CONFLICT (role_code) DO NOTHING;

INSERT INTO identity.permission (permission_code, module_code, action_code, description)
VALUES
    ('identity_user_read', 'identity', 'read', 'Xem tài khoản và vai trò.'),
    ('identity_user_create', 'identity', 'create', 'Tạo tài khoản.'),
    ('identity_user_update', 'identity', 'update', 'Cập nhật hoặc khóa tài khoản.'),
    ('identity_role_read', 'identity', 'read', 'Xem vai trò và quyền.'),
    ('identity_role_update', 'identity', 'update', 'Gán vai trò cho tài khoản.'),

    ('hr_employee_read', 'hr', 'read', 'Xem hồ sơ nhân viên.'),
    ('hr_employee_create', 'hr', 'create', 'Tạo hồ sơ nhân viên.'),
    ('hr_employee_update', 'hr', 'update', 'Cập nhật hồ sơ nhân viên.'),
    ('hr_employee_deactivate', 'hr', 'deactivate', 'Vô hiệu hóa hồ sơ nhân viên.'),
    ('hr_contract_read', 'hr', 'read', 'Xem hợp đồng lao động.'),
    ('hr_contract_create', 'hr', 'create', 'Tạo hợp đồng lao động.'),
    ('hr_contract_update', 'hr', 'update', 'Cập nhật hợp đồng lao động.'),
    ('hr_contract_approve', 'hr', 'approve', 'Duyệt hợp đồng lao động.'),
    ('hr_absence_read', 'hr', 'read', 'Xem đơn nghỉ và vắng mặt.'),
    ('hr_absence_create', 'hr', 'create', 'Tạo đơn nghỉ và vắng mặt.'),
    ('hr_absence_update', 'hr', 'update', 'Cập nhật đơn nghỉ và vắng mặt.'),
    ('hr_absence_approve', 'hr', 'approve', 'Duyệt đơn nghỉ và vắng mặt.'),
    ('hr_reward_read', 'hr', 'read', 'Xem quyết định thưởng và kỷ luật.'),
    ('hr_reward_create', 'hr', 'create', 'Tạo quyết định thưởng và kỷ luật.'),
    ('hr_reward_update', 'hr', 'update', 'Cập nhật quyết định thưởng và kỷ luật.'),
    ('hr_reward_approve', 'hr', 'approve', 'Duyệt quyết định thưởng và kỷ luật.'),
    ('hr_payroll_read', 'hr', 'read', 'Xem kỳ lương và bảng lương.'),
    ('hr_payroll_create', 'hr', 'create', 'Tạo kỳ lương và dữ liệu đầu vào.'),
    ('hr_payroll_update', 'hr', 'update', 'Cập nhật dữ liệu lương chưa khóa.'),
    ('hr_payroll_approve', 'hr', 'approve', 'Duyệt hoặc khóa bảng lương.'),
    ('hr_payroll_export', 'hr', 'export', 'Xuất bảng lương theo quyền.'),

    ('inventory_material_read', 'inventory', 'read', 'Xem nguyên vật liệu và mặt hàng kho.'),
    ('inventory_material_create', 'inventory', 'create', 'Tạo mặt hàng kho.'),
    ('inventory_material_update', 'inventory', 'update', 'Cập nhật mặt hàng kho.'),
    ('inventory_material_deactivate', 'inventory', 'deactivate', 'Ngừng sử dụng mặt hàng kho.'),
    ('inventory_receipt_read', 'inventory', 'read', 'Xem chứng từ nhập kho.'),
    ('inventory_receipt_create', 'inventory', 'create', 'Lập chứng từ nhập kho.'),
    ('inventory_receipt_post', 'inventory', 'post', 'Ghi sổ chứng từ nhập kho.'),
    ('inventory_issue_read', 'inventory', 'read', 'Xem chứng từ xuất kho.'),
    ('inventory_issue_create', 'inventory', 'create', 'Lập chứng từ xuất kho.'),
    ('inventory_issue_post', 'inventory', 'post', 'Ghi sổ chứng từ xuất kho.'),
    ('inventory_transfer_read', 'inventory', 'read', 'Xem chứng từ điều chuyển kho.'),
    ('inventory_transfer_create', 'inventory', 'create', 'Lập chứng từ điều chuyển kho.'),
    ('inventory_transfer_post', 'inventory', 'post', 'Ghi sổ chứng từ điều chuyển kho.'),
    ('inventory_stocktake_read', 'inventory', 'read', 'Xem phiên kiểm kê kho.'),
    ('inventory_stocktake_create', 'inventory', 'create', 'Tạo phiên kiểm kê kho.'),
    ('inventory_stocktake_adjust', 'inventory', 'adjust', 'Duyệt chênh lệch và điều chỉnh tồn kho.'),

    ('production_plan_read', 'production', 'read', 'Xem kế hoạch sản xuất.'),
    ('production_plan_create', 'production', 'create', 'Tạo kế hoạch sản xuất.'),
    ('production_plan_update', 'production', 'update', 'Cập nhật kế hoạch sản xuất.'),
    ('production_plan_approve', 'production', 'approve', 'Duyệt kế hoạch sản xuất.'),
    ('production_bom_read', 'production', 'read', 'Xem định mức nguyên vật liệu.'),
    ('production_bom_create', 'production', 'create', 'Tạo định mức nguyên vật liệu.'),
    ('production_bom_update', 'production', 'update', 'Cập nhật định mức nguyên vật liệu.'),
    ('production_bom_approve', 'production', 'approve', 'Duyệt phiên bản định mức nguyên vật liệu.'),
    ('production_order_read', 'production', 'read', 'Xem lệnh sản xuất.'),
    ('production_order_create', 'production', 'create', 'Tạo lệnh sản xuất.'),
    ('production_order_update', 'production', 'update', 'Cập nhật lệnh sản xuất.'),
    ('production_order_release', 'production', 'release', 'Phát hành lệnh sản xuất.'),
    ('production_order_complete', 'production', 'complete', 'Hoàn tất lệnh sản xuất.'),
    ('production_assignment_read', 'production', 'read', 'Xem phân công nhân sự sản xuất.'),
    ('production_assignment_create', 'production', 'create', 'Tạo phân công nhân sự sản xuất.'),
    ('production_assignment_update', 'production', 'update', 'Cập nhật phân công nhân sự sản xuất.'),
    ('production_output_read', 'production', 'read', 'Xem sản lượng thành phẩm.'),
    ('production_output_create', 'production', 'create', 'Ghi nhận kết quả thành phẩm.'),
    ('production_output_post', 'production', 'post', 'Gửi thành phẩm đạt sang Kho để nhập sổ.')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'system_admin'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'read_only'
  AND permission.action_code = 'read'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'hr_staff'
  AND permission.module_code = 'hr'
  AND permission.action_code IN ('read', 'create', 'update')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'hr_manager'
  AND permission.module_code = 'hr'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'inventory_staff'
  AND permission.module_code = 'inventory'
  AND permission.action_code IN ('read', 'create')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'inventory_manager'
  AND permission.module_code = 'inventory'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'production_staff'
  AND permission.module_code = 'production'
  AND permission.action_code IN ('read', 'create', 'update')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'production_manager'
  AND permission.module_code = 'production'
ON CONFLICT (role_id, permission_id) DO NOTHING;