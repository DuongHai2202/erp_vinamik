package vn.vinamik.erp_backend.production.bom;

import java.time.LocalDate;

public record bom_summary(
        long bom_id,
        String bom_code,
        long stock_item_id,
        String product_item_code,
        String product_item_name,
        int version_number,
        LocalDate valid_from,
        LocalDate valid_to,
        String status,
        int line_count) {
}
