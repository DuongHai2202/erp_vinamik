package vn.vinamik.erp_backend.production.material_needs;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.vinamik.erp_backend.platform.common.api_success_response;

@RestController
@RequestMapping("/api/v1/production/orders")
public class production_material_needs_controller {
    private final production_material_needs_service material_needs_service;

    public production_material_needs_controller(production_material_needs_service material_needs_service) {
        this.material_needs_service = material_needs_service;
    }

    @GetMapping("/{production_order_id}/material-needs")
    @PreAuthorize("hasAuthority('production_order_read')")
    public api_success_response<material_needs_response> find_by_order_id(@PathVariable long production_order_id) {
        return new api_success_response<>("Production material needs retrieved successfully.",
                material_needs_service.find_by_order_id(production_order_id));
    }
}
