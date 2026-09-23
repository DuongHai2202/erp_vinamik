package vn.vinamik.erp_backend.quality_cost;

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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/quality_cost")
public class quality_cost_controller {
    private final quality_cost_service service;

    public quality_cost_controller(quality_cost_service service) { this.service = service; }

    @GetMapping("/inspections") @PreAuthorize("hasAuthority('quality_inspection_read')")
    public api_success_response<quality_cost_page_response> inspections(@RequestParam(required = false) String search, @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page, @RequestParam(name = "page_size", defaultValue = "50") int page_size) { return page("inspections", search, status, page, page_size, "Quality inspections retrieved successfully."); }
    @GetMapping("/nonconformances") @PreAuthorize("hasAuthority('quality_nonconformance_read')")
    public api_success_response<quality_cost_page_response> nonconformances(@RequestParam(required = false) String search, @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page, @RequestParam(name = "page_size", defaultValue = "50") int page_size) { return page("nonconformances", search, status, page, page_size, "Nonconformances retrieved successfully."); }
    @GetMapping("/rules") @PreAuthorize("hasAuthority('cost_period_read')")
    public api_success_response<quality_cost_page_response> rules(@RequestParam(required = false) String search, @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page, @RequestParam(name = "page_size", defaultValue = "50") int page_size) { return page("rules", search, status, page, page_size, "Cost rules retrieved successfully."); }
    @GetMapping("/periods") @PreAuthorize("hasAuthority('cost_period_read')")
    public api_success_response<quality_cost_page_response> periods(@RequestParam(required = false) String search, @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page, @RequestParam(name = "page_size", defaultValue = "50") int page_size) { return page("periods", search, status, page, page_size, "Cost periods retrieved successfully."); }
    @GetMapping("/calculations") @PreAuthorize("hasAuthority('cost_calculation_read')")
    public api_success_response<quality_cost_page_response> calculations(@RequestParam(required = false) String search, @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page, @RequestParam(name = "page_size", defaultValue = "50") int page_size) { return page("calculations", search, status, page, page_size, "Cost calculations retrieved successfully."); }
    @GetMapping("/price_proposals") @PreAuthorize("hasAuthority('price_proposal_read')")
    public api_success_response<quality_cost_page_response> price_proposals(@RequestParam(required = false) String search, @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page, @RequestParam(name = "page_size", defaultValue = "50") int page_size) { return page("price_proposals", search, status, page, page_size, "Price proposals retrieved successfully."); }
    @GetMapping("/price_approvals") @PreAuthorize("hasAuthority('price_proposal_read')")
    public api_success_response<quality_cost_page_response> price_approvals(@RequestParam(required = false) String search, @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page, @RequestParam(name = "page_size", defaultValue = "50") int page_size) { return page("price_approvals", search, status, page, page_size, "Price approvals retrieved successfully."); }

    @GetMapping("/inspections/{id}") @PreAuthorize("hasAuthority('quality_inspection_read')")
    public api_success_response<Map<String, Object>> inspection(@PathVariable long id) { return found("inspections", id); }
    @GetMapping("/nonconformances/{id}") @PreAuthorize("hasAuthority('quality_nonconformance_read')")
    public api_success_response<Map<String, Object>> nonconformance(@PathVariable long id) { return found("nonconformances", id); }
    @GetMapping("/rules/{id}") @PreAuthorize("hasAuthority('cost_period_read')")
    public api_success_response<Map<String, Object>> rule(@PathVariable long id) { return found("rules", id); }
    @GetMapping("/periods/{id}") @PreAuthorize("hasAuthority('cost_period_read')")
    public api_success_response<Map<String, Object>> period(@PathVariable long id) { return found("periods", id); }
    @GetMapping("/calculations/{id}") @PreAuthorize("hasAuthority('cost_calculation_read')")
    public api_success_response<Map<String, Object>> calculation(@PathVariable long id) { return found("calculations", id); }
    @GetMapping("/price_proposals/{id}") @PreAuthorize("hasAuthority('price_proposal_read')")
    public api_success_response<Map<String, Object>> price_proposal(@PathVariable long id) { return found("price_proposals", id); }
    @GetMapping("/price_approvals/{id}") @PreAuthorize("hasAuthority('price_proposal_read')")
    public api_success_response<Map<String, Object>> price_approval(@PathVariable long id) { return found("price_approvals", id); }

