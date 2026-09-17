package vn.vinamik.erp_backend.human_resources.reward_discipline;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record reward_discipline_response(
        long employee_reward_discipline_id,
        String record_code,
        long employee_id,
        String employee_code,
        String employee_name,
        String event_type,
        LocalDate effective_on,
        String reason,
        BigDecimal amount,
        String currency_code,
        String status,
        Long approver_user_id,
        Instant decided_at,
        String decision_note) {
}
