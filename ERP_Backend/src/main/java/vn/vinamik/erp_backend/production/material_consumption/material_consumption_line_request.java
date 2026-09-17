package vn.vinamik.erp_backend.production.material_consumption;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record material_consumption_line_request(
        @NotNull(message = "Material is required.")
        Long material_stock_item_id,
        @NotNull(message = "Warehouse location is required.")
        Long warehouse_location_id,
        Long stock_lot_id,
        @NotNull(message = "Consumed quantity is required.")
        @DecimalMin(value = "0.000001", message = "Consumed quantity must be greater than zero.")
        BigDecimal consumed_quantity) {
}
