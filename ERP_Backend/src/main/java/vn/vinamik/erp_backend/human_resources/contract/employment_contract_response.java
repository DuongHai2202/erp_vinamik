package vn.vinamik.erp_backend.human_resources.contract;

import java.math.BigDecimal;
import java.time.LocalDate;

public record employment_contract_response(
        long employment_contract_id,
        String contract_code,
        long employee_id,
        String employee_code,
        String employee_name,
        String contract_type,
        LocalDate effective_from,
        LocalDate effective_to,
        BigDecimal base_salary,
        String currency_code,
        String status,
        long days_until_expiry,
        boolean expiring_soon,
        String notes) {
}
