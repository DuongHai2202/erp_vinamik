package vn.vinamik.erp_backend.inventory.master_data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record inventory_supplier_request(
        @NotBlank(message = "Supplier code is required.")
        @Size(max = 60, message = "Supplier code must contain at most 60 characters.")
        String supplier_code,
        @NotBlank(message = "Supplier name is required.")
        @Size(max = 180, message = "Supplier name must contain at most 180 characters.")
        String supplier_name,
        @Size(max = 30, message = "Phone number must contain at most 30 characters.")
        String phone_number,
        @Email(message = "Email format is invalid.")
        @Size(max = 254, message = "Email must contain at most 254 characters.")
        String email,
        @Size(max = 2000, message = "Supplier address must contain at most 2000 characters.")
        String address,
        String status) {
}
