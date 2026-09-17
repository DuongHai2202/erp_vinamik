package vn.vinamik.erp_backend.platform.identity;
import vn.vinamik.erp_backend.platform.common.pagination_guard;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class identity_audit_service {
    private static final int max_page_size = 100;
    private static final int max_filter_length = 120;
    private final identity_audit_repository audit_repository;

    public identity_audit_service(identity_audit_repository audit_repository) {
        this.audit_repository = audit_repository;
    }

    @Transactional(readOnly = true)
    public identity_audit_event_page_response search(String module_code, String action_code, String entity_type,
                                                      String entity_id, Long actor_user_id, Instant from_at,
                                                      Instant to_at, int page, int page_size) {
        if (actor_user_id != null && actor_user_id <= 0) {
            throw new IllegalArgumentException("Actor user identifier must be positive.");
        }
        if (from_at != null && to_at != null && from_at.isAfter(to_at)) {
            throw new IllegalArgumentException("From time cannot be after to time.");
        }
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = Math.min(Math.max(page_size, 1), max_page_size);
        int safe_offset = pagination_guard.offset(safe_page, safe_page_size);
        String normalized_module_code = normalize_filter(module_code);
        String normalized_action_code = normalize_filter(action_code);
        String normalized_entity_type = normalize_filter(entity_type);
        String normalized_entity_id = normalize_filter(entity_id);
        long total = audit_repository.count(normalized_module_code, normalized_action_code, normalized_entity_type,
                normalized_entity_id, actor_user_id, from_at, to_at);
        List<identity_audit_event_response> items = audit_repository.search(
                normalized_module_code, normalized_action_code, normalized_entity_type, normalized_entity_id,
                actor_user_id, from_at, to_at, safe_page_size, safe_offset);
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / safe_page_size);
        return new identity_audit_event_page_response(items, safe_page, safe_page_size, total, total_pages);
    }

    private String normalize_filter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > max_filter_length) {
            throw new IllegalArgumentException("Audit filter is too long.");
        }
        return normalized;
    }
}