package vn.vinamik.erp_backend.human_resources.master_data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record department_request(
        @NotBlank(message = "Department code is required.")
        @Size(max = 40, message = "Department code must contain at most 40 characters.")
        String department_code,
        @NotBlank(message = "Department name is required.")
        @Size(max = 160, message = "Department name must contain at most 160 characters.")
        String department_name,
        Long parent_department_id,
        String status) {
}
