-- Allow payroll rejection to follow the documented review lifecycle.
ALTER TABLE hr.payroll_period
    DROP CONSTRAINT IF EXISTS ck_payroll_period_status;

ALTER TABLE hr.payroll_period
    ADD CONSTRAINT ck_payroll_period_status
    CHECK (status IN ('draft', 'calculated', 'approved', 'rejected', 'locked'));

ALTER TABLE hr.payroll_record
    DROP CONSTRAINT IF EXISTS ck_payroll_record_status;

ALTER TABLE hr.payroll_record
    ADD CONSTRAINT ck_payroll_record_status
    CHECK (status IN ('draft', 'calculated', 'approved', 'rejected', 'locked'));
