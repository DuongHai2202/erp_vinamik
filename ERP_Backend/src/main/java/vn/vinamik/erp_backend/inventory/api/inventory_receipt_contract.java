package vn.vinamik.erp_backend.inventory.api;

public interface inventory_receipt_contract {
    inventory_receipt_result create_and_post(inventory_receipt_command command, long actor_user_id, String correlation_id);
}
