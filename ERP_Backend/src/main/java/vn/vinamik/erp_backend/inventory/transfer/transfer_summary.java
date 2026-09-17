package vn.vinamik.erp_backend.inventory.transfer;

import java.time.Instant;

public record transfer_summary(
        long transfer_id,
        String transfer_code,
        long source_warehouse_id,
        String source_warehouse_code,
        long destination_warehouse_id,
        String destination_warehouse_code,
        String status,
        int line_count,
        Instant created_at,
        Instant posted_at) {
}
