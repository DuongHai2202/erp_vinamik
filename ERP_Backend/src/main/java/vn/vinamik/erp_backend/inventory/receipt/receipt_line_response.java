package vn.vinamik.erp_backend.inventory.receipt;

import java.math.BigDecimal;

public record receipt_line_response(
        long receipt_line_id,
        int line_number,
        long stock_item_id,
        String item_code,
        String item_name,
        long warehouse_location_id,
        String location_code,
        Long stock_lot_id,
        String lot_code,
        BigDecimal quantity) {
}
