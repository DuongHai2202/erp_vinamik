package vn.vinamik.erp_backend.inventory.stock_item;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record stock_item_request(
        @NotBlank(message = "Item code is required.")
        @Size(max = 60, message = "Item code must contain at most 60 characters.")
        String item_code,
        @NotBlank(message = "Item name is required.")
        @Size(max = 180, message = "Item name must contain at most 180 characters.")
        String item_name,
        Long item_category_id,
        @NotNull(message = "Base unit is required.")
        Long base_unit_of_measure_id,
        Boolean lot_controlled,
        @PositiveOrZero(message = "Minimum stock quantity cannot be negative.")
        BigDecimal minimum_stock_quantity,
        String status,
        @Size(max = 2000, message = "Description must contain at most 2000 characters.")
        String description,
        String item_type) {

    public stock_item_request(
            String item_code,
            String item_name,
            Long item_category_id,
            Long base_unit_of_measure_id,
            Boolean lot_controlled,
            BigDecimal minimum_stock_quantity,
            String status,
            String description) {
        this(item_code, item_name, item_category_id, base_unit_of_measure_id, lot_controlled,
                minimum_stock_quantity, status, description, "raw_material");
    }
}