    @PostMapping("/rules") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('cost_period_create')")
    public api_success_response<Map<String, Object>> create_rule(@Valid @RequestBody cost_rule_request request, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) { return created(service.create_rule(request, actor, correlation_id(http_request)), "Cost rule created successfully."); }
    @PutMapping("/rules/{id}") @PreAuthorize("hasAuthority('cost_period_update')")
    public api_success_response<Map<String, Object>> update_rule(@PathVariable long id, @Valid @RequestBody cost_rule_request request, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) { return new api_success_response<>("Cost rule updated successfully.", service.update_rule(id, request, actor, correlation_id(http_request))); }

    @PostMapping("/periods") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('cost_period_create')")
    public api_success_response<Map<String, Object>> create_period(@Valid @RequestBody cost_period_request request, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) { return created(service.create_period(request, actor, correlation_id(http_request)), "Cost period created successfully."); }
    @PutMapping("/periods/{id}") @PreAuthorize("hasAuthority('cost_period_update')")
    public api_success_response<Map<String, Object>> update_period(@PathVariable long id, @Valid @RequestBody cost_period_request request, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) { return new api_success_response<>("Cost period updated successfully.", service.update_period(id, request, actor, correlation_id(http_request))); }

    @PostMapping("/inspections") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('quality_inspection_create')")
    public api_success_response<Map<String, Object>> create_inspection(@Valid @RequestBody quality_inspection_request request, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) { return created(service.create_inspection(request, actor, correlation_id(http_request)), "Quality inspection created successfully."); }
    @PutMapping("/inspections/{id}") @PreAuthorize("hasAuthority('quality_inspection_update')")
    public api_success_response<Map<String, Object>> update_inspection(@PathVariable long id, @Valid @RequestBody quality_inspection_request request, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) { return new api_success_response<>("Quality inspection updated successfully.", service.update_inspection(id, request, actor, correlation_id(http_request))); }

    @PostMapping("/nonconformances") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('quality_nonconformance_create')")
    public api_success_response<Map<String, Object>> create_nonconformance(@Valid @RequestBody nonconformance_request request, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) { return created(service.create_nonconformance(request, actor, correlation_id(http_request)), "Nonconformance created successfully."); }
    @PutMapping("/nonconformances/{id}") @PreAuthorize("hasAuthority('quality_nonconformance_update')")
    public api_success_response<Map<String, Object>> update_nonconformance(@PathVariable long id, @Valid @RequestBody nonconformance_request request, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) { return new api_success_response<>("Nonconformance updated successfully.", service.update_nonconformance(id, request, actor, correlation_id(http_request))); }

    @PostMapping("/calculations") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('cost_calculation_create')")
    public api_success_response<Map<String, Object>> create_calculation(@Valid @RequestBody cost_calculation_request request, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) { return created(service.create_calculation(request, actor, correlation_id(http_request)), "Cost calculation created successfully."); }

    @PutMapping("/calculations/{id}") @PreAuthorize("hasAuthority('cost_calculation_update')")
    public api_success_response<Map<String, Object>> update_calculation(@PathVariable long id, @Valid @RequestBody cost_calculation_request request, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) { return new api_success_response<>("Cost calculation updated successfully.", service.update_calculation(id, request, actor, correlation_id(http_request))); }
    @PostMapping("/price_proposals") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('price_proposal_create')")
    public api_success_response<Map<String, Object>> create_price(@Valid @RequestBody price_proposal_request request, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) { return created(service.create_price_proposal(request, actor, correlation_id(http_request)), "Price proposal created successfully."); }
    @PutMapping("/price_proposals/{id}") @PreAuthorize("hasAuthority('price_proposal_update')")
    public api_success_response<Map<String, Object>> update_price(@PathVariable long id, @Valid @RequestBody price_proposal_request request, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) { return new api_success_response<>("Price proposal updated successfully.", service.update_price_proposal(id, request, actor, correlation_id(http_request))); }

