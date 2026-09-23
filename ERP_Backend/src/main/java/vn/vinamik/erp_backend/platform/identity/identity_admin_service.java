package vn.vinamik.erp_backend.platform.identity;
import vn.vinamik.erp_backend.platform.common.pagination_guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.human_resources.api.human_resources_employee_contract;
import vn.vinamik.erp_backend.human_resources.api.human_resources_employee_snapshot;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class identity_admin_service {
    private static final Logger logger = LoggerFactory.getLogger(identity_admin_service.class);
    private static final int max_page_size = 100;
    private static final Set<String> valid_statuses = Set.of("active", "locked", "disabled");
    private final identity_admin_repository admin_repository;
    private final human_resources_employee_contract employee_contract;
    private final PasswordEncoder password_encoder;
    private final audit_event_writer audit_writer;

    public identity_admin_service(
            identity_admin_repository admin_repository,
            human_resources_employee_contract employee_contract,
            PasswordEncoder password_encoder,
            audit_event_writer audit_writer) {
        this.admin_repository = admin_repository;
        this.employee_contract = employee_contract;
        this.password_encoder = password_encoder;
        this.audit_writer = audit_writer;
    }

    @Transactional(readOnly = true)
    public identity_user_page_response search_users(String search, String status, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = Math.min(Math.max(page_size, 1), max_page_size);
        String normalized_search = normalize_optional_lower(search);
        String normalized_status = normalize_optional_lower(status);
        validate_status(normalized_status);
        long total = admin_repository.count_users(normalized_search, normalized_status);
        List<identity_user_response> items = admin_repository.search_users(
                normalized_search, normalized_status, safe_page_size, pagination_guard.offset(safe_page, safe_page_size));
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / safe_page_size);
        return new identity_user_page_response(enrich_employee_display(items), safe_page, safe_page_size, total, total_pages);
    }

    @Transactional(readOnly = true)
    public identity_user_response find_user(long user_id) {
        return enrich_employee_display(List.of(admin_repository.find_user(user_id))).getFirst();
    }

    private List<identity_user_response> enrich_employee_display(List<identity_user_response> users) {
        List<Long> employee_ids = users.stream()
                .map(identity_user_response::employee_id)
                .filter(employee_id -> employee_id != null && employee_id > 0)
                .distinct()
                .toList();
        Map<Long, human_resources_employee_snapshot> employees = employee_contract.find_employees(employee_ids);
        return users.stream().map(user -> {
            human_resources_employee_snapshot employee = user.employee_id() == null
                    ? null
                    : employees.get(user.employee_id());
            return new identity_user_response(
                    user.user_id(), user.username(), user.employee_id(),
                    employee == null ? null : employee.employee_code(),
                    employee == null ? null : employee.full_name(),
                    user.status(), user.role_codes(), user.created_at(),
                    user.last_login_at(), user.super_admin());
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<identity_role_response> list_roles() {
        return admin_repository.list_roles();
    }

    @Transactional
    public identity_user_response create_user(
            identity_user_create_request request,
            authenticated_user actor,
            String correlation_id) {
        String username = normalize_username(request.username());
        List<String> role_codes = normalize_role_codes(request.role_codes());
        if (role_codes.isEmpty()) {
            role_codes = List.of("read_only");
        }
        validate_roles(role_codes);
        ensure_system_admin_assignment_allowed(role_codes, actor);
        try {
            long user_id = admin_repository.insert_user(
                    username, password_encoder.encode(request.password()), request.employee_id(), actor.user_id());
            assign_role_rows(user_id, role_codes, actor.user_id());
            identity_user_response created = find_user(user_id);
            audit_writer.write(actor.user_id(), "identity", "user_create", "user",
                    String.valueOf(user_id), correlation_id, Map.of("username", username));
            logger.info("Đã tạo tài khoản; user_id={}, actor_user_id={}, correlation_id={}",
                    user_id, actor.user_id(), correlation_id);
            return created;
        } catch (DuplicateKeyException exception) {
            throw new field_conflict_exception("username", "Username or employee is already in use.");
        }
    }

    @Transactional
    public identity_user_response update_user(
            long user_id,
            identity_user_update_request request,
            authenticated_user actor,
            String correlation_id) {
        String status = normalize_optional_lower(request.status());
        validate_status(status);
        if (user_id == actor.user_id() && !"active".equals(status)) {
            throw new IllegalArgumentException("You cannot disable or lock your own account.");
        }
        ensure_user_exists(user_id);
        if (admin_repository.is_super_admin(user_id) && !"active".equals(status)) {
            throw new AccessDeniedException("Super admin account cannot be disabled or locked.");
        }
        try {
            int updated = admin_repository.update_user(user_id, status, request.employee_id(), actor.user_id());
            if (updated == 0) {
                throw new resource_not_found_exception("User");
            }
        } catch (DuplicateKeyException exception) {
            throw new field_conflict_exception("employee_id", "Employee is already linked to another user.");
        }
        identity_user_response updated_user = find_user(user_id);
        audit_writer.write(actor.user_id(), "identity", "user_update", "user",
                String.valueOf(user_id), correlation_id, Map.of("status", status));
        logger.info("Đã cập nhật trạng thái tài khoản; user_id={}, actor_user_id={}, correlation_id={}",
                user_id, actor.user_id(), correlation_id);
        return updated_user;
    }

    @Transactional
    public identity_user_response reset_password(
            long user_id,
            identity_user_password_reset_request request,
            authenticated_user actor,
            String correlation_id) {
        ensure_user_exists(user_id);
        int updated = admin_repository.reset_password(user_id, password_encoder.encode(request.password()), actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("User");
        }
        identity_user_response updated_user = find_user(user_id);
        audit_writer.write(actor.user_id(), "identity", "user_password_reset", "user",
                String.valueOf(user_id), correlation_id, Map.of());
        logger.info("Đã đặt lại mật khẩu tài khoản; user_id={}, actor_user_id={}, correlation_id={}",
                user_id, actor.user_id(), correlation_id);
        return updated_user;
    }

    @Transactional
    public identity_user_response update_roles(
            long user_id,
            identity_user_roles_request request,
            authenticated_user actor,
            String correlation_id) {
        ensure_user_exists(user_id);
        List<String> role_codes = normalize_role_codes(request.role_codes());
        if (user_id == actor.user_id() && !can_manage_system_admin(actor)) {
            throw new AccessDeniedException("You cannot change your own roles.");
        }
        validate_roles(role_codes);
        if (admin_repository.is_super_admin(user_id) && !role_codes.contains("system_admin")) {
            throw new AccessDeniedException("Super admin account must keep the system administrator role.");
        }
        ensure_system_admin_assignment_allowed(role_codes, actor);
        admin_repository.delete_user_roles(user_id);
        assign_role_rows(user_id, role_codes, actor.user_id());
        identity_user_response updated_user = find_user(user_id);
        audit_writer.write(actor.user_id(), "identity", "user_roles_update", "user",
                String.valueOf(user_id), correlation_id, Map.of("role_codes", String.join(",", role_codes)));
        logger.info("Đã cập nhật role tài khoản; user_id={}, actor_user_id={}, correlation_id={}",
                user_id, actor.user_id(), correlation_id);
        return updated_user;
    }

    private void ensure_user_exists(long user_id) {
        if (!admin_repository.exists_user(user_id)) {
            throw new resource_not_found_exception("User");
        }
    }

    private void validate_roles(List<String> role_codes) {
        if (role_codes.stream().anyMatch(code -> !code.matches("^[a-z][a-z0-9_]*$"))) {
            throw new IllegalArgumentException("Role code is invalid.");
        }
        Set<String> unknown = new HashSet<>(role_codes);
        unknown.removeAll(admin_repository.active_role_codes());
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Role does not exist or is inactive.");
        }
    }

    private void assign_role_rows(long user_id, List<String> role_codes, long actor_user_id) {
        for (String role_code : role_codes) {
            if (admin_repository.insert_user_role(user_id, role_code, actor_user_id) == 0) {
                throw new IllegalArgumentException("Role does not exist or is inactive.");
            }
        }
    }

    private void ensure_system_admin_assignment_allowed(List<String> role_codes, authenticated_user actor) {
        if (role_codes.contains("system_admin") && !can_manage_system_admin(actor)) {
            throw new AccessDeniedException("System administrator role requires elevated permission.");
        }
    }

    private boolean can_manage_system_admin(authenticated_user actor) {
        return actor.permission_codes().contains("identity_role_admin");
    }

    private String normalize_username(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || !normalized.matches("^[a-z0-9][a-z0-9._-]{2,79}$")) {
            throw new IllegalArgumentException("Username format is invalid.");
        }
        return normalized;
    }

    private List<String> normalize_role_codes(List<String> role_codes) {
        if (role_codes == null) {
            return List.of();
        }
        return role_codes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(code -> code.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private String normalize_optional_lower(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void validate_status(String status) {
        if (status != null && !valid_statuses.contains(status)) {
            throw new IllegalArgumentException("User status is invalid.");
        }
    }
}

