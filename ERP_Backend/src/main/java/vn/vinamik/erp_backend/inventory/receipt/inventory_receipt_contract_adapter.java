package vn.vinamik.erp_backend.inventory.receipt;

import org.springframework.stereotype.Component;
import vn.vinamik.erp_backend.inventory.api.inventory_receipt_command;
import vn.vinamik.erp_backend.inventory.api.inventory_receipt_contract;
import vn.vinamik.erp_backend.inventory.api.inventory_receipt_line_command;
import vn.vinamik.erp_backend.inventory.api.inventory_receipt_line_result;
import vn.vinamik.erp_backend.inventory.api.inventory_receipt_result;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.util.List;

@Component
public class inventory_receipt_contract_adapter implements inventory_receipt_contract {
    private final inventory_receipt_service receipt_service;

    public inventory_receipt_contract_adapter(inventory_receipt_service receipt_service) {
        this.receipt_service = receipt_service;
    }

    @Override
    public inventory_receipt_result create_and_post(inventory_receipt_command command, long actor_user_id, String correlation_id) {
        authenticated_user actor = new authenticated_user(actor_user_id, "internal_production", List.of());
        receipt_request request = new receipt_request(command.receipt_code(), command.warehouse_id(), null,
                command.source_module(), command.source_document_id(), null, command.idempotency_key(), command.notes(),
                command.lines().stream().map(this::to_request).toList());
        receipt_response created = receipt_service.create(request, actor, correlation_id);
        receipt_response posted = receipt_service.post(created.receipt_id(), actor, correlation_id);
        return new inventory_receipt_result(posted.receipt_id(), posted.receipt_code(), posted.lines().stream()
                .map(line -> new inventory_receipt_line_result(line.receipt_line_id(), line.stock_item_id(), line.quantity()))
                .toList());
    }

    private receipt_line_request to_request(inventory_receipt_line_command line) {
        return new receipt_line_request(line.stock_item_id(), line.warehouse_location_id(), line.stock_lot_id(), line.quantity());
    }
}