    @PostMapping("/inspections/{id}/status") @PreAuthorize("hasAuthority('quality_inspection_approve')")
    public api_success_response<Map<String, Object>> inspection_status(@PathVariable long id, @Valid @RequestBody quality_cost_status_request request, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) { return changed("inspections", id, request, actor, http_request); }
    @PostMapping("/nonconformances/{id}/status") @PreAuthorize("hasAuthority('quality_nonconformance_approve')")
    public api_success_response<Map<String, Object>> nonconformance_status(@PathVariable long id, @Valid @RequestBody quality_cost_status_request request, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) { return changed("nonconformances", id, request, actor, http_request); }
    @PostMapping("/periods/{id}/status") @PreAuthorize("hasAuthority('cost_period_approve')")
    public api_success_response<Map<String, Object>> period_status(@PathVariable long id, @Valid @RequestBody quality_cost_status_request request, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) { return changed("periods", id, request, actor, http_request); }
    @PostMapping("/calculations/{id}/status") @PreAuthorize("hasAuthority('cost_calculation_approve')")
    public api_success_response<Map<String, Object>> calculation_status(@PathVariable long id, @Valid @RequestBody quality_cost_status_request request, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) { return changed("calculations", id, request, actor, http_request); }
    @PostMapping("/price_proposals/{id}/status") @PreAuthorize("hasAuthority('price_proposal_approve')")
    public api_success_response<Map<String, Object>> price_status(@PathVariable long id, @Valid @RequestBody quality_cost_status_request request, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) { return changed("price_proposals", id, request, actor, http_request); }
    @PostMapping("/price_approvals/{id}/status") @PreAuthorize("hasAuthority('price_proposal_approve')")
    public api_success_response<Map<String, Object>> approval_status(@PathVariable long id, @Valid @RequestBody quality_cost_status_request request, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) { return changed("price_approvals", id, request, actor, http_request); }

    @DeleteMapping("/inspections/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAuthority('quality_inspection_delete')")
    public void delete_inspection(@PathVariable long id, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest request) { service.delete("inspections", id, actor, correlation_id(request)); }
    @DeleteMapping("/nonconformances/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAuthority('quality_nonconformance_delete')")
    public void delete_nonconformance(@PathVariable long id, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest request) { service.delete("nonconformances", id, actor, correlation_id(request)); }
    @DeleteMapping("/periods/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAuthority('cost_period_delete')")
    public void delete_period(@PathVariable long id, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest request) { service.delete("periods", id, actor, correlation_id(request)); }
    @DeleteMapping("/calculations/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAuthority('cost_calculation_delete')")
    public void delete_calculation(@PathVariable long id, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest request) { service.delete("calculations", id, actor, correlation_id(request)); }
    @DeleteMapping("/price_proposals/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAuthority('price_proposal_delete')")
    public void delete_price(@PathVariable long id, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest request) { service.delete("price_proposals", id, actor, correlation_id(request)); }

    private api_success_response<quality_cost_page_response> page(String resource, String search, String status, int page, int page_size, String message) { return new api_success_response<>(message, service.search(resource, search, status, page, page_size)); }
    private api_success_response<Map<String, Object>> found(String resource, long id) { return new api_success_response<>("Quality/cost record retrieved successfully.", service.find(resource, id)); }
    private api_success_response<Map<String, Object>> created(Map<String, Object> data, String message) { return new api_success_response<>(message, data); }
    private api_success_response<Map<String, Object>> changed(String resource, long id, quality_cost_status_request request, authenticated_user actor, HttpServletRequest http_request) { return new api_success_response<>("Quality/cost status changed successfully.", service.change_status(resource, id, request.status(), actor, correlation_id(http_request))); }
    private String correlation_id(HttpServletRequest request) { Object value = request.getAttribute(correlation_id_filter.correlation_attribute); return value instanceof String correlation_id ? correlation_id : "unavailable"; }
}
