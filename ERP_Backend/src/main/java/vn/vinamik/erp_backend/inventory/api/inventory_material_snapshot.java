package vn.vinamik.erp_backend.inventory.api;

public record inventory_material_snapshot(
        long stock_item_id,
        String item_code,
        String item_name,
        String item_type,
        String unit_code,
        String status) {
}