package vn.vinamik.erp_backend.human_resources.contract;

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
@RequestMapping("/api/v1/human_resources/contracts")
public class human_resources_contract_controller {
    private final human_resources_contract_service contract_service;

    public human_resources_contract_controller(human_resources_contract_service contract_service) {
        this.contract_service = contract_service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('hr_contract_read')")
    public api_success_response<employment_contract_page_response> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long employee_id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean expiring_only,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Employment contracts retrieved successfully.", contract_service.search(search, employee_id, status, expiring_only, page, page_size));
    }

    @GetMapping("/{employment_contract_id}")
    @PreAuthorize("hasAuthority('hr_contract_read')")
    public api_success_response<employment_contract_response> find_by_id(@PathVariable long employment_contract_id) {
        return new api_success_response<>("Employment contract retrieved successfully.", contract_service.find_by_id(employment_contract_id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('hr_contract_create')")
    public api_success_response<employment_contract_response> create(
            @Valid @RequestBody employment_contract_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Employment contract created successfully.", contract_service.create(request, actor, correlation_id(http_request)));
    }

    @PutMapping("/{employment_contract_id}")
    @PreAuthorize("hasAuthority('hr_contract_update')")
    public api_success_response<employment_contract_response> update(
            @PathVariable long employment_contract_id,
            @Valid @RequestBody employment_contract_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Employment contract updated successfully.", contract_service.update(employment_contract_id, request, actor, correlation_id(http_request)));
    }

    @PostMapping("/{employment_contract_id}/status")
    @PreAuthorize("hasAuthority('hr_contract_approve')")
    public api_success_response<employment_contract_response> change_status(
            @PathVariable long employment_contract_id,
            @Valid @RequestBody employment_contract_status_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Employment contract status updated successfully.", contract_service.change_status(employment_contract_id, request, actor, correlation_id(http_request)));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}
