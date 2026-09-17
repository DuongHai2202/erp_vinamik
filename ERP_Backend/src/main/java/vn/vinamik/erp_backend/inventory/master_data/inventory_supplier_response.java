package vn.vinamik.erp_backend.inventory.master_data;

public record inventory_supplier_response(
        long supplier_id,
        String supplier_code,
        String supplier_name,
        String phone_number,
        String email,
        String address,
        String status) {
}
