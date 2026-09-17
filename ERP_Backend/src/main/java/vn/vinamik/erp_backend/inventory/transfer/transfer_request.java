package vn.vinamik.erp_backend.inventory.transfer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record transfer_request(
        @NotBlank(message = "Transfer code is required.")
        @Size(max = 60, message = "Transfer code must contain at most 60 characters.")
        String transfer_code,
        @NotNull(message = "Source warehouse is required.")
        Long source_warehouse_id,
        @NotNull(message = "Destination warehouse is required.")
        Long destination_warehouse_id,
        @Size(max = 120, message = "Idempotency key must contain at most 120 characters.")
        String idempotency_key,
        @Size(max = 2000, message = "Notes must contain at most 2000 characters.")
        String notes,
        @NotEmpty(message = "At least one transfer line is required.")
        List<@Valid transfer_line_request> lines) {
}
