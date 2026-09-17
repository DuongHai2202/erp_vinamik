ALTER TABLE hr.payroll_period
    ADD COLUMN standard_working_days numeric(18, 6) NOT NULL DEFAULT 0,
    ADD COLUMN calculation_version varchar(40) NOT NULL DEFAULT 'monthly_mon_sat_v1',
    ADD COLUMN calculated_at timestamptz NULL,
    ADD COLUMN decision_note text NULL;

ALTER TABLE hr.payroll_period DROP CONSTRAINT ck_payroll_period_status;
ALTER TABLE hr.payroll_period
    ADD CONSTRAINT ck_payroll_period_status
        CHECK (status IN ('draft', 'calculated', 'approved', 'rejected', 'locked')),
    ADD CONSTRAINT ck_payroll_period_standard_working_days
        CHECK (standard_working_days > 0),
    ADD CONSTRAINT ck_payroll_period_month
        CHECK (starts_on = date_trunc('month', starts_on)::date
            AND ends_on = (date_trunc('month', starts_on) + interval '1 month - 1 day')::date);

ALTER TABLE hr.payroll_record
    ADD COLUMN currency_code_snapshot char(3) NOT NULL DEFAULT 'VND',
    ADD COLUMN standard_working_days_snapshot numeric(18, 6) NOT NULL DEFAULT 0,
    ADD COLUMN unpaid_leave_amount numeric(18, 2) NOT NULL DEFAULT 0,
    ADD COLUMN reward_amount numeric(18, 2) NOT NULL DEFAULT 0,
    ADD COLUMN discipline_amount numeric(18, 2) NOT NULL DEFAULT 0,
    ADD COLUMN calculation_version varchar(40) NOT NULL DEFAULT 'monthly_mon_sat_v1';

ALTER TABLE hr.payroll_record DROP CONSTRAINT ck_payroll_record_amounts;
ALTER TABLE hr.payroll_record DROP CONSTRAINT ck_payroll_record_status;
ALTER TABLE hr.payroll_record
    ADD CONSTRAINT ck_payroll_record_amounts
        CHECK (base_salary_snapshot >= 0
            AND standard_working_days_snapshot > 0
            AND unpaid_leave_days >= 0
            AND unpaid_leave_amount >= 0
            AND reward_amount >= 0
            AND discipline_amount >= 0
            AND gross_amount >= 0
            AND deduction_amount >= 0),
    ADD CONSTRAINT ck_payroll_record_status
        CHECK (status IN ('draft', 'calculated', 'approved', 'rejected', 'locked'));

ALTER TABLE hr.payroll_line
    ALTER COLUMN unit_amount TYPE numeric(18, 6);

CREATE UNIQUE INDEX uq_payroll_line_source
    ON hr.payroll_line (payroll_record_id, line_type, source_type, source_id)
    WHERE source_id IS NOT NULL;

CREATE OR REPLACE FUNCTION hr.reject_locked_payroll_period_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.status = 'locked' THEN
        RAISE EXCEPTION 'Locked payroll period is immutable';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END;
$$;

DROP TRIGGER IF EXISTS trg_reject_locked_payroll_period_mutation ON hr.payroll_period;
CREATE TRIGGER trg_reject_locked_payroll_period_mutation
BEFORE UPDATE OR DELETE ON hr.payroll_period
FOR EACH ROW EXECUTE FUNCTION hr.reject_locked_payroll_period_mutation();

CREATE OR REPLACE FUNCTION hr.reject_locked_payroll_record_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    target_period_id bigint;
BEGIN
    target_period_id := CASE WHEN TG_OP = 'DELETE' THEN OLD.payroll_period_id ELSE NEW.payroll_period_id END;
    IF EXISTS (
        SELECT 1 FROM hr.payroll_period
        WHERE payroll_period_id = target_period_id AND status = 'locked'
    ) THEN
        RAISE EXCEPTION 'Records in a locked payroll period are immutable';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END;
$$;

