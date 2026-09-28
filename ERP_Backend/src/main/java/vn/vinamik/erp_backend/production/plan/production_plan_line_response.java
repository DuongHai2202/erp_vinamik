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
        String notes,
        BigDecimal allocated_quantity,
        BigDecimal remaining_quantity) {
    public production_plan_line_response(
            long production_plan_line_id,
            int line_number,
            long stock_item_id,
            String stock_item_code,
            String stock_item_name,
            String unit_code,
            BigDecimal target_quantity,
            LocalDate required_on,
            String notes) {
        this(production_plan_line_id, line_number, stock_item_id, stock_item_code, stock_item_name,
                unit_code, target_quantity, required_on, notes, BigDecimal.ZERO, target_quantity);
    }
}
