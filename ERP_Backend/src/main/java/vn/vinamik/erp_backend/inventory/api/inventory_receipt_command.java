package vn.vinamik.erp_backend.inventory.api;

import java.util.List;

public record inventory_receipt_command(
        String receipt_code,
        long warehouse_id,
        String source_module,
        long source_document_id,
        String idempotency_key,
        String notes,
        List<inventory_receipt_line_command> lines) {
}
