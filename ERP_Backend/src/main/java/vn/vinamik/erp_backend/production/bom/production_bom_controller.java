package vn.vinamik.erp_backend.production.bom;

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
@RequestMapping("/api/v1/production/boms")
public class production_bom_controller {
    private final production_bom_service bom_service;

    public production_bom_controller(production_bom_service bom_service) {
        this.bom_service = bom_service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('production_bom_read')")
    public api_success_response<bom_page_response> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long stock_item_id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("BOMs retrieved successfully.", bom_service.search(search, stock_item_id, status, page, page_size));
    }

    @GetMapping("/{bom_id}")
    @PreAuthorize("hasAuthority('production_bom_read')")
    public api_success_response<bom_response> find_by_id(@PathVariable long bom_id) {
        return new api_success_response<>("BOM retrieved successfully.", bom_service.find_by_id(bom_id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('production_bom_create')")
    public api_success_response<bom_response> create(
            @Valid @RequestBody bom_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("BOM created successfully.", bom_service.create(request, actor, correlation_id(http_request)));
    }

    @PutMapping("/{bom_id}")
    @PreAuthorize("hasAuthority('production_bom_update')")
    public api_success_response<bom_response> update(
            @PathVariable long bom_id,
            @Valid @RequestBody bom_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("BOM updated successfully.", bom_service.update(bom_id, request, actor, correlation_id(http_request)));
    }

    @DeleteMapping("/{bom_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('production_bom_delete')")
    public void delete(@PathVariable long bom_id, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) {
        bom_service.delete(bom_id, actor, correlation_id(http_request));
    }

    @PostMapping("/{bom_id}/status")
    @PreAuthorize("hasAuthority('production_bom_approve')")
    public api_success_response<bom_response> change_status(
            @PathVariable long bom_id,
            @Valid @RequestBody bom_status_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("BOM status changed successfully.", bom_service.change_status(bom_id, request.status(), actor, correlation_id(http_request)));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}
