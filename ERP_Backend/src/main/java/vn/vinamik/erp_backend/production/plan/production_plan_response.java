package vn.vinamik.erp_backend.production.plan;

import java.time.LocalDate;
import java.util.List;

public record production_plan_response(
        long production_plan_id,
        String plan_code,
        String plan_name,
        LocalDate planned_on,
        LocalDate starts_on,
        LocalDate ends_on,
        String status,
        String notes,
        List<production_plan_line_response> lines) {
}