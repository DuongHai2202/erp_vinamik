package vn.vinamik.erp_backend.platform.identity;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record registration_approval_request(
        @NotNull @Positive Long employee_id,
        @NotEmpty List<@Size(max = 80) String> role_codes) {
}
