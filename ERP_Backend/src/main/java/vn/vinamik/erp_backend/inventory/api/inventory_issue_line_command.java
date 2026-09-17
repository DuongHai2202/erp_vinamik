package vn.vinamik.erp_backend.inventory.api;

import java.math.BigDecimal;

public record inventory_issue_line_command(
        long stock_item_id,
        long warehouse_location_id,
        Long stock_lot_id,
        BigDecimal quantity) {
}
