package vn.vinamik.erp_backend.human_resources.contract;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record employment_contract_request(
        @NotBlank(message = "Contract code is required.")
        @Size(max = 60, message = "Contract code must contain at most 60 characters.")
        String contract_code,
        @NotNull(message = "Employee is required.")
        Long employee_id,
        @NotBlank(message = "Contract type is required.")
        @Size(max = 40, message = "Contract type must contain at most 40 characters.")
        String contract_type,
        @NotNull(message = "Effective start date is required.")
        LocalDate effective_from,
        LocalDate effective_to,
        @NotNull(message = "Base salary is required.")
        @DecimalMin(value = "0.00", inclusive = true, message = "Base salary must be zero or greater.")
        BigDecimal base_salary,
        @Size(min = 3, max = 3, message = "Currency code must contain 3 characters.")
        String currency_code,
        String status,
        @Size(max = 2000, message = "Notes must contain at most 2000 characters.")
        String notes) {
}
