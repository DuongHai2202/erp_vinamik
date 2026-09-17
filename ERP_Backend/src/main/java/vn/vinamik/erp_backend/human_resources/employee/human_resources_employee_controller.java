package vn.vinamik.erp_backend.human_resources.employee;

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
@RequestMapping("/api/v1/human_resources/employees")
public class human_resources_employee_controller {
    private final human_resources_employee_service employee_service;

    public human_resources_employee_controller(human_resources_employee_service employee_service) {
        this.employee_service = employee_service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('hr_employee_read')")
    public api_success_response<employee_page_response> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Employees retrieved successfully.", employee_service.search(search, status, page, page_size));
    }

    @GetMapping("/{employee_id}")
    @PreAuthorize("hasAuthority('hr_employee_read')")
    public api_success_response<employee_response> find_by_id(@PathVariable long employee_id) {
        return new api_success_response<>("Employee retrieved successfully.", employee_service.find_by_id(employee_id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('hr_employee_create')")
    public api_success_response<employee_response> create(
            @Valid @RequestBody employee_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Employee created successfully.", employee_service.create(request, actor, correlation_id(http_request)));
    }

    @PutMapping("/{employee_id}")
    @PreAuthorize("hasAuthority('hr_employee_update')")
    public api_success_response<employee_response> update(
            @PathVariable long employee_id,
            @Valid @RequestBody employee_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Employee updated successfully.", employee_service.update(employee_id, request, actor, correlation_id(http_request)));
    }

    @DeleteMapping("/{employee_id}")
    @PreAuthorize("hasAuthority('hr_employee_deactivate')")
    public api_success_response<employee_response> deactivate(
            @PathVariable long employee_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Employee deactivated successfully.", employee_service.deactivate(employee_id, actor, correlation_id(http_request)));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}