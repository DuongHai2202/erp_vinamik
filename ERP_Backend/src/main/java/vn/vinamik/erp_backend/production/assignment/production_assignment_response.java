package vn.vinamik.erp_backend.production.assignment;

import java.time.Instant;

public record production_assignment_response(
        long production_assignment_id,
        long production_order_id,
        String order_code,
        long employee_id,
        String employee_code,
        String employee_name,
        Long work_shift_id,
        String shift_code,
        String shift_name,
        String assignment_name,
        Instant starts_at,
        Instant ends_at,
        String status,
        String notes) {
}
