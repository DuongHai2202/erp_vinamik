package vn.vinamik.erp_backend.production.order_progress;

import java.math.BigDecimal;
import java.util.List;

public record production_order_progress_response(
        long production_order_id,
        String order_code,
        String status,
        BigDecimal planned_quantity,
        BigDecimal actual_good_quantity,
        BigDecimal actual_defective_quantity,
        BigDecimal actual_total_quantity,
        BigDecimal remaining_quantity,
        BigDecimal completion_percent,
        List<production_order_event_response> events) {
}

