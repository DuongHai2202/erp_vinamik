package vn.vinamik.erp_backend.inventory.stocktake;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class inventory_stocktake_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public inventory_stocktake_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public long count(String search, Long warehouse_id, String status) {
        Long total = jpa_query_executor.queryForObject("SELECT count(*) " + common_where(), Long.class,
                search, search, search, warehouse_id, warehouse_id, status, status);
        return total == null ? 0 : total;
    }

    public List<stocktake_summary> search(String search, Long warehouse_id, String status,
                                          int page_size, int offset) {
        return jpa_query_executor.query(
                "SELECT stocktake.stocktake_id, stocktake.stocktake_code, stocktake.warehouse_id, "
                        + "warehouse.warehouse_code, stocktake.status, "
                        + "count(line.stocktake_line_id)::integer AS line_count, "
                        + "count(line.counted_quantity)::integer AS counted_line_count, "
                        + "stocktake.started_at, stocktake.posted_at "
                        + summary_where()
                        + " GROUP BY stocktake.stocktake_id, warehouse.warehouse_code "
                        + "ORDER BY stocktake.started_at DESC NULLS LAST, stocktake.stocktake_code LIMIT ? OFFSET ?",
                this::map_summary,
                search, search, search, warehouse_id, warehouse_id, status, status, page_size, offset);
    }

    public long count_all_stocktakes() {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.stocktake", Long.class);
        return total == null ? 0 : total;
    }

    public List<fixture_warehouse> active_warehouses_for_fixture() {
        return jpa_query_executor.query(
                "SELECT warehouse_id, warehouse_code FROM inventory.warehouse WHERE status = 'active' ORDER BY warehouse_id LIMIT 6",
                (result_set, row_number) -> new fixture_warehouse(
                        result_set.getLong("warehouse_id"), result_set.getString("warehouse_code")));
    }

    public Optional<fixture_actor> active_super_admin_for_fixture() {
        return jpa_query_executor.query(
                "SELECT user_id, username FROM identity.user_account WHERE is_super_admin = true AND status = 'active' ORDER BY user_id LIMIT 1",
                (result_set, row_number) -> new fixture_actor(
                        result_set.getLong("user_id"), result_set.getString("username")))
                .stream().findFirst();
    }

    public boolean warehouse_exists_for_update(long warehouse_id) {
        List<Long> warehouses = jpa_query_executor.query(
                "SELECT warehouse_id FROM inventory.warehouse WHERE warehouse_id = ? FOR UPDATE",
                (result_set, row_number) -> result_set.getLong("warehouse_id"), warehouse_id);
        return !warehouses.isEmpty();
    }

    public boolean active_warehouse(long warehouse_id) {
        Long count = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.warehouse WHERE warehouse_id = ? AND status = 'active'",
                Long.class, warehouse_id);
        return count != null && count > 0;
    }

    public boolean has_active_stocktake(long warehouse_id) {
        Long count = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.stocktake WHERE warehouse_id = ? AND status IN ('draft', 'counting', 'submitted', 'approved')",
                Long.class, warehouse_id);
        return count != null && count > 0;
    }

    public long insert_stocktake(String stocktake_code, long warehouse_id, String notes, long actor_user_id) {
        Long stocktake_id = jpa_query_executor.queryForObject(
                "INSERT INTO inventory.stocktake (stocktake_code, warehouse_id, status, started_at, notes, created_by_user_id, updated_by_user_id) VALUES (?, ?, 'counting', now(), ?, ?, ?) RETURNING stocktake_id",
                Long.class, stocktake_code, warehouse_id, notes, actor_user_id, actor_user_id);
        if (stocktake_id == null) {
            throw new IllegalStateException("Stocktake identifier was not returned.");
        }
        return stocktake_id;
    }

    public void snapshot_balances(long stocktake_id, long warehouse_id) {
        jpa_query_executor.update(
                "INSERT INTO inventory.stocktake_line (stocktake_id, line_number, stock_item_id, warehouse_location_id, stock_lot_id, system_quantity) "
                        + "SELECT ?, row_number() OVER (ORDER BY balance.stock_item_id, balance.warehouse_location_id, balance.stock_lot_id NULLS FIRST)::integer, "
                        + "balance.stock_item_id, balance.warehouse_location_id, balance.stock_lot_id, balance.on_hand_quantity "
                        + "FROM inventory.stock_balance AS balance "
                        + "JOIN inventory.warehouse_location AS location ON location.warehouse_location_id = balance.warehouse_location_id "
                        + "WHERE location.warehouse_id = ? ORDER BY balance.stock_item_id, balance.warehouse_location_id, balance.stock_lot_id NULLS FIRST",
                stocktake_id, warehouse_id);
    }

    public int update_counted_line(long stocktake_id, long stocktake_line_id,
                                   BigDecimal counted_quantity, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.stocktake_line SET counted_quantity = ?, counted_at = now(), counted_by_user_id = ? WHERE stocktake_line_id = ? AND stocktake_id = ?",
                counted_quantity, actor_user_id, stocktake_line_id, stocktake_id);
    }

    public int count_missing_lines(long stocktake_id) {
        Integer count = jpa_query_executor.queryForObject(
                "SELECT count(*)::integer FROM inventory.stocktake_line WHERE stocktake_id = ? AND counted_quantity IS NULL",
                Integer.class, stocktake_id);
        return count == null ? 0 : count;
    }

    public int mark_submitted(long stocktake_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.stocktake SET status = 'submitted', submitted_at = now(), updated_at = now(), updated_by_user_id = ? WHERE stocktake_id = ? AND status = 'counting'",
                actor_user_id, stocktake_id);
    }

    public int mark_approved(long stocktake_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.stocktake SET status = 'approved', approved_at = now(), approved_by_user_id = ?, updated_at = now(), updated_by_user_id = ? WHERE stocktake_id = ? AND status = 'submitted'",
                actor_user_id, actor_user_id, stocktake_id);
    }

    public int mark_posted(long stocktake_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.stocktake SET status = 'posted', posted_at = now(), posted_by_user_id = ?, updated_at = now(), updated_by_user_id = ? WHERE stocktake_id = ? AND status = 'approved'",
                actor_user_id, actor_user_id, stocktake_id);
    }

    public int mark_cancelled(long stocktake_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.stocktake SET status = 'cancelled', updated_at = now(), updated_by_user_id = ? WHERE stocktake_id = ?",
                actor_user_id, stocktake_id);
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

    public void insert_movement(long stocktake_id, long stocktake_line_id, long stock_item_id,
                                long location_id, Long stock_lot_id, BigDecimal difference,
                                long actor_user_id, String idempotency_key) {
        jpa_query_executor.update(
                "INSERT INTO inventory.stock_movement (source_document_type, source_document_id, source_document_line_id, movement_leg, movement_type, stock_item_id, warehouse_location_id, stock_lot_id, quantity_delta, posted_by_user_id, idempotency_key) VALUES ('stocktake', ?, ?, 'adjustment', 'stocktake_adjustment', ?, ?, ?, ?, ?, ?)",
                stocktake_id, stocktake_line_id, stock_item_id, location_id, stock_lot_id,
                difference, actor_user_id, idempotency_key);
    }

    public int update_balance(long stock_item_id, long location_id, Long stock_lot_id, BigDecimal difference) {
        return jpa_query_executor.update(
                "UPDATE inventory.stock_balance SET on_hand_quantity = on_hand_quantity + ?, updated_at = now() WHERE stock_item_id = ? AND warehouse_location_id = ? AND stock_lot_id IS NOT DISTINCT FROM ?",
                difference, stock_item_id, location_id, stock_lot_id);
    }

    public void insert_balance(long stock_item_id, long location_id, Long stock_lot_id, BigDecimal difference) {
        jpa_query_executor.update(
                "INSERT INTO inventory.stock_balance (stock_item_id, warehouse_location_id, stock_lot_id, on_hand_quantity) VALUES (?, ?, ?, ?)",
                stock_item_id, location_id, stock_lot_id, difference);
    }

    public stocktake_response find(long stocktake_id, boolean for_update) {
        List<stocktake_response> headers = jpa_query_executor.query(
                "SELECT stocktake.stocktake_id, stocktake.stocktake_code, stocktake.warehouse_id, warehouse.warehouse_code, "
                        + "stocktake.status, stocktake.started_at, stocktake.submitted_at, stocktake.approved_at, "
                        + "stocktake.approved_by_user_id, stocktake.posted_at, stocktake.posted_by_user_id, stocktake.notes "
                        + "FROM inventory.stocktake AS stocktake "
                        + "JOIN inventory.warehouse AS warehouse ON warehouse.warehouse_id = stocktake.warehouse_id "
                        + "WHERE stocktake.stocktake_id = ?" + (for_update ? " FOR UPDATE OF stocktake" : ""),
                this::map_header, stocktake_id);
        if (headers.isEmpty()) {
            throw new resource_not_found_exception("Stocktake");
        }
        stocktake_response header = headers.getFirst();
        return new stocktake_response(header.stocktake_id(), header.stocktake_code(), header.warehouse_id(),
                header.warehouse_code(), header.status(), header.started_at(), header.submitted_at(),
                header.approved_at(), header.approved_by_user_id(), header.posted_at(), header.posted_by_user_id(),
                header.notes(), load_lines(stocktake_id));
    }

    public stocktake_line_response find_line(long stocktake_id, long stocktake_line_id) {
        List<stocktake_line_response> lines = jpa_query_executor.query(
                line_sql() + " WHERE line.stocktake_id = ? AND line.stocktake_line_id = ?",
                this::map_line, stocktake_id, stocktake_line_id);
        if (lines.isEmpty()) {
            throw new resource_not_found_exception("Stocktake line");
        }
        return lines.getFirst();
    }

    private List<stocktake_line_response> load_lines(long stocktake_id) {
        return jpa_query_executor.query(
                line_sql() + " WHERE line.stocktake_id = ? ORDER BY line.line_number",
                this::map_line, stocktake_id);
    }

    private String common_where() {
        return "FROM inventory.stocktake AS stocktake "
                + "JOIN inventory.warehouse AS warehouse ON warehouse.warehouse_id = stocktake.warehouse_id "
                + "WHERE (CAST(? AS text) IS NULL OR lower(stocktake.stocktake_code) LIKE '%' || ? || '%' "
                + "OR lower(warehouse.warehouse_code) LIKE '%' || ? || '%') "
                + "AND (CAST(? AS text) IS NULL OR stocktake.warehouse_id = ?) "
                + "AND (CAST(? AS text) IS NULL OR stocktake.status = ?)";
    }

    private String summary_where() {
        return "FROM inventory.stocktake AS stocktake "
                + "JOIN inventory.warehouse AS warehouse ON warehouse.warehouse_id = stocktake.warehouse_id "
                + "LEFT JOIN inventory.stocktake_line AS line ON line.stocktake_id = stocktake.stocktake_id "
                + "WHERE (CAST(? AS text) IS NULL OR lower(stocktake.stocktake_code) LIKE '%' || ? || '%' "
                + "OR lower(warehouse.warehouse_code) LIKE '%' || ? || '%') "
                + "AND (CAST(? AS text) IS NULL OR stocktake.warehouse_id = ?) "
                + "AND (CAST(? AS text) IS NULL OR stocktake.status = ?)";
    }

    private String line_sql() {
        return "SELECT line.stocktake_line_id, line.line_number, line.stock_item_id, item.item_code, item.item_name, "
                + "line.warehouse_location_id, location.location_code, line.stock_lot_id, lot.lot_code, "
                + "line.system_quantity, line.counted_quantity, line.counted_quantity - line.system_quantity AS difference_quantity, "
                + "line.counted_at, line.counted_by_user_id FROM inventory.stocktake_line AS line "
                + "JOIN inventory.stock_item AS item ON item.stock_item_id = line.stock_item_id "
                + "JOIN inventory.warehouse_location AS location ON location.warehouse_location_id = line.warehouse_location_id "
                + "LEFT JOIN inventory.stock_lot AS lot ON lot.stock_lot_id = line.stock_lot_id";
    }

    private stocktake_response map_header(jpa_result_row result_set, int row_number) {
        return new stocktake_response(result_set.getLong("stocktake_id"), result_set.getString("stocktake_code"),
                result_set.getLong("warehouse_id"), result_set.getString("warehouse_code"),
                result_set.getString("status"), result_set.get_instant("started_at"),
                result_set.get_instant("submitted_at"), result_set.get_instant("approved_at"),
                result_set.getObject("approved_by_user_id", Long.class),
                result_set.get_instant("posted_at"),
                result_set.getObject("posted_by_user_id", Long.class),
                result_set.getString("notes"), List.of());
    }

    private stocktake_line_response map_line(jpa_result_row result_set, int row_number) {
        return new stocktake_line_response(result_set.getLong("stocktake_line_id"), result_set.getInt("line_number"),
                result_set.getLong("stock_item_id"), result_set.getString("item_code"),
                result_set.getString("item_name"), result_set.getLong("warehouse_location_id"),
                result_set.getString("location_code"), result_set.getObject("stock_lot_id", Long.class),
                result_set.getString("lot_code"), result_set.getBigDecimal("system_quantity"),
                result_set.getBigDecimal("counted_quantity"), result_set.getBigDecimal("difference_quantity"),
                result_set.get_instant("counted_at"),
                result_set.getObject("counted_by_user_id", Long.class));
    }

    private stocktake_summary map_summary(jpa_result_row result_set, int row_number) {
        return new stocktake_summary(result_set.getLong("stocktake_id"), result_set.getString("stocktake_code"),
                result_set.getLong("warehouse_id"), result_set.getString("warehouse_code"),
                result_set.getString("status"), result_set.getInt("line_count"),
                result_set.getInt("counted_line_count"), result_set.get_instant("started_at"),
                result_set.get_instant("posted_at"));
    }


    public record fixture_warehouse(long warehouse_id, String warehouse_code) {
    }

    public record fixture_actor(long user_id, String username) {
    }

}
