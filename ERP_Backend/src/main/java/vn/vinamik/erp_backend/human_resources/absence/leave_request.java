package vn.vinamik.erp_backend.human_resources.absence;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record leave_request(
        @NotBlank(message = "Leave request code is required.")
        @Size(max = 60, message = "Leave request code must contain at most 60 characters.")
        String request_code,
        @NotNull(message = "Employee is required.")
        Long employee_id,
        @NotBlank(message = "Leave type is required.")
        @Size(max = 40, message = "Leave type must contain at most 40 characters.")
        String leave_type_code,
        @NotNull(message = "Leave start date is required.")
        LocalDate starts_on,
        @NotNull(message = "Leave end date is required.")
        LocalDate ends_on,
        boolean is_paid,
        @Size(max = 2000, message = "Leave reason must contain at most 2000 characters.")
        String reason,
        String status) {
}
