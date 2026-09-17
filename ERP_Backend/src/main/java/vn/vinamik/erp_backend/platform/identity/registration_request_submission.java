package vn.vinamik.erp_backend.platform.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record registration_request_submission(
        @NotBlank @Size(max = 160) String full_name,
        @NotBlank @Email @Size(max = 254) String work_email,
        @Size(max = 40) String employee_code,
        @NotBlank @Size(max = 80) @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9._-]{2,79}$") String username,
        @NotBlank @Size(min = 15, max = 128) String password) {
}
