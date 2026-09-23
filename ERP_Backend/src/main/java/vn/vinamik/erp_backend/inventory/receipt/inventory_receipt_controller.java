package vn.vinamik.erp_backend.inventory.receipt;

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
@RequestMapping("/api/v1/inventory/receipts")
public class inventory_receipt_controller {
    private final inventory_receipt_service receipt_service;

    public inventory_receipt_controller(inventory_receipt_service receipt_service) {
        this.receipt_service = receipt_service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory_receipt_read')")
    public api_success_response<receipt_page_response> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long warehouse_id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Receipts retrieved successfully.", receipt_service.search(search, warehouse_id, status, page, page_size));
    }

    @GetMapping("/{receipt_id}")
    @PreAuthorize("hasAuthority('inventory_receipt_read')")
    public api_success_response<receipt_response> find_by_id(@PathVariable long receipt_id) {
        return new api_success_response<>("Receipt retrieved successfully.", receipt_service.find_by_id(receipt_id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('inventory_receipt_create')")
    public api_success_response<receipt_response> create(
            @Valid @RequestBody receipt_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Receipt created successfully.", receipt_service.create(request, actor, correlation_id(http_request)));
    }

    @PutMapping("/{receipt_id}")
    @PreAuthorize("hasAuthority('inventory_receipt_update')")
    public api_success_response<receipt_response> update(@PathVariable long receipt_id, @Valid @RequestBody receipt_request request,
                                                         @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) {
        return new api_success_response<>("Receipt updated successfully.", receipt_service.update(receipt_id, request, actor, correlation_id(http_request)));
    }

    @DeleteMapping("/{receipt_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('inventory_receipt_delete')")
    public void delete(@PathVariable long receipt_id, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) {
        receipt_service.delete(receipt_id, actor, correlation_id(http_request));
    }

    @PostMapping("/{receipt_id}/cancel")
    @PreAuthorize("hasAuthority('inventory_receipt_update')")
    public api_success_response<receipt_response> cancel(@PathVariable long receipt_id,
                                                         @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) {
        return new api_success_response<>("Receipt cancelled successfully.", receipt_service.cancel(receipt_id, actor, correlation_id(http_request)));
    }
    @PostMapping("/{receipt_id}/submit")
    @PreAuthorize("hasAuthority('inventory_receipt_update')")
    public api_success_response<receipt_response> submit(@PathVariable long receipt_id,
                                                         @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) {
        return new api_success_response<>("Receipt submitted successfully.", receipt_service.submit(receipt_id, actor, correlation_id(http_request)));
    }

    @PostMapping("/{receipt_id}/post")
    @PreAuthorize("hasAuthority('inventory_receipt_post')")
    public api_success_response<receipt_response> post(
            @PathVariable long receipt_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Receipt posted successfully.", receipt_service.post(receipt_id, actor, correlation_id(http_request)));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}
