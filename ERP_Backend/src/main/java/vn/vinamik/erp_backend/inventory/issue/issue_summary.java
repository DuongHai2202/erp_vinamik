package vn.vinamik.erp_backend.inventory.issue;

import java.time.Instant;

public record issue_summary(
        long issue_id,
        String issue_code,
        long warehouse_id,
        String warehouse_code,
        String status,
        int line_count,
        Instant created_at,
        Instant posted_at) {
}
