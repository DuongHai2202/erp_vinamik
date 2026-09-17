package vn.vinamik.erp_backend.human_resources.absence;

import java.util.List;

public record leave_page_response(
        List<leave_response> items,
        int page,
        int page_size,
        long total_items,
        int total_pages) {
}
