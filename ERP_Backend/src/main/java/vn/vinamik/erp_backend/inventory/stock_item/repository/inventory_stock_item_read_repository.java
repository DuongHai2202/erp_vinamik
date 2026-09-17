package vn.vinamik.erp_backend.inventory.stock_item.repository;
import vn.vinamik.erp_backend.platform.common.pagination_guard;

import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;

import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class inventory_stock_item_read_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public inventory_stock_item_read_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public long count(String search, String status, String item_type) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.stock_item AS stock_item "
                        + "WHERE stock_item.item_type = ? "
                        + "AND (CAST(? AS text) IS NULL OR lower(stock_item.item_code) LIKE '%' || ? || '%' "
                        + "OR lower(stock_item.item_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR stock_item.status = ?)",
                Long.class, item_type, search, search, search, status, status);
        return total == null ? 0 : total;
    }

    public List<stock_item_read_row> search(String search, String status, String item_type,
                                            int page, int page_size) {
        return jpa_query_executor.query(
                "SELECT stock_item.stock_item_id, stock_item.item_code, stock_item.item_name, "
                        + "stock_item.item_type, stock_item.item_category_id, item_category.category_name, "
                        + "stock_item.base_unit_of_measure_id, unit_of_measure.unit_code, "
                        + "unit_of_measure.unit_name, stock_item.lot_controlled, "
                        + "stock_item.minimum_stock_quantity, stock_item.status, stock_item.description "
                        + "FROM inventory.stock_item AS stock_item "
                        + "LEFT JOIN inventory.item_category AS item_category "
                        + "ON item_category.item_category_id = stock_item.item_category_id "
                        + "JOIN inventory.unit_of_measure AS unit_of_measure "
                        + "ON unit_of_measure.unit_of_measure_id = stock_item.base_unit_of_measure_id "
                        + "WHERE stock_item.item_type = ? "
                        + "AND (CAST(? AS text) IS NULL OR lower(stock_item.item_code) LIKE '%' || ? || '%' "
                        + "OR lower(stock_item.item_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR stock_item.status = ?) "
                        + "ORDER BY stock_item.item_code LIMIT ? OFFSET ?",
                this::map_row, item_type, search, search, search, status, status, page_size, pagination_guard.offset(page, page_size));
    }

    public stock_item_read_row find_by_id(long stock_item_id) {
        List<stock_item_read_row> rows = jpa_query_executor.query(
                "SELECT stock_item.stock_item_id, stock_item.item_code, stock_item.item_name, "
                        + "stock_item.item_type, stock_item.item_category_id, item_category.category_name, "
                        + "stock_item.base_unit_of_measure_id, unit_of_measure.unit_code, "
                        + "unit_of_measure.unit_name, stock_item.lot_controlled, "
                        + "stock_item.minimum_stock_quantity, stock_item.status, stock_item.description "
                        + "FROM inventory.stock_item AS stock_item "
                        + "LEFT JOIN inventory.item_category AS item_category "
                        + "ON item_category.item_category_id = stock_item.item_category_id "
                        + "JOIN inventory.unit_of_measure AS unit_of_measure "
                        + "ON unit_of_measure.unit_of_measure_id = stock_item.base_unit_of_measure_id "
                        + "WHERE stock_item.stock_item_id = ?",
                this::map_row, stock_item_id);
        if (rows.isEmpty()) {
            throw new resource_not_found_exception("Stock item");
        }
        return rows.getFirst();
    }

    public boolean active_unit_of_measure(long unit_of_measure_id) {
        Long count = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.unit_of_measure "
                        + "WHERE unit_of_measure_id = ? AND status = 'active'",
                Long.class, unit_of_measure_id);
        return count != null && count > 0;
    }

    public boolean active_item_category(long item_category_id) {
        Long count = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.item_category "
                        + "WHERE item_category_id = ? AND status = 'active'",
                Long.class, item_category_id);
        return count != null && count > 0;
    }

    private stock_item_read_row map_row(jpa_result_row result_set, int row_number) {
        return new stock_item_read_row(
                result_set.getLong("stock_item_id"),
                result_set.getString("item_code"),
                result_set.getString("item_name"),
                result_set.getString("item_type"),
                result_set.getObject("item_category_id", Long.class),
                result_set.getString("category_name"),
                result_set.getLong("base_unit_of_measure_id"),
                result_set.getString("unit_code"),
                result_set.getString("unit_name"),
                result_set.getBoolean("lot_controlled"),
                result_set.getBigDecimal("minimum_stock_quantity"),
                result_set.getString("status"),
                result_set.getString("description"));
    }
}


