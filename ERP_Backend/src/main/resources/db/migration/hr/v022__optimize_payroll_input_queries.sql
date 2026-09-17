CREATE INDEX ix_contract_active_period
    ON hr.employment_contract (effective_from, effective_to, employee_id)
    INCLUDE (employment_contract_id, base_salary, currency_code)
    WHERE status = 'active';

CREATE INDEX ix_leave_approved_unpaid_period
    ON hr.leave_request (starts_on, ends_on, employee_id)
    INCLUDE (leave_request_id, request_code)
    WHERE status = 'approved' AND is_paid = false;

CREATE INDEX ix_reward_approved_period
    ON hr.employee_reward_discipline (effective_on, employee_id)
    INCLUDE (employee_reward_discipline_id, event_type, record_code, amount, currency_code)
    WHERE status = 'approved' AND amount > 0;

CREATE INDEX ix_payroll_period_status_start
    ON hr.payroll_period (status, starts_on DESC);
