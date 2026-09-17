package vn.vinamik.erp_backend.inventory.balance;
import vn.vinamik.erp_backend.platform.common.pagination_guard;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class inventory_balance_service {
    private static final int max_page_size = 100;
    private final inventory_balance_repository balance_repository;

    public inventory_balance_service(inventory_balance_repository balance_repository) {
        this.balance_repository = balance_repository;
    }

    @Transactional(readOnly = true)
    public stock_balance_page_response search(String search, Long stock_item_id, Long warehouse_id, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = Math.min(Math.max(page_size, 1), max_page_size);
        return balance_repository.search(search, stock_item_id, warehouse_id, safe_page, safe_page_size);
    }
}
