package vn.vinamik.erp_backend.human_resources.payroll;

import java.util.List;

public record payroll_record_detail_response(
        payroll_record_response record,
        List<payroll_line_response> lines) {
}

