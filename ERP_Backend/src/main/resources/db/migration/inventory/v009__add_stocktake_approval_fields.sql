ALTER TABLE inventory.stocktake
    ADD COLUMN approved_at timestamptz NULL,
    ADD COLUMN approved_by_user_id bigint NULL;

CREATE INDEX ix_stocktake_approval_status
    ON inventory.stocktake (status, approved_at);
