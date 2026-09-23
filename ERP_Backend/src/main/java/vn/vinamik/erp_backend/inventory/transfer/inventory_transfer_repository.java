package vn.vinamik.erp_backend.inventory.transfer;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class inventory_transfer_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public inventory_transfer_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public long count(String search, Long source_warehouse_id, Long destination_warehouse_id, String status) {
        Long total = jpa_query_executor.queryForObject("SELECT count(*) " + common_where(), Long.class,
                search, search, search, search,
                source_warehouse_id, source_warehouse_id,
                destination_warehouse_id, destination_warehouse_id,
                status, status);
        return total == null ? 0 : total;
    }

    public List<transfer_summary> search(String search, Long source_warehouse_id, Long destination_warehouse_id,
                                         String status, int page_size, int offset) {
        return jpa_query_executor.query(
                "SELECT transfer.transfer_id, transfer.transfer_code, transfer.source_warehouse_id, "
                        + "source_warehouse.warehouse_code AS source_warehouse_code, transfer.destination_warehouse_id, "
                        + "destination_warehouse.warehouse_code AS destination_warehouse_code, transfer.status, "
                        + "count(line.transfer_line_id)::integer AS line_count, transfer.created_at, transfer.posted_at "
                        + summary_where()
                        + " GROUP BY transfer.transfer_id, source_warehouse.warehouse_code, destination_warehouse.warehouse_code "
                        + "ORDER BY transfer.created_at DESC, transfer.transfer_code LIMIT ? OFFSET ?",
                this::map_summary,
                search, search, search, search,
                source_warehouse_id, source_warehouse_id,
                destination_warehouse_id, destination_warehouse_id,
                status, status, page_size, offset);
    }

    public List<Long> find_by_idempotency_key(String idempotency_key) {
        return jpa_query_executor.query(
                "SELECT transfer_id FROM inventory.transfer WHERE idempotency_key = ?",
                (result_set, row_number) -> result_set.getLong("transfer_id"), idempotency_key);
    }

    public Long insert_transfer(String transfer_code, long source_warehouse_id, long destination_warehouse_id,
                                String idempotency_key, String notes, long actor_user_id) {
        Long transfer_id = jpa_query_executor.queryForObject(
                "INSERT INTO inventory.transfer (transfer_code, source_warehouse_id, destination_warehouse_id, status, idempotency_key, notes, created_by_user_id, updated_by_user_id) VALUES (?, ?, ?, 'draft', ?, ?, ?, ?) ON CONFLICT DO NOTHING RETURNING transfer_id",
                Long.class, transfer_code, source_warehouse_id, destination_warehouse_id,
                idempotency_key, notes, actor_user_id, actor_user_id);
        return transfer_id;
    }

    public boolean idempotency_key_exists(String idempotency_key, Long transfer_id) {
        Long count = transfer_id == null
                ? jpa_query_executor.queryForObject("SELECT count(*) FROM inventory.transfer WHERE idempotency_key = ?", Long.class, idempotency_key)
                : jpa_query_executor.queryForObject("SELECT count(*) FROM inventory.transfer WHERE idempotency_key = ? AND transfer_id <> ?", Long.class, idempotency_key, transfer_id);
        return count != null && count > 0;
    }

    public int update_transfer(long transfer_id, String transfer_code, long source_warehouse_id,
                               long destination_warehouse_id, String idempotency_key, String notes,
                               long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.transfer SET transfer_code = ?, source_warehouse_id = ?, destination_warehouse_id = ?, idempotency_key = ?, notes = ?, updated_at = now(), updated_by_user_id = ? WHERE transfer_id = ? AND status = 'draft'",
                transfer_code, source_warehouse_id, destination_warehouse_id, idempotency_key, notes,
                actor_user_id, transfer_id);
    }

    public int delete_lines(long transfer_id) {
        return jpa_query_executor.update("DELETE FROM inventory.transfer_line WHERE transfer_id = ?", transfer_id);
    }

    public int delete_draft(long transfer_id) {
        return jpa_query_executor.update("DELETE FROM inventory.transfer WHERE transfer_id = ? AND status = 'draft'", transfer_id);
    }

    public int cancel(long transfer_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.transfer SET status = 'cancelled', updated_at = now(), updated_by_user_id = ? WHERE transfer_id = ? AND status IN ('draft', 'pending')",
                actor_user_id, transfer_id);
    }
    public int mark_pending(long transfer_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.transfer SET status = 'pending', updated_at = now(), updated_by_user_id = ? WHERE transfer_id = ? AND status = 'draft'",
                actor_user_id, transfer_id);
    }

    public void insert_line(long transfer_id, int line_number, transfer_line_request line) {
        jpa_query_executor.update(
                "INSERT INTO inventory.transfer_line (transfer_id, line_number, stock_item_id, stock_lot_id, source_location_id, destination_location_id, quantity) VALUES (?, ?, ?, ?, ?, ?, ?)",
                transfer_id, line_number, line.stock_item_id(), line.stock_lot_id(),
                line.source_location_id(), line.destination_location_id(), line.quantity());
    }

    public void lock_balance(String lock_key) {
        jpa_query_executor.execute("SELECT pg_advisory_xact_lock(hashtext(?))", lock_key);
    }

    public Optional<BigDecimal> find_balance(long stock_item_id, long location_id, Long stock_lot_id) {
        List<BigDecimal> values = jpa_query_executor.query(
                "SELECT on_hand_quantity FROM inventory.stock_balance WHERE stock_item_id = ? AND warehouse_location_id = ? AND stock_lot_id IS NOT DISTINCT FROM ? FOR UPDATE",
                (result_set, row_number) -> result_set.getBigDecimal("on_hand_quantity"),
                stock_item_id, location_id, stock_lot_id);
        return values.isEmpty() ? Optional.empty() : Optional.ofNullable(values.getFirst());
    }

    public void insert_movement(long transfer_id, long transfer_line_id, long stock_item_id,
                                long location_id, Long stock_lot_id, BigDecimal quantity_delta,
                                long actor_user_id, String idempotency_key) {
        jpa_query_executor.update(
                "INSERT INTO inventory.stock_movement (source_document_type, source_document_id, source_document_line_id, movement_leg, movement_type, stock_item_id, warehouse_location_id, stock_lot_id, quantity_delta, posted_by_user_id, idempotency_key) VALUES ('transfer', ?, ?, ?, 'transfer', ?, ?, ?, ?, ?, ?)",
                transfer_id, transfer_line_id,
                quantity_delta.signum() < 0 ? "source" : "destination",
                stock_item_id, location_id, stock_lot_id, quantity_delta, actor_user_id, idempotency_key);
    }

    public int update_balance(long stock_item_id, long location_id, Long stock_lot_id, BigDecimal quantity_delta) {
        return jpa_query_executor.update(
                "UPDATE inventory.stock_balance SET on_hand_quantity = on_hand_quantity + ?, updated_at = now() WHERE stock_item_id = ? AND warehouse_location_id = ? AND stock_lot_id IS NOT DISTINCT FROM ?",
                quantity_delta, stock_item_id, location_id, stock_lot_id);
    }

    public void insert_balance(long stock_item_id, long location_id, Long stock_lot_id, BigDecimal quantity_delta) {
        jpa_query_executor.update(
                "INSERT INTO inventory.stock_balance (stock_item_id, warehouse_location_id, stock_lot_id, on_hand_quantity) VALUES (?, ?, ?, ?)",
                stock_item_id, location_id, stock_lot_id, quantity_delta);
    }

    public int mark_posted(long transfer_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.transfer SET status = 'posted', posted_at = now(), posted_by_user_id = ?, updated_at = now(), updated_by_user_id = ? WHERE transfer_id = ? AND status IN ('draft', 'pending')",
                actor_user_id, actor_user_id, transfer_id);
    }

    public transfer_response find(long transfer_id, boolean for_update) {
        List<transfer_response> headers = jpa_query_executor.query(
                "SELECT transfer.transfer_id, transfer.transfer_code, transfer.source_warehouse_id, source_warehouse.warehouse_code AS source_warehouse_code, transfer.destination_warehouse_id, destination_warehouse.warehouse_code AS destination_warehouse_code, transfer.status, transfer.idempotency_key, transfer.notes, transfer.posted_at, transfer.posted_by_user_id FROM inventory.transfer AS transfer JOIN inventory.warehouse AS source_warehouse ON source_warehouse.warehouse_id = transfer.source_warehouse_id JOIN inventory.warehouse AS destination_warehouse ON destination_warehouse.warehouse_id = transfer.destination_warehouse_id WHERE transfer.transfer_id = ?"
                        + (for_update ? " FOR UPDATE OF transfer" : ""),
                this::map_header, transfer_id);
        if (headers.isEmpty()) {
            throw new resource_not_found_exception("Transfer");
        }
        transfer_response header = headers.getFirst();
        return new transfer_response(header.transfer_id(), header.transfer_code(), header.source_warehouse_id(),
                header.source_warehouse_code(), header.destination_warehouse_id(), header.destination_warehouse_code(),
                header.status(), header.idempotency_key(), header.notes(), header.posted_at(), header.posted_by_user_id(),
                load_lines(transfer_id));
    }

    public boolean active_stock_item(long stock_item_id) {
        return exists("SELECT 1 FROM inventory.stock_item WHERE stock_item_id = ? AND status = 'active'", stock_item_id);
    }

    public boolean active_location(long warehouse_location_id, long warehouse_id) {
        return exists(
                "SELECT 1 FROM inventory.warehouse_location WHERE warehouse_location_id = ? AND warehouse_id = ? AND status = 'active'",
                warehouse_location_id, warehouse_id);
    }

    public boolean active_lot(long stock_lot_id, long stock_item_id) {
        return exists(
                "SELECT 1 FROM inventory.stock_lot WHERE stock_lot_id = ? AND stock_item_id = ? AND status = 'active'",
                stock_lot_id, stock_item_id);
    }

    public boolean active_warehouse(long warehouse_id) {
        return count_value("SELECT count(*) FROM inventory.warehouse WHERE warehouse_id = ? AND status = 'active'",
                warehouse_id);
    }

    public boolean transfer_code_exists(String transfer_code, Long transfer_id) {
        return count_value("SELECT count(*) FROM inventory.transfer WHERE transfer_code = ? AND (? IS NULL OR transfer_id <> ?)", transfer_code, transfer_id, transfer_id);
    }

    private List<transfer_line_response> load_lines(long transfer_id) {
        return jpa_query_executor.query(
                "SELECT line.transfer_line_id, line.line_number, line.stock_item_id, item.item_code, item.item_name, line.stock_lot_id, lot.lot_code, line.source_location_id, source_location.location_code AS source_location_code, line.destination_location_id, destination_location.location_code AS destination_location_code, line.quantity FROM inventory.transfer_line AS line JOIN inventory.stock_item AS item ON item.stock_item_id = line.stock_item_id JOIN inventory.warehouse_location AS source_location ON source_location.warehouse_location_id = line.source_location_id JOIN inventory.warehouse_location AS destination_location ON destination_location.warehouse_location_id = line.destination_location_id LEFT JOIN inventory.stock_lot AS lot ON lot.stock_lot_id = line.stock_lot_id WHERE line.transfer_id = ? ORDER BY line.line_number",
                (result_set, row_number) -> new transfer_line_response(
                        result_set.getLong("transfer_line_id"), result_set.getInt("line_number"),
                        result_set.getLong("stock_item_id"), result_set.getString("item_code"),
                        result_set.getString("item_name"), result_set.getObject("stock_lot_id", Long.class),
                        result_set.getString("lot_code"), result_set.getLong("source_location_id"),
                        result_set.getString("source_location_code"), result_set.getLong("destination_location_id"),
                        result_set.getString("destination_location_code"), result_set.getBigDecimal("quantity")),
                transfer_id);
    }

    private boolean exists(String sql, Object... parameters) {
        List<Integer> rows = jpa_query_executor.query(sql,
                (result_set, row_number) -> result_set.getInt(1), parameters);
        return !rows.isEmpty();
    }

    private boolean count_value(String sql, Object... parameters) {
        Long total = jpa_query_executor.queryForObject(sql, Long.class, parameters);
        return total != null && total > 0;
    }

    private String common_where() {
        return "FROM inventory.transfer AS transfer "
                + "JOIN inventory.warehouse AS source_warehouse ON source_warehouse.warehouse_id = transfer.source_warehouse_id "
                + "JOIN inventory.warehouse AS destination_warehouse ON destination_warehouse.warehouse_id = transfer.destination_warehouse_id "
                + "WHERE (CAST(? AS text) IS NULL OR lower(transfer.transfer_code) LIKE '%' || ? || '%' "
                + "OR lower(source_warehouse.warehouse_code) LIKE '%' || ? || '%' "
                + "OR lower(destination_warehouse.warehouse_code) LIKE '%' || ? || '%') "
                + "AND (CAST(? AS text) IS NULL OR transfer.source_warehouse_id = ?) "
                + "AND (CAST(? AS text) IS NULL OR transfer.destination_warehouse_id = ?) "
                + "AND (CAST(? AS text) IS NULL OR transfer.status = ?)";
    }

    private String summary_where() {
        return "FROM inventory.transfer AS transfer "
                + "JOIN inventory.warehouse AS source_warehouse ON source_warehouse.warehouse_id = transfer.source_warehouse_id "
                + "JOIN inventory.warehouse AS destination_warehouse ON destination_warehouse.warehouse_id = transfer.destination_warehouse_id "
                + "LEFT JOIN inventory.transfer_line AS line ON line.transfer_id = transfer.transfer_id "
                + "WHERE (CAST(? AS text) IS NULL OR lower(transfer.transfer_code) LIKE '%' || ? || '%' "
                + "OR lower(source_warehouse.warehouse_code) LIKE '%' || ? || '%' "
                + "OR lower(destination_warehouse.warehouse_code) LIKE '%' || ? || '%') "
                + "AND (CAST(? AS text) IS NULL OR transfer.source_warehouse_id = ?) "
                + "AND (CAST(? AS text) IS NULL OR transfer.destination_warehouse_id = ?) "
                + "AND (CAST(? AS text) IS NULL OR transfer.status = ?)";
    }

    private transfer_response map_header(jpa_result_row result_set, int row_number) {
        return new transfer_response(result_set.getLong("transfer_id"), result_set.getString("transfer_code"),
                result_set.getLong("source_warehouse_id"), result_set.getString("source_warehouse_code"),
                result_set.getLong("destination_warehouse_id"), result_set.getString("destination_warehouse_code"),
                result_set.getString("status"), result_set.getString("idempotency_key"),
                result_set.getString("notes"),
                result_set.get_instant("posted_at"),
                result_set.getObject("posted_by_user_id", Long.class), List.of());
    }

    private transfer_summary map_summary(jpa_result_row result_set, int row_number) {
        return new transfer_summary(result_set.getLong("transfer_id"), result_set.getString("transfer_code"),
                result_set.getLong("source_warehouse_id"), result_set.getString("source_warehouse_code"),
                result_set.getLong("destination_warehouse_id"), result_set.getString("destination_warehouse_code"),
                result_set.getString("status"), result_set.getInt("line_count"),
                result_set.get_instant("created_at"),
                result_set.get_instant("posted_at"));
    }

}
