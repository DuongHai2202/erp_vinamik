package vn.vinamik.erp_backend.production.material_needs;

import java.math.BigDecimal;

public record material_need_response(
        long material_stock_item_id,
        String material_item_code,
        String material_item_name,
        String unit_code,
        BigDecimal required_quantity,
        BigDecimal available_quantity,
        BigDecimal shortage_quantity) {
}
