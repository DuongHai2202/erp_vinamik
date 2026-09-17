package vn.vinamik.erp_backend.human_resources.reward_discipline;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record reward_discipline_request(
        @NotBlank(message = "Reward or discipline code is required.")
        @Size(max = 60, message = "Reward or discipline code must contain at most 60 characters.")
        String record_code,
        @NotNull(message = "Employee is required.")
        Long employee_id,
        @NotBlank(message = "Event type is required.")
        String event_type,
        @NotNull(message = "Effective date is required.")
        LocalDate effective_on,
        @NotBlank(message = "Reason is required.")
        @Size(max = 2000, message = "Reason must contain at most 2000 characters.")
        String reason,
        @DecimalMin(value = "0.00", inclusive = true, message = "Amount must be zero or greater.")
        BigDecimal amount,
        @Size(min = 3, max = 3, message = "Currency code must contain 3 characters.")
        String currency_code,
        String status) {
}
