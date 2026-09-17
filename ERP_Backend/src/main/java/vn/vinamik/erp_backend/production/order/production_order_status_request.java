package vn.vinamik.erp_backend.production.order;

import jakarta.validation.constraints.NotBlank;

public record production_order_status_request(
        @NotBlank(message = "Production order status is required.")
        String status) {
}
