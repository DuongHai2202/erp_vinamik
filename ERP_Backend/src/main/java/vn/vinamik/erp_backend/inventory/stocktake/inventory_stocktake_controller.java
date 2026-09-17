package vn.vinamik.erp_backend.inventory.stocktake;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.vinamik.erp_backend.platform.common.api_success_response;
import vn.vinamik.erp_backend.platform.common.correlation_id_filter;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

@RestController
@RequestMapping("/api/v1/inventory/stocktakes")
public class inventory_stocktake_controller {
    private final inventory_stocktake_service stocktake_service;

    public inventory_stocktake_controller(inventory_stocktake_service stocktake_service) {
        this.stocktake_service = stocktake_service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory_stocktake_read')")
    public api_success_response<stocktake_page_response> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long warehouse_id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Stocktakes retrieved successfully.", stocktake_service.search(
                search, warehouse_id, status, page, page_size));
    }

    @GetMapping("/{stocktake_id}")
    @PreAuthorize("hasAuthority('inventory_stocktake_read')")
    public api_success_response<stocktake_response> find_by_id(@PathVariable long stocktake_id) {
        return new api_success_response<>("Stocktake retrieved successfully.", stocktake_service.find_by_id(stocktake_id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('inventory_stocktake_create')")
    public api_success_response<stocktake_response> create(
            @Valid @RequestBody stocktake_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Stocktake created successfully.", stocktake_service.create(
                request, actor, correlation_id(http_request)));
    }

    @PostMapping("/{stocktake_id}/lines/{stocktake_line_id}/count")
    @PreAuthorize("hasAuthority('inventory_stocktake_create')")
    public api_success_response<stocktake_line_response> count_line(
            @PathVariable long stocktake_id,
            @PathVariable long stocktake_line_id,
            @Valid @RequestBody stocktake_count_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Stocktake line counted successfully.", stocktake_service.count_line(
                stocktake_id, stocktake_line_id, request, actor, correlation_id(http_request)));
    }

    @PostMapping("/{stocktake_id}/submit")
    @PreAuthorize("hasAuthority('inventory_stocktake_create')")
    public api_success_response<stocktake_response> submit(
            @PathVariable long stocktake_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Stocktake submitted successfully.", stocktake_service.submit(
                stocktake_id, actor, correlation_id(http_request)));
    }

    @PostMapping("/{stocktake_id}/approve")
    @PreAuthorize("hasAuthority('inventory_stocktake_adjust')")
    public api_success_response<stocktake_response> approve(
            @PathVariable long stocktake_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Stocktake approved successfully.", stocktake_service.approve(
                stocktake_id, actor, correlation_id(http_request)));
    }

    @PostMapping("/{stocktake_id}/post")
    @PreAuthorize("hasAuthority('inventory_stocktake_adjust')")
    public api_success_response<stocktake_response> post(
            @PathVariable long stocktake_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Stocktake posted successfully.", stocktake_service.post(
                stocktake_id, actor, correlation_id(http_request)));
    }

    @PostMapping("/{stocktake_id}/cancel")
    @PreAuthorize("hasAuthority('inventory_stocktake_create')")
    public api_success_response<stocktake_response> cancel(
            @PathVariable long stocktake_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Stocktake cancelled successfully.", stocktake_service.cancel(
                stocktake_id, actor, correlation_id(http_request)));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}
