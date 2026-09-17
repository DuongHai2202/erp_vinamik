package vn.vinamik.erp_backend.inventory.api;

import java.time.LocalDate;

public record inventory_stock_lot_snapshot(
        long stock_lot_id,
        long stock_item_id,
        String lot_code,
        LocalDate manufactured_on,
        LocalDate expires_on,
        String status) {
}
