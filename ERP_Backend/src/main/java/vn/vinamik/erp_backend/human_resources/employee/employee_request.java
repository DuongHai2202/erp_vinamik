package vn.vinamik.erp_backend.human_resources.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record employee_request(
        @NotBlank(message = "Employee code is required.")
        @Size(max = 40, message = "Employee code must contain at most 40 characters.")
        String employee_code,
        @NotBlank(message = "Full name is required.")
        @Size(max = 160, message = "Full name must contain at most 160 characters.")
        String full_name,
        LocalDate date_of_birth,
        @Size(max = 30, message = "Phone number must contain at most 30 characters.")
        String phone_number,
        @Email(message = "Email format is invalid.")
        @Size(max = 254, message = "Email must contain at most 254 characters.")
        String email,
        Long department_id,
        Long job_title_id,
        Long manager_employee_id,
        String employment_status,
        LocalDate hired_on,
        LocalDate terminated_on,
        @Size(max = 2000, message = "Notes must contain at most 2000 characters.")
        String notes) {
}
