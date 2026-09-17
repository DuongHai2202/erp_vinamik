CREATE OR REPLACE FUNCTION inventory.reject_stock_movement_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION USING
        ERRCODE = '55000',
        MESSAGE = 'Stock movement ledger is append-only; create a reversal movement instead.';
END;
$$;

CREATE TRIGGER trg_stock_movement_append_only
    BEFORE UPDATE OR DELETE ON inventory.stock_movement
    FOR EACH ROW
    EXECUTE FUNCTION inventory.reject_stock_movement_mutation();

REVOKE UPDATE, DELETE ON inventory.stock_movement FROM PUBLIC;