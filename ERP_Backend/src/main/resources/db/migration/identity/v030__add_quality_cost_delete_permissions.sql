INSERT INTO identity.permission (permission_code, module_code, action_code, description)
VALUES
    ('quality_inspection_delete', 'quality_cost', 'delete', 'Delete draft quality inspection.'),
    ('quality_nonconformance_delete', 'quality_cost', 'delete', 'Delete open nonconformance.'),
    ('cost_period_delete', 'quality_cost', 'delete', 'Delete draft cost period.'),
    ('cost_calculation_delete', 'quality_cost', 'delete', 'Delete unapproved cost result.'),
    ('price_proposal_delete', 'quality_cost', 'delete', 'Delete draft price proposal.')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT role.role_id, permission.permission_id
FROM identity.role AS role
CROSS JOIN identity.permission AS permission
WHERE role.role_code = 'system_admin'
  AND permission.permission_code IN (
      'quality_inspection_delete',
      'quality_nonconformance_delete',
      'cost_period_delete',
      'cost_calculation_delete',
      'price_proposal_delete'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;
