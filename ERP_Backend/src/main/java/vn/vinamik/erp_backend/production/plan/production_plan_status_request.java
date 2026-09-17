package vn.vinamik.erp_backend.production.plan;

import jakarta.validation.constraints.NotBlank;

public record production_plan_status_request(@NotBlank(message = "Target status is required.") String status) {
}