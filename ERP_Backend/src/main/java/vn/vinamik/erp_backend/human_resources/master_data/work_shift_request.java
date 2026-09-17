package vn.vinamik.erp_backend.human_resources.master_data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record work_shift_request(
        @NotBlank(message = "Work shift code is required.")
        @Size(max = 40, message = "Work shift code must contain at most 40 characters.")
        String shift_code,
        @NotBlank(message = "Work shift name is required.")
        @Size(max = 120, message = "Work shift name must contain at most 120 characters.")
        String shift_name,
        @NotNull(message = "Work shift start time is required.")
        LocalTime starts_at,
        @NotNull(message = "Work shift end time is required.")
        LocalTime ends_at,
        String status) {
}
