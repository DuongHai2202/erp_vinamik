package vn.vinamik.erp_backend.inventory.master_data;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.vinamik.erp_backend.platform.common.api_success_response;
import vn.vinamik.erp_backend.platform.common.correlation_id_filter;
import vn.vinamik.erp_backend.platform.common.master_data_page_response;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

@RestController
@RequestMapping("/api/v1/inventory/master_data")
public class inventory_master_data_controller {
    private final inventory_master_data_service master_data_service;

    public inventory_master_data_controller(inventory_master_data_service master_data_service) {
        this.master_data_service = master_data_service;
    }

    @GetMapping("/manage/units")
    @PreAuthorize("hasAuthority('inventory_master_read')")
    public api_success_response<master_data_page_response<inventory_unit_response>> search_units(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Units retrieved successfully.",
                master_data_service.search_units(search, status, page, page_size));
    }

    @GetMapping("/units/{unit_of_measure_id}")
    @PreAuthorize("hasAuthority('inventory_master_read')")
    public api_success_response<inventory_unit_response> find_unit(@PathVariable long unit_of_measure_id) {
        return new api_success_response<>("Unit retrieved successfully.",
                master_data_service.find_unit(unit_of_measure_id));
    }

    @PostMapping("/units")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('inventory_master_create')")
    public api_success_response<inventory_unit_response> create_unit(
            @Valid @RequestBody inventory_unit_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Unit created successfully.",
                master_data_service.create_unit(request, actor, correlation_id(http_request)));
    }

    @PutMapping("/units/{unit_of_measure_id}")
    @PreAuthorize("hasAuthority('inventory_master_update')")
    public api_success_response<inventory_unit_response> update_unit(
            @PathVariable long unit_of_measure_id,
            @Valid @RequestBody inventory_unit_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Unit updated successfully.",
                master_data_service.update_unit(unit_of_measure_id, request, actor, correlation_id(http_request)));
    }

    @DeleteMapping("/units/{unit_of_measure_id}")
    @PreAuthorize("hasAuthority('inventory_master_deactivate')")
    public api_success_response<inventory_unit_response> deactivate_unit(
            @PathVariable long unit_of_measure_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Unit deactivated successfully.",
                master_data_service.deactivate_unit(unit_of_measure_id, actor, correlation_id(http_request)));
    }

    @GetMapping("/manage/categories")
    @PreAuthorize("hasAuthority('inventory_master_read')")
    public api_success_response<master_data_page_response<inventory_category_response>> search_categories(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Categories retrieved successfully.",
                master_data_service.search_categories(search, status, page, page_size));
    }

    @GetMapping("/categories/{item_category_id}")
    @PreAuthorize("hasAuthority('inventory_master_read')")
    public api_success_response<inventory_category_response> find_category(@PathVariable long item_category_id) {
        return new api_success_response<>("Category retrieved successfully.",
                master_data_service.find_category(item_category_id));
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('inventory_master_create')")
    public api_success_response<inventory_category_response> create_category(
            @Valid @RequestBody inventory_category_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Category created successfully.",
                master_data_service.create_category(request, actor, correlation_id(http_request)));
    }

    @PutMapping("/categories/{item_category_id}")
    @PreAuthorize("hasAuthority('inventory_master_update')")
    public api_success_response<inventory_category_response> update_category(
            @PathVariable long item_category_id,
            @Valid @RequestBody inventory_category_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Category updated successfully.",
                master_data_service.update_category(item_category_id, request, actor, correlation_id(http_request)));
    }

    @DeleteMapping("/categories/{item_category_id}")
    @PreAuthorize("hasAuthority('inventory_master_deactivate')")
    public api_success_response<inventory_category_response> deactivate_category(
            @PathVariable long item_category_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Category deactivated successfully.",
                master_data_service.deactivate_category(item_category_id, actor, correlation_id(http_request)));
    }

    @GetMapping("/manage/suppliers")
    @PreAuthorize("hasAuthority('inventory_master_read')")
    public api_success_response<master_data_page_response<inventory_supplier_response>> search_suppliers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Suppliers retrieved successfully.",
                master_data_service.search_suppliers(search, status, page, page_size));
    }

    @GetMapping("/suppliers/{supplier_id}")
    @PreAuthorize("hasAuthority('inventory_master_read')")
    public api_success_response<inventory_supplier_response> find_supplier(@PathVariable long supplier_id) {
        return new api_success_response<>("Supplier retrieved successfully.",
                master_data_service.find_supplier(supplier_id));
    }

    @PostMapping("/suppliers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('inventory_master_create')")
    public api_success_response<inventory_supplier_response> create_supplier(
            @Valid @RequestBody inventory_supplier_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Supplier created successfully.",
                master_data_service.create_supplier(request, actor, correlation_id(http_request)));
    }

    @PutMapping("/suppliers/{supplier_id}")
    @PreAuthorize("hasAuthority('inventory_master_update')")
    public api_success_response<inventory_supplier_response> update_supplier(
            @PathVariable long supplier_id,
            @Valid @RequestBody inventory_supplier_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Supplier updated successfully.",
                master_data_service.update_supplier(supplier_id, request, actor, correlation_id(http_request)));
    }

    @DeleteMapping("/suppliers/{supplier_id}")
    @PreAuthorize("hasAuthority('inventory_master_deactivate')")
    public api_success_response<inventory_supplier_response> deactivate_supplier(
            @PathVariable long supplier_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Supplier deactivated successfully.",
                master_data_service.deactivate_supplier(supplier_id, actor, correlation_id(http_request)));
    }

    @GetMapping("/manage/warehouses")
    @PreAuthorize("hasAuthority('inventory_master_read')")
    public api_success_response<master_data_page_response<inventory_warehouse_response>> search_warehouses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Warehouses retrieved successfully.",
                master_data_service.search_warehouses(search, status, page, page_size));
    }

    @GetMapping("/warehouses/{warehouse_id}")
    @PreAuthorize("hasAuthority('inventory_master_read')")
    public api_success_response<inventory_warehouse_response> find_warehouse(@PathVariable long warehouse_id) {
        return new api_success_response<>("Warehouse retrieved successfully.",
                master_data_service.find_warehouse(warehouse_id));
    }

    @PostMapping("/warehouses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('inventory_master_create')")
    public api_success_response<inventory_warehouse_response> create_warehouse(
            @Valid @RequestBody inventory_warehouse_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Warehouse created successfully.",
                master_data_service.create_warehouse(request, actor, correlation_id(http_request)));
    }

    @PutMapping("/warehouses/{warehouse_id}")
    @PreAuthorize("hasAuthority('inventory_master_update')")
    public api_success_response<inventory_warehouse_response> update_warehouse(
            @PathVariable long warehouse_id,
            @Valid @RequestBody inventory_warehouse_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Warehouse updated successfully.",
                master_data_service.update_warehouse(warehouse_id, request, actor, correlation_id(http_request)));
    }

    @DeleteMapping("/warehouses/{warehouse_id}")
    @PreAuthorize("hasAuthority('inventory_master_deactivate')")
    public api_success_response<inventory_warehouse_response> deactivate_warehouse(
            @PathVariable long warehouse_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Warehouse deactivated successfully.",
                master_data_service.deactivate_warehouse(warehouse_id, actor, correlation_id(http_request)));
    }

    @GetMapping("/manage/warehouses/{warehouse_id}/locations")
    @PreAuthorize("hasAuthority('inventory_master_read')")
    public api_success_response<master_data_page_response<inventory_location_response>> search_locations(
            @PathVariable long warehouse_id,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Warehouse locations retrieved successfully.",
                master_data_service.search_locations(warehouse_id, search, status, page, page_size));
    }

    @GetMapping("/locations/{warehouse_location_id}")
    @PreAuthorize("hasAuthority('inventory_master_read')")
    public api_success_response<inventory_location_response> find_location(@PathVariable long warehouse_location_id) {
        return new api_success_response<>("Warehouse location retrieved successfully.",
                master_data_service.find_location(warehouse_location_id));
    }

    @PostMapping("/locations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('inventory_master_create')")
    public api_success_response<inventory_location_response> create_location(
            @Valid @RequestBody inventory_location_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Warehouse location created successfully.",
                master_data_service.create_location(request, actor, correlation_id(http_request)));
    }

    @PutMapping("/locations/{warehouse_location_id}")
    @PreAuthorize("hasAuthority('inventory_master_update')")
    public api_success_response<inventory_location_response> update_location(
            @PathVariable long warehouse_location_id,
            @Valid @RequestBody inventory_location_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Warehouse location updated successfully.",
                master_data_service.update_location(warehouse_location_id, request, actor, correlation_id(http_request)));
    }

    @DeleteMapping("/locations/{warehouse_location_id}")
    @PreAuthorize("hasAuthority('inventory_master_deactivate')")
    public api_success_response<inventory_location_response> deactivate_location(
            @PathVariable long warehouse_location_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Warehouse location deactivated successfully.",
                master_data_service.deactivate_location(warehouse_location_id, actor, correlation_id(http_request)));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}
