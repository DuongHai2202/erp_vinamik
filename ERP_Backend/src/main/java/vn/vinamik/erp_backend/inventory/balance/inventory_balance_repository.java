package vn.vinamik.erp_backend.inventory.balance;
import vn.vinamik.erp_backend.platform.common.pagination_guard;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;

import java.util.List;
import java.util.Locale;

@Repository
public class inventory_balance_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public inventory_balance_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public stock_balance_page_response search(String search, Long stock_item_id, Long warehouse_id, int page, int page_size) {
        String normalized_search = search == null || search.isBlank() ? null : search.trim().toLowerCase(Locale.ROOT);
        String common_where = "FROM inventory.stock_balance AS balance JOIN inventory.stock_item AS item ON item.stock_item_id = balance.stock_item_id "
                + "JOIN inventory.warehouse_location AS location ON location.warehouse_location_id = balance.warehouse_location_id "
                + "JOIN inventory.warehouse AS warehouse ON warehouse.warehouse_id = location.warehouse_id "
                + "JOIN inventory.unit_of_measure AS unit ON unit.unit_of_measure_id = item.base_unit_of_measure_id "
                + "LEFT JOIN inventory.stock_lot AS lot ON lot.stock_lot_id = balance.stock_lot_id "
                + "WHERE (CAST(? AS text) IS NULL OR lower(item.item_code) LIKE '%' || ? || '%' OR lower(item.item_name) LIKE '%' || ? || '%') "
                + "AND (CAST(? AS text) IS NULL OR balance.stock_item_id = ?) AND (CAST(? AS text) IS NULL OR warehouse.warehouse_id = ?)";
        Long total_items = jpa_query_executor.queryForObject("SELECT count(*) " + common_where, Long.class,
                normalized_search, normalized_search, normalized_search, stock_item_id, stock_item_id, warehouse_id, warehouse_id);
        List<stock_balance_response> items = jpa_query_executor.query(
                "SELECT balance.stock_item_id, item.item_code, item.item_name, warehouse.warehouse_id, warehouse.warehouse_code, balance.warehouse_location_id, location.location_code, balance.stock_lot_id, lot.lot_code, balance.on_hand_quantity, unit.unit_code "
                        + common_where + " ORDER BY item.item_code, warehouse.warehouse_code, location.location_code, lot.lot_code NULLS FIRST LIMIT ? OFFSET ?",
                (result_set, row_number) -> new stock_balance_response(
                        result_set.getLong("stock_item_id"), result_set.getString("item_code"), result_set.getString("item_name"),
                        result_set.getLong("warehouse_id"), result_set.getString("warehouse_code"), result_set.getLong("warehouse_location_id"),
                        result_set.getString("location_code"), result_set.getObject("stock_lot_id", Long.class), result_set.getString("lot_code"),
                        result_set.getBigDecimal("on_hand_quantity"), result_set.getString("unit_code")),
                normalized_search, normalized_search, normalized_search, stock_item_id, stock_item_id, warehouse_id, warehouse_id,
                page_size, pagination_guard.offset(page, page_size));
        long total = total_items == null ? 0 : total_items;
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / page_size);
        return new stock_balance_page_response(items, page, page_size, total, total_pages);
    }
}
