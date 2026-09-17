ALTER TABLE production.material_consumption
    ADD COLUMN line_number integer NULL;

ALTER TABLE production.material_consumption
    DROP CONSTRAINT uq_material_consumption_idempotency_key;

WITH ranked_consumptions AS (
    SELECT material_consumption_id,
           row_number() OVER (
               PARTITION BY production_order_id, regexp_replace(idempotency_key, ':[0-9]+$', '')
               ORDER BY material_consumption_id
           )::integer AS line_number,
           regexp_replace(idempotency_key, ':[0-9]+$', '') AS request_idempotency_key
    FROM production.material_consumption
)
UPDATE production.material_consumption AS consumption
SET idempotency_key = ranked_consumptions.request_idempotency_key,
    line_number = ranked_consumptions.line_number
FROM ranked_consumptions
WHERE ranked_consumptions.material_consumption_id = consumption.material_consumption_id;

ALTER TABLE production.material_consumption
    ALTER COLUMN line_number SET NOT NULL;

ALTER TABLE production.material_consumption
    ADD CONSTRAINT uq_material_consumption_request_line
        UNIQUE (production_order_id, idempotency_key, line_number),
    ADD CONSTRAINT ck_material_consumption_line_number
        CHECK (line_number > 0);

