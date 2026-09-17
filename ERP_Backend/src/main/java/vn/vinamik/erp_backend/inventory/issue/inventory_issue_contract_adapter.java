package vn.vinamik.erp_backend.inventory.issue;

import org.springframework.stereotype.Component;
import vn.vinamik.erp_backend.inventory.api.inventory_issue_command;
import vn.vinamik.erp_backend.inventory.api.inventory_issue_contract;
import vn.vinamik.erp_backend.inventory.api.inventory_issue_line_command;
import vn.vinamik.erp_backend.inventory.api.inventory_issue_line_result;
import vn.vinamik.erp_backend.inventory.api.inventory_issue_result;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.util.List;

@Component
public class inventory_issue_contract_adapter implements inventory_issue_contract {
    private final inventory_issue_service issue_service;

    public inventory_issue_contract_adapter(inventory_issue_service issue_service) {
        this.issue_service = issue_service;
    }

    @Override
    public inventory_issue_result create_and_post(inventory_issue_command command, long actor_user_id, String correlation_id) {
        authenticated_user actor = new authenticated_user(actor_user_id, "internal_production", List.of());
        issue_request request = new issue_request(command.issue_code(), command.warehouse_id(), command.source_module(),
                command.source_document_id(), command.reason_code(), command.idempotency_key(), command.notes(),
                command.lines().stream().map(this::to_request).toList());
        issue_response created = issue_service.create(request, actor, correlation_id);
        issue_response posted = issue_service.post(created.issue_id(), actor, correlation_id);
        return new inventory_issue_result(posted.issue_id(), posted.issue_code(), posted.lines().stream()
                .map(line -> new inventory_issue_line_result(line.issue_line_id(), line.stock_item_id(), line.quantity()))
                .toList());
    }

    private issue_line_request to_request(inventory_issue_line_command line) {
        return new issue_line_request(line.stock_item_id(), line.warehouse_location_id(), line.stock_lot_id(), line.quantity());
    }
}
