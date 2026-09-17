package vn.vinamik.erp_backend.production.bom;

import jakarta.validation.constraints.NotBlank;

public record bom_status_request(
        @NotBlank(message = "BOM status is required.")
        String status) {
}
