package vn.vinamik.erp_backend.production.material_consumption;

import java.math.BigDecimal;
import java.time.Instant;

public record material_consumption_line_response(
        long material_consumption_id,
        long inventory_issue_line_id,
        long material_stock_item_id,
        String material_item_code,
        String material_item_name,
        String unit_code,
        BigDecimal consumed_quantity,
        Instant consumed_at) {
}
