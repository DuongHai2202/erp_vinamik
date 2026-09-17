package vn.vinamik.erp_backend.human_resources.contract;

import jakarta.validation.constraints.NotBlank;

public record employment_contract_status_request(
        @NotBlank(message = "Contract status is required.")
        String status,
        String notes) {
}
