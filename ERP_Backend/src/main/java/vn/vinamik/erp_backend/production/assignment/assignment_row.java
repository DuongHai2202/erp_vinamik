package vn.vinamik.erp_backend.production.assignment;

import java.time.Instant;

record assignment_row(long production_assignment_id, long production_order_id, String order_code,
                      long employee_id, Long work_shift_id, String assignment_name, Instant starts_at,
                      Instant ends_at, String status, String notes) {
}
