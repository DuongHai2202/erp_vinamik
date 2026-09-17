package vn.vinamik.erp_backend.inventory.stock_item;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.vinamik.erp_backend.platform.common.api_success_response;
import vn.vinamik.erp_backend.inventory.master_data.inventory_location_lookup_response;
import vn.vinamik.erp_backend.inventory.master_data.inventory_supplier_lookup_response;
import vn.vinamik.erp_backend.inventory.master_data.inventory_warehouse_lookup_response;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/master_data")
public class inventory_master_lookup_controller {
    private final inventory_master_lookup_repository lookup_repository;

    public inventory_master_lookup_controller(inventory_master_lookup_repository lookup_repository) {
        this.lookup_repository = lookup_repository;
    }

    @GetMapping("/units")
    @PreAuthorize("hasAnyAuthority('inventory_material_read', 'production_plan_read', 'production_bom_read', 'production_order_read', 'production_output_read')")
    public api_success_response<List<stock_unit_response>> units() {
        return new api_success_response<>("Units retrieved successfully.", lookup_repository.find_active_units());
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyAuthority('inventory_material_read', 'production_plan_read', 'production_bom_read', 'production_order_read', 'production_output_read')")
    public api_success_response<List<stock_category_response>> categories() {
        return new api_success_response<>("Categories retrieved successfully.", lookup_repository.find_active_categories());
    }
    @GetMapping("/suppliers")
    @PreAuthorize("hasAnyAuthority('inventory_master_read', 'inventory_receipt_read', 'inventory_receipt_create')")
    public api_success_response<List<inventory_supplier_lookup_response>> suppliers() {
        return new api_success_response<>("Suppliers retrieved successfully.", lookup_repository.find_active_suppliers());
    }

    @GetMapping("/warehouses")
    @PreAuthorize("hasAnyAuthority('inventory_master_read', 'inventory_receipt_read', 'inventory_receipt_create', 'inventory_issue_read', 'inventory_issue_create', 'inventory_transfer_read', 'inventory_transfer_create', 'inventory_stocktake_read', 'inventory_stocktake_create', 'production_output_read', 'production_output_create')")
    public api_success_response<List<inventory_warehouse_lookup_response>> warehouses() {
        return new api_success_response<>("Warehouses retrieved successfully.", lookup_repository.find_active_warehouses());
    }

    @GetMapping("/warehouses/{warehouse_id}/locations")
    @PreAuthorize("hasAnyAuthority('inventory_master_read', 'inventory_receipt_read', 'inventory_receipt_create', 'inventory_issue_read', 'inventory_issue_create', 'inventory_transfer_read', 'inventory_transfer_create', 'inventory_stocktake_read', 'inventory_stocktake_create', 'production_output_read', 'production_output_create')")
    public api_success_response<List<inventory_location_lookup_response>> locations(@PathVariable long warehouse_id) {
        return new api_success_response<>("Warehouse locations retrieved successfully.", lookup_repository.find_active_locations(warehouse_id));
    }
}
