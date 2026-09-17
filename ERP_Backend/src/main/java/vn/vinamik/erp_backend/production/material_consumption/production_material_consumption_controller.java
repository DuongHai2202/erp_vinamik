package vn.vinamik.erp_backend.production.material_consumption;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.vinamik.erp_backend.platform.common.api_success_response;
import vn.vinamik.erp_backend.platform.common.correlation_id_filter;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.util.List;

@RestController
@RequestMapping("/api/v1/production/orders")
public class production_material_consumption_controller {
    private final production_material_consumption_service consumption_service;

    public production_material_consumption_controller(production_material_consumption_service consumption_service) {
        this.consumption_service = consumption_service;
    }

    @GetMapping("/{production_order_id}/material-consumption")
    @PreAuthorize("hasAuthority('production_order_read')")
    public api_success_response<List<material_consumption_response>> list(@PathVariable long production_order_id) {
        return new api_success_response<>("Material consumption retrieved successfully.", consumption_service.list(production_order_id));
    }

    @PostMapping("/{production_order_id}/material-consumption")
    @PreAuthorize("hasAuthority('production_order_update')")
    public api_success_response<material_consumption_response> create(
            @PathVariable long production_order_id,
            @Valid @RequestBody material_consumption_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Material consumption created successfully.", consumption_service.create(
                production_order_id, request, actor, correlation_id(http_request)));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}
