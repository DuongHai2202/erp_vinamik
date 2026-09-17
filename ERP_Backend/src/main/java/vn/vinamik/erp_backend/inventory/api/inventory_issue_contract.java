package vn.vinamik.erp_backend.inventory.api;

public interface inventory_issue_contract {
    inventory_issue_result create_and_post(inventory_issue_command command, long actor_user_id, String correlation_id);
}
