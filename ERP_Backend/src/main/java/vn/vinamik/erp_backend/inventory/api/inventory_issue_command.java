package vn.vinamik.erp_backend.inventory.api;

import java.util.List;

public record inventory_issue_command(
        String issue_code,
        long warehouse_id,
        String source_module,
        long source_document_id,
        String reason_code,
        String idempotency_key,
        String notes,
        List<inventory_issue_line_command> lines) {
}
