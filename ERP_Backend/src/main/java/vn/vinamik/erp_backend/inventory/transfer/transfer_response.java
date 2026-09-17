package vn.vinamik.erp_backend.inventory.transfer;

import java.time.Instant;
import java.util.List;

public record transfer_response(
        long transfer_id,
        String transfer_code,
        long source_warehouse_id,
        String source_warehouse_code,
        long destination_warehouse_id,
        String destination_warehouse_code,
        String status,
        String idempotency_key,
        String notes,
        Instant posted_at,
        Long posted_by_user_id,
        List<transfer_line_response> lines) {
}
