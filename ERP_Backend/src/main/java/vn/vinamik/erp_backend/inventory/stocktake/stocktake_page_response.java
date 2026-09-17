package vn.vinamik.erp_backend.inventory.stocktake;

import java.util.List;

public record stocktake_page_response(
        List<stocktake_summary> items,
        int page,
        int page_size,
        long total_items,
        int total_pages) {
}
