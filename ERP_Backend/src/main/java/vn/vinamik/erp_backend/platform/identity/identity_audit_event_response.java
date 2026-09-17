package vn.vinamik.erp_backend.platform.identity;

import java.time.Instant;
import java.util.Map;

public record identity_audit_event_response(
        long audit_id,
        Long actor_user_id,
        String actor_username,
        String module_code,
        String action_code,
        String entity_type,
        String entity_id,
        String correlation_id,
        Instant occurred_at,
        Map<String, Object> metadata) {
}
