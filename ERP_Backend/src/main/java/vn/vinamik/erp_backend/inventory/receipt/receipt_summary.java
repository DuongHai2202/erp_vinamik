package vn.vinamik.erp_backend.inventory.receipt;

import java.time.Instant;

public record receipt_summary(
        long receipt_id,
        String receipt_code,
        long warehouse_id,
        String warehouse_code,
        String status,
        int line_count,
        Instant created_at,
        Instant posted_at) {
}
