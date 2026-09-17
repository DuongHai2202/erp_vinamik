ALTER TABLE identity.user_account
    ADD COLUMN is_super_admin boolean NOT NULL DEFAULT false,
    ADD CONSTRAINT ck_user_account_super_admin_active
        CHECK (NOT is_super_admin OR status = 'active');

CREATE UNIQUE INDEX uq_user_account_single_super_admin
    ON identity.user_account (is_super_admin)
    WHERE is_super_admin;

-- Preserve the first existing account when this migration upgrades a database
-- that predates the protected super-admin invariant.
UPDATE identity.user_account
SET is_super_admin = true
WHERE user_id = (
    SELECT min(user_id)
    FROM identity.user_account
    WHERE NOT EXISTS (
        SELECT 1
        FROM identity.user_account
        WHERE is_super_admin
    )
);

CREATE OR REPLACE FUNCTION identity.protect_super_admin_account()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' AND OLD.is_super_admin THEN
        RAISE EXCEPTION 'Super admin account cannot be deleted';
    END IF;

    IF TG_OP = 'UPDATE' AND OLD.is_super_admin
            AND (NEW.is_super_admin = false OR NEW.status <> 'active') THEN
        RAISE EXCEPTION 'Super admin account must remain active and protected';
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_user_account_protect_super_admin
    BEFORE UPDATE OR DELETE ON identity.user_account
    FOR EACH ROW
    EXECUTE FUNCTION identity.protect_super_admin_account();
