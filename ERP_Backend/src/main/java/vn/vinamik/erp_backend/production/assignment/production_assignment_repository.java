package vn.vinamik.erp_backend.production.assignment;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;

import java.time.Instant;
import java.util.List;

@Repository
public class production_assignment_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public production_assignment_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public long count(Long production_order_id, Long employee_id, String status,
                      Instant from_at, Instant to_at) {
        Long total = jpa_query_executor.queryForObject("SELECT count(*) " + common_where(), Long.class,
                production_order_id, production_order_id, employee_id, employee_id,
                status, status, from_at, from_at,
                to_at, to_at);
        return total == null ? 0 : total;
    }

    public List<assignment_row> search(Long production_order_id, Long employee_id, String status,
                                       Instant from_at, Instant to_at, int page_size, int offset) {
        return jpa_query_executor.query(
                "SELECT assignment.production_assignment_id, assignment.production_order_id, production_order.order_code, "
                        + "assignment.employee_id, assignment.work_shift_id, assignment.assignment_name, "
                        + "assignment.starts_at, assignment.ends_at, assignment.status, assignment.notes "
                        + common_where()
                        + " ORDER BY assignment.starts_at, assignment.production_assignment_id LIMIT ? OFFSET ?",
                this::map_row,
                production_order_id, production_order_id, employee_id, employee_id,
                status, status, from_at, from_at,
                to_at, to_at, page_size, offset);
    }

    public assignment_row find(long production_assignment_id) {
        List<assignment_row> rows = jpa_query_executor.query(
                "SELECT assignment.production_assignment_id, assignment.production_order_id, production_order.order_code, "
                        + "assignment.employee_id, assignment.work_shift_id, assignment.assignment_name, "
                        + "assignment.starts_at, assignment.ends_at, assignment.status, assignment.notes "
                        + "FROM production.production_assignment AS assignment "
                        + "JOIN production.production_order AS production_order "
                        + "ON production_order.production_order_id = assignment.production_order_id "
                        + "WHERE assignment.production_assignment_id = ?",
                this::map_row, production_assignment_id);
        if (rows.isEmpty()) {
            throw new resource_not_found_exception("Production assignment");
        }
        return rows.getFirst();
    }

    public production_order_snapshot find_order(long production_order_id) {
        List<production_order_snapshot> orders = jpa_query_executor.query(
                "SELECT production_order_id, order_code, status FROM production.production_order WHERE production_order_id = ?",
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

    public long insert(long production_order_id, long employee_id, Long work_shift_id,
                       String assignment_name, Instant starts_at, Instant ends_at,
                       String notes, long actor_user_id) {
        Long assignment_id = jpa_query_executor.queryForObject(
                "INSERT INTO production.production_assignment (production_order_id, employee_id, work_shift_id, assignment_name, starts_at, ends_at, status, notes, created_by_user_id, updated_by_user_id) VALUES (?, ?, ?, ?, ?, ?, 'planned', ?, ?, ?) RETURNING production_assignment_id",
                Long.class, production_order_id, employee_id, work_shift_id, assignment_name,
                starts_at, ends_at, notes, actor_user_id, actor_user_id);
        if (assignment_id == null) {
            throw new IllegalStateException("Production assignment identifier was not returned.");
        }
        return assignment_id;
    }

    public int update(long production_assignment_id, long production_order_id, long employee_id,
                      Long work_shift_id, String assignment_name, Instant starts_at, Instant ends_at,
                      String notes, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE production.production_assignment SET production_order_id = ?, employee_id = ?, work_shift_id = ?, assignment_name = ?, starts_at = ?, ends_at = ?, notes = ?, updated_at = now(), updated_by_user_id = ? WHERE production_assignment_id = ? AND status = 'planned'",
                production_order_id, employee_id, work_shift_id, assignment_name, starts_at,
                ends_at, notes, actor_user_id, production_assignment_id);
    }

    public int change_status(long production_assignment_id, String status, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE production.production_assignment SET status = ?, updated_at = now(), updated_by_user_id = ? WHERE production_assignment_id = ?",
                status, actor_user_id, production_assignment_id);
    }

    public String current_status(long production_assignment_id) {
        List<String> statuses = jpa_query_executor.query(
                "SELECT status FROM production.production_assignment WHERE production_assignment_id = ?",
                (result_set, row_number) -> result_set.getString("status"), production_assignment_id);
        if (statuses.isEmpty()) {
            throw new resource_not_found_exception("Production assignment");
        }
        return statuses.getFirst();
    }

    /**
     * Serializes scheduling checks for one employee for the lifetime of the
     * surrounding transaction. The key is independent of row identifiers so
     * concurrent creates and updates cannot pass the overlap check together.
     */
    public void lock_employee_schedule(long employee_id) {
        jpa_query_executor.execute(
                "SELECT pg_advisory_xact_lock(hashtext(?))",
                "production_assignment_employee:" + employee_id);
    }

    public boolean has_overlap(long employee_id, Instant starts_at, Instant ends_at, Long excluded_id) {
        String sql = excluded_id == null
                ? "SELECT count(*) FROM production.production_assignment WHERE employee_id = ? AND status <> 'cancelled' AND starts_at < ? AND ends_at > ?"
                : "SELECT count(*) FROM production.production_assignment WHERE employee_id = ? AND production_assignment_id <> ? AND status <> 'cancelled' AND starts_at < ? AND ends_at > ?";
        Long count = excluded_id == null
                ? jpa_query_executor.queryForObject(sql, Long.class, employee_id,
                        ends_at, starts_at)
                : jpa_query_executor.queryForObject(sql, Long.class, employee_id, excluded_id,
                        ends_at, starts_at);
        return count != null && count > 0;
    }

    private String common_where() {
        return "FROM production.production_assignment AS assignment "
                + "JOIN production.production_order AS production_order "
                + "ON production_order.production_order_id = assignment.production_order_id "
                + "WHERE (CAST(? AS text) IS NULL OR assignment.production_order_id = ?) "
                + "AND (CAST(? AS text) IS NULL OR assignment.employee_id = ?) "
                + "AND (CAST(? AS text) IS NULL OR assignment.status = ?) "
                + "AND (CAST(? AS text) IS NULL OR assignment.ends_at >= ?) "
                + "AND (CAST(? AS text) IS NULL OR assignment.starts_at <= ?)";
    }

    private assignment_row map_row(jpa_result_row result_set, int row_number) {
        return new assignment_row(result_set.getLong("production_assignment_id"),
                result_set.getLong("production_order_id"), result_set.getString("order_code"),
                result_set.getLong("employee_id"), result_set.getObject("work_shift_id", Long.class),
                result_set.getString("assignment_name"), result_set.get_instant("starts_at"),
                result_set.get_instant("ends_at"), result_set.getString("status"),
                result_set.getString("notes"));
    }

}
