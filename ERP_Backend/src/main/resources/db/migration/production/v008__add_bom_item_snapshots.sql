ALTER TABLE production.bom
    ADD COLUMN product_item_code_snapshot varchar(60) NULL,
    ADD COLUMN product_item_name_snapshot varchar(180) NULL;

ALTER TABLE production.bom_line
    ADD COLUMN material_item_code_snapshot varchar(60) NULL,
    ADD COLUMN material_item_name_snapshot varchar(180) NULL;