package vn.vinamik.erp_backend.inventory.transfer;

import java.math.BigDecimal;

public record transfer_line_response(
        long transfer_line_id,
        int line_number,
        long stock_item_id,
        String item_code,
        String item_name,
        Long stock_lot_id,
        String lot_code,
        long source_location_id,
        String source_location_code,
        long destination_location_id,
        String destination_location_code,
        BigDecimal quantity) {
}
