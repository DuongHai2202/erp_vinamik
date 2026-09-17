package vn.vinamik.erp_backend.inventory.master_data;

public record inventory_category_response(
        long item_category_id,
        String category_code,
        String category_name,
        String status) {
}
