package vn.vinamik.erp_backend.inventory.api;

import java.math.BigDecimal;

public record inventory_issue_line_result(
        long issue_line_id,
        long stock_item_id,
        BigDecimal quantity) {
}
