package vn.vinamik.erp_backend.production.bom;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record bom_request(
        @NotBlank(message = "BOM code is required.")
        @Size(max = 60, message = "BOM code must contain at most 60 characters.")
        String bom_code,
        @NotNull(message = "Finished product is required.")
        Long stock_item_id,
        @NotNull(message = "BOM version is required.")
        Integer version_number,
        @NotNull(message = "Base quantity is required.")
        @DecimalMin(value = "0.000001", inclusive = true, message = "Base quantity must be greater than zero.")
        BigDecimal base_quantity,
        @NotNull(message = "BOM effective start date is required.")
        LocalDate valid_from,
        LocalDate valid_to,
        @Size(max = 2000, message = "Notes must contain at most 2000 characters.")
        String notes,
        @NotEmpty(message = "At least one BOM line is required.")
        List<@Valid bom_line_request> lines) {
}
