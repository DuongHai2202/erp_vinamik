package vn.vinamik.erp_backend.inventory.issue;

import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;

import java.math.BigDecimal;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;


import java.time.Instant;
import java.util.List;

@Repository
public class inventory_issue_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public inventory_issue_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public long count(String search, Long warehouse_id, String status) {
        Long total = jpa_query_executor.queryForObject("SELECT count(*) " + common_where(), Long.class,
                search, search, search, warehouse_id, warehouse_id, status, status);
        return total == null ? 0 : total;
    }

    public List<issue_summary> search(String search, Long warehouse_id, String status, int page_size, int offset) {
        return jpa_query_executor.query(
                "SELECT issue.issue_id, issue.issue_code, issue.warehouse_id, warehouse.warehouse_code, issue.status, count(line.issue_line_id)::integer AS line_count, issue.created_at, issue.posted_at "
                        + summary_where() + " GROUP BY issue.issue_id, warehouse.warehouse_code ORDER BY issue.created_at DESC, issue.issue_code LIMIT ? OFFSET ?",
                this::map_summary,
                search, search, search, warehouse_id, warehouse_id, status, status, page_size, offset);
    }

    public List<Long> find_by_idempotency_key(String idempotency_key) {
        return jpa_query_executor.query("SELECT issue_id FROM inventory.issue WHERE idempotency_key = ?",
                (result_set, row_number) -> result_set.getLong("issue_id"), idempotency_key);
    }

    public Long insert_issue(String issue_code, Long warehouse_id, String source_module, Long source_document_id,
                             String reason_code, String idempotency_key, String notes, long actor_user_id) {
        Long issue_id = jpa_query_executor.queryForObject(
                "INSERT INTO inventory.issue (issue_code, warehouse_id, source_module, source_document_id, reason_code, status, idempotency_key, notes, created_by_user_id, updated_by_user_id) VALUES (?, ?, ?, ?, ?, 'draft', ?, ?, ?, ?) ON CONFLICT DO NOTHING RETURNING issue_id",
                Long.class, issue_code, warehouse_id, source_module, source_document_id, reason_code,
                idempotency_key, notes, actor_user_id, actor_user_id);
        return issue_id;
    }

    public void insert_line(long issue_id, int line_number, issue_line_request line) {
        jpa_query_executor.update(
                "INSERT INTO inventory.issue_line (issue_id, line_number, stock_item_id, warehouse_location_id, stock_lot_id, quantity) VALUES (?, ?, ?, ?, ?, ?)",
                issue_id, line_number, line.stock_item_id(), line.warehouse_location_id(), line.stock_lot_id(), line.quantity());
    }

    public int mark_posted(long issue_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.issue SET status = 'posted', posted_at = now(), posted_by_user_id = ?, updated_at = now(), updated_by_user_id = ? WHERE issue_id = ? AND status IN ('draft', 'pending')",
                actor_user_id, actor_user_id, issue_id);
    }

    public int movement_exists(long issue_id, long issue_line_id) {
        Integer count = jpa_query_executor.queryForObject(
                "SELECT count(*)::integer FROM inventory.stock_movement WHERE source_document_type = 'issue' AND source_document_id = ? AND source_document_line_id = ? AND movement_leg = 'single'",
                Integer.class, issue_id, issue_line_id);
        return count == null ? 0 : count;
    }

    public void lock_balance(String lock_key) {
        jpa_query_executor.execute("SELECT pg_advisory_xact_lock(hashtext(?))", lock_key);
    }

    public List<BigDecimal> lock_balance_row(issue_line_response line) {
        return jpa_query_executor.query(
                "SELECT on_hand_quantity FROM inventory.stock_balance WHERE stock_item_id = ? AND warehouse_location_id = ? AND stock_lot_id IS NOT DISTINCT FROM ? FOR UPDATE",
                (result_set, row_number) -> result_set.getBigDecimal("on_hand_quantity"),
                line.stock_item_id(), line.warehouse_location_id(), line.stock_lot_id());
    }

    public void insert_movement(issue_response issue, issue_line_response line, long actor_user_id, String idempotency_key) {
        jpa_query_executor.update(
                "INSERT INTO inventory.stock_movement (source_document_type, source_document_id, source_document_line_id, movement_leg, movement_type, stock_item_id, warehouse_location_id, stock_lot_id, quantity_delta, posted_by_user_id, idempotency_key) VALUES ('issue', ?, ?, 'single', 'issue', ?, ?, ?, ?, ?, ?)",
                issue.issue_id(), line.issue_line_id(), line.stock_item_id(), line.warehouse_location_id(),
                line.stock_lot_id(), line.quantity().negate(), actor_user_id, idempotency_key);
    }

    public void decrement_balance(issue_line_response line) {
        jpa_query_executor.update(
                "UPDATE inventory.stock_balance SET on_hand_quantity = on_hand_quantity - ?, updated_at = now() WHERE stock_item_id = ? AND warehouse_location_id = ? AND stock_lot_id IS NOT DISTINCT FROM ?",
                line.quantity(), line.stock_item_id(), line.warehouse_location_id(), line.stock_lot_id());
    }

    public boolean active_stock_item(long stock_item_id) {
        Integer count = jpa_query_executor.queryForObject(
                "SELECT count(*)::integer FROM inventory.stock_item WHERE stock_item_id = ? AND status = 'active'",
                Integer.class, stock_item_id);
        return count != null && count > 0;
    }

    public boolean active_location(long warehouse_location_id, long warehouse_id) {
        Integer count = jpa_query_executor.queryForObject(
                "SELECT count(*)::integer FROM inventory.warehouse_location WHERE warehouse_location_id = ? AND warehouse_id = ? AND status = 'active'",
                Integer.class, warehouse_location_id, warehouse_id);
        return count != null && count > 0;
    }

    public boolean active_lot(long stock_lot_id, long stock_item_id) {
        Integer count = jpa_query_executor.queryForObject(
                "SELECT count(*)::integer FROM inventory.stock_lot WHERE stock_lot_id = ? AND stock_item_id = ? AND status = 'active'",
                Integer.class, stock_lot_id, stock_item_id);
        return count != null && count > 0;
    }

    public boolean active_warehouse(long warehouse_id) {
        Long count = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.warehouse WHERE warehouse_id = ? AND status = 'active'",
                Long.class, warehouse_id);
        return count != null && count > 0;
    }

    public boolean issue_code_exists(String issue_code) {
        Long count = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM inventory.issue WHERE issue_code = ?", Long.class, issue_code);
        return count != null && count > 0;
    }

    private issue_response find_issue(long issue_id, boolean for_update) {
        List<issue_response> headers = jpa_query_executor.query(
                "SELECT issue.issue_id, issue.issue_code, issue.warehouse_id, warehouse.warehouse_code, issue.source_module, issue.source_document_id, issue.reason_code, issue.status, issue.idempotency_key, issue.notes, issue.posted_at, issue.posted_by_user_id FROM inventory.issue AS issue JOIN inventory.warehouse AS warehouse ON warehouse.warehouse_id = issue.warehouse_id WHERE issue.issue_id = ?"
                        + (for_update ? " FOR UPDATE OF issue" : ""),
                this::map_header, issue_id);
        if (headers.isEmpty()) {
            throw new resource_not_found_exception("Issue");
        }
        issue_response header = headers.getFirst();
        return new issue_response(header.issue_id(), header.issue_code(), header.warehouse_id(), header.warehouse_code(),
                header.source_module(), header.source_document_id(), header.reason_code(), header.status(),
                header.idempotency_key(), header.notes(), header.posted_at(), header.posted_by_user_id(), load_lines(issue_id));
    }

    public issue_response find(long issue_id, boolean for_update) {
        return find_issue(issue_id, for_update);
    }

    private List<issue_line_response> load_lines(long issue_id) {
        return jpa_query_executor.query(
                "SELECT line.issue_line_id, line.line_number, line.stock_item_id, item.item_code, item.item_name, line.warehouse_location_id, location.location_code, line.stock_lot_id, lot.lot_code, line.quantity FROM inventory.issue_line AS line JOIN inventory.stock_item AS item ON item.stock_item_id = line.stock_item_id JOIN inventory.warehouse_location AS location ON location.warehouse_location_id = line.warehouse_location_id LEFT JOIN inventory.stock_lot AS lot ON lot.stock_lot_id = line.stock_lot_id WHERE line.issue_id = ? ORDER BY line.line_number",
                (result_set, row_number) -> new issue_line_response(
                        result_set.getLong("issue_line_id"), result_set.getInt("line_number"),
                        result_set.getLong("stock_item_id"), result_set.getString("item_code"),
                        result_set.getString("item_name"), result_set.getLong("warehouse_location_id"),
                        result_set.getString("location_code"), result_set.getObject("stock_lot_id", Long.class),
                        result_set.getString("lot_code"), result_set.getBigDecimal("quantity")), issue_id);
    }

    private String common_where() {
        return "FROM inventory.issue AS issue JOIN inventory.warehouse AS warehouse ON warehouse.warehouse_id = issue.warehouse_id "
                + "WHERE (CAST(? AS text) IS NULL OR lower(issue.issue_code) LIKE '%' || ? || '%' OR lower(warehouse.warehouse_code) LIKE '%' || ? || '%') "
                + "AND (CAST(? AS text) IS NULL OR issue.warehouse_id = ?) AND (CAST(? AS text) IS NULL OR issue.status = ?)";
    }

    private String summary_where() {
        return "FROM inventory.issue AS issue JOIN inventory.warehouse AS warehouse ON warehouse.warehouse_id = issue.warehouse_id "
                + "LEFT JOIN inventory.issue_line AS line ON line.issue_id = issue.issue_id "
                + "WHERE (CAST(? AS text) IS NULL OR lower(issue.issue_code) LIKE '%' || ? || '%' OR lower(warehouse.warehouse_code) LIKE '%' || ? || '%') "
                + "AND (CAST(? AS text) IS NULL OR issue.warehouse_id = ?) AND (CAST(? AS text) IS NULL OR issue.status = ?)";
    }

    private issue_response map_header(jpa_result_row result_set, int row_number) {
        return new issue_response(
                result_set.getLong("issue_id"), result_set.getString("issue_code"), result_set.getLong("warehouse_id"),
                result_set.getString("warehouse_code"), result_set.getString("source_module"),
                result_set.getObject("source_document_id", Long.class), result_set.getString("reason_code"),
                result_set.getString("status"), result_set.getString("idempotency_key"),
                result_set.getString("notes"), result_set.get_instant("posted_at"),
                result_set.getObject("posted_by_user_id", Long.class), List.of());
    }

    private issue_summary map_summary(jpa_result_row result_set, int row_number) {
        return new issue_summary(
                result_set.getLong("issue_id"), result_set.getString("issue_code"),
                result_set.getLong("warehouse_id"), result_set.getString("warehouse_code"),
                result_set.getString("status"), result_set.getInt("line_count"),
                result_set.get_instant("created_at"), result_set.get_instant("posted_at"));
    }

}



