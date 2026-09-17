package vn.vinamik.erp_backend.inventory.stocktake;

import java.time.Instant;

public record stocktake_summary(
        long stocktake_id,
        String stocktake_code,
        long warehouse_id,
        String warehouse_code,
        String status,
        int line_count,
        int counted_line_count,
        Instant started_at,
        Instant posted_at) {
}
