package vn.vinamik.erp_backend.inventory.receipt;

import java.util.List;

public record receipt_page_response(
        List<receipt_summary> items,
        int page,
        int page_size,
        long total_items,
        int total_pages) {
}
