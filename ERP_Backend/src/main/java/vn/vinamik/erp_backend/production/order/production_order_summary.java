package vn.vinamik.erp_backend.production.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record production_order_summary(
        long production_order_id,
        String order_code,
        long production_plan_line_id,
        long stock_item_id,
        String stock_item_code,
        String stock_item_name,
        BigDecimal target_quantity,
        LocalDate planned_starts_on,
        LocalDate planned_ends_on,
        String status,
        Instant released_at) {
}
