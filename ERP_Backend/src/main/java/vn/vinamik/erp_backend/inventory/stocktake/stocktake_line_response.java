package vn.vinamik.erp_backend.inventory.stocktake;

import java.math.BigDecimal;
import java.time.Instant;

public record stocktake_line_response(
        long stocktake_line_id,
        int line_number,
        long stock_item_id,
        String item_code,
        String item_name,
        long warehouse_location_id,
        String location_code,
        Long stock_lot_id,
        String lot_code,
        BigDecimal system_quantity,
        BigDecimal counted_quantity,
        BigDecimal difference_quantity,
        Instant counted_at,
        Long counted_by_user_id) {
}
