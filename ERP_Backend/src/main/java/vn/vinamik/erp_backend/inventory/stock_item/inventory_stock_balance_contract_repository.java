package vn.vinamik.erp_backend.inventory.stock_item;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class inventory_stock_balance_contract_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public inventory_stock_balance_contract_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public Map<Long, BigDecimal> find_available_quantities(Collection<Long> stock_item_ids) {
        Map<Long, BigDecimal> quantities = new LinkedHashMap<>();
        if (stock_item_ids == null || stock_item_ids.isEmpty()) {
            return quantities;
        }
        for (Long stock_item_id : stock_item_ids) {
            if (stock_item_id == null) {
                continue;
            }
            BigDecimal quantity = jpa_query_executor.queryForObject(
                    "SELECT coalesce(sum(on_hand_quantity), 0) FROM inventory.stock_balance WHERE stock_item_id = ?",
                    BigDecimal.class, stock_item_id);
            quantities.put(stock_item_id, quantity == null ? BigDecimal.ZERO : quantity);
        }
        return quantities;
    }
}
