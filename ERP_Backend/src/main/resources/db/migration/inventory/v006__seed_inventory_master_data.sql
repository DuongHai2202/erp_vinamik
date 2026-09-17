INSERT INTO inventory.unit_of_measure (unit_code, unit_name, decimal_places)
VALUES
    ('kg', 'Kilogram', 3),
    ('litre', 'Litre', 3),
    ('piece', 'Piece', 0)
ON CONFLICT (unit_code) DO NOTHING;

INSERT INTO inventory.item_category (category_code, category_name)
VALUES
    ('raw_material', 'Raw material'),
    ('packaging', 'Packaging material'),
    ('finished_product', 'Finished product')
ON CONFLICT (category_code) DO NOTHING;