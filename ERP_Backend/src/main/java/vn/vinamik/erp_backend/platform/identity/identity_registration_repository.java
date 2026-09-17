package vn.vinamik.erp_backend.platform.identity;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class identity_registration_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public identity_registration_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public Long insert_pending(
            String full_name,
            String work_email,
            String username,
            String employee_code,
            String password_hash,
            Instant expires_at) {
        return jpa_query_executor.queryForObject(
                """
                INSERT INTO identity.registration_request
                    (full_name, work_email, username, employee_code, password_hash, status, expires_at)
                VALUES (?, ?, ?, ?, ?, 'pending', ?)
                ON CONFLICT DO NOTHING
                RETURNING registration_request_id
                """,
                Long.class, full_name, work_email, username, employee_code, password_hash, expires_at);
    }

    public long count_requests(String search, String status) {
        List<Object> parameters = new ArrayList<>();
        String where = where_clause(search, status, parameters);
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM identity.registration_request " + where,
                Long.class, parameters.toArray());
        return total == null ? 0 : total;
    }

    public List<registration_request_response> search_requests(String search, String status, int page_size, int offset) {
        List<Object> parameters = new ArrayList<>();
        String where = where_clause(search, status, parameters);
        parameters.add(page_size);
        parameters.add(offset);
        return jpa_query_executor.query(
                """
                SELECT registration_request_id, full_name, work_email, username, employee_code,
                       status, requested_at, expires_at, reviewed_at, review_note
                FROM identity.registration_request
                """ + where + " ORDER BY requested_at DESC, registration_request_id DESC LIMIT ? OFFSET ?",
                this::map_response, parameters.toArray());
    }

    public registration_request_row find_for_update(long registration_request_id) {
        return jpa_query_executor.queryForObject(
                """
                SELECT registration_request_id, full_name, work_email, username, employee_code,
                       password_hash, status, requested_at, expires_at, reviewed_at, review_note
                FROM identity.registration_request
                WHERE registration_request_id = ?
                FOR UPDATE
                """,
                this::map_row, registration_request_id);
    }

    public int mark_expired(long registration_request_id) {
        return jpa_query_executor.update(
                """
                UPDATE identity.registration_request
                SET status = 'expired', reviewed_at = now(), password_hash = NULL
                WHERE registration_request_id = ? AND status = 'pending'
                """,
                registration_request_id);
    }

    public int mark_approved(long registration_request_id, long actor_user_id) {
        return jpa_query_executor.update(
                """
                UPDATE identity.registration_request
                SET status = 'approved', reviewed_at = now(), reviewed_by_user_id = ?, password_hash = NULL
                WHERE registration_request_id = ? AND status = 'pending'
                """,
                actor_user_id, registration_request_id);
    }

    public int mark_rejected(long registration_request_id, long actor_user_id, String review_note) {
        return jpa_query_executor.update(
                """
                UPDATE identity.registration_request
                SET status = 'rejected', reviewed_at = now(), reviewed_by_user_id = ?,
                    review_note = ?, password_hash = NULL
                WHERE registration_request_id = ? AND status = 'pending'
                """,
                actor_user_id, review_note, registration_request_id);
    }

    public boolean employee_is_eligible(long employee_id) {
        Boolean exists = jpa_query_executor.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1 FROM hr.employee
                    WHERE employee_id = ? AND employment_status IN ('active', 'on_leave')
                )
                """,
                Boolean.class, employee_id);
        return Boolean.TRUE.equals(exists);
    }

    private String where_clause(String search, String status, List<Object> parameters) {
        StringBuilder where = new StringBuilder("WHERE 1 = 1");
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase() + "%";
            where.append(" AND (lower(full_name) LIKE ? OR lower(work_email) LIKE ? OR lower(username) LIKE ? OR lower(coalesce(employee_code, '')) LIKE ?)");
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND status = ?");
            parameters.add(status);
        }
        return where.toString();
    }

    private registration_request_response map_response(jpa_result_row row, int row_number) {
        return new registration_request_response(
                row.getLong("registration_request_id"), row.getString("full_name"),
                row.getString("work_email"), row.getString("username"), row.getString("employee_code"),
                row.getString("status"), row.get_instant("requested_at"), row.get_instant("expires_at"),
                row.get_instant("reviewed_at"), row.getString("review_note"));
    }

    private registration_request_row map_row(jpa_result_row row, int row_number) {
        return new registration_request_row(
                row.getLong("registration_request_id"), row.getString("full_name"),
                row.getString("work_email"), row.getString("username"), row.getString("employee_code"),
                row.getString("password_hash"), row.getString("status"), row.get_instant("requested_at"),
                row.get_instant("expires_at"), row.get_instant("reviewed_at"), row.getString("review_note"));
    }
}
