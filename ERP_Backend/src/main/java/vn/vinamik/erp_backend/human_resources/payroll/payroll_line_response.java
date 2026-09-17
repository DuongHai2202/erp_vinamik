package vn.vinamik.erp_backend.human_resources.payroll;

import java.math.BigDecimal;

public record payroll_line_response(
        long payroll_line_id,
        String line_type,
        String direction,
        String line_label,
        BigDecimal quantity,
        BigDecimal unit_amount,
        BigDecimal amount,
        String source_type,
        Long source_id) {
}

