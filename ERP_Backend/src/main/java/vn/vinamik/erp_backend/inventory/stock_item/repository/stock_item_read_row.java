package vn.vinamik.erp_backend.inventory.stock_item.repository;

import java.math.BigDecimal;

public record stock_item_read_row(
        long stock_item_id,
        String item_code,
        String item_name,
        String item_type,
        Long item_category_id,
        String category_name,
        long base_unit_of_measure_id,
        String unit_code,
        String unit_name,
        boolean lot_controlled,
        BigDecimal minimum_stock_quantity,
        String status,
        String description) {
}

