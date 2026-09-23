package vn.vinamik.erp_backend.inventory.transfer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/inventory/transfers")
public class inventory_transfer_controller {
    private final inventory_transfer_service transfer_service;

    public inventory_transfer_controller(inventory_transfer_service transfer_service) {
        this.transfer_service = transfer_service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory_transfer_read')")
    public api_success_response<transfer_page_response> search(
            @RequestParam(required = false) String search,
            @RequestParam(name = "source_warehouse_id", required = false) Long source_warehouse_id,
            @RequestParam(name = "destination_warehouse_id", required = false) Long destination_warehouse_id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Transfers retrieved successfully.", transfer_service.search(
                search, source_warehouse_id, destination_warehouse_id, status, page, page_size));
    }

    @GetMapping("/{transfer_id}")
    @PreAuthorize("hasAuthority('inventory_transfer_read')")
    public api_success_response<transfer_response> find_by_id(@PathVariable long transfer_id) {
        return new api_success_response<>("Transfer retrieved successfully.", transfer_service.find_by_id(transfer_id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('inventory_transfer_create')")
    public api_success_response<transfer_response> create(
            @Valid @RequestBody transfer_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Transfer created successfully.", transfer_service.create(
                request, actor, correlation_id(http_request)));
    }

    @PutMapping("/{transfer_id}")
    @PreAuthorize("hasAuthority('inventory_transfer_update')")
    public api_success_response<transfer_response> update(@PathVariable long transfer_id, @Valid @RequestBody transfer_request request,
                                                          @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) {
        return new api_success_response<>("Transfer updated successfully.", transfer_service.update(transfer_id, request, actor, correlation_id(http_request)));
    }

    @DeleteMapping("/{transfer_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('inventory_transfer_delete')")
    public void delete(@PathVariable long transfer_id, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) {
        transfer_service.delete(transfer_id, actor, correlation_id(http_request));
    }

    @PostMapping("/{transfer_id}/cancel")
    @PreAuthorize("hasAuthority('inventory_transfer_update')")
    public api_success_response<transfer_response> cancel(@PathVariable long transfer_id,
                                                          @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) {
        return new api_success_response<>("Transfer cancelled successfully.", transfer_service.cancel(transfer_id, actor, correlation_id(http_request)));
    }
    @PostMapping("/{transfer_id}/submit")
    @PreAuthorize("hasAuthority('inventory_transfer_update')")
    public api_success_response<transfer_response> submit(@PathVariable long transfer_id,
                                                          @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) {
        return new api_success_response<>("Transfer submitted successfully.", transfer_service.submit(transfer_id, actor, correlation_id(http_request)));
    }

    @PostMapping("/{transfer_id}/post")
    @PreAuthorize("hasAuthority('inventory_transfer_post')")
    public api_success_response<transfer_response> post(
            @PathVariable long transfer_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Transfer posted successfully.", transfer_service.post(
                transfer_id, actor, correlation_id(http_request)));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}
