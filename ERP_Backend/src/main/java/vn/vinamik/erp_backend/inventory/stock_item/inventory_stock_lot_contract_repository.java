package vn.vinamik.erp_backend.inventory.stock_item;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.inventory.api.inventory_stock_lot_snapshot;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;

import java.time.LocalDate;
import java.util.List;

@Repository
public class inventory_stock_lot_contract_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public inventory_stock_lot_contract_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public boolean active_stock_item(long stock_item_id) {
        Long count = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.stock_item WHERE stock_item_id = ? AND status = 'active'",
                Long.class, stock_item_id);
        return count != null && count > 0;
    }

    public Long insert_lot(long stock_item_id, String lot_code, LocalDate manufactured_on, LocalDate expires_on, long actor_user_id) {
        return jpa_query_executor.queryForObject(
                "INSERT INTO inventory.stock_lot (stock_item_id, lot_code, manufactured_on, expires_on, status, created_by_user_id, updated_by_user_id) VALUES (?, ?, ?, ?, 'active', ?, ?) ON CONFLICT (stock_item_id, lot_code) DO NOTHING RETURNING stock_lot_id",
                Long.class, stock_item_id, lot_code, manufactured_on, expires_on, actor_user_id, actor_user_id);
    }

    public List<inventory_stock_lot_snapshot> find(long stock_item_id, String lot_code) {
        return jpa_query_executor.query(
                "SELECT stock_lot_id, stock_item_id, lot_code, manufactured_on, expires_on, status FROM inventory.stock_lot WHERE stock_item_id = ? AND lot_code = ?",
                (result_set, row_number) -> new inventory_stock_lot_snapshot(
                        result_set.getLong("stock_lot_id"), result_set.getLong("stock_item_id"),
                        result_set.getString("lot_code"), result_set.getObject("manufactured_on", LocalDate.class),
                        result_set.getObject("expires_on", LocalDate.class), result_set.getString("status")),
                stock_item_id, lot_code);
    }
}
