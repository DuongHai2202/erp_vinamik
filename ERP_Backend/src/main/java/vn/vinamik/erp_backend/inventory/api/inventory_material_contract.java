package vn.vinamik.erp_backend.inventory.api;

import java.util.Optional;

public interface inventory_material_contract {
    Optional<inventory_material_snapshot> find_active_stock_item(long stock_item_id);
}