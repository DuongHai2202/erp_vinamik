package vn.vinamik.erp_backend.inventory.balance;

import java.math.BigDecimal;

public record stock_balance_response(
        long stock_item_id,
        String item_code,
        String item_name,
        long warehouse_id,
        String warehouse_code,
        long warehouse_location_id,
        String location_code,
        Long stock_lot_id,
        String lot_code,
        BigDecimal on_hand_quantity,
        String unit_code) {
}
