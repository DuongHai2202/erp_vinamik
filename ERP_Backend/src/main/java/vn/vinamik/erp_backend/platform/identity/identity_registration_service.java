package vn.vinamik.erp_backend.platform.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;
import vn.vinamik.erp_backend.platform.common.pagination_guard;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class identity_registration_service {
    private static final Logger logger = LoggerFactory.getLogger(identity_registration_service.class);
    private static final int max_page_size = 100;
    private static final Duration request_ttl = Duration.ofHours(48);
    private static final Set<String> valid_statuses = Set.of("pending", "approved", "rejected", "expired");

    private final identity_registration_repository registration_repository;
    private final identity_admin_repository admin_repository;
    private final PasswordEncoder password_encoder;
    private final audit_event_writer audit_writer;

    public identity_registration_service(
            identity_registration_repository registration_repository,
            identity_admin_repository admin_repository,
            PasswordEncoder password_encoder,
            audit_event_writer audit_writer) {
        this.registration_repository = registration_repository;
        this.admin_repository = admin_repository;
        this.password_encoder = password_encoder;
        this.audit_writer = audit_writer;
    }

    @Transactional
    public void submit(registration_request_submission request, String correlation_id) {
        String full_name = normalize_name(request.full_name());
        String work_email = normalize_email(request.work_email());
        String username = normalize_username(request.username());
        String employee_code = normalize_optional_code(request.employee_code());
        String password_hash = password_encoder.encode(request.password());
        Long request_id = registration_repository.insert_pending(
                full_name, work_email, username, employee_code, password_hash, Instant.now().plus(request_ttl));
        if (request_id == null) {
            logger.info("Yêu cầu cấp tài khoản bị trùng định danh; username={}, correlation_id={}", username, correlation_id);
            return;
        }
        audit_writer.write(null, "identity", "registration_request_submitted", "registration_request",
                String.valueOf(request_id), correlation_id, Map.of("status", "pending"));
        logger.info("Đã tiếp nhận yêu cầu cấp tài khoản; request_id={}, correlation_id={}", request_id, correlation_id);
    }

    @Transactional(readOnly = true)
    public registration_request_page_response search_requests(String search, String status, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = Math.min(Math.max(page_size, 1), max_page_size);
        String normalized_status = normalize_optional_lower(status);
        validate_status(normalized_status);
        String normalized_search = normalize_optional_lower(search);
        long total = registration_repository.count_requests(normalized_search, normalized_status);
        List<registration_request_response> items = registration_repository.search_requests(
                normalized_search, normalized_status, safe_page_size, pagination_guard.offset(safe_page, safe_page_size));
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / safe_page_size);
        return new registration_request_page_response(items, safe_page, safe_page_size, total, total_pages);
    }

    @Transactional(noRollbackFor = registration_request_expired_exception.class)
    public identity_user_response approve(
            long registration_request_id,
            registration_approval_request request,
            authenticated_user actor,
            String correlation_id) {
        ensure_approval_permission(actor);
        registration_request_row registration = require_pending(registration_request_id);
        List<String> role_codes = normalize_role_codes(request.role_codes());
        validate_roles(role_codes);
        if (role_codes.contains("system_admin")) {
            throw new AccessDeniedException("System administrator role cannot be granted through registration approval.");
        }
        if (!registration_repository.employee_is_eligible(request.employee_id())) {
            throw new IllegalArgumentException("Employee is invalid or inactive.");
        }
        long user_id;
        try {
            user_id = admin_repository.insert_user(
                    registration.username(), registration.password_hash(), request.employee_id(), actor.user_id());
        } catch (DuplicateKeyException exception) {
            throw new field_conflict_exception("username", "Username or employee is already in use.");
        }
        for (String role_code : role_codes) {
            if (admin_repository.insert_user_role(user_id, role_code, actor.user_id()) == 0) {
                throw new IllegalArgumentException("Role does not exist or is inactive.");
            }
        }
        if (registration_repository.mark_approved(registration_request_id, actor.user_id()) == 0) {
            throw new resource_not_found_exception("Registration request");
        }
        audit_writer.write(actor.user_id(), "identity", "registration_request_approved", "registration_request",
                String.valueOf(registration_request_id), correlation_id, Map.of("user_id", user_id));
        logger.info("Đã duyệt yêu cầu cấp tài khoản; request_id={}, user_id={}, actor_user_id={}, correlation_id={}",
                registration_request_id, user_id, actor.user_id(), correlation_id);
        return admin_repository.find_user(user_id);
    }

    @Transactional(noRollbackFor = registration_request_expired_exception.class)
    public void reject(long registration_request_id, registration_rejection_request request, authenticated_user actor, String correlation_id) {
        ensure_approval_permission(actor);
        registration_request_row registration = require_pending(registration_request_id);
        String review_note = request == null || request.review_note() == null
                ? null : request.review_note().trim();
        if (registration_repository.mark_rejected(registration_request_id, actor.user_id(), review_note) == 0) {
            throw new resource_not_found_exception("Registration request");
        }
        audit_writer.write(actor.user_id(), "identity", "registration_request_rejected", "registration_request",
                String.valueOf(registration_request_id), correlation_id, Map.of("status", "rejected"));
        logger.info("Đã từ chối yêu cầu cấp tài khoản; request_id={}, actor_user_id={}, correlation_id={}",
                registration.registration_request_id(), actor.user_id(), correlation_id);
    }

    private registration_request_row require_pending(long registration_request_id) {
        registration_request_row registration = registration_repository.find_for_update(registration_request_id);
        if (registration == null) {
            throw new resource_not_found_exception("Registration request");
        }
        if (!"pending".equals(registration.status())) {
            throw new IllegalArgumentException("Registration request has already been reviewed.");
        }
        if (registration.expires_at().isBefore(Instant.now())) {
            registration_repository.mark_expired(registration_request_id);
            throw new registration_request_expired_exception();
        }
        return registration;
    }

    private void ensure_approval_permission(authenticated_user actor) {
        if (actor == null || !actor.permission_codes().contains("identity_registration_approve")) {
            throw new AccessDeniedException("Registration approval permission is required.");
        }
    }

    private void validate_roles(List<String> role_codes) {
        if (role_codes.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required.");
        }
        Set<String> unknown = new HashSet<>(role_codes);
        unknown.removeAll(admin_repository.active_role_codes());
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Role does not exist or is inactive.");
        }
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

    private String normalize_name(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        return normalized;
    }

    private String normalize_email(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Work email is required.");
        }
        return normalized;
    }

    private String normalize_username(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[a-z0-9][a-z0-9._-]{2,79}$")) {
            throw new IllegalArgumentException("Username format is invalid.");
        }
        return normalized;
    }

    private String normalize_optional_code(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalize_optional_lower(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void validate_status(String status) {
        if (status != null && !valid_statuses.contains(status)) {
            throw new IllegalArgumentException("Registration request status is invalid.");
        }
    }
}
