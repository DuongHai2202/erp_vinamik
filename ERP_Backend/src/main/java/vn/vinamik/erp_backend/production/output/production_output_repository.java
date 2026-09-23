package vn.vinamik.erp_backend.production.output;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import java.util.List;

@Repository
public class production_output_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public production_output_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public List<production_output_response> find_by_order(long production_order_id) {
        return jpa_query_executor.query(
                output_select_sql() + " WHERE output.production_order_id = ? "
                        + "ORDER BY output.recorded_at, output.production_output_id",
                this::map_response, production_order_id);
    }

    public production_output_response find_by_idempotency(long production_order_id, String idempotency_key) {
        List<production_output_response> outputs = jpa_query_executor.query(
                output_select_sql() + " WHERE output.production_order_id = ? AND output.idempotency_key = ?",
                this::map_response, production_order_id, idempotency_key);
        return outputs.isEmpty() ? null : outputs.getFirst();
    }

    public production_output_response find_by_id(long output_id) {
        List<production_output_response> outputs = jpa_query_executor.query(
                output_select_sql() + " WHERE output.production_output_id = ?",
                this::map_response, output_id);
        if (outputs.isEmpty()) {
            throw new resource_not_found_exception("Production output");
        }
        return outputs.getFirst();
    }

    public production_output_response find_by_id_for_update(long production_order_id, long output_id) {
        List<production_output_response> outputs = jpa_query_executor.query(
                output_select_sql() + " WHERE output.production_order_id = ? "
                        + "AND output.production_output_id = ? FOR UPDATE OF output",
                this::map_response, production_order_id, output_id);
        if (outputs.isEmpty()) {
            throw new resource_not_found_exception("Production output");
        }
        return outputs.getFirst();
    }

    public order_snapshot load_order(long production_order_id, boolean for_update) {
        List<order_snapshot> orders = jpa_query_executor.query(
                "SELECT production_order_id, order_code, stock_item_id, target_quantity, status "
                        + "FROM production.production_order WHERE production_order_id = ?"
                        + (for_update ? " FOR UPDATE" : ""),
                (result_set, row_number) -> new order_snapshot(
                        result_set.getLong("production_order_id"),
                        result_set.getString("order_code"),
                        result_set.getLong("stock_item_id"),
                        result_set.getBigDecimal("target_quantity"),
                        result_set.getString("status")),
                production_order_id);
        if (orders.isEmpty()) {
            throw new resource_not_found_exception("Production order");
        }
        return orders.getFirst();
    }

    public BigDecimal total_output_quantity(long production_order_id) {
        BigDecimal total = jpa_query_executor.queryForObject(
                "SELECT coalesce(sum(good_quantity + defective_quantity), 0) "
                        + "FROM production.production_output "
                        + "WHERE production_order_id = ? AND status <> 'cancelled'",
                BigDecimal.class, production_order_id);
        return total == null ? BigDecimal.ZERO : total;
    }

    public Long insert(long production_order_id, long stock_item_id, String lot_code,
                       LocalDate manufactured_on, LocalDate expires_on, BigDecimal good_quantity,
                       BigDecimal defective_quantity, long warehouse_id, long warehouse_location_id,
                       String status, String idempotency_key, long actor_user_id, String notes) {
        Long output_id = jpa_query_executor.queryForObject(
                "INSERT INTO production.production_output "
                        + "(production_order_id, stock_item_id, lot_code_snapshot, manufactured_on, expires_on, "
                        + "good_quantity, defective_quantity, inventory_warehouse_id, inventory_warehouse_location_id, "
                        + "status, idempotency_key, recorded_by_user_id, notes) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING "
                        + "RETURNING production_output_id",
                Long.class, production_order_id, stock_item_id, lot_code, manufactured_on, expires_on,
                good_quantity, defective_quantity, warehouse_id, warehouse_location_id, status,
                idempotency_key, actor_user_id, notes);
        return output_id;
    }

    public production_output_response find_by_order_lot(long production_order_id, String lot_code) {
        List<production_output_response> outputs = jpa_query_executor.query(
                output_select_sql() + " WHERE output.production_order_id = ? AND output.lot_code_snapshot = ?",
                this::map_response, production_order_id, lot_code);
        return outputs.isEmpty() ? null : outputs.getFirst();
    }

    public BigDecimal total_output_quantity_except(long production_order_id, long output_id) {
        BigDecimal total = jpa_query_executor.queryForObject(
                "SELECT coalesce(sum(good_quantity + defective_quantity), 0) FROM production.production_output WHERE production_order_id = ? AND production_output_id <> ? AND status <> 'cancelled'",
                BigDecimal.class, production_order_id, output_id);
        return total == null ? BigDecimal.ZERO : total;
    }

    public production_output_response find_by_order_lot_except(long production_order_id, String lot_code, long output_id) {
        List<production_output_response> outputs = jpa_query_executor.query(
                output_select_sql() + " WHERE output.production_order_id = ? AND output.lot_code_snapshot = ? AND output.production_output_id <> ?",
                this::map_response, production_order_id, lot_code, output_id);
        return outputs.isEmpty() ? null : outputs.getFirst();
    }

    public boolean idempotency_key_exists_for_other(String idempotency_key, long output_id) {
        Long count = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM production.production_output WHERE idempotency_key = ? AND production_output_id <> ?",
                Long.class, idempotency_key, output_id);
        return count != null && count > 0;
    }

    public int update_draft(long output_id, long production_order_id, String lot_code, LocalDate manufactured_on,
                            LocalDate expires_on, BigDecimal good_quantity, BigDecimal defective_quantity,
                            long warehouse_id, long warehouse_location_id, String status, String idempotency_key,
                            String notes) {
        return jpa_query_executor.update(
                "UPDATE production.production_output SET lot_code_snapshot = ?, manufactured_on = ?, expires_on = ?, good_quantity = ?, defective_quantity = ?, inventory_warehouse_id = ?, inventory_warehouse_location_id = ?, status = ?, idempotency_key = ?, notes = ? WHERE production_output_id = ? AND production_order_id = ? AND status = 'draft'",
                lot_code, manufactured_on, expires_on, good_quantity, defective_quantity, warehouse_id,
                warehouse_location_id, status, idempotency_key, notes, output_id, production_order_id);
    }

    public int delete_draft(long production_order_id, long output_id) {
        return jpa_query_executor.update(
                "DELETE FROM production.production_output WHERE production_order_id = ? AND production_output_id = ? AND status = 'draft'",
                production_order_id, output_id);
    }

    public int cancel(long production_order_id, long output_id) {
        return jpa_query_executor.update(
                "UPDATE production.production_output SET status = 'cancelled' WHERE production_order_id = ? AND production_output_id = ? AND status IN ('draft', 'pending_receipt') AND inventory_receipt_id IS NULL",
                production_order_id, output_id);
    }

    public int mark_failed(long production_order_id, long output_id) {
        return jpa_query_executor.update(
                "UPDATE production.production_output SET status = 'failed' WHERE production_order_id = ? AND production_output_id = ? AND status IN ('draft', 'pending_receipt') AND inventory_receipt_id IS NULL",
                production_order_id, output_id);
    }

    public boolean idempotency_key_exists(String idempotency_key) {
        Long count = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM production.production_output WHERE idempotency_key = ?", Long.class, idempotency_key);
        return count != null && count > 0;
    }

    public int mark_received(long production_order_id, long output_id, Long stock_lot_id,
                             Long receipt_id) {
        return jpa_query_executor.update(
                "UPDATE production.production_output SET status = 'received', inventory_stock_lot_id = ?, "
                        + "inventory_receipt_id = ?, received_at = now() "
                        + "WHERE production_output_id = ? AND production_order_id = ? "
                        + "AND status IN ('draft', 'pending_receipt')",
                stock_lot_id, receipt_id, output_id, production_order_id);
    }

    private String output_select_sql() {
        return "SELECT output.production_output_id, output.production_order_id, production_order.order_code, "
                + "output.stock_item_id, output.inventory_warehouse_id, output.inventory_warehouse_location_id, "
                + "output.lot_code_snapshot, output.manufactured_on, output.expires_on, output.good_quantity, "
                + "output.defective_quantity, output.inventory_stock_lot_id, output.inventory_receipt_id, "
                + "output.status, output.idempotency_key, output.recorded_at, output.received_at, output.notes "
                + "FROM production.production_output AS output "
                + "JOIN production.production_order AS production_order "
                + "ON production_order.production_order_id = output.production_order_id";
    }

    private production_output_response map_response(jpa_result_row result_set, int row_number) {
        return new production_output_response(
                result_set.getLong("production_output_id"),
                result_set.getLong("production_order_id"),
                result_set.getString("order_code"),
                result_set.getLong("stock_item_id"),
                result_set.getObject("inventory_warehouse_id", Long.class),
                result_set.getObject("inventory_warehouse_location_id", Long.class),
                result_set.getString("lot_code_snapshot"),
                result_set.getObject("manufactured_on", LocalDate.class),
                result_set.getObject("expires_on", LocalDate.class),
                result_set.getBigDecimal("good_quantity"),
                result_set.getBigDecimal("defective_quantity"),
                result_set.getObject("inventory_stock_lot_id", Long.class),
                result_set.getObject("inventory_receipt_id", Long.class),
                result_set.getString("status"),
                result_set.getString("idempotency_key"),
                result_set.get_instant("recorded_at"),
                result_set.get_instant("received_at"),
                result_set.getString("notes"));
    }

    record order_snapshot(long production_order_id, String order_code, long stock_item_id,
                          BigDecimal target_quantity, String status) {
    }
}
