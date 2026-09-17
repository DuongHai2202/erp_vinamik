package vn.vinamik.erp_backend.human_resources.employee;

import java.util.List;

public record employee_page_response(
        List<employee_response> items,
        int page,
        int page_size,
        long total_items,
        int total_pages) {
}