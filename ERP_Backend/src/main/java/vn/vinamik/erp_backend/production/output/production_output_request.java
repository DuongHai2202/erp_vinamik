package vn.vinamik.erp_backend.production.output;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record production_output_request(
        @NotNull(message = "Warehouse is required.")
        Long warehouse_id,
        @NotNull(message = "Warehouse location is required.")
        Long warehouse_location_id,
        @NotBlank(message = "Lot code is required.")
        @Size(max = 80, message = "Lot code must contain at most 80 characters.")
        String lot_code,
        @NotNull(message = "Manufactured date is required.")
        LocalDate manufactured_on,
        LocalDate expires_on,
        @NotNull(message = "Good quantity is required.")
        @DecimalMin(value = "0.000000", message = "Good quantity cannot be negative.")
        BigDecimal good_quantity,
        @NotNull(message = "Defective quantity is required.")
        @DecimalMin(value = "0.000000", message = "Defective quantity cannot be negative.")
        BigDecimal defective_quantity,
        @NotBlank(message = "Idempotency key is required.")
        @Size(max = 120, message = "Idempotency key must contain at most 120 characters.")
        String idempotency_key,
        @Size(max = 2000, message = "Notes must contain at most 2000 characters.")
        String notes) {
}
