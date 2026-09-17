package vn.vinamik.erp_backend.production.bom;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record bom_response(
        long bom_id,
        String bom_code,
        long stock_item_id,
        String product_item_code,
        String product_item_name,
        int version_number,
        BigDecimal base_quantity,
        String unit_code_snapshot,
        LocalDate valid_from,
        LocalDate valid_to,
        String status,
        String notes,
        List<bom_line_response> lines) {
}
