package vn.vinamik.erp_backend.production.assignment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record production_assignment_request(
        @NotNull(message = "Production order is required.")
        Long production_order_id,
        @NotNull(message = "Employee is required.")
        Long employee_id,
        Long work_shift_id,
        @Size(max = 120, message = "Assignment name must contain at most 120 characters.")
        String assignment_name,
        @NotNull(message = "Assignment start time is required.")
        Instant starts_at,
        @NotNull(message = "Assignment end time is required.")
        Instant ends_at,
        @Size(max = 2000, message = "Notes must contain at most 2000 characters.")
        String notes) {
}
