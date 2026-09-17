package vn.vinamik.erp_backend.inventory.issue;

import java.util.List;

public record issue_page_response(
        List<issue_summary> items,
        int page,
        int page_size,
        long total_items,
        int total_pages) {
}
