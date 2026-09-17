package vn.vinamik.erp_backend.production.plan;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record production_plan_line_request(
        @NotNull(message = "Stock item is required.")
        Long stock_item_id,
        @NotNull(message = "Target quantity is required.")
        @Positive(message = "Target quantity must be positive.")
        BigDecimal target_quantity,
        LocalDate required_on,
        @Size(max = 2000, message = "Line notes must contain at most 2000 characters.")
        String notes) {
}