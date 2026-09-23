package vn.vinamik.erp_backend.human_resources.reward_discipline;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/human_resources/rewards_discipline")
public class human_resources_reward_discipline_controller {
    private final human_resources_reward_discipline_service reward_discipline_service;

    public human_resources_reward_discipline_controller(human_resources_reward_discipline_service reward_discipline_service) {
        this.reward_discipline_service = reward_discipline_service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('hr_reward_read')")
    public api_success_response<reward_discipline_page_response> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long employee_id,
            @RequestParam(required = false) String event_type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate from_date,
            @RequestParam(required = false) LocalDate to_date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Reward and discipline records retrieved successfully.", reward_discipline_service.search(search, employee_id, event_type, status, from_date, to_date, page, page_size));
    }

    @GetMapping("/payroll_inputs")
    @PreAuthorize("hasAuthority('hr_reward_read')")
    public api_success_response<reward_discipline_page_response> search_payroll_inputs(
            @RequestParam(required = false) Long employee_id,
            @RequestParam(required = false) LocalDate from_date,
            @RequestParam(required = false) LocalDate to_date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Approved payroll inputs retrieved successfully.", reward_discipline_service.search_payroll_inputs(employee_id, from_date, to_date, page, page_size));
    }

    @GetMapping("/{record_id}")
    @PreAuthorize("hasAuthority('hr_reward_read')")
    public api_success_response<reward_discipline_response> find_by_id(@PathVariable long record_id) {
        return new api_success_response<>("Reward or discipline record retrieved successfully.", reward_discipline_service.find_by_id(record_id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('hr_reward_create')")
    public api_success_response<reward_discipline_response> create(
            @Valid @RequestBody reward_discipline_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Reward or discipline record created successfully.", reward_discipline_service.create(request, actor, correlation_id(http_request)));
    }

    @PutMapping("/{record_id}")
    @PreAuthorize("hasAuthority('hr_reward_update')")
    public api_success_response<reward_discipline_response> update(
            @PathVariable long record_id,
            @Valid @RequestBody reward_discipline_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Reward or discipline record updated successfully.", reward_discipline_service.update(record_id, request, actor, correlation_id(http_request)));
    }

    @DeleteMapping("/{record_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('hr_reward_delete')")
    public void delete(@PathVariable long record_id, @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) {
        reward_discipline_service.delete(record_id, actor, correlation_id(http_request));
    }

    @PostMapping("/{record_id}/cancel")
    @PreAuthorize("hasAuthority('hr_reward_update')")
    public api_success_response<reward_discipline_response> cancel(@PathVariable long record_id,
                                                                    @AuthenticationPrincipal authenticated_user actor, HttpServletRequest http_request) {
        return new api_success_response<>("Reward or discipline record cancelled successfully.",
                reward_discipline_service.cancel(record_id, actor, correlation_id(http_request)));
    }
    @PostMapping("/{record_id}/submit")
    @PreAuthorize("hasAuthority('hr_reward_update')")
    public api_success_response<reward_discipline_response> submit(
            @PathVariable long record_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Reward or discipline record submitted successfully.", reward_discipline_service.submit(record_id, actor, correlation_id(http_request)));
    }

    @PostMapping("/{record_id}/decision")
    @PreAuthorize("hasAuthority('hr_reward_approve')")
    public api_success_response<reward_discipline_response> decide(
            @PathVariable long record_id,
            @Valid @RequestBody reward_discipline_decision_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Reward or discipline decision saved successfully.", reward_discipline_service.decide(record_id, request, actor, correlation_id(http_request)));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}
