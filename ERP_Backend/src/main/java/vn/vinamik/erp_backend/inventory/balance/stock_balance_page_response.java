package vn.vinamik.erp_backend.inventory.balance;

import java.util.List;

public record stock_balance_page_response(
        List<stock_balance_response> items,
        int page,
        int page_size,
        long total_items,
        int total_pages) {
}
