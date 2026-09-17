package vn.vinamik.erp_backend.production.material_needs;

import java.math.BigDecimal;
import java.util.List;

public record material_needs_response(
        long production_order_id,
        String order_code,
        BigDecimal target_quantity,
        List<material_need_response> items) {
}
