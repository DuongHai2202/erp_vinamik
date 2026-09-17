package vn.vinamik.erp_backend.inventory.master_data;

public record inventory_supplier_lookup_response(
        long supplier_id,
        String supplier_code,
        String supplier_name) {
}
