package vn.vinamik.erp_backend.inventory.stocktake;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record stocktake_request(
        @NotBlank(message = "Stocktake code is required.")
        @Size(max = 60, message = "Stocktake code must contain at most 60 characters.")
        String stocktake_code,
        @NotNull(message = "Warehouse is required.")
        Long warehouse_id,
        @Size(max = 2000, message = "Notes must contain at most 2000 characters.")
        String notes) {
}
