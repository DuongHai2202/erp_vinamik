package vn.vinamik.erp_backend.production.assignment;

import java.util.List;

public record production_assignment_page_response(
        List<production_assignment_response> items,
        int page,
        int page_size,
        long total_items,
        int total_pages) {
}
