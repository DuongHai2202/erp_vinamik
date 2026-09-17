package vn.vinamik.erp_backend.platform.identity;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;

import java.time.Instant;
import java.util.List;

@Repository
public class identity_authentication_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public identity_authentication_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public List<identity_account_snapshot> find_account_for_update(String username) {
        return jpa_query_executor.query(
                """
                SELECT user_id, username, password_hash, status, locked_until, is_super_admin
                FROM identity.user_account
                WHERE username = ?
                FOR UPDATE
                """,
                (result_set, row_number) -> new identity_account_snapshot(
                        result_set.getLong("user_id"), result_set.getString("username"),
                        result_set.getString("password_hash"), result_set.getString("status"),
                        result_set.getObject("locked_until", Instant.class),
                        result_set.getBoolean("is_super_admin")), username);
    }

    public int reset_login_state(long user_id) {
        return jpa_query_executor.update(
                """
                UPDATE identity.user_account
                SET failed_login_count = 0,
                    locked_until = NULL,
                    last_login_at = now(),
                    updated_at = now()
                WHERE user_id = ?
                """, user_id);
    }

    public int insert_session(long user_id, String session_secret_hash, Instant expires_at) {
        return jpa_query_executor.update(
                """
                INSERT INTO identity.user_session (user_id, session_secret_hash, expires_at)
                VALUES (?, ?, ?)
                """, user_id, session_secret_hash, expires_at);
    }

    public List<authenticated_user> find_authenticated_user(String secret_hash) {
        return jpa_query_executor.query(
                """
                SELECT account.user_id, account.username, account.is_super_admin
                FROM identity.user_session AS active_session
                JOIN identity.user_account AS account ON account.user_id = active_session.user_id
                WHERE active_session.session_secret_hash = ?
                  AND active_session.revoked_at IS NULL
                  AND active_session.expires_at > now()
                  AND account.status = 'active'
                  AND (account.locked_until IS NULL OR account.locked_until <= now())
                """,
                (result_set, row_number) -> new authenticated_user(
                        result_set.getLong("user_id"), result_set.getString("username"), List.of(),
                        result_set.getBoolean("is_super_admin")), secret_hash);
    }

    public int revoke_session(String secret_hash) {
        return jpa_query_executor.update(
                """
                UPDATE identity.user_session
                SET revoked_at = now()
                WHERE session_secret_hash = ?
                  AND revoked_at IS NULL
                """, secret_hash);
    }

    public int increment_failed_login_count(long user_id, int max_failed_attempts) {
        return jpa_query_executor.update(
                """
                UPDATE identity.user_account
                SET failed_login_count = CASE
                        WHEN locked_until IS NOT NULL AND locked_until <= now() THEN 1
                        ELSE failed_login_count + 1
                    END,
                    locked_until = CASE
                        WHEN (CASE
                            WHEN locked_until IS NOT NULL AND locked_until <= now() THEN 1
                            ELSE failed_login_count + 1
                        END) >= ? THEN now() + interval '15 minutes'
                        ELSE NULL
                    END,
                    updated_at = now()
                WHERE user_id = ?
                """, max_failed_attempts, user_id);
    }

    public List<String> find_permission_codes(long user_id) {
        return jpa_query_executor.query(
                """
                SELECT permission.permission_code
                FROM identity.permission AS permission
                WHERE permission.status = 'active'
                  AND (
                      EXISTS (
                          SELECT 1
                          FROM identity.user_account AS account
                          WHERE account.user_id = ?
                            AND account.is_super_admin
                      )
                      OR EXISTS (
                          SELECT 1
                          FROM identity.user_role AS user_role
                          JOIN identity.role AS role
                            ON role.role_id = user_role.role_id
                           AND role.status = 'active'
                          JOIN identity.role_permission AS role_permission
                            ON role_permission.role_id = role.role_id
                          WHERE user_role.user_id = ?
                            AND role_permission.permission_id = permission.permission_id
                      )
                  )
                ORDER BY permission.permission_code
                """,
                (result_set, row_number) -> result_set.getString("permission_code"), user_id, user_id);
    }
}
