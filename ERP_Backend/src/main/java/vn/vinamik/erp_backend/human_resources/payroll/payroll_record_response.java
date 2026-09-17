package vn.vinamik.erp_backend.human_resources.payroll;

import java.math.BigDecimal;
import java.time.Instant;

public record payroll_record_response(
        long payroll_record_id,
        long payroll_period_id,
        long employee_id,
        String employee_code,
        String full_name,
        long employment_contract_id,
        String contract_code,
        BigDecimal base_salary_snapshot,
        String currency_code_snapshot,
        BigDecimal standard_working_days_snapshot,
        BigDecimal unpaid_leave_days,
        BigDecimal unpaid_leave_amount,
        BigDecimal reward_amount,
        BigDecimal discipline_amount,
        BigDecimal gross_amount,
        BigDecimal deduction_amount,
        BigDecimal net_amount,
        String calculation_version,
        String status,
        Instant calculated_at,
        Long approved_by_user_id,
        Instant approved_at,
        Instant locked_at) {
}

