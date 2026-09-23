package vn.vinamik.erp_backend.inventory.stock_item;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
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
        List<Long> normalized_ids = stock_item_ids.stream()
                .filter(stock_item_id -> stock_item_id != null && stock_item_id > 0)
                .distinct()
                .toList();
        if (normalized_ids.isEmpty()) {
            return quantities;
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(normalized_ids.size(), "?"));
        List<stock_quantity_row> rows = jpa_query_executor.query(
                "SELECT stock_item_id, coalesce(sum(on_hand_quantity), 0) AS available_quantity "
                        + "FROM inventory.stock_balance WHERE stock_item_id IN (" + placeholders + ") "
                        + "GROUP BY stock_item_id",
                (result_set, row_number) -> new stock_quantity_row(
                        result_set.getLong("stock_item_id"),
                        result_set.getBigDecimal("available_quantity")),
                normalized_ids.toArray());
        Map<Long, BigDecimal> fetched = new LinkedHashMap<>();
        rows.forEach(row -> fetched.put(row.stock_item_id(), row.available_quantity()));
        normalized_ids.forEach(stock_item_id -> quantities.put(
                stock_item_id, fetched.getOrDefault(stock_item_id, BigDecimal.ZERO)));
        return quantities;
    }

    private record stock_quantity_row(long stock_item_id, BigDecimal available_quantity) {
    }
}
