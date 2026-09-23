package vn.vinamik.erp_backend.production.assignment;
import vn.vinamik.erp_backend.platform.common.pagination_guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.human_resources.api.human_resources_employee_contract;
import vn.vinamik.erp_backend.human_resources.api.human_resources_employee_snapshot;
import vn.vinamik.erp_backend.human_resources.api.human_resources_work_shift_contract;
import vn.vinamik.erp_backend.human_resources.api.human_resources_work_shift_snapshot;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class production_assignment_service {
    private static final Logger logger = LoggerFactory.getLogger(production_assignment_service.class);
    private static final int max_page_size = 100;
    private static final List<String> valid_statuses = List.of("planned", "active", "completed", "cancelled");

    private final production_assignment_repository assignment_repository;
    private final human_resources_employee_contract employee_contract;
    private final human_resources_work_shift_contract work_shift_contract;
    private final audit_event_writer audit_writer;

    public production_assignment_service(
            production_assignment_repository assignment_repository,
            human_resources_employee_contract employee_contract,
            human_resources_work_shift_contract work_shift_contract,
            audit_event_writer audit_writer) {
        this.assignment_repository = assignment_repository;
        this.employee_contract = employee_contract;
        this.work_shift_contract = work_shift_contract;
        this.audit_writer = audit_writer;
    }

    @Transactional(readOnly = true)
    public production_assignment_page_response search(Long production_order_id, Long employee_id, String status,
                                                      Instant from_at, Instant to_at, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = Math.min(Math.max(page_size, 1), max_page_size);
        String normalized_status = normalize_lower(status);
        validate_status_filter(normalized_status);
        validate_time_range(from_at, to_at);
        long total = assignment_repository.count(production_order_id, employee_id, normalized_status, from_at, to_at);
        List<assignment_row> rows = assignment_repository.search(production_order_id, employee_id, normalized_status,
                from_at, to_at, safe_page_size, pagination_guard.offset(safe_page, safe_page_size));
        Map<Long, human_resources_employee_snapshot> employees = employee_contract.find_employees(
                rows.stream().map(assignment_row::employee_id).distinct().toList());
        Map<Long, human_resources_work_shift_snapshot> shifts = work_shift_contract.find_work_shifts(
                rows.stream().map(assignment_row::work_shift_id).filter(java.util.Objects::nonNull).distinct().toList());
        List<production_assignment_response> items = rows.stream()
                .map(row -> to_response(row, employees.get(row.employee_id()),
                        row.work_shift_id() == null ? null : shifts.get(row.work_shift_id()), row.order_code()))
                .toList();
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / safe_page_size);
        return new production_assignment_page_response(items, safe_page, safe_page_size, total, total_pages);
    }

    @Transactional(readOnly = true)
    public production_assignment_response find_by_id(long production_assignment_id) {
        return to_response(assignment_repository.find(production_assignment_id));
    }

    @Transactional
    public production_assignment_response create(production_assignment_request request, authenticated_user actor,
                                                 String correlation_id) {
        validate_request(request);
        production_order_snapshot order = assignment_repository.find_order(request.production_order_id());
        validate_order_for_assignment(order);
        human_resources_employee_snapshot employee = require_active_employee(request.employee_id());
        human_resources_work_shift_snapshot shift = require_active_shift(request.work_shift_id());
        ensure_no_overlap(request, null);
        long assignment_id;
        try {
            assignment_id = assignment_repository.insert(request.production_order_id(), request.employee_id(),
                    request.work_shift_id(), normalize_optional(request.assignment_name()), request.starts_at(),
                    request.ends_at(), normalize_optional(request.notes()), actor.user_id());
        } catch (DuplicateKeyException exception) {
            throw new field_conflict_exception("assignment", "The production assignment conflicts with an existing record.");
        }
        production_assignment_response created = to_response(assignment_repository.find(assignment_id), employee, shift,
                order.order_code());
        audit_writer.write(actor.user_id(), "production", "production_assignment_create", "production_assignment",
                String.valueOf(assignment_id), correlation_id,
                Map.of("production_order_id", request.production_order_id(), "employee_id", request.employee_id()));
        logger.info("Đã tạo phân công nhân sự sản xuất; production_assignment_id={}, production_order_id={}, employee_id={}, actor_user_id={}, correlation_id={}",
                assignment_id, request.production_order_id(), request.employee_id(), actor.user_id(), correlation_id);
        return created;
    }

    @Transactional
    public production_assignment_response update(long production_assignment_id, production_assignment_request request,
                                                 authenticated_user actor, String correlation_id) {
        validate_request(request);
        String current_status = assignment_repository.current_status(production_assignment_id);
        if (!"planned".equals(current_status)) {
            throw new IllegalArgumentException("Only planned production assignments can be edited.");
        }
        production_order_snapshot order = assignment_repository.find_order(request.production_order_id());
        validate_order_for_assignment(order);
        human_resources_employee_snapshot employee = require_active_employee(request.employee_id());
        human_resources_work_shift_snapshot shift = require_active_shift(request.work_shift_id());
        ensure_no_overlap(request, production_assignment_id);
        int updated = assignment_repository.update(production_assignment_id, request.production_order_id(),
                request.employee_id(), request.work_shift_id(), normalize_optional(request.assignment_name()),
                request.starts_at(), request.ends_at(), normalize_optional(request.notes()), actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Planned production assignment");
        }
        production_assignment_response changed = to_response(assignment_repository.find(production_assignment_id),
                employee, shift, order.order_code());
        audit_writer.write(actor.user_id(), "production", "production_assignment_update", "production_assignment",
                String.valueOf(production_assignment_id), correlation_id,
                Map.of("employee_id", request.employee_id()));
        logger.info("Đã cập nhật phân công nhân sự sản xuất; production_assignment_id={}, actor_user_id={}, correlation_id={}",
                production_assignment_id, actor.user_id(), correlation_id);
        return changed;
    }

    @Transactional
    public production_assignment_response change_status(long production_assignment_id, String requested_status,
                                                        authenticated_user actor, String correlation_id) {
        String target = normalize_lower(requested_status);
        validate_status(target);
        String current = assignment_repository.current_status(production_assignment_id);
        if (!allowed_transition(current, target)) {
            throw new IllegalArgumentException("The production assignment status transition is not allowed.");
        }
        int updated = assignment_repository.change_status(production_assignment_id, target, actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Production assignment");
        }
        production_assignment_response changed = to_response(assignment_repository.find(production_assignment_id));
        audit_writer.write(actor.user_id(), "production", "production_assignment_status_change",
                "production_assignment", String.valueOf(production_assignment_id), correlation_id,
                Map.of("from_status", current, "to_status", target));
        logger.info("Đã chuyển trạng thái phân công sản xuất; production_assignment_id={}, from_status={}, to_status={}, actor_user_id={}, correlation_id={}",
                production_assignment_id, current, target, actor.user_id(), correlation_id);
        return changed;
    }

    private production_assignment_response to_response(assignment_row row) {
        human_resources_employee_snapshot employee = employee_contract.find_employee(row.employee_id()).orElse(null);
        human_resources_work_shift_snapshot shift = row.work_shift_id() == null
                ? null
                : work_shift_contract.find_work_shift(row.work_shift_id()).orElse(null);
        return to_response(row, employee, shift, row.order_code());
    }

    private production_assignment_response to_response(assignment_row row,
                                                       human_resources_employee_snapshot employee,
                                                       human_resources_work_shift_snapshot shift,
                                                       String order_code) {
        return new production_assignment_response(row.production_assignment_id(), row.production_order_id(), order_code,
                row.employee_id(), employee == null ? null : employee.employee_code(),
                employee == null ? null : employee.full_name(), row.work_shift_id(),
                shift == null ? null : shift.shift_code(), shift == null ? null : shift.shift_name(),
                row.assignment_name(), row.starts_at(), row.ends_at(), row.status(), row.notes());
    }

    private human_resources_employee_snapshot require_active_employee(long employee_id) {
        human_resources_employee_snapshot employee = employee_contract.find_employee(employee_id)
                .orElseThrow(() -> new resource_not_found_exception("Employee"));
        if (!"active".equals(employee.employment_status())) {
            throw new IllegalArgumentException("Employee must be active for a production assignment.");
        }
        return employee;
    }

    private human_resources_work_shift_snapshot require_active_shift(Long work_shift_id) {
        if (work_shift_id == null) {
            return null;
        }
        human_resources_work_shift_snapshot shift = work_shift_contract.find_work_shift(work_shift_id)
                .orElseThrow(() -> new resource_not_found_exception("Work shift"));
        if (!"active".equals(shift.status())) {
            throw new IllegalArgumentException("Work shift must be active for a production assignment.");
        }
        return shift;
    }
    private void ensure_no_overlap(production_assignment_request request, Long excluded_id) {
        assignment_repository.lock_employee_schedule(request.employee_id());
        if (assignment_repository.has_overlap(request.employee_id(), request.starts_at(), request.ends_at(),
                excluded_id)) {
            throw new field_conflict_exception("starts_at", "The employee assignment overlaps an existing assignment.");
        }
    }

    private void validate_order_for_assignment(production_order_snapshot order) {
        if (List.of("cancelled", "completed").contains(order.status())) {
            throw new IllegalArgumentException("Assignments cannot be added to a cancelled or completed production order.");
        }
    }

    private boolean allowed_transition(String current, String target) {
        return switch (current) {
            case "planned" -> target.equals("active") || target.equals("cancelled");
            case "active" -> target.equals("completed") || target.equals("cancelled");
            default -> false;
        };
    }

    private void validate_request(production_assignment_request request) {
        if (request == null || request.employee_id() == null || request.production_order_id() == null
                || request.starts_at() == null || request.ends_at() == null) {
            throw new IllegalArgumentException("Production assignment fields are required.");
        }
        if (!request.ends_at().isAfter(request.starts_at())) {
            throw new IllegalArgumentException("Assignment end time must be after start time.");
        }
    }

    private void validate_time_range(Instant from_at, Instant to_at) {
        if (from_at != null && to_at != null && to_at.isBefore(from_at)) {
            throw new IllegalArgumentException("Assignment end filter cannot be before start filter.");
        }
    }

    private void validate_status_filter(String status) {
        if (status != null && !valid_statuses.contains(status)) {
            throw new IllegalArgumentException("Production assignment status is invalid.");
        }
    }

    private void validate_status(String status) {
        if (status == null || !valid_statuses.contains(status)) {
            throw new IllegalArgumentException("Production assignment status is invalid.");
        }
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
