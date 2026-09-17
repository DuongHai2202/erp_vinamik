package vn.vinamik.erp_backend.inventory.balance;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.vinamik.erp_backend.platform.common.api_success_response;

@RestController
@RequestMapping("/api/v1/inventory/balances")
public class inventory_balance_controller {
    private final inventory_balance_service balance_service;

    public inventory_balance_controller(inventory_balance_service balance_service) {
        this.balance_service = balance_service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory_material_read')")
    public api_success_response<stock_balance_page_response> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long stock_item_id,
            @RequestParam(required = false) Long warehouse_id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Stock balances retrieved successfully.", balance_service.search(search, stock_item_id, warehouse_id, page, page_size));
    }
}
