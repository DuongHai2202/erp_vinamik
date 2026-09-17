package vn.vinamik.erp_backend.inventory.master_data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record inventory_warehouse_request(
        @NotBlank(message = "Warehouse code is required.")
        @Size(max = 40, message = "Warehouse code must contain at most 40 characters.")
        String warehouse_code,
        @NotBlank(message = "Warehouse name is required.")
        @Size(max = 160, message = "Warehouse name must contain at most 160 characters.")
        String warehouse_name,
        @Size(max = 2000, message = "Warehouse address must contain at most 2000 characters.")
        String address,
        String status) {
}
