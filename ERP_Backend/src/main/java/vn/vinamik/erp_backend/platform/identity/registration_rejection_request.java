package vn.vinamik.erp_backend.platform.identity;

import jakarta.validation.constraints.Size;

public record registration_rejection_request(@Size(max = 500) String review_note) {
}
