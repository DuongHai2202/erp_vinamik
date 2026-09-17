package vn.vinamik.erp_backend.production.material_consumption;

import java.util.List;

public record material_consumption_response(
        long production_order_id,
        String order_code,
        String idempotency_key,
        List<material_consumption_line_response> lines) {
}
