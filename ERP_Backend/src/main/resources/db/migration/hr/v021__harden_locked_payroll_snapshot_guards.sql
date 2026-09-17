CREATE OR REPLACE FUNCTION hr.reject_locked_payroll_record_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' AND EXISTS (
        SELECT 1 FROM hr.payroll_period
        WHERE payroll_period_id = NEW.payroll_period_id AND status = 'locked'
    ) THEN
        RAISE EXCEPTION 'Records in a locked payroll period are immutable';
    END IF;

    IF TG_OP = 'DELETE' AND EXISTS (
        SELECT 1 FROM hr.payroll_period
        WHERE payroll_period_id = OLD.payroll_period_id AND status = 'locked'
    ) THEN
        RAISE EXCEPTION 'Records in a locked payroll period are immutable';
    END IF;

    IF TG_OP = 'UPDATE' AND EXISTS (
        SELECT 1 FROM hr.payroll_period
        WHERE payroll_period_id IN (OLD.payroll_period_id, NEW.payroll_period_id)
          AND status = 'locked'
    ) THEN
        RAISE EXCEPTION 'Records in a locked payroll period are immutable';
    END IF;

    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END;
$$;

CREATE OR REPLACE FUNCTION hr.reject_locked_payroll_line_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' AND EXISTS (
        SELECT 1
        FROM hr.payroll_record AS payroll_record
        JOIN hr.payroll_period AS payroll_period
          ON payroll_period.payroll_period_id = payroll_record.payroll_period_id
        WHERE payroll_record.payroll_record_id = NEW.payroll_record_id
          AND payroll_period.status = 'locked'
    ) THEN
        RAISE EXCEPTION 'Lines in a locked payroll period are immutable';
    END IF;

    IF TG_OP = 'DELETE' AND EXISTS (
        SELECT 1
        FROM hr.payroll_record AS payroll_record
        JOIN hr.payroll_period AS payroll_period
          ON payroll_period.payroll_period_id = payroll_record.payroll_period_id
        WHERE payroll_record.payroll_record_id = OLD.payroll_record_id
          AND payroll_period.status = 'locked'
    ) THEN
        RAISE EXCEPTION 'Lines in a locked payroll period are immutable';
    END IF;

    IF TG_OP = 'UPDATE' AND EXISTS (
        SELECT 1
        FROM hr.payroll_record AS payroll_record
        JOIN hr.payroll_period AS payroll_period
          ON payroll_period.payroll_period_id = payroll_record.payroll_period_id
        WHERE payroll_record.payroll_record_id IN (OLD.payroll_record_id, NEW.payroll_record_id)
          AND payroll_period.status = 'locked'
    ) THEN
        RAISE EXCEPTION 'Lines in a locked payroll period are immutable';
    END IF;

    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END;
$$;