DROP TRIGGER IF EXISTS trg_reject_locked_payroll_record_mutation ON hr.payroll_record;
CREATE TRIGGER trg_reject_locked_payroll_record_mutation
BEFORE INSERT OR UPDATE OR DELETE ON hr.payroll_record
FOR EACH ROW EXECUTE FUNCTION hr.reject_locked_payroll_record_mutation();

CREATE OR REPLACE FUNCTION hr.reject_locked_payroll_line_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    target_record_id bigint;
BEGIN
    target_record_id := CASE WHEN TG_OP = 'DELETE' THEN OLD.payroll_record_id ELSE NEW.payroll_record_id END;
    IF EXISTS (
        SELECT 1
        FROM hr.payroll_record AS payroll_record
        JOIN hr.payroll_period AS payroll_period
          ON payroll_period.payroll_period_id = payroll_record.payroll_period_id
        WHERE payroll_record.payroll_record_id = target_record_id
          AND payroll_period.status = 'locked'
    ) THEN
        RAISE EXCEPTION 'Lines in a locked payroll period are immutable';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END;
$$;

DROP TRIGGER IF EXISTS trg_reject_locked_payroll_line_mutation ON hr.payroll_line;
CREATE TRIGGER trg_reject_locked_payroll_line_mutation
BEFORE INSERT OR UPDATE OR DELETE ON hr.payroll_line
FOR EACH ROW EXECUTE FUNCTION hr.reject_locked_payroll_line_mutation();

CREATE OR REPLACE FUNCTION hr.reject_locked_payroll_source_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    source_name text;
    source_identifier bigint;
BEGIN
    source_name := TG_ARGV[0];
    source_identifier := CASE source_name
        WHEN 'employment_contract' THEN OLD.employment_contract_id
        WHEN 'leave_request' THEN OLD.leave_request_id
        WHEN 'employee_reward_discipline' THEN OLD.employee_reward_discipline_id
    END;

    IF source_name = 'employment_contract' AND EXISTS (
        SELECT 1
        FROM hr.payroll_record AS payroll_record
        JOIN hr.payroll_period AS payroll_period
          ON payroll_period.payroll_period_id = payroll_record.payroll_period_id
        WHERE payroll_record.employment_contract_id = source_identifier
          AND payroll_period.status = 'locked'
    ) THEN
        RAISE EXCEPTION 'Payroll source is referenced by a locked period';
    END IF;

    IF source_name <> 'employment_contract' AND EXISTS (
        SELECT 1
        FROM hr.payroll_line AS payroll_line
        JOIN hr.payroll_record AS payroll_record
          ON payroll_record.payroll_record_id = payroll_line.payroll_record_id
        JOIN hr.payroll_period AS payroll_period
          ON payroll_period.payroll_period_id = payroll_record.payroll_period_id
        WHERE payroll_line.source_type = source_name
          AND payroll_line.source_id = source_identifier
          AND payroll_period.status = 'locked'
    ) THEN
        RAISE EXCEPTION 'Payroll source is referenced by a locked period';
    END IF;

    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END;
$$;

DROP TRIGGER IF EXISTS trg_reject_locked_contract_mutation ON hr.employment_contract;
CREATE TRIGGER trg_reject_locked_contract_mutation
BEFORE UPDATE OR DELETE ON hr.employment_contract
FOR EACH ROW EXECUTE FUNCTION hr.reject_locked_payroll_source_mutation('employment_contract');

DROP TRIGGER IF EXISTS trg_reject_locked_leave_mutation ON hr.leave_request;
CREATE TRIGGER trg_reject_locked_leave_mutation
BEFORE UPDATE OR DELETE ON hr.leave_request
FOR EACH ROW EXECUTE FUNCTION hr.reject_locked_payroll_source_mutation('leave_request');

DROP TRIGGER IF EXISTS trg_reject_locked_reward_mutation ON hr.employee_reward_discipline;
CREATE TRIGGER trg_reject_locked_reward_mutation
BEFORE UPDATE OR DELETE ON hr.employee_reward_discipline
FOR EACH ROW EXECUTE FUNCTION hr.reject_locked_payroll_source_mutation('employee_reward_discipline');

