package vn.vinamik.erp_backend.production.order_progress;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;

import java.math.BigDecimal;
import java.time.Instant;

import java.util.List;

@Repository
public class production_order_progress_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public production_order_progress_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public order_snapshot find_order(long production_order_id) {
        List<order_snapshot> orders = jpa_query_executor.query(
                "SELECT production_order_id, order_code, status, target_quantity "
                        + "FROM production.production_order WHERE production_order_id = ?",
                (result_set, row_number) -> new order_snapshot(
                        result_set.getLong("production_order_id"),
                        result_set.getString("order_code"),
                        result_set.getString("status"),
                        result_set.getBigDecimal("target_quantity")),
                production_order_id);
        if (orders.isEmpty()) {
            throw new resource_not_found_exception("Production order");
        }
        return orders.getFirst();
    }

    public output_totals find_output_totals(long production_order_id) {
        output_totals totals = jpa_query_executor.queryForObject(
                "SELECT coalesce(sum(good_quantity) FILTER (WHERE status <> 'cancelled'), 0) AS good_quantity, "
                        + "coalesce(sum(defective_quantity) FILTER (WHERE status <> 'cancelled'), 0) AS defective_quantity "
                        + "FROM production.production_output WHERE production_order_id = ?",
                (result_set, row_number) -> new output_totals(
                        result_set.getBigDecimal("good_quantity"),
                        result_set.getBigDecimal("defective_quantity")),
                production_order_id);
        return totals == null ? new output_totals(BigDecimal.ZERO, BigDecimal.ZERO) : totals;
    }

    public List<production_order_event_response> find_events(long production_order_id) {
        return jpa_query_executor.query(
                "SELECT production_order_event_id, event_type, previous_status, new_status, note, occurred_at, "
                        + "actor_user_id, idempotency_key "
                        + "FROM production.production_order_event WHERE production_order_id = ? "
                        + "ORDER BY occurred_at, production_order_event_id",
                this::map_event, production_order_id);
    }

    private production_order_event_response map_event(jpa_result_row result_set, int row_number) {
        return new production_order_event_response(
                result_set.getLong("production_order_event_id"),
                result_set.getString("event_type"),
                result_set.getString("previous_status"),
                result_set.getString("new_status"),
                result_set.getString("note"),
                result_set.get_instant("occurred_at"),
                result_set.getObject("actor_user_id", Long.class),
                result_set.getString("idempotency_key"));
    }


    record order_snapshot(long production_order_id, String order_code, String status,
                          BigDecimal target_quantity) {
    }

    record output_totals(BigDecimal good_quantity, BigDecimal defective_quantity) {
    }
}
