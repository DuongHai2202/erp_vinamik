package vn.vinamik.erp_backend.inventory.issue;

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
@RequestMapping("/api/v1/inventory/issues")
public class inventory_issue_controller {
    private final inventory_issue_service issue_service;

    public inventory_issue_controller(inventory_issue_service issue_service) {
        this.issue_service = issue_service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory_issue_read')")
    public api_success_response<issue_page_response> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long warehouse_id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Issues retrieved successfully.", issue_service.search(search, warehouse_id, status, page, page_size));
    }

    @GetMapping("/{issue_id}")
    @PreAuthorize("hasAuthority('inventory_issue_read')")
    public api_success_response<issue_response> find_by_id(@PathVariable long issue_id) {
        return new api_success_response<>("Issue retrieved successfully.", issue_service.find_by_id(issue_id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('inventory_issue_create')")
    public api_success_response<issue_response> create(
            @Valid @RequestBody issue_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Issue created successfully.", issue_service.create(request, actor, correlation_id(http_request)));
    }

    @PutMapping("/{issue_id}")
    @PreAuthorize("hasAuthority('inventory_issue_update')")
    public api_success_response<issue_response> update(@PathVariable long issue_id, @Valid @RequestBody issue_request request,
                                                       @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) {
        return new api_success_response<>("Issue updated successfully.", issue_service.update(issue_id, request, actor, correlation_id(http_request)));
    }

    @DeleteMapping("/{issue_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('inventory_issue_delete')")
    public void delete(@PathVariable long issue_id, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) {
        issue_service.delete(issue_id, actor, correlation_id(http_request));
    }

    @PostMapping("/{issue_id}/cancel")
    @PreAuthorize("hasAuthority('inventory_issue_update')")
    public api_success_response<issue_response> cancel(@PathVariable long issue_id,
                                                       @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) {
        return new api_success_response<>("Issue cancelled successfully.", issue_service.cancel(issue_id, actor, correlation_id(http_request)));
    }
    @PostMapping("/{issue_id}/submit")
    @PreAuthorize("hasAuthority('inventory_issue_update')")
    public api_success_response<issue_response> submit(@PathVariable long issue_id,
                                                       @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) {
        return new api_success_response<>("Issue submitted successfully.", issue_service.submit(issue_id, actor, correlation_id(http_request)));
    }

    @PostMapping("/{issue_id}/post")
    @PreAuthorize("hasAuthority('inventory_issue_post')")
    public api_success_response<issue_response> post(
            @PathVariable long issue_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Issue posted successfully.", issue_service.post(issue_id, actor, correlation_id(http_request)));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}
