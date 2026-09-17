package vn.vinamik.erp_backend.platform.identity;

import java.time.Instant;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.vinamik.erp_backend.platform.common.api_success_response;

@RestController
@RequestMapping("/api/v1/identity/audit_events")
public class identity_audit_controller {
    private final identity_audit_service audit_service;

    public identity_audit_controller(identity_audit_service audit_service) {
        this.audit_service = audit_service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('identity_audit_read')")
    public api_success_response<identity_audit_event_page_response> search(
            @RequestParam(required = false) String module_code,
            @RequestParam(required = false) String action_code,
            @RequestParam(required = false) String entity_type,
            @RequestParam(required = false) String entity_id,
            @RequestParam(required = false) Long actor_user_id,
            @RequestParam(required = false) Instant from_at,
            @RequestParam(required = false) Instant to_at,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Audit events retrieved successfully.",
                audit_service.search(module_code, action_code, entity_type, entity_id, actor_user_id,
                        from_at, to_at, page, page_size));
    }
}