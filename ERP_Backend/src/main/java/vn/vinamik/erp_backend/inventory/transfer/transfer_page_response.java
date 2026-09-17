package vn.vinamik.erp_backend.inventory.transfer;

import java.util.List;

public record transfer_page_response(
        List<transfer_summary> items,
        int page,
        int page_size,
        long total_items,
        int total_pages) {
}
