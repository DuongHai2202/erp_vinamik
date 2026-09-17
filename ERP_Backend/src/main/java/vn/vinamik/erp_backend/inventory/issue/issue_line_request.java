package vn.vinamik.erp_backend.inventory.issue;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record issue_line_request(
        @NotNull(message = "Stock item is required.")
        Long stock_item_id,
        @NotNull(message = "Warehouse location is required.")
        Long warehouse_location_id,
        Long stock_lot_id,
        @NotNull(message = "Quantity is required.")
        @DecimalMin(value = "0.000001", inclusive = true, message = "Quantity must be greater than zero.")
        BigDecimal quantity) {
}
