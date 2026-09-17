package vn.vinamik.erp_backend.production.order;

import java.util.List;

public record production_order_page_response(
        List<production_order_summary> items,
        int page,
        int page_size,
        long total_items,
        int total_pages) {
}
