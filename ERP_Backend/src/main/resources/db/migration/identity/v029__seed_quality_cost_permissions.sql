INSERT INTO identity.permission (permission_code, module_code, action_code, description)
VALUES
    ('quality_inspection_read', 'quality_cost', 'read', 'Xem kết quả kiểm tra chất lượng.'),
    ('quality_inspection_create', 'quality_cost', 'create', 'Tạo phiếu kiểm tra chất lượng.'),
    ('quality_inspection_update', 'quality_cost', 'update', 'Cập nhật phiếu kiểm tra chất lượng.'),
    ('quality_inspection_approve', 'quality_cost', 'approve', 'Duyệt kết quả kiểm tra chất lượng.'),
    ('quality_nonconformance_read', 'quality_cost', 'read', 'Xem sản phẩm không phù hợp.'),
    ('quality_nonconformance_create', 'quality_cost', 'create', 'Tạo hồ sơ sản phẩm không phù hợp.'),
    ('quality_nonconformance_update', 'quality_cost', 'update', 'Cập nhật xử lý sản phẩm không phù hợp.'),
    ('quality_nonconformance_approve', 'quality_cost', 'approve', 'Phê duyệt xử lý sản phẩm không phù hợp.'),
    ('cost_period_read', 'quality_cost', 'read', 'Xem kỳ và chính sách giá thành.'),
    ('cost_period_create', 'quality_cost', 'create', 'Tạo kỳ tính giá thành.'),
    ('cost_period_update', 'quality_cost', 'update', 'Cập nhật kỳ tính giá thành.'),
    ('cost_period_approve', 'quality_cost', 'approve', 'Duyệt hoặc khóa kỳ tính giá thành.'),
    ('cost_calculation_read', 'quality_cost', 'read', 'Xem kết quả tính giá thành.'),
    ('cost_calculation_create', 'quality_cost', 'create', 'Tạo kết quả tính giá thành.'),
    ('cost_calculation_update', 'quality_cost', 'update', 'Điều chỉnh kết quả tính giá thành chưa khóa.'),
    ('cost_calculation_approve', 'quality_cost', 'approve', 'Duyệt hoặc khóa kết quả giá thành.'),
    ('price_proposal_read', 'quality_cost', 'read', 'Xem đề xuất giá bán.'),
    ('price_proposal_create', 'quality_cost', 'create', 'Tạo đề xuất giá bán.'),
    ('price_proposal_update', 'quality_cost', 'update', 'Cập nhật đề xuất giá bán.'),
    ('price_proposal_approve', 'quality_cost', 'approve', 'Duyệt và công bố bảng giá.')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'system_admin'
  AND permission.module_code = 'quality_cost'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'read_only'
  AND permission.module_code = 'quality_cost'
  AND permission.action_code = 'read'
ON CONFLICT (role_id, permission_id) DO NOTHING;
