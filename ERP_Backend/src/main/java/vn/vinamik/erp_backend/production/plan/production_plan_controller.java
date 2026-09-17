package vn.vinamik.erp_backend.production.plan;

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
@RequestMapping("/api/v1/production/plans")
public class production_plan_controller {
    private final production_plan_service plan_service;

    public production_plan_controller(production_plan_service plan_service) {
        this.plan_service = plan_service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('production_plan_read')")
    public api_success_response<production_plan_page_response> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Production plans retrieved successfully.", plan_service.search(search, status, page, page_size));
    }

    @GetMapping("/{plan_id}")
    @PreAuthorize("hasAuthority('production_plan_read')")
    public api_success_response<production_plan_response> find_by_id(@PathVariable long plan_id) {
        return new api_success_response<>("Production plan retrieved successfully.", plan_service.find_by_id(plan_id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('production_plan_create')")
    public api_success_response<production_plan_response> create(
            @Valid @RequestBody production_plan_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Production plan created successfully.", plan_service.create(request, actor, correlation_id(http_request)));
    }

    @PutMapping("/{plan_id}")
    @PreAuthorize("hasAuthority('production_plan_update')")
    public api_success_response<production_plan_response> update(
            @PathVariable long plan_id,
            @Valid @RequestBody production_plan_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Production plan updated successfully.", plan_service.update(plan_id, request, actor, correlation_id(http_request)));
    }

    @PostMapping("/{plan_id}/status")
    @PreAuthorize("hasAuthority('production_plan_approve')")
    public api_success_response<production_plan_response> change_status(
            @PathVariable long plan_id,
            @Valid @RequestBody production_plan_status_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Production plan status changed successfully.", plan_service.change_status(plan_id, request.status(), actor, correlation_id(http_request)));
    }

    @DeleteMapping("/{plan_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('production_plan_delete')")
    public void delete(
            @PathVariable long plan_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        plan_service.delete(plan_id, actor, correlation_id(http_request));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}