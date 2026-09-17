package vn.vinamik.erp_backend.production.assignment;

import jakarta.validation.constraints.NotBlank;

public record production_assignment_status_request(
        @NotBlank(message = "Assignment status is required.")
        String status) {
}
