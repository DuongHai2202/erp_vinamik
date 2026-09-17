package vn.vinamik.erp_backend.inventory.stock_item;

import java.util.List;

public record stock_item_page_response(
        List<stock_item_response> items,
        int page,
        int page_size,
        long total_items,
        int total_pages) {
}