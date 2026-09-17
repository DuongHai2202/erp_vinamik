package vn.vinamik.erp_backend.inventory.master_data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record inventory_unit_request(
        @NotBlank(message = "Unit code is required.")
        @Size(max = 32, message = "Unit code must contain at most 32 characters.")
        String unit_code,
        @NotBlank(message = "Unit name is required.")
        @Size(max = 100, message = "Unit name must contain at most 100 characters.")
        String unit_name,
        @Min(value = 0, message = "Decimal places cannot be negative.")
        @Max(value = 6, message = "Decimal places cannot exceed 6.")
        Short decimal_places,
        String status) {
}
