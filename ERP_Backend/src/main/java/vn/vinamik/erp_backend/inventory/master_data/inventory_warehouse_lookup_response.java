package vn.vinamik.erp_backend.inventory.master_data;

public record inventory_warehouse_lookup_response(
        long warehouse_id,
        String warehouse_code,
        String warehouse_name) {
}
