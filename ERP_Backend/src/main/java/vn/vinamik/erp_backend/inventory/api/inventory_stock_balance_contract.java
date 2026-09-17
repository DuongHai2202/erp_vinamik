package vn.vinamik.erp_backend.inventory.api;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

public interface inventory_stock_balance_contract {
    Map<Long, BigDecimal> find_available_quantities(Collection<Long> stock_item_ids);
}
