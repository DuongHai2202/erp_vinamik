package vn.vinamik.erp_backend.inventory.master_data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record inventory_category_request(
        @NotBlank(message = "Category code is required.")
        @Size(max = 40, message = "Category code must contain at most 40 characters.")
        String category_code,
        @NotBlank(message = "Category name is required.")
        @Size(max = 120, message = "Category name must contain at most 120 characters.")
        String category_name,
        String status) {
}
