package vn.vinamik.erp_backend.production.order_progress;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.vinamik.erp_backend.platform.common.api_success_response;

@RestController
@RequestMapping("/api/v1/production/orders")
public class production_order_progress_controller {
    private final production_order_progress_service progress_service;

    public production_order_progress_controller(production_order_progress_service progress_service) {
        this.progress_service = progress_service;
    }

    @GetMapping("/{production_order_id}/progress")
    @PreAuthorize("hasAnyAuthority('production_order_read', 'production_output_read')")
    public api_success_response<production_order_progress_response> find_by_order_id(
            @PathVariable long production_order_id) {
        return new api_success_response<>(
                "Production order progress retrieved successfully.",
                progress_service.find_by_order_id(production_order_id));
    }
}

