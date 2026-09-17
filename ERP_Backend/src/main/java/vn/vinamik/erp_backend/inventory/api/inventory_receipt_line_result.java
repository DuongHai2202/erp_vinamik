package vn.vinamik.erp_backend.inventory.api;

import java.math.BigDecimal;

public record inventory_receipt_line_result(
        long receipt_line_id,
        long stock_item_id,
        BigDecimal quantity) {
}
