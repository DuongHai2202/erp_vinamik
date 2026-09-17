package vn.vinamik.erp_backend.production.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record production_order_request(
        @NotBlank(message = "Production order code is required.")
        @Size(max = 60, message = "Production order code must contain at most 60 characters.")
        String order_code,
        @NotNull(message = "Production plan line is required.")
        Long production_plan_line_id,
        @NotNull(message = "BOM is required.")
        Long bom_id,
        @NotNull(message = "Target quantity is required.")
        @DecimalMin(value = "0.000001", message = "Target quantity must be greater than zero.")
        BigDecimal target_quantity,
        @NotNull(message = "Planned start date is required.")
        LocalDate planned_starts_on,
        @NotNull(message = "Planned end date is required.")
        LocalDate planned_ends_on,
        @Size(max = 120, message = "Production line name must contain at most 120 characters.")
        String production_line_name,
        @Size(max = 2000, message = "Notes must contain at most 2000 characters.")
        String notes) {
}
