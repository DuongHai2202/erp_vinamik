package vn.vinamik.erp_backend.inventory.stocktake;

import java.time.Instant;
import java.util.List;

public record stocktake_response(
        long stocktake_id,
        String stocktake_code,
        long warehouse_id,
        String warehouse_code,
        String status,
        Instant started_at,
        Instant submitted_at,
        Instant approved_at,
        Long approved_by_user_id,
        Instant posted_at,
        Long posted_by_user_id,
        String notes,
        List<stocktake_line_response> lines) {
}
