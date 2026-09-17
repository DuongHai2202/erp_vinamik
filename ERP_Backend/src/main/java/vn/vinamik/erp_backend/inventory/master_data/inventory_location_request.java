package vn.vinamik.erp_backend.inventory.master_data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record inventory_location_request(
        @NotNull(message = "Warehouse is required.")
        Long warehouse_id,
        @NotBlank(message = "Location code is required.")
        @Size(max = 60, message = "Location code must contain at most 60 characters.")
        String location_code,
        @NotBlank(message = "Location name is required.")
        @Size(max = 160, message = "Location name must contain at most 160 characters.")
        String location_name,
        String status) {
}
