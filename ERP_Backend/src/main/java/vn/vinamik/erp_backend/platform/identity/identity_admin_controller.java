package vn.vinamik.erp_backend.platform.identity;

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

import java.util.List;

@RestController
@RequestMapping("/api/v1/identity")
public class identity_admin_controller {
    private final identity_admin_service admin_service;

    public identity_admin_controller(identity_admin_service admin_service) {
        this.admin_service = admin_service;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('identity_user_read')")
    public api_success_response<identity_user_page_response> search_users(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Users retrieved successfully.",
                admin_service.search_users(search, status, page, page_size));
    }

    @GetMapping("/users/{user_id}")
    @PreAuthorize("hasAuthority('identity_user_read')")
    public api_success_response<identity_user_response> find_user(@PathVariable long user_id) {
        return new api_success_response<>("User retrieved successfully.", admin_service.find_user(user_id));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('identity_role_read')")
    public api_success_response<List<identity_role_response>> list_roles() {
        return new api_success_response<>("Roles retrieved successfully.", admin_service.list_roles());
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('identity_user_create')")
    public api_success_response<identity_user_response> create_user(
            @Valid @RequestBody identity_user_create_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("User created successfully.",
                admin_service.create_user(request, actor, correlation_id(http_request)));
    }

    @PutMapping("/users/{user_id}")
    @PreAuthorize("hasAuthority('identity_user_update')")
    public api_success_response<identity_user_response> update_user(
            @PathVariable long user_id,
            @Valid @RequestBody identity_user_update_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("User updated successfully.",
                admin_service.update_user(user_id, request, actor, correlation_id(http_request)));
    }

    @PostMapping("/users/{user_id}/reset-password")
    @PreAuthorize("hasAuthority('identity_user_update')")
    public api_success_response<identity_user_response> reset_password(
            @PathVariable long user_id,
            @Valid @RequestBody identity_user_password_reset_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Password reset successfully.",
                admin_service.reset_password(user_id, request, actor, correlation_id(http_request)));
    }

    @PutMapping("/users/{user_id}/roles")
    @PreAuthorize("hasAuthority('identity_role_update')")
    public api_success_response<identity_user_response> update_roles(
            @PathVariable long user_id,
            @Valid @RequestBody identity_user_roles_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("User roles updated successfully.",
                admin_service.update_roles(user_id, request, actor, correlation_id(http_request)));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}
