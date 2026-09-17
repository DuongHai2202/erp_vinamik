package vn.vinamik.erp_backend.platform.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;

@Component
public class initial_admin_bootstrapper implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(initial_admin_bootstrapper.class);
    private final jpa_native_query_executor jpa_query_executor;
    private final PasswordEncoder password_encoder;
    private final TransactionTemplate transaction_template;
    private final String bootstrap_username;
    private final String bootstrap_password;

    public initial_admin_bootstrapper(
            jpa_native_query_executor jpa_query_executor,
            PasswordEncoder password_encoder,
            PlatformTransactionManager transaction_manager,
            @Value("${erp.bootstrap-admin.username:}") String bootstrap_username,
            @Value("${erp.bootstrap-admin.password:}") String bootstrap_password) {
        this.jpa_query_executor = jpa_query_executor;
        this.password_encoder = password_encoder;
        this.transaction_template = new TransactionTemplate(transaction_manager);
        this.bootstrap_username = bootstrap_username;
        this.bootstrap_password = bootstrap_password;
    }

    @Override
    public void run(ApplicationArguments application_arguments) {
        boolean username_missing = bootstrap_username == null || bootstrap_username.isBlank();
        boolean password_missing = bootstrap_password == null || bootstrap_password.isBlank();
        if (username_missing && password_missing) {
            logger.info("Chưa cấu hình tài khoản quản trị khởi tạo; bỏ qua bước tạo tài khoản ban đầu.");
            return;
        }
        if (username_missing || password_missing) {
            throw new IllegalStateException("Bootstrap admin username and password must be configured together.");
        }
        if (bootstrap_password.length() < 12) {
            throw new IllegalStateException("Bootstrap admin password must contain at least 12 characters.");
        }

        String normalized_username = bootstrap_username.trim().toLowerCase(Locale.ROOT);
        String password_hash = password_encoder.encode(bootstrap_password);
        transaction_template.executeWithoutResult(transaction_status ->
                create_initial_admin_if_required(normalized_username, password_hash));
    }

    private void create_initial_admin_if_required(String username, String password_hash) {
        // Serialize startup across multiple backend instances before checking
        // and creating the single protected super admin account.
        jpa_query_executor.execute(
                "SELECT pg_advisory_xact_lock(hashtext(?))",
                "identity:bootstrap_super_admin");
        Long account_count = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM identity.user_account", Long.class);
        if (account_count != null && account_count > 0) {
            logger.info("Đã có tài khoản trong hệ thống; bỏ qua tạo quản trị khởi tạo.");
            return;
        }

        Long user_id = jpa_query_executor.queryForObject(
                """
                INSERT INTO identity.user_account (username, password_hash, status, is_super_admin)
                VALUES (?, ?, 'active', true)
                RETURNING user_id
                """,
                Long.class,
                username,
                password_hash);
        jpa_query_executor.update(
                """
                INSERT INTO identity.user_role (user_id, role_id)
                SELECT ?, role_id
                FROM identity.role
                WHERE role_code = 'system_admin'
                ON CONFLICT (user_id, role_id) DO NOTHING
                """,
                user_id);
        logger.info("Đã tạo tài khoản quản trị khởi tạo.");
    }
}


