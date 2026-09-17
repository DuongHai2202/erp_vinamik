package vn.vinamik.erp_backend.production.plan;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;


import java.time.LocalDate;
import java.util.List;

@Repository
public class production_plan_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public production_plan_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public long count(String search, String status) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM production.production_plan AS plan "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(plan.plan_code) LIKE '%' || ? || '%' "
                        + "OR lower(plan.plan_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR plan.status = ?)",
                Long.class, search, search, search, status, status);
        return total == null ? 0 : total;
    }

    public List<production_plan_summary> search(String search, String status, int page_size, int offset) {
        return jpa_query_executor.query(
                "SELECT plan.production_plan_id, plan.plan_code, plan.plan_name, plan.planned_on, "
                        + "plan.starts_on, plan.ends_on, plan.status, "
                        + "count(line.production_plan_line_id)::integer AS line_count "
                        + "FROM production.production_plan AS plan "
                        + "LEFT JOIN production.production_plan_line AS line "
                        + "ON line.production_plan_id = plan.production_plan_id "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(plan.plan_code) LIKE '%' || ? || '%' "
                        + "OR lower(plan.plan_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR plan.status = ?) "
                        + "GROUP BY plan.production_plan_id "
                        + "ORDER BY plan.starts_on DESC, plan.plan_code LIMIT ? OFFSET ?",
                this::map_summary, search, search, search, status, status, page_size, offset);
    }

    public production_plan_response find(long production_plan_id) {
        List<production_plan_response> plans = jpa_query_executor.query(
                "SELECT production_plan_id, plan_code, plan_name, planned_on, starts_on, ends_on, status, notes "
                        + "FROM production.production_plan WHERE production_plan_id = ?",
                this::map_header, production_plan_id);
        if (plans.isEmpty()) {
            throw new resource_not_found_exception("Production plan");
        }
        production_plan_response plan = plans.getFirst();
        List<production_plan_line_response> lines = jpa_query_executor.query(
                "SELECT production_plan_line_id, line_number, stock_item_id, stock_item_code_snapshot, "
                        + "stock_item_name_snapshot, unit_code_snapshot, target_quantity, required_on, notes "
                        + "FROM production.production_plan_line WHERE production_plan_id = ? ORDER BY line_number",
                this::map_line, production_plan_id);
        return new production_plan_response(plan.production_plan_id(), plan.plan_code(), plan.plan_name(),
                plan.planned_on(), plan.starts_on(), plan.ends_on(), plan.status(), plan.notes(), lines);
    }

    public long insert(String plan_code, String plan_name, LocalDate planned_on, LocalDate starts_on,
                       LocalDate ends_on, String notes, long actor_user_id) {
        Long plan_id = jpa_query_executor.queryForObject(
                "INSERT INTO production.production_plan "
                        + "(plan_code, plan_name, planned_on, starts_on, ends_on, status, notes, created_by_user_id, updated_by_user_id) "
                        + "VALUES (?, ?, ?, ?, ?, 'draft', ?, ?, ?) RETURNING production_plan_id",
                Long.class, plan_code, plan_name, planned_on, starts_on, ends_on, notes,
                actor_user_id, actor_user_id);
        if (plan_id == null) {
            throw new IllegalStateException("Production plan identifier was not returned.");
        }
        return plan_id;
    }

    public int update(long plan_id, String plan_code, String plan_name, LocalDate planned_on,
                      LocalDate starts_on, LocalDate ends_on, String notes, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE production.production_plan SET plan_code = ?, plan_name = ?, planned_on = ?, starts_on = ?, "
                        + "ends_on = ?, notes = ?, updated_at = now(), updated_by_user_id = ? "
                        + "WHERE production_plan_id = ? AND status = 'draft'",
                plan_code, plan_name, planned_on, starts_on, ends_on, notes, actor_user_id, plan_id);
    }

    public void delete_lines(long plan_id) {
        jpa_query_executor.update("DELETE FROM production.production_plan_line WHERE production_plan_id = ?", plan_id);
    }

    public void insert_line(long plan_id, int line_number, production_plan_line_request line,
                            long stock_item_id, String item_code, String item_name, String unit_code, String notes) {
        jpa_query_executor.update(
                "INSERT INTO production.production_plan_line "
                        + "(production_plan_id, line_number, stock_item_id, stock_item_code_snapshot, "
                        + "stock_item_name_snapshot, unit_code_snapshot, target_quantity, required_on, notes) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                plan_id, line_number, stock_item_id, item_code, item_name, unit_code,
                line.target_quantity(), line.required_on(), notes);
    }

    public int change_status(long plan_id, String status, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE production.production_plan SET status = ?, updated_at = now(), updated_by_user_id = ? "
                        + "WHERE production_plan_id = ?",
                status, actor_user_id, plan_id);
    }

    public String current_status(long plan_id) {
        List<String> statuses = jpa_query_executor.query(
                "SELECT status FROM production.production_plan WHERE production_plan_id = ?",
                (result_set, row_number) -> result_set.getString("status"), plan_id);
        if (statuses.isEmpty()) {
            throw new resource_not_found_exception("Production plan");
        }
        return statuses.getFirst();
    }

    public boolean plan_code_exists(String plan_code, Long plan_id) {
        Long count = plan_id == null
                ? jpa_query_executor.queryForObject(
                        "SELECT count(*) FROM production.production_plan WHERE plan_code = ?",
                        Long.class, plan_code)
                : jpa_query_executor.queryForObject(
                        "SELECT count(*) FROM production.production_plan "
                                + "WHERE plan_code = ? AND production_plan_id <> ?",
                        Long.class, plan_code, plan_id);
        return count != null && count > 0;
    }

    public boolean has_orders(long plan_id) {
        Long count = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM production.production_order AS production_order "
                        + "JOIN production.production_plan_line AS line "
                        + "ON line.production_plan_line_id = production_order.production_plan_line_id "
                        + "WHERE line.production_plan_id = ?",
                Long.class, plan_id);
        return count != null && count > 0;
    }

    public int delete(long plan_id) {
        return jpa_query_executor.update(
                "DELETE FROM production.production_plan WHERE production_plan_id = ? AND status = 'draft'",
                plan_id);
    }

    private production_plan_response map_header(jpa_result_row result_set, int row_number) {
        return new production_plan_response(result_set.getLong("production_plan_id"),
                result_set.getString("plan_code"), result_set.getString("plan_name"),
                to_local_date(result_set, "planned_on"), to_local_date(result_set, "starts_on"),
                to_local_date(result_set, "ends_on"), result_set.getString("status"),
                result_set.getString("notes"), List.of());
    }

    private production_plan_summary map_summary(jpa_result_row result_set, int row_number) {
        return new production_plan_summary(result_set.getLong("production_plan_id"),
                result_set.getString("plan_code"), result_set.getString("plan_name"),
                to_local_date(result_set, "planned_on"), to_local_date(result_set, "starts_on"),
                to_local_date(result_set, "ends_on"), result_set.getString("status"),
                result_set.getInt("line_count"));
    }

    private production_plan_line_response map_line(jpa_result_row result_set, int row_number) {
        return new production_plan_line_response(result_set.getLong("production_plan_line_id"),
                result_set.getInt("line_number"), result_set.getLong("stock_item_id"),
                result_set.getString("stock_item_code_snapshot"),
                result_set.getString("stock_item_name_snapshot"),
                result_set.getString("unit_code_snapshot"), result_set.getBigDecimal("target_quantity"),
                to_local_date(result_set, "required_on"), result_set.getString("notes"));
    }

    private LocalDate to_local_date(jpa_result_row result_set, String column) {
        LocalDate date = result_set.get_local_date(column);
        return date == null ? null : date;
    }
}
