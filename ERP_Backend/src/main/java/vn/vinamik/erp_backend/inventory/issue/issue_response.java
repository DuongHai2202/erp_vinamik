package vn.vinamik.erp_backend.inventory.issue;

import java.time.Instant;
import java.util.List;

public record issue_response(
        long issue_id,
        String issue_code,
        long warehouse_id,
        String warehouse_code,
        String source_module,
        Long source_document_id,
        String reason_code,
        String status,
        String idempotency_key,
        String notes,
        Instant posted_at,
        Long posted_by_user_id,
        List<issue_line_response> lines) {
}
