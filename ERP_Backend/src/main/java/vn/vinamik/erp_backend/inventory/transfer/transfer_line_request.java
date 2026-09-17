package vn.vinamik.erp_backend.inventory.transfer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record transfer_line_request(
        @NotNull(message = "Stock item is required.")
        Long stock_item_id,
        Long stock_lot_id,
        @NotNull(message = "Source location is required.")
        Long source_location_id,
        @NotNull(message = "Destination location is required.")
        Long destination_location_id,
        @NotNull(message = "Transfer quantity is required.")
        @DecimalMin(value = "0.000001", message = "Transfer quantity must be greater than zero.")
        BigDecimal quantity) {
}
