package vn.vinamik.erp_backend.human_resources.payroll;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.vinamik.erp_backend.platform.common.api_success_response;
import vn.vinamik.erp_backend.platform.common.correlation_id_filter;
import vn.vinamik.erp_backend.platform.common.master_data_page_response;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

@RestController
@RequestMapping("/api/v1/human_resources/payroll")
public class payroll_controller {
    private final payroll_service payroll_service;

    public payroll_controller(payroll_service payroll_service) {
        this.payroll_service = payroll_service;
    }

    @GetMapping("/periods")
    @PreAuthorize("hasAuthority('hr_payroll_read')")
    public api_success_response<master_data_page_response<payroll_period_response>> search_periods(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Payroll periods retrieved successfully.",
                payroll_service.search_periods(status, page, page_size));
    }

    @GetMapping("/periods/{payroll_period_id}")
    @PreAuthorize("hasAuthority('hr_payroll_read')")
    public api_success_response<payroll_period_response> find_period(@PathVariable long payroll_period_id) {
        return new api_success_response<>("Payroll period retrieved successfully.",
                payroll_service.find_period(payroll_period_id));
    }

    @PostMapping("/periods")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('hr_payroll_create')")
    public api_success_response<payroll_period_response> create_period(
            @Valid @RequestBody payroll_period_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Payroll period created successfully.",
                payroll_service.create_period(request, actor, correlation_id(http_request)));
    }

    @DeleteMapping("/periods/{payroll_period_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('hr_payroll_delete')")
    public void delete_period(@PathVariable long payroll_period_id,
                               @AuthenticationPrincipal authenticated_user actor,
                               HttpServletRequest http_request) {
        payroll_service.delete_period(payroll_period_id, actor, correlation_id(http_request));
    }

    @PostMapping("/periods/{payroll_period_id}/calculate")
    @PreAuthorize("hasAuthority('hr_payroll_update')")
    public api_success_response<payroll_period_response> calculate(
            @PathVariable long payroll_period_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Payroll calculated successfully.",
                payroll_service.calculate(payroll_period_id, actor, correlation_id(http_request)));
    }

    @PostMapping("/periods/{payroll_period_id}/approve")
    @PreAuthorize("hasAuthority('hr_payroll_approve')")
    public api_success_response<payroll_period_response> approve(
            @PathVariable long payroll_period_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Payroll period approved successfully.",
                payroll_service.approve(payroll_period_id, actor, correlation_id(http_request)));
    }

    @PostMapping("/periods/{payroll_period_id}/reject")
    @PreAuthorize("hasAuthority('hr_payroll_approve')")
    public api_success_response<payroll_period_response> reject(
            @PathVariable long payroll_period_id,
            @Valid @RequestBody payroll_decision_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Payroll period rejected successfully.",
                payroll_service.reject(payroll_period_id, request, actor, correlation_id(http_request)));
    }

    @PostMapping("/periods/{payroll_period_id}/lock")
    @PreAuthorize("hasAuthority('hr_payroll_approve')")
    public api_success_response<payroll_period_response> lock(
            @PathVariable long payroll_period_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Payroll period locked successfully.",
                payroll_service.lock(payroll_period_id, actor, correlation_id(http_request)));
    }

    @GetMapping("/periods/{payroll_period_id}/records")
    @PreAuthorize("hasAuthority('hr_payroll_read')")
    public api_success_response<master_data_page_response<payroll_record_response>> search_records(
            @PathVariable long payroll_period_id,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Payroll records retrieved successfully.",
                payroll_service.search_records(payroll_period_id, search, page, page_size));
    }

    @GetMapping("/records/{payroll_record_id}")
    @PreAuthorize("hasAuthority('hr_payroll_read')")
    public api_success_response<payroll_record_detail_response> find_record(
            @PathVariable long payroll_record_id) {
        return new api_success_response<>("Payroll record retrieved successfully.",
                payroll_service.find_record(payroll_record_id));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}
