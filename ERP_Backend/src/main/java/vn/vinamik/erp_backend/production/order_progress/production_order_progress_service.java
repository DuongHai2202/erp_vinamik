package vn.vinamik.erp_backend.production.order_progress;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class production_order_progress_service {
    private static final int quantity_scale = 6;
    private final production_order_progress_repository progress_repository;

    public production_order_progress_service(production_order_progress_repository progress_repository) {
        this.progress_repository = progress_repository;
    }

    @Transactional(readOnly = true)
    public production_order_progress_response find_by_order_id(long production_order_id) {
        if (production_order_id <= 0) {
            throw new IllegalArgumentException("Production order id must be positive.");
        }
        production_order_progress_repository.order_snapshot order =
                progress_repository.find_order(production_order_id);
        production_order_progress_repository.output_totals totals =
                progress_repository.find_output_totals(production_order_id);
        BigDecimal planned = normalize(order.target_quantity());
        BigDecimal good = normalize(totals.good_quantity());
        BigDecimal defective = normalize(totals.defective_quantity());
        BigDecimal actual = normalize(good.add(defective));
        BigDecimal remaining = normalize(planned.subtract(actual).max(BigDecimal.ZERO));
        BigDecimal completion = planned.signum() == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : actual.multiply(new BigDecimal("100"))
                        .divide(planned, 2, RoundingMode.HALF_UP)
                        .min(new BigDecimal("100.00"));
        List<production_order_event_response> events =
                progress_repository.find_events(production_order_id);
        return new production_order_progress_response(
                order.production_order_id(), order.order_code(), order.status(),
                planned, good, defective, actual, remaining, completion, events);
    }

    private BigDecimal normalize(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(quantity_scale, RoundingMode.HALF_UP);
    }
}
