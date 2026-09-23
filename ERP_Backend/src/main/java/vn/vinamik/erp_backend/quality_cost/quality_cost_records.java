package vn.vinamik.erp_backend.quality_cost;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

record quality_cost_page_response(List<Map<String, Object>> items, int page, int page_size,
                                  long total_items, int total_pages) {
}

record quality_cost_status_request(@NotBlank String status, String decision_note) {
}

record cost_rule_request(@NotBlank String rule_code,
                         @NotBlank String material_valuation_method,
                         @NotBlank String labor_basis,
                         @NotBlank String overhead_basis,
                         @NotBlank String defective_policy,
                         @NotNull LocalDate effective_from,
                         LocalDate effective_to,
                         String currency_code,
                         Integer rounding_scale,
                         String notes) {
}

record cost_period_request(@NotBlank String period_code,
                           @NotNull LocalDate starts_on,
                           @NotNull LocalDate ends_on,
                           @NotNull Long rule_version_id,
                           String notes) {
}

record quality_inspection_request(@NotBlank String inspection_code,
                                  Long production_order_id,
                                  Long production_output_id,
                                  @NotNull Long stock_item_id,
                                  String lot_code,
                                  @NotNull @DecimalMin("0.000001") BigDecimal inspected_quantity,
                                  @NotNull @DecimalMin("0") BigDecimal good_quantity,
                                  @NotNull @DecimalMin("0") BigDecimal defective_quantity,
                                  @NotNull LocalDate inspected_on,
                                  String notes) {
}

record nonconformance_request(@NotBlank String nonconformance_code,
                              @NotNull Long inspection_id,
                              @NotBlank String defect_code,
                              @NotNull @DecimalMin("0.000001") BigDecimal quantity,
                              @NotBlank String disposition,
                              String notes) {
}

record cost_calculation_request(@NotNull Long cost_period_id,
                                Long production_order_id,
                                @NotNull Long stock_item_id,
                                String stock_item_code,
                                String stock_item_name,
                                @NotNull @DecimalMin("0.000001") BigDecimal good_quantity,
                                @NotNull @DecimalMin("0") BigDecimal material_cost,
                                @NotNull @DecimalMin("0") BigDecimal direct_labor_cost,
                                @NotNull @DecimalMin("0") BigDecimal overhead_cost,
                                BigDecimal adjustment_amount,
                                String notes) {
}

record price_proposal_request(@NotBlank String proposal_code,
                              @NotNull Long cost_period_id,
                              @NotNull Long stock_item_id,
                              String stock_item_code,
                              String stock_item_name,
                              @NotNull @DecimalMin("0") BigDecimal unit_cost,
                              @NotNull @DecimalMin("0") BigDecimal margin_percent,
                              @NotNull @DecimalMin("0") BigDecimal proposed_price,
                              @NotNull LocalDate effective_on,
                              String currency_code,
                              String notes) {
}
