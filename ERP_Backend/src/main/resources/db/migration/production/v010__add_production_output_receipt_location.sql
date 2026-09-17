ALTER TABLE production.production_output
    ADD COLUMN inventory_warehouse_id bigint NULL,
    ADD COLUMN inventory_warehouse_location_id bigint NULL;

ALTER TABLE production.production_output
    ADD CONSTRAINT ck_production_output_receipt_location_pair
    CHECK ((inventory_warehouse_id IS NULL) = (inventory_warehouse_location_id IS NULL));
