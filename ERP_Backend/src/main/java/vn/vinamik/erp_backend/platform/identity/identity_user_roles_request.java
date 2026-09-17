package vn.vinamik.erp_backend.platform.identity;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record identity_user_roles_request(
        @NotNull @Size(max = 32) List<@Size(max = 80) String> role_codes) {
}
