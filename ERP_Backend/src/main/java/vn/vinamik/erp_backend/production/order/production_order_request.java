package vn.vinamik.erp_backend.production.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record production_order_request(
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
        String notes,
        @Size(max = 24, message = "Production order status must contain at most 24 characters.")
        String status) {

    public production_order_request(
            String order_code,
            Long production_plan_line_id,
            Long bom_id,
            BigDecimal target_quantity,
            LocalDate planned_starts_on,
            LocalDate planned_ends_on,
            String production_line_name,
            String notes) {
        this(order_code, production_plan_line_id, bom_id, target_quantity, planned_starts_on,
                planned_ends_on, production_line_name, notes, null);
    }
}
