package vn.vinamik.erp_backend.production.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

public record production_plan_line_response(
        long production_plan_line_id,
        int line_number,
        long stock_item_id,
        String stock_item_code,
        String stock_item_name,
        String unit_code,
        BigDecimal target_quantity,
        LocalDate required_on,
        String notes) {
}