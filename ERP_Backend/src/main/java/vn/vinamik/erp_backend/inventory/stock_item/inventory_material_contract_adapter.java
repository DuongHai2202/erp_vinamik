package vn.vinamik.erp_backend.inventory.stock_item;

import org.springframework.stereotype.Component;
import vn.vinamik.erp_backend.inventory.api.inventory_material_contract;
import vn.vinamik.erp_backend.inventory.api.inventory_material_snapshot;

import java.util.Optional;

@Component
public class inventory_material_contract_adapter implements inventory_material_contract {
    private final inventory_material_contract_repository material_repository;

    public inventory_material_contract_adapter(inventory_material_contract_repository material_repository) {
        this.material_repository = material_repository;
    }

    @Override
    public Optional<inventory_material_snapshot> find_active_stock_item(long stock_item_id) {
        return material_repository.find_active_stock_item(stock_item_id).stream().findFirst();
    }
}
