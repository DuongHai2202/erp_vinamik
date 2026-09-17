package vn.vinamik.erp_backend.platform.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record identity_user_create_request(
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Size(min = 12, max = 128) String password,
        Long employee_id,
        List<@NotBlank @Size(max = 80) String> role_codes) {
}
