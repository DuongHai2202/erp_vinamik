DROP TRIGGER IF EXISTS trg_reject_locked_contract_mutation ON hr.employment_contract;
DROP TRIGGER IF EXISTS trg_reject_locked_leave_mutation ON hr.leave_request;
DROP TRIGGER IF EXISTS trg_reject_locked_reward_mutation ON hr.employee_reward_discipline;

DROP FUNCTION IF EXISTS hr.reject_locked_payroll_source_mutation();

COMMENT ON TABLE hr.payroll_record IS
    'Immutable payroll calculation snapshot after its payroll period is locked';

COMMENT ON TABLE hr.payroll_line IS
    'Immutable payroll input breakdown snapshot after its payroll period is locked';
