package vn.vinamik.erp_backend.inventory.stocktake;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record stocktake_count_request(
        @NotNull(message = "Counted quantity is required.")
        @DecimalMin(value = "0.000000", message = "Counted quantity cannot be negative.")
        BigDecimal counted_quantity) {
}
