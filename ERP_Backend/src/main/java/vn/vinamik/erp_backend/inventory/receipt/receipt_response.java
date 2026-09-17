package vn.vinamik.erp_backend.inventory.receipt;

import java.time.Instant;
import java.util.List;

public record receipt_response(
        long receipt_id,
        String receipt_code,
        long warehouse_id,
        String warehouse_code,
        Long supplier_id,
        String supplier_code,
        String source_module,
        Long source_document_id,
        String reference_number,
        String status,
        String idempotency_key,
        String notes,
        Instant posted_at,
        Long posted_by_user_id,
        List<receipt_line_response> lines) {
}
