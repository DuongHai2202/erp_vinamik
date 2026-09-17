package vn.vinamik.erp_backend.human_resources.payroll;

import jakarta.validation.constraints.Size;

public record payroll_decision_request(
        @Size(max = 2000, message = "Decision note must contain at most 2000 characters.")
        String decision_note) {
}

