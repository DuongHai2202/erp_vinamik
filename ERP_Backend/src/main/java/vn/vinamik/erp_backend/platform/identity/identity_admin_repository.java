package vn.vinamik.erp_backend.platform.identity;

import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;

import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;


import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class identity_admin_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public identity_admin_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public long count_users(String search, String status) {
        Long total = jpa_query_executor.queryForObject(
                """
                SELECT count(*) FROM identity.user_account AS account
                WHERE (CAST(? AS text) IS NULL OR lower(account.username) LIKE '%' || ? || '%')
                  AND (CAST(? AS text) IS NULL OR account.status = ?)
                """,
                Long.class, search, search, status, status);
        return total == null ? 0 : total;
    }

    public List<identity_user_response> search_users(String search, String status, int page_size, int offset) {
        return jpa_query_executor.query(
                """
                SELECT account.user_id, account.username, account.employee_id, account.status,
                       account.is_super_admin, account.created_at, account.last_login_at,
                       COALESCE(string_agg(role.role_code, ',' ORDER BY role.role_code), '') AS role_codes
                FROM identity.user_account AS account
                LEFT JOIN identity.user_role AS user_role ON user_role.user_id = account.user_id
                LEFT JOIN identity.role AS role ON role.role_id = user_role.role_id
                WHERE (CAST(? AS text) IS NULL OR lower(account.username) LIKE '%' || ? || '%')
                  AND (CAST(? AS text) IS NULL OR account.status = ?)
                GROUP BY account.user_id, account.username, account.employee_id, account.status,
                         account.is_super_admin, account.created_at, account.last_login_at
                ORDER BY account.username
                LIMIT ? OFFSET ?
                """,
                this::map_user,
                search, search, status, status, page_size, offset);
    }

    public identity_user_response find_user(long user_id) {
        List<identity_user_response> users = jpa_query_executor.query(
                """
                SELECT account.user_id, account.username, account.employee_id, account.status,
                       account.is_super_admin, account.created_at, account.last_login_at,
                       COALESCE(string_agg(role.role_code, ',' ORDER BY role.role_code), '') AS role_codes
                FROM identity.user_account AS account
                LEFT JOIN identity.user_role AS user_role ON user_role.user_id = account.user_id
                LEFT JOIN identity.role AS role ON role.role_id = user_role.role_id
                WHERE account.user_id = ?
                GROUP BY account.user_id, account.username, account.employee_id, account.status,
                         account.is_super_admin, account.created_at, account.last_login_at
                """,
                this::map_user, user_id);
        if (users.isEmpty()) {
            throw new resource_not_found_exception("User");
        }
        return users.getFirst();
    }

    public List<identity_role_response> list_roles() {
        return jpa_query_executor.query(
                """
                SELECT role_id, role_code, display_name, description, status
                FROM identity.role
                WHERE status = 'active'
                ORDER BY role_code
                """,
                (result_set, row_number) -> new identity_role_response(
                        result_set.getLong("role_id"), result_set.getString("role_code"),
                        result_set.getString("display_name"), result_set.getString("description"),
                        result_set.getString("status")));
    }

    public long insert_user(String username, String password_hash, Long employee_id, long actor_user_id) {
        return jpa_query_executor.queryForObject(
                """
                INSERT INTO identity.user_account
                    (username, password_hash, employee_id, status, created_by_user_id, updated_by_user_id)
                VALUES (?, ?, ?, 'active', ?, ?)
                RETURNING user_id
                """,
                Long.class, username, password_hash, employee_id, actor_user_id, actor_user_id);
    }

    public boolean exists_user(long user_id) {
        Boolean exists = jpa_query_executor.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM identity.user_account WHERE user_id = ?)",
                Boolean.class, user_id);
        return Boolean.TRUE.equals(exists);
    }

    public boolean is_super_admin(long user_id) {
        Boolean value = jpa_query_executor.queryForObject(
                "SELECT is_super_admin FROM identity.user_account WHERE user_id = ?",
                Boolean.class, user_id);
        return Boolean.TRUE.equals(value);
    }

    public int update_user(long user_id, String status, Long employee_id, long actor_user_id) {
        return jpa_query_executor.update(
                """
                UPDATE identity.user_account
                SET status = ?, employee_id = ?,
                    failed_login_count = CASE WHEN ? = 'active' THEN 0 ELSE failed_login_count END,
                    locked_until = CASE WHEN ? = 'active' THEN NULL ELSE locked_until END,
                    updated_at = now(), updated_by_user_id = ?
                WHERE user_id = ?
                """,
                status, employee_id, status, status, actor_user_id, user_id);
    }

    public int reset_password(long user_id, String password_hash, long actor_user_id) {
        return jpa_query_executor.update(
                """
                UPDATE identity.user_account
                SET password_hash = ?, failed_login_count = 0, locked_until = NULL,
                    updated_at = now(), updated_by_user_id = ?
                WHERE user_id = ?
                """,
                password_hash, actor_user_id, user_id);
    }

    public Set<String> active_role_codes() {
        return new HashSet<>(jpa_query_executor.query(
                "SELECT role_code FROM identity.role WHERE status = 'active'",
                (result_set, row_number) -> result_set.getString("role_code")));
    }

    public void delete_user_roles(long user_id) {
        jpa_query_executor.update("DELETE FROM identity.user_role WHERE user_id = ?", user_id);
    }

    public int insert_user_role(long user_id, String role_code, long actor_user_id) {
        return jpa_query_executor.update(
                """
                INSERT INTO identity.user_role (user_id, role_id, assigned_by_user_id)
                SELECT ?, role_id, ? FROM identity.role
                WHERE role_code = ? AND status = 'active'
                """,
                user_id, actor_user_id, role_code);
    }

    private identity_user_response map_user(jpa_result_row result_set, int row_number) {
        String role_text = result_set.getString("role_codes");
        List<String> role_codes = role_text == null || role_text.isBlank()
                ? List.of()
                : Arrays.stream(role_text.split(",")).toList();
        return new identity_user_response(
                result_set.getLong("user_id"), result_set.getString("username"),
                result_set.getObject("employee_id", Long.class), result_set.getString("status"), role_codes,
                result_set.get_instant("created_at"),
                result_set.get_instant("last_login_at"),
                result_set.getBoolean("is_super_admin"));
    }

}



