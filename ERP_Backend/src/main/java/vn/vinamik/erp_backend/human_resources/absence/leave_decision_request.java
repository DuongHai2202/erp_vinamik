package vn.vinamik.erp_backend.human_resources.absence;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record leave_decision_request(
        @NotBlank(message = "Leave decision is required.")
        String status,
        @Size(max = 2000, message = "Decision note must contain at most 2000 characters.")
        String decision_note) {
}
