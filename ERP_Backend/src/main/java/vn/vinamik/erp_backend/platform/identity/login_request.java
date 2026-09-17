package vn.vinamik.erp_backend.platform.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record login_request(
        @NotBlank(message = "Username is required.")
        @Size(max = 80, message = "Username must contain at most 80 characters.")
        String username,
        @NotBlank(message = "Password is required.")
        @Size(max = 200, message = "Password must contain at most 200 characters.")
        String password) {
}