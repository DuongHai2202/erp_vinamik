package vn.vinamik.erp_backend.human_resources.payroll;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record payroll_period_response(
        long payroll_period_id,
        String period_code,
        LocalDate starts_on,
        LocalDate ends_on,
        BigDecimal standard_working_days,
        String calculation_version,
        String status,
        long record_count,
        BigDecimal total_net_amount,
        String currency_code,
        Long approved_by_user_id,
        Instant calculated_at,
        Instant approved_at,
        Instant locked_at,
        String decision_note) {
}
