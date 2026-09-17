CREATE OR REPLACE FUNCTION identity.protect_super_admin_role()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    system_admin_role_id bigint;
    protected_user boolean;
BEGIN
    SELECT role_id
    INTO system_admin_role_id
    FROM identity.role
    WHERE role_code = 'system_admin';

    SELECT is_super_admin
    INTO protected_user
    FROM identity.user_account
    WHERE user_id = OLD.user_id;

    IF COALESCE(protected_user, false)
            AND OLD.role_id = system_admin_role_id
            AND (
                TG_OP = 'DELETE'
                OR (TG_OP = 'UPDATE' AND (
                    NEW.user_id <> OLD.user_id
                    OR NEW.role_id <> OLD.role_id
                ))
            ) THEN
        RAISE EXCEPTION 'Super admin account must retain system_admin role';
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_user_role_protect_super_admin
    BEFORE UPDATE OR DELETE ON identity.user_role
    FOR EACH ROW
    EXECUTE FUNCTION identity.protect_super_admin_role();
