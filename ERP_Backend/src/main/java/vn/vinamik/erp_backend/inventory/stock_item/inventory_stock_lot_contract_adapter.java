package vn.vinamik.erp_backend.inventory.stock_item;

import org.springframework.stereotype.Component;
import vn.vinamik.erp_backend.inventory.api.inventory_stock_lot_contract;
import vn.vinamik.erp_backend.inventory.api.inventory_stock_lot_snapshot;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;

import java.time.LocalDate;
import java.util.List;

@Component
public class inventory_stock_lot_contract_adapter implements inventory_stock_lot_contract {
    private final inventory_stock_lot_contract_repository lot_repository;

    public inventory_stock_lot_contract_adapter(inventory_stock_lot_contract_repository lot_repository) {
        this.lot_repository = lot_repository;
    }

    @Override
    public inventory_stock_lot_snapshot ensure_active_lot(long stock_item_id, String lot_code, LocalDate manufactured_on,
                                                          LocalDate expires_on, long actor_user_id) {
        if (lot_code == null || lot_code.isBlank()) {
            throw new IllegalArgumentException("Lot code is required.");
        }
        if (expires_on != null && manufactured_on != null && expires_on.isBefore(manufactured_on)) {
            throw new IllegalArgumentException("Expiry date cannot be before manufactured date.");
        }
        if (!lot_repository.active_stock_item(stock_item_id)) {
            throw new resource_not_found_exception("Active stock item");
        }
        String normalized_lot_code = lot_code.trim();
        List<inventory_stock_lot_snapshot> existing = lot_repository.find(stock_item_id, normalized_lot_code);
        if (!existing.isEmpty()) {
            inventory_stock_lot_snapshot lot = existing.getFirst();
            if (!"active".equals(lot.status())) {
                throw new IllegalArgumentException("The stock lot is not active.");
            }
            if ((manufactured_on != null && lot.manufactured_on() != null && !manufactured_on.equals(lot.manufactured_on()))
                    || (expires_on != null && lot.expires_on() != null && !expires_on.equals(lot.expires_on()))) {
                throw new field_conflict_exception("lot_code", "Lot dates do not match the existing stock lot.");
            }
            return lot;
        }
        lot_repository.insert_lot(stock_item_id, normalized_lot_code, manufactured_on, expires_on, actor_user_id);
        return lot_repository.find(stock_item_id, normalized_lot_code).stream().findFirst()
                .orElseThrow(() -> new resource_not_found_exception("Stock lot"));
    }
}
