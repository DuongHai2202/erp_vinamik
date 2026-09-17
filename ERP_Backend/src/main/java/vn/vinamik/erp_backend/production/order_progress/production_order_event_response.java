package vn.vinamik.erp_backend.production.order_progress;

import java.math.BigDecimal;
import java.time.Instant;

public record production_order_event_response(
        long production_order_event_id,
        String event_type,
        String previous_status,
        String new_status,
        String note,
        Instant occurred_at,
        Long actor_user_id,
        String idempotency_key) {
}

