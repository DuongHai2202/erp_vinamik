CREATE OR REPLACE FUNCTION identity.reject_audit_log_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION USING
        ERRCODE = '55000',
        MESSAGE = 'Audit log is append-only; corrective information must be recorded as a new event.';
END;
$$;

CREATE TRIGGER trg_audit_log_append_only
    BEFORE UPDATE OR DELETE ON identity.audit_log
    FOR EACH ROW
    EXECUTE FUNCTION identity.reject_audit_log_mutation();

REVOKE UPDATE, DELETE ON identity.audit_log FROM PUBLIC;
