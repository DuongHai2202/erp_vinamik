package vn.vinamik.erp_backend.inventory.receipt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record receipt_request(
        @NotBlank(message = "Receipt code is required.")
        @Size(max = 60, message = "Receipt code must contain at most 60 characters.")
        String receipt_code,
        @NotNull(message = "Warehouse is required.")
        Long warehouse_id,
        Long supplier_id,
        @Size(max = 40, message = "Source module must contain at most 40 characters.")
        String source_module,
        Long source_document_id,
        @Size(max = 100, message = "Reference number must contain at most 100 characters.")
        String reference_number,
        @Size(max = 120, message = "Idempotency key must contain at most 120 characters.")
        String idempotency_key,
        @Size(max = 2000, message = "Notes must contain at most 2000 characters.")
        String notes,
        @NotEmpty(message = "At least one receipt line is required.")
        List<@Valid receipt_line_request> lines) {
}
