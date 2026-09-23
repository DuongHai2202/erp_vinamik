package vn.vinamik.erp_backend.production.order;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/v1/production/orders")
public class production_order_controller {
    private final production_order_service order_service;

    public production_order_controller(production_order_service order_service) {
        this.order_service = order_service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('production_order_read', 'production_output_read')")
    public api_success_response<production_order_page_response> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long stock_item_id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Production orders retrieved successfully.", order_service.search(
                search, status, stock_item_id, page, page_size));
    }

    @GetMapping("/overdue_count")
    @PreAuthorize("hasAuthority('production_order_read')")
    public api_success_response<Long> count_overdue() {
        return new api_success_response<>("Overdue production orders count retrieved successfully.",
                order_service.count_overdue());
    }
    @GetMapping("/{production_order_id}")
    @PreAuthorize("hasAnyAuthority('production_order_read', 'production_output_read')")
    public api_success_response<production_order_response> find_by_id(@PathVariable long production_order_id) {
        return new api_success_response<>("Production order retrieved successfully.", order_service.find_by_id(production_order_id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('production_order_create')")
    public api_success_response<production_order_response> create(
            @Valid @RequestBody production_order_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Production order created successfully.", order_service.create(
                request, actor, correlation_id(http_request)));
    }

    @PutMapping("/{production_order_id}")
    @PreAuthorize("hasAuthority('production_order_update')")
    public api_success_response<production_order_response> update(
            @PathVariable long production_order_id,
            @Valid @RequestBody production_order_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Production order updated successfully.", order_service.update(
                production_order_id, request, actor, correlation_id(http_request)));
    }

    @PostMapping("/{production_order_id}/release")
    @PreAuthorize("hasAuthority('production_order_release')")
    public api_success_response<production_order_response> release(
            @PathVariable long production_order_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Production order released successfully.", order_service.release(
                production_order_id, actor, correlation_id(http_request)));
    }

    @PostMapping("/{production_order_id}/status")
    @PreAuthorize("hasAuthority('production_order_update')")
    public api_success_response<production_order_response> change_operational_status(
            @PathVariable long production_order_id,
            @Valid @RequestBody production_order_status_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Production order status changed successfully.", order_service.change_operational_status(
                production_order_id, request.status(), actor, correlation_id(http_request)));
    }

    @PostMapping("/{production_order_id}/complete")
    @PreAuthorize("hasAuthority('production_order_complete')")
    public api_success_response<production_order_response> complete(
            @PathVariable long production_order_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Production order completed successfully.", order_service.change_status(
                production_order_id, "completed", actor, correlation_id(http_request)));
    }

    @PostMapping("/{production_order_id}/cancel")
    @PreAuthorize("hasAuthority('production_order_update')")
    public api_success_response<production_order_response> cancel(
            @PathVariable long production_order_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Production order cancelled successfully.", order_service.change_operational_status(
                production_order_id, "cancelled", actor, correlation_id(http_request)));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}

