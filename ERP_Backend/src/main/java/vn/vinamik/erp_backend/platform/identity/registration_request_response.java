package vn.vinamik.erp_backend.platform.identity;

import java.time.Instant;

public record registration_request_response(
        long registration_request_id,
        String full_name,
        String work_email,
        String username,
        String employee_code,
        String status,
        Instant requested_at,
        Instant expires_at,
        Instant reviewed_at,
        String review_note) {
}
