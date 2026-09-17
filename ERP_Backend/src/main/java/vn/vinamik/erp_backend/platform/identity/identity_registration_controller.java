package vn.vinamik.erp_backend.platform.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.vinamik.erp_backend.platform.common.api_success_response;
import vn.vinamik.erp_backend.platform.common.correlation_id_filter;

@RestController
@RequestMapping("/api/v1/identity/registration_requests")
public class identity_registration_controller {
    private final identity_registration_service registration_service;

    public identity_registration_controller(identity_registration_service registration_service) {
        this.registration_service = registration_service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('identity_registration_read')")
    public api_success_response<registration_request_page_response> search_requests(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Registration requests retrieved successfully.",
                registration_service.search_requests(search, status, page, page_size));
    }

    @PostMapping("/{registration_request_id}/approve")
    @PreAuthorize("hasAuthority('identity_registration_approve')")
    public api_success_response<identity_user_response> approve(
            @PathVariable long registration_request_id,
            @Valid @RequestBody registration_approval_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Registration request approved successfully.", registration_service.approve(
                registration_request_id, request, actor, correlation_id(http_request)));
    }

    @PostMapping("/{registration_request_id}/reject")
    @PreAuthorize("hasAuthority('identity_registration_approve')")
    public api_success_response<Void> reject(
            @PathVariable long registration_request_id,
            @Valid @RequestBody(required = false) registration_rejection_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        registration_service.reject(registration_request_id, request, actor, correlation_id(http_request));
        return new api_success_response<>("Registration request rejected successfully.", null);
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}
