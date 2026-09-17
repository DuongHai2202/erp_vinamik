package vn.vinamik.erp_backend.production.plan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record production_plan_request(
        @NotBlank(message = "Plan code is required.")
        @Size(max = 60, message = "Plan code must contain at most 60 characters.")
        String plan_code,
        @NotBlank(message = "Plan name is required.")
        @Size(max = 180, message = "Plan name must contain at most 180 characters.")
        String plan_name,
        @NotNull(message = "Planned date is required.")
        LocalDate planned_on,
        @NotNull(message = "Start date is required.")
        LocalDate starts_on,
        @NotNull(message = "End date is required.")
        LocalDate ends_on,
        @Size(max = 2000, message = "Notes must contain at most 2000 characters.")
        String notes,
        @NotEmpty(message = "At least one plan line is required.")
        List<@Valid production_plan_line_request> lines) {
}