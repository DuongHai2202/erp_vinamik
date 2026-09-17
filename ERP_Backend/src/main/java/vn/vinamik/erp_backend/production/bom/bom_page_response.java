package vn.vinamik.erp_backend.production.bom;

import java.util.List;

public record bom_page_response(
        List<bom_summary> items,
        int page,
        int page_size,
        long total_items,
        int total_pages) {
}
