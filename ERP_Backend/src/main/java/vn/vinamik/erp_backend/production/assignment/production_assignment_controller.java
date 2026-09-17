package vn.vinamik.erp_backend.production.assignment;

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

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/production/assignments")
public class production_assignment_controller {
    private final production_assignment_service assignment_service;

    public production_assignment_controller(production_assignment_service assignment_service) {
        this.assignment_service = assignment_service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('production_assignment_read')")
    public api_success_response<production_assignment_page_response> search(
            @RequestParam(required = false) Long production_order_id,
            @RequestParam(required = false) Long employee_id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Instant from_at,
            @RequestParam(required = false) Instant to_at,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Production assignments retrieved successfully.", assignment_service.search(
                production_order_id, employee_id, status, from_at, to_at, page, page_size));
    }

    @GetMapping("/{production_assignment_id}")
    @PreAuthorize("hasAuthority('production_assignment_read')")
    public api_success_response<production_assignment_response> find_by_id(@PathVariable long production_assignment_id) {
        return new api_success_response<>("Production assignment retrieved successfully.", assignment_service.find_by_id(production_assignment_id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('production_assignment_create')")
    public api_success_response<production_assignment_response> create(
            @Valid @RequestBody production_assignment_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Production assignment created successfully.", assignment_service.create(
                request, actor, correlation_id(http_request)));
    }

    @PutMapping("/{production_assignment_id}")
    @PreAuthorize("hasAuthority('production_assignment_update')")
    public api_success_response<production_assignment_response> update(
            @PathVariable long production_assignment_id,
            @Valid @RequestBody production_assignment_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Production assignment updated successfully.", assignment_service.update(
                production_assignment_id, request, actor, correlation_id(http_request)));
    }

    @PostMapping("/{production_assignment_id}/status")
    @PreAuthorize("hasAuthority('production_assignment_update')")
    public api_success_response<production_assignment_response> change_status(
            @PathVariable long production_assignment_id,
            @Valid @RequestBody production_assignment_status_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Production assignment status changed successfully.", assignment_service.change_status(
                production_assignment_id, request.status(), actor, correlation_id(http_request)));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}
