package vn.vinamik.erp_backend.production.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record production_order_response(
        long production_order_id,
        String order_code,
        long production_plan_line_id,
        long stock_item_id,
        String stock_item_code,
        String stock_item_name,
        long bom_id,
        String bom_code,
        int bom_version_number,
        BigDecimal target_quantity,
        String unit_code_snapshot,
        LocalDate planned_starts_on,
        LocalDate planned_ends_on,
        String production_line_name,
        String status,
        String notes,
        Instant released_at,
        List<order_material_requirement_response> material_requirements) {
}
