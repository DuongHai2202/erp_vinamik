package vn.vinamik.erp_backend.human_resources.employee;
import vn.vinamik.erp_backend.platform.common.pagination_guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.human_resources.employee.entity.employee_entity;
import vn.vinamik.erp_backend.human_resources.employee.repository.employee_read_row;
import vn.vinamik.erp_backend.human_resources.employee.repository.human_resources_employee_read_repository;
import vn.vinamik.erp_backend.human_resources.employee.repository.human_resources_employee_repository;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class human_resources_employee_service {
    private static final Logger logger = LoggerFactory.getLogger(human_resources_employee_service.class);
    private static final int max_page_size = 100;
    private static final List<String> valid_statuses = List.of("active", "inactive", "on_leave", "terminated");

    private final human_resources_employee_repository employee_repository;
    private final human_resources_employee_read_repository read_repository;
    private final audit_event_writer audit_writer;

    @Autowired
    public human_resources_employee_service(
            human_resources_employee_repository employee_repository,
            human_resources_employee_read_repository read_repository,
            audit_event_writer audit_writer) {
        this.employee_repository = employee_repository;
        this.read_repository = read_repository;
        this.audit_writer = audit_writer;
    }


    public human_resources_employee_service(human_resources_employee_repository employee_repository, audit_event_writer audit_writer) {
        this(employee_repository, null, audit_writer);
    }

    @Transactional(readOnly = true)
    public employee_page_response search(String search, String status, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = Math.min(Math.max(page_size, 1), max_page_size);
        String normalized_search = normalize_lower(search);
        String normalized_status = normalize_lower(status);
        validate_status(normalized_status);
        long total = read_repository.count(normalized_search, normalized_status);
        List<employee_response> items = read_repository.search(normalized_search, normalized_status,
                        safe_page, safe_page_size)
                .stream()
                .map(this::to_response)
                .toList();
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / safe_page_size);
        return new employee_page_response(items, safe_page, safe_page_size, total, total_pages);
    }

    @Transactional(readOnly = true)
    public employee_response find_by_id(long employee_id) {
        return to_response(read_repository.find_by_id(employee_id));
    }

    @Transactional
    public employee_response create(employee_request request, authenticated_user actor, String correlation_id) {
        validate_request(request);
        String employee_code = normalize_required(request.employee_code());
        ensure_unique_code(employee_code, null);
        employee_entity entity = new employee_entity(
                employee_code,
                request.full_name().trim(),
                request.date_of_birth(),
                normalize_phone_number(request.phone_number()),
                normalize_optional(request.email()),
                request.department_id(),
                request.job_title_id(),
                request.manager_employee_id(),
                normalized_status_or_default(request.employment_status()),
                request.hired_on(),
                request.terminated_on(),
                normalize_optional(request.notes()),
                actor.user_id());
        try {
            employee_repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("employee_code", "Employee code already exists.");
        }
        employee_response created = find_by_id(entity.employee_id());
        audit_writer.write(actor.user_id(), "hr", "employee_create", "employee", String.valueOf(entity.employee_id()),
                correlation_id, Map.of("employee_code", created.employee_code()));
        logger.info("Đã tạo hồ sơ nhân viên; employee_id={}, actor_user_id={}, correlation_id={}",
                entity.employee_id(), actor.user_id(), correlation_id);
        return created;
    }

    @Transactional
    public employee_response update(long employee_id, employee_request request, authenticated_user actor,
                                    String correlation_id) {
        validate_request(request);
        String employee_code = normalize_required(request.employee_code());
        ensure_unique_code(employee_code, employee_id);
        employee_entity entity = employee_repository.findById(employee_id)
                .orElseThrow(() -> new resource_not_found_exception("Employee"));
        entity.update_values(
                employee_code,
                request.full_name().trim(),
                request.date_of_birth(),
                normalize_phone_number(request.phone_number()),
                normalize_optional(request.email()),
                request.department_id(),
                request.job_title_id(),
                request.manager_employee_id(),
                normalized_status_or_default(request.employment_status()),
                request.hired_on(),
                request.terminated_on(),
                normalize_optional(request.notes()),
                actor.user_id());
        try {
            employee_repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("employee_code", "Employee code already exists.");
        }
        employee_response updated_employee = find_by_id(employee_id);
        audit_writer.write(actor.user_id(), "hr", "employee_update", "employee", String.valueOf(employee_id),
                correlation_id, Map.of("employee_code", updated_employee.employee_code()));
        logger.info("Đã cập nhật hồ sơ nhân viên; employee_id={}, actor_user_id={}, correlation_id={}",
                employee_id, actor.user_id(), correlation_id);
        return updated_employee;
    }

    @Transactional
    public employee_response deactivate(long employee_id, authenticated_user actor, String correlation_id) {
        employee_entity entity = employee_repository.findById(employee_id)
                .orElseThrow(() -> new resource_not_found_exception("Employee"));
        entity.deactivate(actor.user_id());
        employee_repository.saveAndFlush(entity);
        employee_response deactivated = find_by_id(employee_id);
        audit_writer.write(actor.user_id(), "hr", "employee_deactivate", "employee", String.valueOf(employee_id),
                correlation_id, Map.of("employee_code", deactivated.employee_code()));
        logger.info("Đã vô hiệu hóa hồ sơ nhân viên; employee_id={}, actor_user_id={}, correlation_id={}",
                employee_id, actor.user_id(), correlation_id);
        return deactivated;
    }

    private void ensure_unique_code(String employee_code, Long employee_id) {
        if (employee_repository == null) {
            throw new IllegalStateException("Employee repository is not configured.");
        }
        boolean exists = employee_id == null
                ? employee_repository.exists_by_employee_code(employee_code)
                : employee_repository.exists_by_employee_code_excluding_id(employee_code, employee_id);
        if (exists) {
            throw new field_conflict_exception("employee_code", "Employee code already exists.");
        }
    }

    private void validate_request(employee_request request) {
        if (request == null) {
            throw new IllegalArgumentException("Employee request is required.");
        }
        if (normalize_optional(request.employee_code()) == null
                || normalize_optional(request.employee_code()).length() > 40
                || normalize_optional(request.full_name()) == null
                || normalize_optional(request.full_name()).length() > 160) {
            throw new IllegalArgumentException("Employee code and full name are required.");
        }
        normalize_phone_number(request.phone_number());
        if (request.manager_employee_id() != null && request.manager_employee_id() <= 0) {
            throw new IllegalArgumentException("Manager employee is invalid.");
        }
        if (request.department_id() != null && request.department_id() <= 0) {
            throw new IllegalArgumentException("Department is invalid.");
        }
        if (request.department_id() != null
                && (read_repository == null || !read_repository.active_department(request.department_id()))) {
            throw new resource_not_found_exception("Active department");
        }
        if (request.job_title_id() != null && request.job_title_id() <= 0) {
            throw new IllegalArgumentException("Job title is invalid.");
        }
        if (request.job_title_id() != null
                && (read_repository == null || !read_repository.active_job_title(request.job_title_id()))) {
            throw new resource_not_found_exception("Active job title");
        }
        if (request.date_of_birth() != null && request.date_of_birth().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth cannot be in the future.");
        }
        LocalDate adult_cutoff = request.hired_on() == null
                ? LocalDate.now().minusYears(18)
                : request.hired_on().minusYears(18);
        if (request.date_of_birth() != null && request.date_of_birth().isAfter(adult_cutoff)) {
            throw new IllegalArgumentException("Employee must be at least 18 years old on the hiring date.");
        }
        if (request.terminated_on() != null && request.hired_on() != null
                && request.terminated_on().isBefore(request.hired_on())) {
            throw new IllegalArgumentException("Termination date cannot be before hiring date.");
        }
        if (normalize_optional(request.notes()) != null && normalize_optional(request.notes()).length() > 2000) {
            throw new IllegalArgumentException("Employee notes must contain at most 2000 characters.");
        }
        validate_status(normalized_status_or_default(request.employment_status()));
    }

    private String normalized_status_or_default(String status) {
        String normalized_status = normalize_optional(status);
        return normalized_status == null ? "active" : normalized_status.toLowerCase(Locale.ROOT);
    }

    private void validate_status(String status) {
        if (status != null && !valid_statuses.contains(status)) {
            throw new IllegalArgumentException("Employment status is invalid.");
        }
    }

    private String normalize_required(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalize_lower(String value) {
        String normalized = normalize_optional(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalize_optional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalize_phone_number(String value) {
        String normalized = normalize_optional(value);
        if (normalized == null) {
            return null;
        }
        String digits = normalized.replaceAll("[\\s().-]+", "");
        if (digits.startsWith("+84")) {
            digits = "0" + digits.substring(3);
        } else if (digits.startsWith("84") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        }
        if (!digits.matches("0\\d{9,10}")) {
            throw new IllegalArgumentException("Phone number format is invalid.");
        }
        return digits;
    }

    private employee_response to_response(employee_read_row row) {
        return new employee_response(
                row.employee_id(),
                row.employee_code(),
                row.full_name(),
                row.date_of_birth(),
                row.phone_number(),
                row.email(),
                row.department_id(),
                row.department_name(),
                row.job_title_id(),
                row.job_title_name(),
                row.manager_employee_id(),
                row.employment_status(),
                row.hired_on(),
                row.terminated_on(),
                row.notes());
    }
}



