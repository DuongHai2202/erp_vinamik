package vn.vinamik.erp_backend.platform.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record identity_user_password_reset_request(
        @NotBlank @Size(min = 12, max = 128) String password) {
}
