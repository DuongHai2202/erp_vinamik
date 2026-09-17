package vn.vinamik.erp_backend.inventory.stock_item;

import org.springframework.stereotype.Component;
import vn.vinamik.erp_backend.inventory.api.inventory_stock_balance_contract;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

@Component
public class inventory_stock_balance_contract_adapter implements inventory_stock_balance_contract {
    private final inventory_stock_balance_contract_repository balance_repository;

    public inventory_stock_balance_contract_adapter(inventory_stock_balance_contract_repository balance_repository) {
        this.balance_repository = balance_repository;
    }

    @Override
    public Map<Long, BigDecimal> find_available_quantities(Collection<Long> stock_item_ids) {
        return balance_repository.find_available_quantities(stock_item_ids);
    }
}
