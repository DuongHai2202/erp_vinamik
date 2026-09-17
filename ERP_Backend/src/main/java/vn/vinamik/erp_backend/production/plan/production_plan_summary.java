package vn.vinamik.erp_backend.production.plan;

import java.time.LocalDate;

public record production_plan_summary(
        long production_plan_id,
        String plan_code,
        String plan_name,
        LocalDate planned_on,
        LocalDate starts_on,
        LocalDate ends_on,
        String status,
        int line_count) {
}