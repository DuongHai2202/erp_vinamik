package vn.vinamik.erp_backend.production.output;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record production_output_response(
        long production_output_id,
        long production_order_id,
        String order_code,
        long stock_item_id,
        Long warehouse_id,
        Long warehouse_location_id,
        String lot_code,
        LocalDate manufactured_on,
        LocalDate expires_on,
        BigDecimal good_quantity,
        BigDecimal defective_quantity,
        Long inventory_stock_lot_id,
        Long inventory_receipt_id,
        String status,
        String idempotency_key,
        Instant recorded_at,
        Instant received_at,
        String notes) {
}

