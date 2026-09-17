package vn.vinamik.erp_backend.human_resources.absence;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;



import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Repository
public class human_resources_absence_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public human_resources_absence_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public long count(String search, Long employee_id, String status, LocalDate from_date, LocalDate to_date) {
        Long total = jpa_query_executor.queryForObject("SELECT count(*) " + common_where(), Long.class,
                search, search, search, search,
                employee_id, employee_id, status, status,
                from_date, from_date, to_date, to_date);
        return total == null ? 0 : total;
    }

    public List<leave_response> search(String search, Long employee_id, String status,
                                       LocalDate from_date, LocalDate to_date, int page_size, int offset) {
        return jpa_query_executor.query(
                "SELECT leave_request.leave_request_id, leave_request.request_code, leave_request.employee_id, "
                        + "employee.employee_code, employee.full_name, leave_request.leave_type_code, "
                        + "leave_request.starts_on, leave_request.ends_on, leave_request.is_paid, leave_request.reason, "
                        + "leave_request.status, leave_request.approver_user_id, leave_request.decided_at, "
                        + "leave_request.decision_note " + common_where()
                        + " ORDER BY leave_request.starts_on DESC, leave_request.request_code LIMIT ? OFFSET ?",
                this::map_leave,
                search, search, search, search,
                employee_id, employee_id, status, status,
                from_date, from_date, to_date, to_date, page_size, offset);
    }

    public leave_response find(long leave_request_id) {
        List<leave_response> requests = jpa_query_executor.query(
                "SELECT leave_request.leave_request_id, leave_request.request_code, leave_request.employee_id, "
                        + "employee.employee_code, employee.full_name, leave_request.leave_type_code, "
                        + "leave_request.starts_on, leave_request.ends_on, leave_request.is_paid, leave_request.reason, "
                        + "leave_request.status, leave_request.approver_user_id, leave_request.decided_at, "
                        + "leave_request.decision_note FROM hr.leave_request AS leave_request "
                        + "JOIN hr.employee AS employee ON employee.employee_id = leave_request.employee_id "
                        + "WHERE leave_request.leave_request_id = ?",
                this::map_leave, leave_request_id);
        if (requests.isEmpty()) {
            throw new resource_not_found_exception("Leave request");
        }
        return requests.getFirst();
    }

    public long insert(String request_code, long employee_id, String leave_type_code,
                       LocalDate starts_on, LocalDate ends_on, boolean is_paid,
                       String reason, String status, long actor_user_id) {
        Long leave_request_id = jpa_query_executor.queryForObject(
                "INSERT INTO hr.leave_request (request_code, employee_id, leave_type_code, starts_on, ends_on, is_paid, reason, status, created_by_user_id, updated_by_user_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING leave_request_id",
                Long.class, request_code, employee_id, leave_type_code, starts_on, ends_on, is_paid, reason,
                status, actor_user_id, actor_user_id);
        if (leave_request_id == null) {
            throw new IllegalStateException("Leave request identifier was not returned.");
        }
        return leave_request_id;
    }

    public int update(long leave_request_id, String request_code, long employee_id, String leave_type_code,
                      LocalDate starts_on, LocalDate ends_on, boolean is_paid, String reason,
                      String status, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE hr.leave_request SET request_code = ?, employee_id = ?, leave_type_code = ?, starts_on = ?, ends_on = ?, is_paid = ?, reason = ?, status = ?, updated_at = now(), updated_by_user_id = ? WHERE leave_request_id = ?",
                request_code, employee_id, leave_type_code, starts_on, ends_on, is_paid, reason, status,
                actor_user_id, leave_request_id);
    }

    public int decide(long leave_request_id, String status, long actor_user_id, String decision_note) {
        return jpa_query_executor.update(
                "UPDATE hr.leave_request SET status = ?, approver_user_id = ?, decided_at = now(), decision_note = ?, updated_at = now(), updated_by_user_id = ? WHERE leave_request_id = ? AND status = 'pending'",
                status, actor_user_id, decision_note, actor_user_id, leave_request_id);
    }

    public int cancel(long leave_request_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE hr.leave_request SET status = 'cancelled', updated_at = now(), updated_by_user_id = ? WHERE leave_request_id = ? AND status IN ('draft', 'pending')",
                actor_user_id, leave_request_id);
    }

    public String current_status(long leave_request_id) {
        List<String> statuses = jpa_query_executor.query(
                "SELECT status FROM hr.leave_request WHERE leave_request_id = ?",
                (result_set, row_number) -> result_set.getString("status"), leave_request_id);
        if (statuses.isEmpty()) {
            throw new resource_not_found_exception("Leave request");
        }
        return statuses.getFirst();
    }

    /**
     * Serializes approval decisions for one employee and locks the request row
     * for the lifetime of the surrounding transaction. This keeps the
     * overlap check and status update atomic when two requests are approved at
     * the same time.
     */
    public void lock_employee_schedule(long leave_request_id) {
        Long employee_id = jpa_query_executor.queryForObject(
                "SELECT employee_id FROM hr.leave_request WHERE leave_request_id = ? FOR UPDATE",
                Long.class, leave_request_id);
        if (employee_id == null) {
            throw new resource_not_found_exception("Leave request");
        }
        jpa_query_executor.execute(
                "SELECT pg_advisory_xact_lock(hashtext(?))",
                "hr_leave_employee:" + employee_id);
    }

    public boolean has_approved_overlap(long leave_request_id) {
        Integer count = jpa_query_executor.queryForObject(
                "SELECT count(*)::integer FROM hr.leave_request AS candidate "
                        + "JOIN hr.leave_request AS existing ON existing.employee_id = candidate.employee_id "
                        + "WHERE candidate.leave_request_id = ? "
                        + "AND existing.leave_request_id <> candidate.leave_request_id "
                        + "AND existing.status = 'approved' "
                        + "AND existing.starts_on <= candidate.ends_on "
                        + "AND existing.ends_on >= candidate.starts_on",
                Integer.class, leave_request_id);
        return count != null && count > 0;
    }

    public boolean employee_exists(long employee_id) {
        Long count = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.employee WHERE employee_id = ?", Long.class, employee_id);
        return count != null && count > 0;
    }

    public boolean request_code_exists(String request_code, Long leave_request_id) {
        Long count = leave_request_id == null
                ? jpa_query_executor.queryForObject(
                        "SELECT count(*) FROM hr.leave_request WHERE request_code = ?", Long.class, request_code)
                : jpa_query_executor.queryForObject(
                        "SELECT count(*) FROM hr.leave_request WHERE request_code = ? AND leave_request_id <> ?",
                        Long.class, request_code, leave_request_id);
        return count != null && count > 0;
    }

    private String common_where() {
        return "FROM hr.leave_request AS leave_request "
                + "JOIN hr.employee AS employee ON employee.employee_id = leave_request.employee_id "
                + "WHERE (CAST(? AS text) IS NULL OR lower(leave_request.request_code) LIKE '%' || ? || '%' "
                + "OR lower(employee.employee_code) LIKE '%' || ? || '%' "
                + "OR lower(employee.full_name) LIKE '%' || ? || '%') "
                + "AND (CAST(? AS text) IS NULL OR leave_request.employee_id = ?) "
                + "AND (CAST(? AS text) IS NULL OR leave_request.status = ?) "
                + "AND (CAST(? AS text) IS NULL OR leave_request.ends_on >= ?) "
                + "AND (CAST(? AS text) IS NULL OR leave_request.starts_on <= ?)";
    }

    private leave_response map_leave(jpa_result_row result_set, int row_number) {
        LocalDate starts_on = result_set.get_local_date("starts_on");
        LocalDate ends_on = result_set.get_local_date("ends_on");
        LocalDate start = starts_on;
        LocalDate end = ends_on;
        Instant decided_at = result_set.get_instant("decided_at");
        return new leave_response(
                result_set.getLong("leave_request_id"), result_set.getString("request_code"),
                result_set.getLong("employee_id"), result_set.getString("employee_code"),
                result_set.getString("full_name"), result_set.getString("leave_type_code"),
                start, end, ChronoUnit.DAYS.between(start, end) + 1,
                result_set.getBoolean("is_paid"), result_set.getString("reason"),
                result_set.getString("status"), result_set.getObject("approver_user_id", Long.class),
                decided_at == null ? null : decided_at,
                result_set.getString("decision_note"));
    }
}
