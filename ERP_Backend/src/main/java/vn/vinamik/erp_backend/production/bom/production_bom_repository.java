package vn.vinamik.erp_backend.production.bom;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.util.List;

@Repository
public class production_bom_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public production_bom_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public long count(String search, Long stock_item_id, String status) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM production.bom AS bom "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(bom.bom_code) LIKE '%' || ? || '%' "
                        + "OR lower(bom.product_item_code_snapshot) LIKE '%' || ? || '%' "
                        + "OR lower(bom.product_item_name_snapshot) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR bom.stock_item_id = ?) "
                        + "AND (CAST(? AS text) IS NULL OR bom.status = ?)",
                Long.class, search, search, search, search, stock_item_id, stock_item_id, status, status);
        return total == null ? 0 : total;
    }

    public List<bom_summary> search(String search, Long stock_item_id, String status,
                                     int page_size, int offset) {
        return jpa_query_executor.query(
                "SELECT bom.bom_id, bom.bom_code, bom.stock_item_id, "
                        + "bom.product_item_code_snapshot AS item_code, "
                        + "bom.product_item_name_snapshot AS item_name, bom.version_number, "
                        + "bom.valid_from, bom.valid_to, bom.status, "
                        + "count(line.bom_line_id)::integer AS line_count "
                        + "FROM production.bom AS bom "
                        + "LEFT JOIN production.bom_line AS line ON line.bom_id = bom.bom_id "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(bom.bom_code) LIKE '%' || ? || '%' "
                        + "OR lower(bom.product_item_code_snapshot) LIKE '%' || ? || '%' "
                        + "OR lower(bom.product_item_name_snapshot) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR bom.stock_item_id = ?) "
                        + "AND (CAST(? AS text) IS NULL OR bom.status = ?) "
                        + "GROUP BY bom.bom_id "
                        + "ORDER BY bom.product_item_code_snapshot, bom.version_number DESC LIMIT ? OFFSET ?",
                this::map_summary, search, search, search, search, stock_item_id, stock_item_id,
                status, status, page_size, offset);
    }

    public bom_response find(long bom_id) {
        List<bom_response> headers = jpa_query_executor.query(
                "SELECT bom.bom_id, bom.bom_code, bom.stock_item_id, "
                        + "bom.product_item_code_snapshot AS item_code, "
                        + "bom.product_item_name_snapshot AS item_name, bom.version_number, "
                        + "bom.base_quantity, bom.unit_code_snapshot, bom.valid_from, bom.valid_to, "
                        + "bom.status, bom.notes FROM production.bom AS bom WHERE bom.bom_id = ?",
                this::map_header, bom_id);
        if (headers.isEmpty()) {
            throw new resource_not_found_exception("BOM");
        }
        bom_response header = headers.getFirst();
        List<bom_line_response> lines = jpa_query_executor.query(
                "SELECT line.bom_line_id, line.line_number, line.material_stock_item_id, "
                        + "line.material_item_code_snapshot AS item_code, "
                        + "line.material_item_name_snapshot AS item_name, line.unit_code_snapshot, "
                        + "line.quantity_per_base, line.scrap_percent, line.notes "
                        + "FROM production.bom_line AS line WHERE line.bom_id = ? ORDER BY line.line_number",
                (result_set, row_number) -> new bom_line_response(
                        result_set.getLong("bom_line_id"), result_set.getInt("line_number"),
                        result_set.getLong("material_stock_item_id"), result_set.getString("item_code"),
                        result_set.getString("item_name"), result_set.getString("unit_code_snapshot"),
                        result_set.getBigDecimal("quantity_per_base"),
                        result_set.getBigDecimal("scrap_percent"), result_set.getString("notes")),
                bom_id);
        return new bom_response(header.bom_id(), header.bom_code(), header.stock_item_id(),
                header.product_item_code(), header.product_item_name(), header.version_number(),
                header.base_quantity(), header.unit_code_snapshot(), header.valid_from(), header.valid_to(),
                header.status(), header.notes(), lines);
    }

    public long insert(String bom_code, long stock_item_id, String item_code, String item_name,
                       int version_number, BigDecimal base_quantity, String unit_code,
                       LocalDate valid_from, LocalDate valid_to, String notes, long actor_user_id) {
        Long bom_id = jpa_query_executor.queryForObject(
                "INSERT INTO production.bom (bom_code, stock_item_id, product_item_code_snapshot, "
                        + "product_item_name_snapshot, version_number, base_quantity, unit_code_snapshot, "
                        + "valid_from, valid_to, status, notes, created_by_user_id, updated_by_user_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'draft', ?, ?, ?) RETURNING bom_id",
                Long.class, bom_code, stock_item_id, item_code, item_name, version_number,
                base_quantity, unit_code, valid_from, valid_to, notes, actor_user_id, actor_user_id);
        if (bom_id == null) {
            throw new IllegalStateException("BOM identifier was not returned.");
        }
        return bom_id;
    }

    public int update(long bom_id, String bom_code, long stock_item_id, String item_code, String item_name,
                      int version_number, BigDecimal base_quantity, String unit_code,
                      LocalDate valid_from, LocalDate valid_to, String notes, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE production.bom SET bom_code = ?, stock_item_id = ?, "
                        + "product_item_code_snapshot = ?, product_item_name_snapshot = ?, "
                        + "version_number = ?, base_quantity = ?, unit_code_snapshot = ?, "
                        + "valid_from = ?, valid_to = ?, notes = ?, updated_at = now(), "
                        + "updated_by_user_id = ? WHERE bom_id = ? AND status = 'draft'",
                bom_code, stock_item_id, item_code, item_name, version_number, base_quantity,
                unit_code, valid_from, valid_to, notes, actor_user_id, bom_id);
    }

    public void delete_lines(long bom_id) {
        jpa_query_executor.update("DELETE FROM production.bom_line WHERE bom_id = ?", bom_id);
    }

    public void insert_line(long bom_id, int line_number, bom_line_request line,
                            String item_code, String item_name, String unit_code, BigDecimal scrap_percent,
                            String notes) {
        jpa_query_executor.update(
                "INSERT INTO production.bom_line (bom_id, line_number, material_stock_item_id, "
                        + "material_item_code_snapshot, material_item_name_snapshot, unit_code_snapshot, "
                        + "quantity_per_base, scrap_percent, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                bom_id, line_number, line.material_stock_item_id(), item_code, item_name, unit_code,
                line.quantity_per_base(), scrap_percent, notes);
    }

    public int change_status(long bom_id, String status, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE production.bom SET status = ?, updated_at = now(), updated_by_user_id = ? WHERE bom_id = ?",
                status, actor_user_id, bom_id);
    }

    public boolean bom_code_version_exists(String bom_code, int version_number, Long bom_id) {
        Long count = bom_id == null
                ? jpa_query_executor.queryForObject(
                        "SELECT count(*) FROM production.bom WHERE bom_code = ? AND version_number = ?",
                        Long.class, bom_code, version_number)
                : jpa_query_executor.queryForObject(
                        "SELECT count(*) FROM production.bom "
                                + "WHERE bom_code = ? AND version_number = ? AND bom_id <> ?",
                        Long.class, bom_code, version_number, bom_id);
        return count != null && count > 0;
    }

    public boolean has_active_overlap(long bom_id) {
        Integer count = jpa_query_executor.queryForObject(
                "SELECT count(*)::integer FROM production.bom AS candidate "
                        + "JOIN production.bom AS existing "
                        + "ON existing.stock_item_id = candidate.stock_item_id "
                        + "AND existing.bom_id <> candidate.bom_id "
                        + "WHERE candidate.bom_id = ? AND existing.status = 'active' "
                        + "AND existing.valid_from <= COALESCE(candidate.valid_to, DATE '9999-12-31') "
                        + "AND COALESCE(existing.valid_to, DATE '9999-12-31') >= candidate.valid_from",
                Integer.class, bom_id);
        return count != null && count > 0;
    }

    public String current_status(long bom_id) {
        List<String> statuses = jpa_query_executor.query(
                "SELECT status FROM production.bom WHERE bom_id = ?",
                (result_set, row_number) -> result_set.getString("status"), bom_id);
        if (statuses.isEmpty()) {
            throw new resource_not_found_exception("BOM");
        }
        return statuses.getFirst();
    }

    private bom_response map_header(jpa_result_row result_set, int row_number) {
        return new bom_response(result_set.getLong("bom_id"), result_set.getString("bom_code"),
                result_set.getLong("stock_item_id"), result_set.getString("item_code"),
                result_set.getString("item_name"), result_set.getInt("version_number"),
                result_set.getBigDecimal("base_quantity"), result_set.getString("unit_code_snapshot"),
                to_local_date(result_set, "valid_from"), to_local_date(result_set, "valid_to"),
                result_set.getString("status"), result_set.getString("notes"), List.of());
    }

    private bom_summary map_summary(jpa_result_row result_set, int row_number) {
        return new bom_summary(result_set.getLong("bom_id"), result_set.getString("bom_code"),
                result_set.getLong("stock_item_id"), result_set.getString("item_code"),
                result_set.getString("item_name"), result_set.getInt("version_number"),
                to_local_date(result_set, "valid_from"), to_local_date(result_set, "valid_to"),
                result_set.getString("status"), result_set.getInt("line_count"));
    }

    private LocalDate to_local_date(jpa_result_row result_set, String column) {
        LocalDate date = result_set.get_local_date(column);
        return date == null ? null : date;
    }
}
