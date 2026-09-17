package vn.vinamik.erp_backend.inventory.api;

import java.time.LocalDate;

public interface inventory_stock_lot_contract {
    inventory_stock_lot_snapshot ensure_active_lot(long stock_item_id, String lot_code, LocalDate manufactured_on,
                                                    LocalDate expires_on, long actor_user_id);
}
