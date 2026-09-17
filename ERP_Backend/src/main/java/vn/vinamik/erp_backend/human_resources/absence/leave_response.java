package vn.vinamik.erp_backend.human_resources.absence;

import java.time.Instant;
import java.time.LocalDate;

public record leave_response(
        long leave_request_id,
        String request_code,
        long employee_id,
        String employee_code,
        String employee_name,
        String leave_type_code,
        LocalDate starts_on,
        LocalDate ends_on,
        long day_count,
        boolean is_paid,
        String reason,
        String status,
        Long approver_user_id,
        Instant decided_at,
        String decision_note) {
}
