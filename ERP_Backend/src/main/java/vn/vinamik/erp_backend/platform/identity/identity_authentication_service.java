package vn.vinamik.erp_backend.platform.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class identity_authentication_service {
    private static final int max_failed_attempts = 5;
    private final identity_authentication_repository authentication_repository;
    private final PasswordEncoder password_encoder;
    private final long session_ttl_seconds;
    private final String dummy_password_hash;
    private final audit_event_writer audit_writer;

    public identity_authentication_service(
            identity_authentication_repository authentication_repository,
            PasswordEncoder password_encoder,
            @Value("${erp.auth.session-ttl-seconds:43200}") long session_ttl_seconds,
            audit_event_writer audit_writer) {
        this.authentication_repository = authentication_repository;
        this.password_encoder = password_encoder;
        this.session_ttl_seconds = session_ttl_seconds;
        this.audit_writer = audit_writer;
        this.dummy_password_hash = password_encoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public Optional<login_result> login(String supplied_username, String supplied_password) {
        return login(supplied_username, supplied_password, "unavailable");
    }

    @Transactional
    public Optional<login_result> login(String supplied_username, String supplied_password, String correlation_id) {
        String normalized_username = supplied_username == null
                ? ""
                : supplied_username.trim().toLowerCase(Locale.ROOT);
        List<identity_account_snapshot> accounts = authentication_repository.find_account_for_update(normalized_username);
        if (accounts.isEmpty()) {
            password_encoder.matches(supplied_password == null ? "" : supplied_password, dummy_password_hash);
            audit_writer.write(null, "identity", "login_failed", "user_account", null, correlation_id,
                    Map.of("reason", "invalid_credentials"));
            return Optional.empty();
        }

        identity_account_snapshot account = accounts.getFirst();
        Instant now = Instant.now();
        boolean temporarily_locked = account.locked_until() != null && account.locked_until().isAfter(now);
        if (!"active".equals(account.status()) || temporarily_locked) {
            password_encoder.matches(supplied_password == null ? "" : supplied_password, account.password_hash());
            audit_writer.write(account.user_id(), "identity", "login_failed", "user_account",
                    String.valueOf(account.user_id()), correlation_id, Map.of("reason", "account_unavailable"));
            return Optional.empty();
        }

        if (!password_encoder.matches(supplied_password == null ? "" : supplied_password, account.password_hash())) {
            authentication_repository.increment_failed_login_count(account.user_id(), max_failed_attempts);
            audit_writer.write(account.user_id(), "identity", "login_failed", "user_account",
                    String.valueOf(account.user_id()), correlation_id, Map.of("reason", "invalid_credentials"));
            return Optional.empty();
        }

        authentication_repository.reset_login_state(account.user_id());
        session_secret.generated_session_secret generated_secret = session_secret.generate();
        Instant expires_at = now.plusSeconds(session_ttl_seconds);
        authentication_repository.insert_session(account.user_id(), generated_secret.hash(), expires_at);

        List<String> permission_codes = authentication_repository.find_permission_codes(account.user_id());
        auth_user_response user = new auth_user_response(account.user_id(), account.username(), permission_codes, account.super_admin());
        audit_writer.write(account.user_id(), "identity", "login_success", "user_account",
                String.valueOf(account.user_id()), correlation_id, Map.of());
        return Optional.of(new login_result(generated_secret.value(), user));
    }

    @Transactional(readOnly = true)
    public Optional<authenticated_user> authenticate_secret(String raw_secret) {
        if (raw_secret == null || raw_secret.length() < 32 || raw_secret.length() > 128) {
            return Optional.empty();
        }
        String secret_hash = session_secret.hash(raw_secret);
        List<authenticated_user> users = authentication_repository.find_authenticated_user(secret_hash);
        if (users.isEmpty()) {
            return Optional.empty();
        }
        authenticated_user user = users.getFirst();
        return Optional.of(new authenticated_user(
                user.user_id(), user.username(), authentication_repository.find_permission_codes(user.user_id()), user.super_admin()));
    }

    @Transactional
    public int revoke_secret(String raw_secret) {
        return revoke_secret(raw_secret, null, "unavailable");
    }

    @Transactional
    public int revoke_secret(String raw_secret, Long actor_user_id, String correlation_id) {
        if (raw_secret == null || raw_secret.length() < 32 || raw_secret.length() > 128) {
            return 0;
        }
        int revoked_count = authentication_repository.revoke_session(session_secret.hash(raw_secret));
        if (revoked_count > 0) {
            audit_writer.write(actor_user_id, "identity", "logout", "user_session", null, correlation_id, Map.of());
        }
        return revoked_count;
    }
}
