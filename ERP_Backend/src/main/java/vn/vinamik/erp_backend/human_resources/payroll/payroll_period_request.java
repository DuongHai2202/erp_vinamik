package vn.vinamik.erp_backend.human_resources.payroll;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record payroll_period_request(
        @NotNull(message = "Payroll year is required.")
        @Min(value = 2000, message = "Payroll year must be 2000 or later.")
        @Max(value = 2100, message = "Payroll year must be 2100 or earlier.")
        Integer year,
        @NotNull(message = "Payroll month is required.")
        @Min(value = 1, message = "Payroll month must be between 1 and 12.")
        @Max(value = 12, message = "Payroll month must be between 1 and 12.")
        Integer month) {
}

