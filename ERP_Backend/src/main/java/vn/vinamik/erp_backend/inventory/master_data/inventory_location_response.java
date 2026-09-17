package vn.vinamik.erp_backend.inventory.master_data;

public record inventory_location_response(
        long warehouse_location_id,
        long warehouse_id,
        String location_code,
        String location_name,
        String status) {
}
