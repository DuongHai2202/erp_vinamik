package vn.vinamik.erp_backend.platform.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record identity_user_update_request(
        @NotBlank @Size(max = 20) String status,
        Long employee_id) {
}
