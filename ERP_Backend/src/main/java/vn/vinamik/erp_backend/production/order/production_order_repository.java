package vn.vinamik.erp_backend.production.order;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Repository
public class production_order_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public production_order_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public long count(String search, String status, Long stock_item_id) {
        Object[] parameters = order_filter_parameters(search, status, stock_item_id);
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) " + order_filter_where(), Long.class, parameters);
        return total == null ? 0 : total;
    }

    public List<production_order_summary> search(String search, String status, Long stock_item_id,
                                                 int page_size, int offset) {
        Object[] parameters = append(order_filter_parameters(search, status, stock_item_id), page_size, offset);
        return jpa_query_executor.query(
                "SELECT production_order.production_order_id, production_order.order_code, "
                        + "production_order.production_plan_line_id, production_order.stock_item_id, "
                        + "plan_line.stock_item_code_snapshot, plan_line.stock_item_name_snapshot, "
                        + "production_order.target_quantity, production_order.planned_starts_on, "
                        + "production_order.planned_ends_on, production_order.status, production_order.released_at "
                        + order_filter_where()
                        + " ORDER BY production_order.planned_starts_on DESC, production_order.order_code, "
                        + "production_order.production_order_id LIMIT ? OFFSET ?",
                this::map_summary, parameters);
    }

    public production_order_response find_by_id(long production_order_id) {
        List<production_order_response> orders = jpa_query_executor.query(
                "SELECT production_order.production_order_id, production_order.order_code, "
                        + "production_order.production_plan_line_id, production_order.stock_item_id, "
                        + "plan_line.stock_item_code_snapshot, plan_line.stock_item_name_snapshot, "
                        + "production_order.bom_id, bom.bom_code, bom.version_number, production_order.target_quantity, "
                        + "production_order.unit_code_snapshot, production_order.planned_starts_on, "
                        + "production_order.planned_ends_on, production_order.production_line_name, "
                        + "production_order.status, production_order.notes, production_order.released_at "
                        + "FROM production.production_order AS production_order "
                        + "JOIN production.production_plan_line AS plan_line "
                        + "ON plan_line.production_plan_line_id = production_order.production_plan_line_id "
                        + "JOIN production.bom AS bom ON bom.bom_id = production_order.bom_id "
                        + "WHERE production_order.production_order_id = ?",
                this::map_header, production_order_id);
        if (orders.isEmpty()) {
            throw new resource_not_found_exception("Production order");
        }
        production_order_response header = orders.getFirst();
        List<order_material_requirement_response> requirements = jpa_query_executor.query(
                "SELECT requirement.production_order_material_requirement_id, requirement.bom_line_id, "
                        + "requirement.material_stock_item_id, bom_line.material_item_code_snapshot, "
                        + "bom_line.material_item_name_snapshot, requirement.unit_code_snapshot, "
                        + "requirement.base_quantity_snapshot, requirement.scrap_percent_snapshot, "
                        + "requirement.required_quantity "
                        + "FROM production.production_order_material_requirement AS requirement "
                        + "JOIN production.bom_line AS bom_line ON bom_line.bom_line_id = requirement.bom_line_id "
                        + "WHERE requirement.production_order_id = ? "
                        + "ORDER BY bom_line.line_number, requirement.production_order_material_requirement_id",
                (result_set, row_number) -> new order_material_requirement_response(
                        result_set.getLong("production_order_material_requirement_id"),
                        result_set.getLong("bom_line_id"),
                        result_set.getLong("material_stock_item_id"),
                        result_set.getString("material_item_code_snapshot"),
                        result_set.getString("material_item_name_snapshot"),
                        result_set.getString("unit_code_snapshot"),
                        result_set.getBigDecimal("base_quantity_snapshot"),
                        result_set.getBigDecimal("scrap_percent_snapshot"),
                        result_set.getBigDecimal("required_quantity")),
                production_order_id);
        return new production_order_response(
                header.production_order_id(), header.order_code(), header.production_plan_line_id(),
                header.stock_item_id(), header.stock_item_code(), header.stock_item_name(), header.bom_id(),
                header.bom_code(), header.bom_version_number(), header.target_quantity(),
                header.unit_code_snapshot(), header.planned_starts_on(), header.planned_ends_on(),
                header.production_line_name(), header.status(), header.notes(), header.released_at(), requirements);
    }

    public long insert_order(String order_code, long plan_line_id, long stock_item_id, long bom_id,
                             BigDecimal target_quantity, String unit_code, LocalDate planned_starts_on,
                             LocalDate planned_ends_on, String production_line_name, String notes,
                             long actor_user_id) {
        Long order_id = jpa_query_executor.queryForObject(
                "INSERT INTO production.production_order "
                        + "(order_code, production_plan_line_id, stock_item_id, bom_id, target_quantity, "
                        + "unit_code_snapshot, planned_starts_on, planned_ends_on, production_line_name, "
                        + "status, notes, created_by_user_id, updated_by_user_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'planned', ?, ?, ?) "
                        + "RETURNING production_order_id",
                Long.class, order_code, plan_line_id, stock_item_id, bom_id, target_quantity, unit_code,
                planned_starts_on, planned_ends_on, production_line_name, notes, actor_user_id, actor_user_id);
        if (order_id == null) {
            throw new IllegalStateException("Production order identifier was not returned.");
        }
        return order_id;
    }

    public int update_order(long production_order_id, String order_code, long plan_line_id, long stock_item_id,
                            long bom_id, BigDecimal target_quantity, String unit_code,
                            LocalDate planned_starts_on, LocalDate planned_ends_on,
                            String production_line_name, String notes, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE production.production_order SET order_code = ?, production_plan_line_id = ?, "
                        + "stock_item_id = ?, bom_id = ?, target_quantity = ?, unit_code_snapshot = ?, "
                        + "planned_starts_on = ?, planned_ends_on = ?, production_line_name = ?, notes = ?, "
                        + "updated_at = now(), updated_by_user_id = ? "
                        + "WHERE production_order_id = ? AND status IN ('draft', 'planned')",
                order_code, plan_line_id, stock_item_id, bom_id, target_quantity, unit_code, planned_starts_on,
                planned_ends_on, production_line_name, notes, actor_user_id, production_order_id);
    }

    public int change_status(long production_order_id, String current_status, String target_status,
                             long actor_user_id) {
        int updated = jpa_query_executor.update(
                "UPDATE production.production_order SET status = ?, "
                        + "released_at = CASE WHEN ? = 'released' THEN now() ELSE released_at END, "
                        + "updated_at = now(), updated_by_user_id = ? "
                        + "WHERE production_order_id = ? AND status = ?",
                target_status, target_status, actor_user_id, production_order_id, current_status);
        if (updated == 1) {
            int event_inserted = jpa_query_executor.update(
                    "INSERT INTO production.production_order_event "
                            + "(production_order_id, event_type, previous_status, new_status, actor_user_id) "
                            + "VALUES (?, 'status_changed', ?, ?, ?)",
                    production_order_id, current_status, target_status, actor_user_id);
            if (event_inserted != 1) {
                throw new IllegalStateException("Production order status event was not recorded.");
            }
        }
        return updated;
    }

    public void delete_requirements(long production_order_id) {
        jpa_query_executor.update(
                "DELETE FROM production.production_order_material_requirement WHERE production_order_id = ?",
                production_order_id);
    }

    public void insert_requirements(long production_order_id, long bom_id,
                                    List<order_requirement_insert> requirements) {
        if (requirements.isEmpty()) {
            return;
        }
        StringBuilder sql = new StringBuilder(
                "INSERT INTO production.production_order_material_requirement "
                        + "(production_order_id, bom_id, bom_line_id, material_stock_item_id, unit_code_snapshot, "
                        + "base_quantity_snapshot, scrap_percent_snapshot, required_quantity) VALUES ");
        Object[] parameters = new Object[requirements.size() * 8];
        int parameter_index = 0;
        for (int index = 0; index < requirements.size(); index++) {
            if (index > 0) {
                sql.append(", ");
            }
            sql.append("(?, ?, ?, ?, ?, ?, ?, ?)");
            order_requirement_insert requirement = requirements.get(index);
            parameters[parameter_index++] = production_order_id;
            parameters[parameter_index++] = bom_id;
            parameters[parameter_index++] = requirement.bom_line_id();
            parameters[parameter_index++] = requirement.material_stock_item_id();
            parameters[parameter_index++] = requirement.unit_code_snapshot();
            parameters[parameter_index++] = requirement.base_quantity_snapshot();
            parameters[parameter_index++] = requirement.scrap_percent_snapshot();
            parameters[parameter_index++] = requirement.required_quantity();
        }
        int inserted = jpa_query_executor.update(sql.toString(), parameters);
        if (inserted != requirements.size()) {
            throw new IllegalStateException("Production order material requirements were not inserted completely.");
        }
    }

    public plan_line_snapshot lock_plan_line(long production_plan_line_id) {
        List<plan_line_snapshot> lines = jpa_query_executor.query(
                "SELECT line.production_plan_line_id, line.production_plan_id, plan.status AS plan_status, "
                        + "line.stock_item_id, line.stock_item_code_snapshot, line.stock_item_name_snapshot, "
                        + "line.unit_code_snapshot, line.target_quantity, plan.starts_on, plan.ends_on "
                        + "FROM production.production_plan_line AS line "
                        + "JOIN production.production_plan AS plan ON plan.production_plan_id = line.production_plan_id "
                        + "WHERE line.production_plan_line_id = ? FOR UPDATE",
                (result_set, row_number) -> new plan_line_snapshot(
                        result_set.getLong("production_plan_line_id"),
                        result_set.getLong("production_plan_id"),
                        result_set.getString("plan_status"),
                        result_set.getLong("stock_item_id"),
                        result_set.getString("stock_item_code_snapshot"),
                        result_set.getString("stock_item_name_snapshot"),
                        result_set.getString("unit_code_snapshot"),
                        result_set.getBigDecimal("target_quantity"),
                        result_set.getObject("starts_on", LocalDate.class),
                        result_set.getObject("ends_on", LocalDate.class)),
                production_plan_line_id);
        if (lines.isEmpty()) {
            throw new resource_not_found_exception("Production plan line");
        }
        return lines.getFirst();
    }

    public bom_snapshot load_active_bom(long bom_id, long stock_item_id, LocalDate effective_date) {
        List<bom_snapshot> boms = jpa_query_executor.query(
                "SELECT bom.bom_id, bom.bom_code, bom.stock_item_id, bom.version_number, "
                        + "bom.base_quantity, bom.unit_code_snapshot "
                        + "FROM production.bom AS bom "
                        + "WHERE bom.bom_id = ? AND bom.stock_item_id = ? AND bom.status = 'active' "
                        + "AND bom.valid_from <= ? AND (bom.valid_to IS NULL OR bom.valid_to >= ?)",
                (result_set, row_number) -> new bom_snapshot(
                        result_set.getLong("bom_id"), result_set.getString("bom_code"),
                        result_set.getLong("stock_item_id"), result_set.getInt("version_number"),
                        result_set.getBigDecimal("base_quantity"), result_set.getString("unit_code_snapshot"),
                        List.of()),
                bom_id, stock_item_id, effective_date, effective_date);
        if (boms.isEmpty()) {
            throw new resource_not_found_exception("Active BOM effective for the planned start date");
        }
        bom_snapshot header = boms.getFirst();
        List<bom_line_snapshot> lines = jpa_query_executor.query(
                "SELECT bom_line_id, material_stock_item_id, unit_code_snapshot, quantity_per_base, scrap_percent "
                        + "FROM production.bom_line WHERE bom_id = ? ORDER BY line_number, bom_line_id",
                (result_set, row_number) -> new bom_line_snapshot(
                        result_set.getLong("bom_line_id"),
                        result_set.getLong("material_stock_item_id"),
                        result_set.getString("unit_code_snapshot"),
                        result_set.getBigDecimal("quantity_per_base"),
                        result_set.getBigDecimal("scrap_percent")),
                bom_id);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("The selected BOM must contain at least one material line.");
        }
        return new bom_snapshot(
                header.bom_id(), header.bom_code(), header.stock_item_id(), header.version_number(),
                header.base_quantity(), header.unit_code_snapshot(), lines);
    }

    public BigDecimal sum_target_quantity(long production_plan_line_id, Long excluded_order_id) {
        String sql = excluded_order_id == null
                ? "SELECT coalesce(sum(target_quantity), 0) FROM production.production_order "
                        + "WHERE production_plan_line_id = ? AND status <> 'cancelled'"
                : "SELECT coalesce(sum(target_quantity), 0) FROM production.production_order "
                        + "WHERE production_plan_line_id = ? AND production_order_id <> ? "
                        + "AND status <> 'cancelled'";
        BigDecimal existing = excluded_order_id == null
                ? jpa_query_executor.queryForObject(sql, BigDecimal.class, production_plan_line_id)
                : jpa_query_executor.queryForObject(sql, BigDecimal.class, production_plan_line_id, excluded_order_id);
        return existing == null ? BigDecimal.ZERO : existing;
    }

    public boolean production_line_overlaps(String line_name, LocalDate starts_on, LocalDate ends_on,
                                             Long excluded_order_id) {
        String sql = excluded_order_id == null
                ? "SELECT count(*) FROM production.production_order "
                        + "WHERE lower(production_line_name) = lower(?) AND status NOT IN ('cancelled', 'completed') "
                        + "AND planned_starts_on <= ? AND planned_ends_on >= ?"
                : "SELECT count(*) FROM production.production_order "
                        + "WHERE lower(production_line_name) = lower(?) AND production_order_id <> ? "
                        + "AND status NOT IN ('cancelled', 'completed') "
                        + "AND planned_starts_on <= ? AND planned_ends_on >= ?";
        Long count = excluded_order_id == null
                ? jpa_query_executor.queryForObject(sql, Long.class, line_name, ends_on, starts_on)
                : jpa_query_executor.queryForObject(sql, Long.class, line_name, excluded_order_id, ends_on, starts_on);
        return count != null && count > 0;
    }

    /**
     * Serializes production-line schedule checks for the lifetime of the
     * surrounding transaction. Line names are normalized to match the
     * case-insensitive overlap query.
     */
    public void lock_production_line_schedule(String line_name) {
        jpa_query_executor.execute(
                "SELECT pg_advisory_xact_lock(hashtext(?))",
                "production_line:" + line_name.trim().toLowerCase(Locale.ROOT));
    }

    public boolean order_code_exists(String order_code, Long production_order_id) {
        Long count = production_order_id == null
                ? jpa_query_executor.queryForObject(
                        "SELECT count(*) FROM production.production_order WHERE order_code = ?",
                        Long.class, order_code)
                : jpa_query_executor.queryForObject(
                        "SELECT count(*) FROM production.production_order "
                                + "WHERE order_code = ? AND production_order_id <> ?",
                        Long.class, order_code, production_order_id);
        return count != null && count > 0;
    }

    public long count_overdue() {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM production.production_order "
                        + "WHERE planned_ends_on < CURRENT_DATE "
                        + "AND status IN ('planned', 'released', 'in_progress')",
                Long.class);
        return total == null ? 0 : total;
    }
    public String current_status(long production_order_id) {
        List<String> statuses = jpa_query_executor.query(
                "SELECT status FROM production.production_order WHERE production_order_id = ?",
                (result_set, row_number) -> result_set.getString("status"), production_order_id);
        if (statuses.isEmpty()) {
            throw new resource_not_found_exception("Production order");
        }
        return statuses.getFirst();
    }

    private Object[] order_filter_parameters(String search, String status, Long stock_item_id) {
        return new Object[]{search, search, search, search, status, status, stock_item_id, stock_item_id};
    }

    private String order_filter_where() {
        return "FROM production.production_order AS production_order "
                + "JOIN production.production_plan_line AS plan_line "
                + "ON plan_line.production_plan_line_id = production_order.production_plan_line_id "
                + "WHERE (CAST(? AS text) IS NULL OR lower(production_order.order_code) LIKE '%' || ? || '%' "
                + "OR lower(plan_line.stock_item_code_snapshot) LIKE '%' || ? || '%' "
                + "OR lower(plan_line.stock_item_name_snapshot) LIKE '%' || ? || '%') "
                + "AND (CAST(? AS text) IS NULL OR production_order.status = ?) "
                + "AND (CAST(? AS text) IS NULL OR production_order.stock_item_id = ?)";
    }

    private Object[] append(Object[] values, Object... suffix) {
        Object[] result = Arrays.copyOf(values, values.length + suffix.length);
        System.arraycopy(suffix, 0, result, values.length, suffix.length);
        return result;
    }

    private production_order_summary map_summary(jpa_result_row result_set, int row_number) {
        return new production_order_summary(
                result_set.getLong("production_order_id"),
                result_set.getString("order_code"),
                result_set.getLong("production_plan_line_id"),
                result_set.getLong("stock_item_id"),
                result_set.getString("stock_item_code_snapshot"),
                result_set.getString("stock_item_name_snapshot"),
                result_set.getBigDecimal("target_quantity"),
                result_set.getObject("planned_starts_on", LocalDate.class),
                result_set.getObject("planned_ends_on", LocalDate.class),
                result_set.getString("status"),
                result_set.get_instant("released_at"));
    }

    private production_order_response map_header(jpa_result_row result_set, int row_number) {
        return new production_order_response(
                result_set.getLong("production_order_id"),
                result_set.getString("order_code"),
                result_set.getLong("production_plan_line_id"),
                result_set.getLong("stock_item_id"),
                result_set.getString("stock_item_code_snapshot"),
                result_set.getString("stock_item_name_snapshot"),
                result_set.getLong("bom_id"),
                result_set.getString("bom_code"),
                result_set.getInt("version_number"),
                result_set.getBigDecimal("target_quantity"),
                result_set.getString("unit_code_snapshot"),
                result_set.getObject("planned_starts_on", LocalDate.class),
                result_set.getObject("planned_ends_on", LocalDate.class),
                result_set.getString("production_line_name"),
                result_set.getString("status"),
                result_set.getString("notes"),
                result_set.get_instant("released_at"),
                List.of());
    }

    record plan_line_snapshot(long production_plan_line_id, long production_plan_id, String plan_status,
                              long stock_item_id, String stock_item_code, String stock_item_name,
                              String unit_code, BigDecimal target_quantity, LocalDate plan_starts_on,
                              LocalDate plan_ends_on) {
    }

    record bom_snapshot(long bom_id, String bom_code, long stock_item_id, int version_number,
                        BigDecimal base_quantity, String unit_code_snapshot, List<bom_line_snapshot> lines) {
    }

    record bom_line_snapshot(long bom_line_id, long material_stock_item_id, String unit_code_snapshot,
                             BigDecimal quantity_per_base, BigDecimal scrap_percent) {
    }

    record order_requirement_insert(long bom_line_id, long material_stock_item_id, String unit_code_snapshot,
                                    BigDecimal base_quantity_snapshot, BigDecimal scrap_percent_snapshot,
                                    BigDecimal required_quantity) {
    }
}

