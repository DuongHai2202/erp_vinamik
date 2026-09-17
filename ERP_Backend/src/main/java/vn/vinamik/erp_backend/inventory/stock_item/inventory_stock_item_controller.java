package vn.vinamik.erp_backend.inventory.stock_item;

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
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

@RestController
@RequestMapping("/api/v1/inventory/materials")
public class inventory_stock_item_controller {
    private final inventory_stock_item_service stock_item_service;

    public inventory_stock_item_controller(inventory_stock_item_service stock_item_service) {
        this.stock_item_service = stock_item_service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('inventory_material_read', 'production_plan_read', 'production_bom_read', 'production_order_read', 'production_output_read')")
    public api_success_response<stock_item_page_response> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(name = "item_type", defaultValue = "raw_material") String item_type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Materials retrieved successfully.", stock_item_service.search(search, status, item_type, page, page_size));
    }

    @GetMapping("/{stock_item_id}")
    @PreAuthorize("hasAnyAuthority('inventory_material_read', 'production_plan_read', 'production_bom_read', 'production_order_read', 'production_output_read')")
    public api_success_response<stock_item_response> find_by_id(@PathVariable long stock_item_id) {
        return new api_success_response<>("Material retrieved successfully.", stock_item_service.find_by_id(stock_item_id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('inventory_material_create')")
    public api_success_response<stock_item_response> create(
            @Valid @RequestBody stock_item_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Material created successfully.", stock_item_service.create(request, actor, correlation_id(http_request)));
    }

    @PutMapping("/{stock_item_id}")
    @PreAuthorize("hasAuthority('inventory_material_update')")
    public api_success_response<stock_item_response> update(
            @PathVariable long stock_item_id,
            @Valid @RequestBody stock_item_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Material updated successfully.", stock_item_service.update(stock_item_id, request, actor, correlation_id(http_request)));
    }

    @DeleteMapping("/{stock_item_id}")
    @PreAuthorize("hasAuthority('inventory_material_deactivate')")
    public api_success_response<stock_item_response> deactivate(
            @PathVariable long stock_item_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Material deactivated successfully.", stock_item_service.deactivate(stock_item_id, actor, correlation_id(http_request)));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}