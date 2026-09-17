package vn.vinamik.erp_backend.production.material_consumption;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record material_consumption_request(
        @NotNull(message = "Warehouse is required.")
        Long warehouse_id,
        @NotBlank(message = "Idempotency key is required.")
        @Size(max = 120, message = "Idempotency key must contain at most 120 characters.")
        String idempotency_key,
        @Size(max = 2000, message = "Notes must contain at most 2000 characters.")
        String notes,
        @NotEmpty(message = "At least one consumption line is required.")
        List<@Valid material_consumption_line_request> lines) {
}
