package vn.vinamik.erp_backend.production.output;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.vinamik.erp_backend.platform.common.api_success_response;
import vn.vinamik.erp_backend.platform.common.correlation_id_filter;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.util.List;

@RestController
@RequestMapping("/api/v1/production/orders")
public class production_output_controller {
    private final production_output_service output_service;

    public production_output_controller(production_output_service output_service) {
        this.output_service = output_service;
    }

    @GetMapping("/{production_order_id}/outputs")
    @PreAuthorize("hasAuthority('production_output_read')")
    public api_success_response<List<production_output_response>> list(@PathVariable long production_order_id) {
        return new api_success_response<>("Production outputs retrieved successfully.", output_service.list(production_order_id));
    }

    @PostMapping("/{production_order_id}/outputs")
    @PreAuthorize("hasAuthority('production_output_create')")
    public api_success_response<production_output_response> create(
            @PathVariable long production_order_id,
            @Valid @RequestBody production_output_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Production output created successfully.", output_service.create(
                production_order_id, request, actor, correlation_id(http_request)));
    }

    @PutMapping("/{production_order_id}/outputs/{output_id}")
    @PreAuthorize("hasAuthority('production_output_update')")
    public api_success_response<production_output_response> update(
            @PathVariable long production_order_id,
            @PathVariable long output_id,
            @Valid @RequestBody production_output_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Production output updated successfully.", output_service.update(
                production_order_id, output_id, request, actor, correlation_id(http_request)));
    }

    @DeleteMapping("/{production_order_id}/outputs/{output_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('production_output_delete')")
    public void delete(
            @PathVariable long production_order_id,
            @PathVariable long output_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        output_service.delete(production_order_id, output_id, actor, correlation_id(http_request));
    }

    @PostMapping("/{production_order_id}/outputs/{output_id}/cancel")
    @PreAuthorize("hasAuthority('production_output_update')")
    public api_success_response<production_output_response> cancel(
            @PathVariable long production_order_id,
            @PathVariable long output_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Production output cancelled successfully.", output_service.cancel(
                production_order_id, output_id, actor, correlation_id(http_request)));
    }

    @PostMapping("/{production_order_id}/outputs/{output_id}/fail")
    @PreAuthorize("hasAuthority('production_output_update')")
    public api_success_response<production_output_response> fail(
            @PathVariable long production_order_id,
            @PathVariable long output_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Production output marked as failed successfully.", output_service.fail(
                production_order_id, output_id, actor, correlation_id(http_request)));
    }

    @PostMapping("/{production_order_id}/outputs/{output_id}/post")
    @PreAuthorize("hasAuthority('production_output_post')")
    public api_success_response<production_output_response> post(
            @PathVariable long production_order_id,
            @PathVariable long output_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Production output posted successfully.", output_service.post(
                production_order_id, output_id, actor, correlation_id(http_request)));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}

