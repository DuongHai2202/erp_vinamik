package vn.vinamik.erp_backend.production.bom;

import java.math.BigDecimal;

public record bom_line_response(
        long bom_line_id,
        int line_number,
        long material_stock_item_id,
        String material_item_code,
        String material_item_name,
        String unit_code_snapshot,
        BigDecimal quantity_per_base,
        BigDecimal scrap_percent,
        String notes) {
}
