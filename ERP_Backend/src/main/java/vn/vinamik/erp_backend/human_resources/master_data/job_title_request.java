package vn.vinamik.erp_backend.human_resources.master_data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record job_title_request(
        @NotBlank(message = "Job title code is required.")
        @Size(max = 40, message = "Job title code must contain at most 40 characters.")
        String job_title_code,
        @NotBlank(message = "Job title name is required.")
        @Size(max = 160, message = "Job title name must contain at most 160 characters.")
        String job_title_name,
        @Size(max = 2000, message = "Job title description must contain at most 2000 characters.")
        String description,
        String status) {
}
