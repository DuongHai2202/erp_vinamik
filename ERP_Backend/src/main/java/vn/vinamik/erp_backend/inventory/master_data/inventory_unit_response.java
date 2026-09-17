package vn.vinamik.erp_backend.inventory.master_data;

public record inventory_unit_response(
        long unit_of_measure_id,
        String unit_code,
        String unit_name,
        short decimal_places,
        String status) {
}
