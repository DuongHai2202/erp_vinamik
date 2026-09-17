package vn.vinamik.erp_backend.production.material_consumption;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;

import java.math.BigDecimal;
import java.time.Instant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class production_material_consumption_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public production_material_consumption_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public production_order_snapshot load_order(long production_order_id, boolean for_update) {
        List<production_order_snapshot> orders = jpa_query_executor.query(
                "SELECT production_order_id, order_code, status FROM production.production_order "
                        + "WHERE production_order_id = ?" + (for_update ? " FOR UPDATE" : ""),
                (result_set, row_number) -> new production_order_snapshot(
                        result_set.getLong("production_order_id"),
                        result_set.getString("order_code"),
                        result_set.getString("status")),
                production_order_id);
        if (orders.isEmpty()) {
            throw new resource_not_found_exception("Production order");
        }
        return orders.getFirst();
    }

    public Map<Long, material_requirement_snapshot> load_requirements(long production_order_id) {
        List<material_requirement_snapshot> rows = jpa_query_executor.query(
                "SELECT requirement.material_stock_item_id, requirement.unit_code_snapshot, requirement.required_quantity, "
                        + "bom_line.material_item_code_snapshot, bom_line.material_item_name_snapshot "
                        + "FROM production.production_order_material_requirement AS requirement "
                        + "JOIN production.bom_line AS bom_line ON bom_line.bom_line_id = requirement.bom_line_id "
                        + "WHERE requirement.production_order_id = ?",
                (result_set, row_number) -> new material_requirement_snapshot(
                        result_set.getLong("material_stock_item_id"),
                        result_set.getString("unit_code_snapshot"),
                        result_set.getBigDecimal("required_quantity"),
                        result_set.getString("material_item_code_snapshot"),
                        result_set.getString("material_item_name_snapshot")),
                production_order_id);
        Map<Long, material_requirement_snapshot> result = new LinkedHashMap<>();
        rows.forEach(row -> result.put(row.material_stock_item_id(), row));
        return result;
    }

    public Map<Long, BigDecimal> consumed_quantities(long production_order_id) {
        List<consumed_quantity_row> rows = jpa_query_executor.query(
                "SELECT material_stock_item_id, coalesce(sum(consumed_quantity), 0) AS consumed_quantity "
                        + "FROM production.material_consumption WHERE production_order_id = ? "
                        + "GROUP BY material_stock_item_id",
                (result_set, row_number) -> new consumed_quantity_row(
                        result_set.getLong("material_stock_item_id"),
                        result_set.getBigDecimal("consumed_quantity")),
                production_order_id);
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        rows.forEach(row -> result.put(row.material_stock_item_id(), row.consumed_quantity()));
        return result;
    }

    public material_consumption_response find_by_key(long production_order_id, String request_key, String order_code) {
        List<material_consumption_line_response> lines = query_lines(production_order_id, request_key);
        return lines.isEmpty() ? null
                : new material_consumption_response(production_order_id, order_code, request_key, lines);
    }

    public List<material_consumption_response> list_by_order(long production_order_id, String order_code) {
        List<consumption_line_with_key> rows = jpa_query_executor.query(
                "SELECT consumption.idempotency_key, consumption.material_consumption_id, consumption.inventory_issue_line_id, "
                        + "consumption.material_stock_item_id, bom_line.material_item_code_snapshot, "
                        + "bom_line.material_item_name_snapshot, consumption.unit_code_snapshot, "
                        + "consumption.consumed_quantity, consumption.consumed_at "
                        + "FROM production.material_consumption AS consumption "
                        + "JOIN production.production_order_material_requirement AS requirement "
                        + "ON requirement.production_order_id = consumption.production_order_id "
                        + "AND requirement.material_stock_item_id = consumption.material_stock_item_id "
                        + "JOIN production.bom_line AS bom_line ON bom_line.bom_line_id = requirement.bom_line_id "
                        + "WHERE consumption.production_order_id = ? "
                        + "ORDER BY consumption.idempotency_key, consumption.line_number",
                this::map_line_with_key, production_order_id);
        Map<String, List<material_consumption_line_response>> grouped = new LinkedHashMap<>();
        for (consumption_line_with_key row : rows) {
            grouped.computeIfAbsent(row.request_key(), ignored -> new ArrayList<>()).add(row.line());
        }
        return grouped.entrySet().stream()
                .map(entry -> new material_consumption_response(production_order_id, order_code,
                        entry.getKey(), entry.getValue()))
                .toList();
    }

    public void insert(long production_order_id, long material_stock_item_id, long inventory_issue_line_id,
                       String unit_code, BigDecimal consumed_quantity, long actor_user_id,
                       String request_key, int line_number, String notes) {
        int inserted = jpa_query_executor.update(
                "INSERT INTO production.material_consumption (production_order_id, material_stock_item_id, "
                        + "inventory_issue_line_id, unit_code_snapshot, consumed_quantity, recorded_by_user_id, "
                        + "idempotency_key, line_number, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                production_order_id, material_stock_item_id, inventory_issue_line_id, unit_code,
                consumed_quantity, actor_user_id, request_key, line_number, notes);
        if (inserted != 1) {
            throw new IllegalStateException("Material consumption line was not inserted.");
        }
    }

    private List<material_consumption_line_response> query_lines(long production_order_id, String request_key) {
        return jpa_query_executor.query(
                "SELECT consumption.material_consumption_id, consumption.inventory_issue_line_id, "
                        + "consumption.material_stock_item_id, bom_line.material_item_code_snapshot, "
                        + "bom_line.material_item_name_snapshot, consumption.unit_code_snapshot, "
                        + "consumption.consumed_quantity, consumption.consumed_at "
                        + "FROM production.material_consumption AS consumption "
                        + "JOIN production.production_order_material_requirement AS requirement "
                        + "ON requirement.production_order_id = consumption.production_order_id "
                        + "AND requirement.material_stock_item_id = consumption.material_stock_item_id "
                        + "JOIN production.bom_line AS bom_line ON bom_line.bom_line_id = requirement.bom_line_id "
                        + "WHERE consumption.production_order_id = ? AND consumption.idempotency_key = ? "
                        + "ORDER BY consumption.line_number",
                this::map_line, production_order_id, request_key);
    }

    private material_consumption_line_response map_line(jpa_result_row result_set, int row_number) {
        return new material_consumption_line_response(
                result_set.getLong("material_consumption_id"),
                result_set.getLong("inventory_issue_line_id"),
                result_set.getLong("material_stock_item_id"),
                result_set.getString("material_item_code_snapshot"),
                result_set.getString("material_item_name_snapshot"),
                result_set.getString("unit_code_snapshot"),
                result_set.getBigDecimal("consumed_quantity"),
                result_set.get_instant("consumed_at"));
    }

    private consumption_line_with_key map_line_with_key(jpa_result_row result_set, int row_number) {
        return new consumption_line_with_key(result_set.getString("idempotency_key"), map_line(result_set, row_number));
    }


    record production_order_snapshot(long production_order_id, String order_code, String status) {
    }

    record material_requirement_snapshot(long material_stock_item_id, String unit_code,
                                         BigDecimal required_quantity, String material_item_code,
                                         String material_item_name) {
    }

    private record consumed_quantity_row(long material_stock_item_id, BigDecimal consumed_quantity) {
    }

    private record consumption_line_with_key(String request_key, material_consumption_line_response line) {
    }
}
