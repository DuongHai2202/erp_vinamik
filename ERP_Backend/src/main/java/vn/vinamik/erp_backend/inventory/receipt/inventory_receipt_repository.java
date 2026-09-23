package vn.vinamik.erp_backend.inventory.receipt;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public class inventory_receipt_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public inventory_receipt_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public long count(String search, Long warehouse_id, String status) {
        Long total = jpa_query_executor.queryForObject("SELECT count(*) " + common_where(), Long.class,
                search, search, search, warehouse_id, warehouse_id, status, status);
        return total == null ? 0 : total;
    }

    public List<receipt_summary> search(String search, Long warehouse_id, String status, int page_size, int offset) {
        return jpa_query_executor.query(
                "SELECT receipt.receipt_id, receipt.receipt_code, receipt.warehouse_id, warehouse.warehouse_code, receipt.status, count(line.receipt_line_id)::integer AS line_count, receipt.created_at, receipt.posted_at "
                        + summary_where() + " GROUP BY receipt.receipt_id, warehouse.warehouse_code ORDER BY receipt.created_at DESC, receipt.receipt_code LIMIT ? OFFSET ?",
                this::map_summary, search, search, search, warehouse_id, warehouse_id, status, status, page_size, offset);
    }

    public List<Long> find_by_idempotency_key(String idempotency_key) {
        return jpa_query_executor.query("SELECT receipt_id FROM inventory.receipt WHERE idempotency_key = ?",
                (result_set, row_number) -> result_set.getLong("receipt_id"), idempotency_key);
    }

    public Long insert_receipt(String receipt_code, Long warehouse_id, Long supplier_id, String source_module,
                               Long source_document_id, String reference_number, String idempotency_key,
                               String notes, long actor_user_id) {
        Long receipt_id = jpa_query_executor.queryForObject(
                "INSERT INTO inventory.receipt (receipt_code, warehouse_id, supplier_id, source_module, source_document_id, reference_number, status, idempotency_key, notes, created_by_user_id, updated_by_user_id) VALUES (?, ?, ?, ?, ?, ?, 'draft', ?, ?, ?, ?) ON CONFLICT DO NOTHING RETURNING receipt_id",
                Long.class, receipt_code, warehouse_id, supplier_id, source_module, source_document_id,
                reference_number, idempotency_key, notes, actor_user_id, actor_user_id);
        return receipt_id;
    }

    public boolean idempotency_key_exists(String idempotency_key, Long receipt_id) {
        Long count = receipt_id == null
                ? jpa_query_executor.queryForObject("SELECT count(*) FROM inventory.receipt WHERE idempotency_key = ?", Long.class, idempotency_key)
                : jpa_query_executor.queryForObject("SELECT count(*) FROM inventory.receipt WHERE idempotency_key = ? AND receipt_id <> ?", Long.class, idempotency_key, receipt_id);
        return count != null && count > 0;
    }

    public int update_receipt(long receipt_id, String receipt_code, Long warehouse_id, Long supplier_id,
                              String source_module, Long source_document_id, String reference_number,
                              String idempotency_key, String notes, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.receipt SET receipt_code = ?, warehouse_id = ?, supplier_id = ?, source_module = ?, source_document_id = ?, reference_number = ?, idempotency_key = ?, notes = ?, updated_at = now(), updated_by_user_id = ? WHERE receipt_id = ? AND status = 'draft'",
                receipt_code, warehouse_id, supplier_id, source_module, source_document_id, reference_number,
                idempotency_key, notes, actor_user_id, receipt_id);
    }

    public int delete_lines(long receipt_id) {
        return jpa_query_executor.update("DELETE FROM inventory.receipt_line WHERE receipt_id = ?", receipt_id);
    }

    public int delete_draft(long receipt_id) {
        return jpa_query_executor.update("DELETE FROM inventory.receipt WHERE receipt_id = ? AND status = 'draft'", receipt_id);
    }

    public int cancel(long receipt_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.receipt SET status = 'cancelled', updated_at = now(), updated_by_user_id = ? WHERE receipt_id = ? AND status IN ('draft', 'pending')",
                actor_user_id, receipt_id);
    }
    public int mark_pending(long receipt_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.receipt SET status = 'pending', updated_at = now(), updated_by_user_id = ? WHERE receipt_id = ? AND status = 'draft'",
                actor_user_id, receipt_id);
    }

    public void insert_line(long receipt_id, int line_number, receipt_line_request line) {
        jpa_query_executor.update(
                "INSERT INTO inventory.receipt_line (receipt_id, line_number, stock_item_id, warehouse_location_id, stock_lot_id, quantity) VALUES (?, ?, ?, ?, ?, ?)",
                receipt_id, line_number, line.stock_item_id(), line.warehouse_location_id(), line.stock_lot_id(), line.quantity());
    }

    public int mark_posted(long receipt_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE inventory.receipt SET status = 'posted', posted_at = now(), posted_by_user_id = ?, updated_at = now(), updated_by_user_id = ? WHERE receipt_id = ? AND status IN ('draft', 'pending')",
                actor_user_id, actor_user_id, receipt_id);
    }

    public int movement_exists(long receipt_id, long receipt_line_id) {
        Integer count = jpa_query_executor.queryForObject(
                "SELECT count(*)::integer FROM inventory.stock_movement WHERE source_document_type = 'receipt' AND source_document_id = ? AND source_document_line_id = ? AND movement_leg = 'single'",
                Integer.class, receipt_id, receipt_line_id);
        return count == null ? 0 : count;
    }

    public void lock_balance(String lock_key) {
        jpa_query_executor.execute("SELECT pg_advisory_xact_lock(hashtext(?))", lock_key);
    }

    public void insert_movement(receipt_response receipt, receipt_line_response line, long actor_user_id, String idempotency_key) {
        jpa_query_executor.update(
                "INSERT INTO inventory.stock_movement (source_document_type, source_document_id, source_document_line_id, movement_leg, movement_type, stock_item_id, warehouse_location_id, stock_lot_id, quantity_delta, posted_by_user_id, idempotency_key) VALUES ('receipt', ?, ?, 'single', 'receipt', ?, ?, ?, ?, ?, ?)",
                receipt.receipt_id(), line.receipt_line_id(), line.stock_item_id(), line.warehouse_location_id(),
                line.stock_lot_id(), line.quantity(), actor_user_id, idempotency_key);
    }

    public int increment_balance(receipt_line_response line) {
        return jpa_query_executor.update(
                "UPDATE inventory.stock_balance SET on_hand_quantity = on_hand_quantity + ?, updated_at = now() WHERE stock_item_id = ? AND warehouse_location_id = ? AND stock_lot_id IS NOT DISTINCT FROM ?",
                line.quantity(), line.stock_item_id(), line.warehouse_location_id(), line.stock_lot_id());
    }

    public void insert_balance(receipt_line_response line) {
        jpa_query_executor.update(
                "INSERT INTO inventory.stock_balance (stock_item_id, warehouse_location_id, stock_lot_id, on_hand_quantity) VALUES (?, ?, ?, ?)",
                line.stock_item_id(), line.warehouse_location_id(), line.stock_lot_id(), line.quantity());
    }

    public receipt_response find(long receipt_id, boolean for_update) {
        List<receipt_response> headers = jpa_query_executor.query(
                "SELECT receipt.receipt_id, receipt.receipt_code, receipt.warehouse_id, warehouse.warehouse_code, receipt.supplier_id, supplier.supplier_code, receipt.source_module, receipt.source_document_id, receipt.reference_number, receipt.status, receipt.idempotency_key, receipt.notes, receipt.posted_at, receipt.posted_by_user_id FROM inventory.receipt AS receipt JOIN inventory.warehouse AS warehouse ON warehouse.warehouse_id = receipt.warehouse_id LEFT JOIN inventory.supplier AS supplier ON supplier.supplier_id = receipt.supplier_id WHERE receipt.receipt_id = ?"
                        + (for_update ? " FOR UPDATE OF receipt" : ""), this::map_header, receipt_id);
        if (headers.isEmpty()) {
            throw new resource_not_found_exception("Receipt");
        }
        receipt_response header = headers.getFirst();
        return new receipt_response(header.receipt_id(), header.receipt_code(), header.warehouse_id(), header.warehouse_code(),
                header.supplier_id(), header.supplier_code(), header.source_module(), header.source_document_id(),
                header.reference_number(), header.status(), header.idempotency_key(), header.notes(), header.posted_at(),
                header.posted_by_user_id(), load_lines(receipt_id));
    }

    public boolean active_stock_item(long stock_item_id) {
        return exists("SELECT 1 FROM inventory.stock_item WHERE stock_item_id = ? AND status = 'active'", stock_item_id);
    }

    public boolean active_location(long warehouse_location_id, long warehouse_id) {
        return exists("SELECT 1 FROM inventory.warehouse_location WHERE warehouse_location_id = ? AND warehouse_id = ? AND status = 'active'",
                warehouse_location_id, warehouse_id);
    }

    public boolean active_lot(long stock_lot_id, long stock_item_id) {
        return exists("SELECT 1 FROM inventory.stock_lot WHERE stock_lot_id = ? AND stock_item_id = ? AND status = 'active'",
                stock_lot_id, stock_item_id);
    }

    public boolean active_warehouse(long warehouse_id) {
        return count("SELECT count(*) FROM inventory.warehouse WHERE warehouse_id = ? AND status = 'active'", warehouse_id);
    }

    public boolean active_supplier(long supplier_id) {
        return count("SELECT count(*) FROM inventory.supplier WHERE supplier_id = ? AND status = 'active'", supplier_id);
    }

    public boolean receipt_code_exists(String receipt_code, Long receipt_id) {
        Long total = receipt_id == null
                ? jpa_query_executor.queryForObject("SELECT count(*) FROM inventory.receipt WHERE receipt_code = ?", Long.class, receipt_code)
                : jpa_query_executor.queryForObject("SELECT count(*) FROM inventory.receipt WHERE receipt_code = ? AND receipt_id <> ?", Long.class, receipt_code, receipt_id);
        return total != null && total > 0;
    }

    private List<receipt_line_response> load_lines(long receipt_id) {
        return jpa_query_executor.query(
                "SELECT line.receipt_line_id, line.line_number, line.stock_item_id, item.item_code, item.item_name, line.warehouse_location_id, location.location_code, line.stock_lot_id, lot.lot_code, line.quantity FROM inventory.receipt_line AS line JOIN inventory.stock_item AS item ON item.stock_item_id = line.stock_item_id JOIN inventory.warehouse_location AS location ON location.warehouse_location_id = line.warehouse_location_id LEFT JOIN inventory.stock_lot AS lot ON lot.stock_lot_id = line.stock_lot_id WHERE line.receipt_id = ? ORDER BY line.line_number",
                (result_set, row_number) -> new receipt_line_response(
                        result_set.getLong("receipt_line_id"), result_set.getInt("line_number"),
                        result_set.getLong("stock_item_id"), result_set.getString("item_code"), result_set.getString("item_name"),
                        result_set.getLong("warehouse_location_id"), result_set.getString("location_code"),
                        result_set.getObject("stock_lot_id", Long.class), result_set.getString("lot_code"),
                        result_set.getBigDecimal("quantity")), receipt_id);
    }

    private boolean exists(String sql, Object... parameters) {
        List<Integer> rows = jpa_query_executor.query(sql, (result_set, row_number) -> result_set.getInt(1), parameters);
        return !rows.isEmpty();
    }

    private boolean count(String sql, Object... parameters) {
        Long total = jpa_query_executor.queryForObject(sql, Long.class, parameters);
        return total != null && total > 0;
    }

    private String common_where() {
        return "FROM inventory.receipt AS receipt JOIN inventory.warehouse AS warehouse ON warehouse.warehouse_id = receipt.warehouse_id "
                + "WHERE (CAST(? AS text) IS NULL OR lower(receipt.receipt_code) LIKE '%' || ? || '%' OR lower(warehouse.warehouse_code) LIKE '%' || ? || '%') "
                + "AND (CAST(? AS text) IS NULL OR receipt.warehouse_id = ?) AND (CAST(? AS text) IS NULL OR receipt.status = ?)";
    }

    private String summary_where() {
        return "FROM inventory.receipt AS receipt JOIN inventory.warehouse AS warehouse ON warehouse.warehouse_id = receipt.warehouse_id "
                + "LEFT JOIN inventory.receipt_line AS line ON line.receipt_id = receipt.receipt_id "
                + "WHERE (CAST(? AS text) IS NULL OR lower(receipt.receipt_code) LIKE '%' || ? || '%' OR lower(warehouse.warehouse_code) LIKE '%' || ? || '%') "
                + "AND (CAST(? AS text) IS NULL OR receipt.warehouse_id = ?) AND (CAST(? AS text) IS NULL OR receipt.status = ?)";
    }

    private receipt_response map_header(jpa_result_row result_set, int row_number) {
        return new receipt_response(result_set.getLong("receipt_id"), result_set.getString("receipt_code"),
                result_set.getLong("warehouse_id"), result_set.getString("warehouse_code"),
                result_set.getObject("supplier_id", Long.class), result_set.getString("supplier_code"),
                result_set.getString("source_module"), result_set.getObject("source_document_id", Long.class),
                result_set.getString("reference_number"), result_set.getString("status"),
                result_set.getString("idempotency_key"), result_set.getString("notes"),
                result_set.get_instant("posted_at"),
                result_set.getObject("posted_by_user_id", Long.class), List.of());
    }

    private receipt_summary map_summary(jpa_result_row result_set, int row_number) {
        Instant created_at = result_set.get_instant("created_at");
        Instant posted_at = result_set.get_instant("posted_at");
        return new receipt_summary(result_set.getLong("receipt_id"), result_set.getString("receipt_code"),
                result_set.getLong("warehouse_id"), result_set.getString("warehouse_code"),
                result_set.getString("status"), result_set.getInt("line_count"), created_at, posted_at);
    }
}
