package vn.vinamik.erp_backend.human_resources.master_data;

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
import vn.vinamik.erp_backend.platform.common.master_data_page_response;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.util.List;

@RestController
@RequestMapping("/api/v1/human_resources/master_data")
public class human_resources_master_data_controller {
    private final human_resources_master_data_service master_data_service;

    public human_resources_master_data_controller(human_resources_master_data_service master_data_service) {
        this.master_data_service = master_data_service;
    }

    @GetMapping("/departments")
    @PreAuthorize("hasAuthority('hr_master_read')")
    public api_success_response<master_data_page_response<department_response>> search_departments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Departments retrieved successfully.",
                master_data_service.search_departments(search, status, page, page_size));
    }

    @GetMapping("/departments/{department_id}")
    @PreAuthorize("hasAuthority('hr_master_read')")
    public api_success_response<department_response> find_department(@PathVariable long department_id) {
        return new api_success_response<>("Department retrieved successfully.",
                master_data_service.find_department(department_id));
    }

    @PostMapping("/departments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('hr_master_create')")
    public api_success_response<department_response> create_department(
            @Valid @RequestBody department_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Department created successfully.",
                master_data_service.create_department(request, actor, correlation_id(http_request)));
    }

    @PutMapping("/departments/{department_id}")
    @PreAuthorize("hasAuthority('hr_master_update')")
    public api_success_response<department_response> update_department(
            @PathVariable long department_id,
            @Valid @RequestBody department_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Department updated successfully.",
                master_data_service.update_department(department_id, request, actor, correlation_id(http_request)));
    }

    @DeleteMapping("/departments/{department_id}")
    @PreAuthorize("hasAuthority('hr_master_deactivate')")
    public api_success_response<department_response> deactivate_department(
            @PathVariable long department_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Department deactivated successfully.",
                master_data_service.deactivate_department(department_id, actor, correlation_id(http_request)));
    }

    @GetMapping("/job_titles")
    @PreAuthorize("hasAuthority('hr_master_read')")
    public api_success_response<master_data_page_response<job_title_response>> search_job_titles(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Job titles retrieved successfully.",
                master_data_service.search_job_titles(search, status, page, page_size));
    }

    @GetMapping("/job_titles/{job_title_id}")
    @PreAuthorize("hasAuthority('hr_master_read')")
    public api_success_response<job_title_response> find_job_title(@PathVariable long job_title_id) {
        return new api_success_response<>("Job title retrieved successfully.",
                master_data_service.find_job_title(job_title_id));
    }

    @PostMapping("/job_titles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('hr_master_create')")
    public api_success_response<job_title_response> create_job_title(
            @Valid @RequestBody job_title_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Job title created successfully.",
                master_data_service.create_job_title(request, actor, correlation_id(http_request)));
    }

    @PutMapping("/job_titles/{job_title_id}")
    @PreAuthorize("hasAuthority('hr_master_update')")
    public api_success_response<job_title_response> update_job_title(
            @PathVariable long job_title_id,
            @Valid @RequestBody job_title_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Job title updated successfully.",
                master_data_service.update_job_title(job_title_id, request, actor, correlation_id(http_request)));
    }

    @DeleteMapping("/job_titles/{job_title_id}")
    @PreAuthorize("hasAuthority('hr_master_deactivate')")
    public api_success_response<job_title_response> deactivate_job_title(
            @PathVariable long job_title_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Job title deactivated successfully.",
                master_data_service.deactivate_job_title(job_title_id, actor, correlation_id(http_request)));
    }

    @GetMapping("/work_shifts")
    @PreAuthorize("hasAuthority('hr_master_read')")
    public api_success_response<master_data_page_response<work_shift_response>> search_work_shifts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int page_size) {
        return new api_success_response<>("Work shifts retrieved successfully.",
                master_data_service.search_work_shifts(search, status, page, page_size));
    }

    @GetMapping("/work_shifts/active")
    @PreAuthorize("hasAnyAuthority('hr_master_read', 'production_assignment_read', 'production_assignment_create', 'production_assignment_update')")
    public api_success_response<List<work_shift_response>> find_active_work_shifts() {
        return new api_success_response<>("Active work shifts retrieved successfully.",
                master_data_service.find_active_work_shifts());
    }

    @GetMapping("/work_shifts/{work_shift_id}")
    @PreAuthorize("hasAuthority('hr_master_read')")
    public api_success_response<work_shift_response> find_work_shift(@PathVariable long work_shift_id) {
        return new api_success_response<>("Work shift retrieved successfully.",
                master_data_service.find_work_shift(work_shift_id));
    }

    @PostMapping("/work_shifts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('hr_master_create')")
    public api_success_response<work_shift_response> create_work_shift(
            @Valid @RequestBody work_shift_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Work shift created successfully.",
                master_data_service.create_work_shift(request, actor, correlation_id(http_request)));
    }

    @PutMapping("/work_shifts/{work_shift_id}")
    @PreAuthorize("hasAuthority('hr_master_update')")
    public api_success_response<work_shift_response> update_work_shift(
            @PathVariable long work_shift_id,
            @Valid @RequestBody work_shift_request request,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Work shift updated successfully.",
                master_data_service.update_work_shift(work_shift_id, request, actor, correlation_id(http_request)));
    }

    @DeleteMapping("/work_shifts/{work_shift_id}")
    @PreAuthorize("hasAuthority('hr_master_deactivate')")
    public api_success_response<work_shift_response> deactivate_work_shift(
            @PathVariable long work_shift_id,
            @AuthenticationPrincipal authenticated_user actor,
            HttpServletRequest http_request) {
        return new api_success_response<>("Work shift deactivated successfully.",
                master_data_service.deactivate_work_shift(work_shift_id, actor, correlation_id(http_request)));
    }

    private String correlation_id(HttpServletRequest request) {
        Object value = request.getAttribute(correlation_id_filter.correlation_attribute);
        return value instanceof String correlation_id ? correlation_id : "unavailable";
    }
}
