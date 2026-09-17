package vn.vinamik.erp_backend.production.order;

import java.math.BigDecimal;

public record order_material_requirement_response(
        long production_order_material_requirement_id,
        long bom_line_id,
        long material_stock_item_id,
        String material_item_code,
        String material_item_name,
        String unit_code,
        BigDecimal base_quantity_snapshot,
        BigDecimal scrap_percent_snapshot,
        BigDecimal required_quantity) {
}
