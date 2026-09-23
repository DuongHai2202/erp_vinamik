package vn.vinamik.erp_backend.human_resources.master_data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;
import vn.vinamik.erp_backend.platform.common.master_data_page_response;
import vn.vinamik.erp_backend.platform.common.pagination_guard;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class human_resources_master_data_service {
    private static final Logger logger = LoggerFactory.getLogger(human_resources_master_data_service.class);
    private static final int max_page_size = 100;
    private static final List<String> valid_statuses = List.of("active", "inactive");

    private final human_resources_master_data_repository master_data_repository;
    private final audit_event_writer audit_writer;

    public human_resources_master_data_service(
            human_resources_master_data_repository master_data_repository,
            audit_event_writer audit_writer) {
        this.master_data_repository = master_data_repository;
        this.audit_writer = audit_writer;
    }

    @Transactional(readOnly = true)
    public master_data_page_response<department_response> search_departments(
            String search, String status, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = safe_page_size(page_size);
        String normalized_search = normalize_lower(search);
        String normalized_status = normalize_lower(status);
        validate_status(normalized_status);
        long total = master_data_repository.count_departments(normalized_search, normalized_status);
        return page_response(master_data_repository.search_departments(
                normalized_search, normalized_status, safe_page, safe_page_size),
                safe_page, safe_page_size, total);
    }

    @Transactional(readOnly = true)
    public department_response find_department(long department_id) {
        return require_department(department_id);
    }

    @Transactional
    public department_response create_department(department_request request, authenticated_user actor,
                                                 String correlation_id) {
        validate_department(request, null);
        String code = normalize_required(request.department_code());
        String name = normalize_text(request.department_name());
        ensure_department_code_available(code, null);
        ensure_parent_department(request.parent_department_id(), null);
        try {
            Long id = master_data_repository.insert_department(code, name, request.parent_department_id(),
                    status_or_default(request.status()), actor.user_id());
            department_response created = require_department(id);
            audit_writer.write(actor.user_id(), "hr", "department_create", "department",
                    String.valueOf(id), correlation_id, Map.of("department_code", code));
            logger.info("Đã tạo phòng ban; department_id={}, actor_user_id={}, correlation_id={}",
                    id, actor.user_id(), correlation_id);
            return created;
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("department_code", "Department code already exists.");
        }
    }

    @Transactional
    public department_response update_department(long department_id, department_request request,
                                                 authenticated_user actor, String correlation_id) {
        validate_department(request, department_id);
        String code = normalize_required(request.department_code());
        ensure_department_code_available(code, department_id);
        ensure_parent_department(request.parent_department_id(), department_id);
        require_department(department_id);
        try {
            int updated = master_data_repository.update_department(department_id, code,
                    normalize_text(request.department_name()), request.parent_department_id(),
                    status_or_default(request.status()), actor.user_id());
            if (updated == 0) {
                throw new resource_not_found_exception("Department");
            }
            department_response result = require_department(department_id);
            audit_writer.write(actor.user_id(), "hr", "department_update", "department",
                    String.valueOf(department_id), correlation_id, Map.of("department_code", code));
            logger.info("Đã cập nhật phòng ban; department_id={}, actor_user_id={}, correlation_id={}",
                    department_id, actor.user_id(), correlation_id);
            return result;
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("department_code", "Department code already exists.");
        }
    }

    @Transactional
    public department_response deactivate_department(long department_id, authenticated_user actor,
                                                     String correlation_id) {
        department_response current = require_department(department_id);
        int updated = master_data_repository.deactivate_department(department_id, actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Department");
        }
        department_response result = require_department(department_id);
        audit_writer.write(actor.user_id(), "hr", "department_deactivate", "department",
                String.valueOf(department_id), correlation_id, Map.of("department_code", current.department_code()));
        logger.info("Đã vô hiệu hóa phòng ban; department_id={}, actor_user_id={}, correlation_id={}",
                department_id, actor.user_id(), correlation_id);
        return result;
    }

    @Transactional(readOnly = true)
    public master_data_page_response<job_title_response> search_job_titles(
            String search, String status, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = safe_page_size(page_size);
        String normalized_search = normalize_lower(search);
        String normalized_status = normalize_lower(status);
        validate_status(normalized_status);
        long total = master_data_repository.count_job_titles(normalized_search, normalized_status);
        return page_response(master_data_repository.search_job_titles(
                normalized_search, normalized_status, safe_page, safe_page_size),
                safe_page, safe_page_size, total);
    }

    @Transactional(readOnly = true)
    public job_title_response find_job_title(long job_title_id) {
        return require_job_title(job_title_id);
    }

    @Transactional
    public job_title_response create_job_title(job_title_request request, authenticated_user actor,
                                               String correlation_id) {
        validate_job_title(request);
        String code = normalize_required(request.job_title_code());
        ensure_job_title_code_available(code, null);
        try {
            Long id = master_data_repository.insert_job_title(code, normalize_text(request.job_title_name()),
                    normalize_optional(request.description()), status_or_default(request.status()), actor.user_id());
            job_title_response created = require_job_title(id);
            audit_writer.write(actor.user_id(), "hr", "job_title_create", "job_title",
                    String.valueOf(id), correlation_id, Map.of("job_title_code", code));
            logger.info("Đã tạo chức danh; job_title_id={}, actor_user_id={}, correlation_id={}",
                    id, actor.user_id(), correlation_id);
            return created;
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("job_title_code", "Job title code already exists.");
        }
    }

    @Transactional
    public job_title_response update_job_title(long job_title_id, job_title_request request,
                                               authenticated_user actor, String correlation_id) {
        validate_job_title(request);
        String code = normalize_required(request.job_title_code());
        ensure_job_title_code_available(code, job_title_id);
        require_job_title(job_title_id);
        try {
            int updated = master_data_repository.update_job_title(job_title_id, code,
                    normalize_text(request.job_title_name()), normalize_optional(request.description()),
                    status_or_default(request.status()), actor.user_id());
            if (updated == 0) {
                throw new resource_not_found_exception("Job title");
            }
            job_title_response result = require_job_title(job_title_id);
            audit_writer.write(actor.user_id(), "hr", "job_title_update", "job_title",
                    String.valueOf(job_title_id), correlation_id, Map.of("job_title_code", code));
            logger.info("Đã cập nhật chức danh; job_title_id={}, actor_user_id={}, correlation_id={}",
                    job_title_id, actor.user_id(), correlation_id);
            return result;
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("job_title_code", "Job title code already exists.");
        }
    }

    @Transactional
    public job_title_response deactivate_job_title(long job_title_id, authenticated_user actor,
                                                   String correlation_id) {
        job_title_response current = require_job_title(job_title_id);
        int updated = master_data_repository.deactivate_job_title(job_title_id, actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Job title");
        }
        job_title_response result = require_job_title(job_title_id);
        audit_writer.write(actor.user_id(), "hr", "job_title_deactivate", "job_title",
                String.valueOf(job_title_id), correlation_id, Map.of("job_title_code", current.job_title_code()));
        logger.info("Đã vô hiệu hóa chức danh; job_title_id={}, actor_user_id={}, correlation_id={}",
                job_title_id, actor.user_id(), correlation_id);
        return result;
    }

    @Transactional(readOnly = true)
    public master_data_page_response<work_shift_response> search_work_shifts(
            String search, String status, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = safe_page_size(page_size);
        String normalized_search = normalize_lower(search);
        String normalized_status = normalize_lower(status);
        validate_status(normalized_status);
        long total = master_data_repository.count_work_shifts(normalized_search, normalized_status);
        return page_response(master_data_repository.search_work_shifts(
                normalized_search, normalized_status, safe_page, safe_page_size),
                safe_page, safe_page_size, total);
    }

    @Transactional(readOnly = true)
    public List<work_shift_response> find_active_work_shifts() {
        return master_data_repository.find_active_work_shifts();
    }

    @Transactional(readOnly = true)
    public work_shift_response find_work_shift(long work_shift_id) {
        return require_work_shift(work_shift_id);
    }

    @Transactional
    public work_shift_response create_work_shift(work_shift_request request, authenticated_user actor,
                                                 String correlation_id) {
        validate_work_shift(request);
        String code = normalize_required(request.shift_code());
        ensure_work_shift_code_available(code, null);
        try {
            Long id = master_data_repository.insert_work_shift(code, normalize_text(request.shift_name()),
                    request.starts_at(), request.ends_at(), status_or_default(request.status()), actor.user_id());
            work_shift_response created = require_work_shift(id);
            audit_writer.write(actor.user_id(), "hr", "work_shift_create", "work_shift",
                    String.valueOf(id), correlation_id, Map.of("shift_code", code));
            logger.info("Đã tạo ca làm việc; work_shift_id={}, actor_user_id={}, correlation_id={}",
                    id, actor.user_id(), correlation_id);
            return created;
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("shift_code", "Work shift code already exists.");
        }
    }

    @Transactional
    public work_shift_response update_work_shift(long work_shift_id, work_shift_request request,
                                                 authenticated_user actor, String correlation_id) {
        validate_work_shift(request);
        String code = normalize_required(request.shift_code());
        ensure_work_shift_code_available(code, work_shift_id);
        require_work_shift(work_shift_id);
        try {
            int updated = master_data_repository.update_work_shift(work_shift_id, code,
                    normalize_text(request.shift_name()), request.starts_at(), request.ends_at(),
                    status_or_default(request.status()), actor.user_id());
            if (updated == 0) {
                throw new resource_not_found_exception("Work shift");
            }
            work_shift_response result = require_work_shift(work_shift_id);
            audit_writer.write(actor.user_id(), "hr", "work_shift_update", "work_shift",
                    String.valueOf(work_shift_id), correlation_id, Map.of("shift_code", code));
            logger.info("Đã cập nhật ca làm việc; work_shift_id={}, actor_user_id={}, correlation_id={}",
                    work_shift_id, actor.user_id(), correlation_id);
            return result;
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("shift_code", "Work shift code already exists.");
        }
    }

    @Transactional
    public work_shift_response deactivate_work_shift(long work_shift_id, authenticated_user actor,
                                                     String correlation_id) {
        work_shift_response current = require_work_shift(work_shift_id);
        int updated = master_data_repository.deactivate_work_shift(work_shift_id, actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Work shift");
        }
        work_shift_response result = require_work_shift(work_shift_id);
        audit_writer.write(actor.user_id(), "hr", "work_shift_deactivate", "work_shift",
                String.valueOf(work_shift_id), correlation_id, Map.of("shift_code", current.shift_code()));
        logger.info("Đã vô hiệu hóa ca làm việc; work_shift_id={}, actor_user_id={}, correlation_id={}",
                work_shift_id, actor.user_id(), correlation_id);
        return result;
    }

    private void validate_department(department_request request, Long department_id) {
        if (request == null) {
            throw new IllegalArgumentException("Department request is required.");
        }
        require_text(request.department_code(), "Department code", 40);
        require_text(request.department_name(), "Department name", 160);
        validate_status(status_or_default(request.status()));
        if (request.parent_department_id() != null && request.parent_department_id() <= 0) {
            throw new IllegalArgumentException("Parent department is invalid.");
        }
        if (department_id != null && department_id.equals(request.parent_department_id())) {
            throw new IllegalArgumentException("Department cannot be its own parent.");
        }
    }

    private void validate_job_title(job_title_request request) {
        if (request == null) {
            throw new IllegalArgumentException("Job title request is required.");
        }
        require_text(request.job_title_code(), "Job title code", 40);
        require_text(request.job_title_name(), "Job title name", 160);
        if (normalize_optional(request.description()) != null && normalize_optional(request.description()).length() > 2000) {
            throw new IllegalArgumentException("Job title description must contain at most 2000 characters.");
        }
        validate_status(status_or_default(request.status()));
    }

    private void validate_work_shift(work_shift_request request) {
        if (request == null) {
            throw new IllegalArgumentException("Work shift request is required.");
        }
        require_text(request.shift_code(), "Work shift code", 40);
        require_text(request.shift_name(), "Work shift name", 120);
        if (request.starts_at() == null || request.ends_at() == null) {
            throw new IllegalArgumentException("Work shift start and end time are required.");
        }
        if (request.starts_at().equals(request.ends_at())) {
            throw new IllegalArgumentException("Work shift start and end time cannot be equal.");
        }
        validate_status(status_or_default(request.status()));
    }

    private void ensure_parent_department(Long parent_department_id, Long department_id) {
        if (parent_department_id == null) {
            return;
        }
        if (!master_data_repository.active_department(parent_department_id)) {
            throw new resource_not_found_exception("Active parent department");
        }
        if (department_id != null && master_data_repository.department_is_descendant(
                department_id, parent_department_id)) {
            throw new IllegalArgumentException("Department hierarchy cannot contain a cycle.");
        }
    }

    private void ensure_department_code_available(String code, Long id) {
        if (master_data_repository.department_code_exists(code, id)) {
            throw new field_conflict_exception("department_code", "Department code already exists.");
        }
    }

    private void ensure_job_title_code_available(String code, Long id) {
        if (master_data_repository.job_title_code_exists(code, id)) {
            throw new field_conflict_exception("job_title_code", "Job title code already exists.");
        }
    }

    private void ensure_work_shift_code_available(String code, Long id) {
        if (master_data_repository.work_shift_code_exists(code, id)) {
            throw new field_conflict_exception("shift_code", "Work shift code already exists.");
        }
    }

    private department_response require_department(Long id) {
        if (id == null) {
            throw new resource_not_found_exception("Department");
        }
        department_response result = master_data_repository.find_department(id);
        if (result == null) {
            throw new resource_not_found_exception("Department");
        }
        return result;
    }

    private job_title_response require_job_title(Long id) {
        if (id == null) {
            throw new resource_not_found_exception("Job title");
        }
        job_title_response result = master_data_repository.find_job_title(id);
        if (result == null) {
            throw new resource_not_found_exception("Job title");
        }
        return result;
    }

    private work_shift_response require_work_shift(Long id) {
        if (id == null) {
            throw new resource_not_found_exception("Work shift");
        }
        work_shift_response result = master_data_repository.find_work_shift(id);
        if (result == null) {
            throw new resource_not_found_exception("Work shift");
        }
        return result;
    }

    private <T> master_data_page_response<T> page_response(List<T> items, int page, int page_size, long total) {
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / page_size);
        return new master_data_page_response<>(items, page, page_size, total, total_pages);
    }

    private int safe_page_size(int requested) {
        return Math.min(Math.max(requested, 1), max_page_size);
    }

    private void require_text(String value, String label, int max_length) {
        String normalized = normalize_optional(value);
        if (normalized == null || normalized.length() > max_length) {
            throw new IllegalArgumentException(label + " is required and must contain at most " + max_length + " characters.");
        }
    }

    private void validate_status(String status) {
        if (status != null && !valid_statuses.contains(status)) {
            throw new IllegalArgumentException("Master data status is invalid.");
        }
    }

    private String status_or_default(String status) {
        String normalized = normalize_lower(status);
        return normalized == null ? "active" : normalized;
    }

    private String normalize_text(String value) {
        return value == null ? null : value.trim();
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
}
