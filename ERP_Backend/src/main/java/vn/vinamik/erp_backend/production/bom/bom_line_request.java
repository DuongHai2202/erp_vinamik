package vn.vinamik.erp_backend.production.bom;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record bom_line_request(
        @NotNull(message = "Material is required.")
        Long material_stock_item_id,
        @NotNull(message = "Material quantity is required.")
        @DecimalMin(value = "0.000001", inclusive = true, message = "Material quantity must be greater than zero.")
        BigDecimal quantity_per_base,
        @DecimalMin(value = "0.00", inclusive = true, message = "Scrap percentage must be zero or greater.")
        @DecimalMax(value = "100.00", inclusive = true, message = "Scrap percentage must be at most 100.")
        BigDecimal scrap_percent,
        String notes) {
}
