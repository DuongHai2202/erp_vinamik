package vn.vinamik.erp_backend.human_resources.absence;

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

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/human_resources/absences")
public class human_resources_absence_controller {
    private final human_resources_absence_service absence_service;

    public human_resources_absence_controller(human_resources_absence_service absence_service) {
        this.absence_service = absence_service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('hr_absence_read')")
    public api_success_response<leave_page_response> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long employee_id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate from_date,
            @RequestParam(required = false) LocalDate to_date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Leave requests retrieved successfully.", absence_service.search(search, employee_id, status, from_date, to_date, page, page_size));
    }

    @GetMapping("/{leave_request_id}")
    @PreAuthorize("hasAuthority('hr_absence_read')")
    public api_success_response<leave_response> find_by_id(@PathVariable long leave_request_id) {
        return new api_success_response<>("Leave request retrieved successfully.", absence_service.find_by_id(leave_request_id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('hr_absence_create')")
    public api_success_response<leave_response> create(
            @Valid @RequestBody leave_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Leave request created successfully.", absence_service.create(request, actor, correlation_id(http_request)));
    }

    @PutMapping("/{leave_request_id}")
    @PreAuthorize("hasAuthority('hr_absence_update')")
    public api_success_response<leave_response> update(
            @PathVariable long leave_request_id,
            @Valid @RequestBody leave_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Leave request updated successfully.", absence_service.update(leave_request_id, request, actor, correlation_id(http_request)));
    }

    @PostMapping("/{leave_request_id}/decision")
    @PreAuthorize("hasAuthority('hr_absence_approve')")
    public api_success_response<leave_response> decide(
            @PathVariable long leave_request_id,
            @Valid @RequestBody leave_decision_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Leave request decision saved successfully.", absence_service.decide(leave_request_id, request, actor, correlation_id(http_request)));
    }

    @PostMapping("/{leave_request_id}/cancel")
    @PreAuthorize("hasAuthority('hr_absence_update')")
    public api_success_response<leave_response> cancel(
            @PathVariable long leave_request_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Leave request cancelled successfully.", absence_service.cancel(leave_request_id, actor, correlation_id(http_request)));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}
