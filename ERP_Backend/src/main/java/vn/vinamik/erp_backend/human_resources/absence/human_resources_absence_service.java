package vn.vinamik.erp_backend.human_resources.absence;
import vn.vinamik.erp_backend.platform.common.pagination_guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class human_resources_absence_service {
    private static final Logger logger = LoggerFactory.getLogger(human_resources_absence_service.class);
    private static final int max_page_size = 100;
    private static final List<String> valid_statuses = List.of("draft", "pending", "approved", "rejected", "cancelled");

    private final human_resources_absence_repository absence_repository;
    private final audit_event_writer audit_writer;

    public human_resources_absence_service(human_resources_absence_repository absence_repository,
                                            audit_event_writer audit_writer) {
        this.absence_repository = absence_repository;
        this.audit_writer = audit_writer;
    }

    @Transactional(readOnly = true)
    public leave_page_response search(String search, Long employee_id, String status, LocalDate from_date,
                                      LocalDate to_date, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = Math.min(Math.max(page_size, 1), max_page_size);
        String normalized_search = normalize_lower(search);
        String normalized_status = normalize_lower(status);
        validate_status(normalized_status);
        if (from_date != null && to_date != null && to_date.isBefore(from_date)) {
            throw new IllegalArgumentException("Filter end date cannot be before filter start date.");
        }
        long total = absence_repository.count(normalized_search, employee_id, normalized_status, from_date, to_date);
        List<leave_response> items = absence_repository.search(normalized_search, employee_id, normalized_status,
                from_date, to_date, safe_page_size, pagination_guard.offset(safe_page, safe_page_size));
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / safe_page_size);
        return new leave_page_response(items, safe_page, safe_page_size, total, total_pages);
    }

    @Transactional(readOnly = true)
    public leave_response find_by_id(long leave_request_id) {
        return absence_repository.find(leave_request_id);
    }

    @Transactional
    public leave_response create(leave_request request, authenticated_user actor, String correlation_id) {
        validate_request(request);
        String requested_status = normalize_status_or_default(request.status());
        if (!requested_status.equals("draft") && !requested_status.equals("pending")) {
            throw new IllegalArgumentException("New leave requests must start in draft or pending status.");
        }
        String request_code = normalize_required(request.request_code());
        ensure_unique_code(request_code, null);
        ensure_employee_exists(request.employee_id());
        long leave_request_id;
        try {
            leave_request_id = absence_repository.insert(request_code, request.employee_id(),
                    normalize_required(request.leave_type_code()), request.starts_on(), request.ends_on(),
                    request.is_paid(), normalize_optional(request.reason()), requested_status, actor.user_id());
        } catch (DuplicateKeyException exception) {
            throw new field_conflict_exception("request_code", "Leave request code already exists.");
        }
        leave_response created = absence_repository.find(leave_request_id);
        audit_writer.write(actor.user_id(), "hr", "absence_create", "leave_request",
                String.valueOf(leave_request_id), correlation_id,
                Map.of("request_code", created.request_code(), "employee_id", created.employee_id()));
        logger.info("Đã tạo yêu cầu nghỉ phép; leave_request_id={}, actor_user_id={}, correlation_id={}",
                leave_request_id, actor.user_id(), correlation_id);
        return created;
    }

    @Transactional
    public leave_response update(long leave_request_id, leave_request request, authenticated_user actor,
                                 String correlation_id) {
        validate_request(request);
        String current_status = absence_repository.current_status(leave_request_id);
        if (!current_status.equals("draft") && !current_status.equals("pending")) {
            throw new IllegalArgumentException("Only draft or pending leave requests can be edited.");
        }
        String requested_status = normalize_status_or_default(request.status());
        if (!requested_status.equals("draft") && !requested_status.equals("pending")) {
            throw new IllegalArgumentException("Only draft or pending status is allowed while editing a leave request.");
        }
        String request_code = normalize_required(request.request_code());
        ensure_unique_code(request_code, leave_request_id);
        ensure_employee_exists(request.employee_id());
        int updated = absence_repository.update(leave_request_id, request_code, request.employee_id(),
                normalize_required(request.leave_type_code()), request.starts_on(), request.ends_on(),
                request.is_paid(), normalize_optional(request.reason()), requested_status, actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Leave request");
        }
        leave_response updated_request = absence_repository.find(leave_request_id);
        audit_writer.write(actor.user_id(), "hr", "absence_update", "leave_request",
                String.valueOf(leave_request_id), correlation_id,
                Map.of("request_code", updated_request.request_code(), "employee_id", updated_request.employee_id()));
        logger.info("Đã cập nhật yêu cầu nghỉ phép; leave_request_id={}, actor_user_id={}, correlation_id={}",
                leave_request_id, actor.user_id(), correlation_id);
        return updated_request;
    }

    @Transactional
    public leave_response decide(long leave_request_id, leave_decision_request request,
                                 authenticated_user actor, String correlation_id) {
        if (request == null) {
            throw new IllegalArgumentException("Leave decision request is required.");
        }
        String requested_status = normalize_required(request.status());
        if (!requested_status.equals("approved") && !requested_status.equals("rejected")) {
            throw new IllegalArgumentException("Leave decision must be approved or rejected.");
        }
        String current_status = absence_repository.current_status(leave_request_id);
        if (!current_status.equals("pending")) {
            throw new IllegalArgumentException("Only pending leave requests can be decided.");
        }
        if (requested_status.equals("approved")) {
            absence_repository.lock_employee_schedule(leave_request_id);
            String locked_status = absence_repository.current_status(leave_request_id);
            if (!locked_status.equals("pending")) {
                throw new IllegalArgumentException("Only pending leave requests can be decided.");
            }
            if (absence_repository.has_approved_overlap(leave_request_id)) {
                throw new field_conflict_exception(
                        "starts_on", "Approved leave requests cannot overlap for the same employee.");
            }
        }
        int updated = absence_repository.decide(leave_request_id, requested_status, actor.user_id(),
                normalize_optional(request.decision_note()));
        if (updated == 0) {
            throw new resource_not_found_exception("Leave request");
        }
        leave_response decided = absence_repository.find(leave_request_id);
        audit_writer.write(actor.user_id(), "hr", "absence_" + requested_status, "leave_request",
                String.valueOf(leave_request_id), correlation_id,
                Map.of("status", requested_status, "employee_id", decided.employee_id()));
        logger.info("Đã xử lý yêu cầu nghỉ phép; leave_request_id={}, status={}, actor_user_id={}, correlation_id={}",
                leave_request_id, requested_status, actor.user_id(), correlation_id);
        return decided;
    }

    @Transactional
    public leave_response cancel(long leave_request_id, authenticated_user actor, String correlation_id) {
        String current_status = absence_repository.current_status(leave_request_id);
        if (!current_status.equals("draft") && !current_status.equals("pending")) {
            throw new IllegalArgumentException("Only draft or pending leave requests can be cancelled.");
        }
        int updated = absence_repository.cancel(leave_request_id, actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Leave request");
        }
        leave_response cancelled = absence_repository.find(leave_request_id);
        audit_writer.write(actor.user_id(), "hr", "absence_cancel", "leave_request",
                String.valueOf(leave_request_id), correlation_id,
                Map.of("employee_id", cancelled.employee_id()));
        logger.info("Đã hủy yêu cầu nghỉ phép; leave_request_id={}, actor_user_id={}, correlation_id={}",
                leave_request_id, actor.user_id(), correlation_id);
        return cancelled;
    }

    private void validate_request(leave_request request) {
        if (request == null) {
            throw new IllegalArgumentException("Leave request is required.");
        }
        if (normalize_optional(request.request_code()) == null
                || normalize_optional(request.request_code()).length() > 60
                || request.employee_id() == null || request.employee_id() <= 0
                || normalize_optional(request.leave_type_code()) == null
                || request.starts_on() == null || request.ends_on() == null) {
            throw new IllegalArgumentException("Leave request code, employee, leave type and dates are required.");
        }
        if (request.ends_on().isBefore(request.starts_on())) {
            throw new IllegalArgumentException("Leave end date cannot be before start date.");
        }
        if (normalize_optional(request.reason()) != null && normalize_optional(request.reason()).length() > 2000) {
            throw new IllegalArgumentException("Leave reason must contain at most 2000 characters.");
        }
        validate_status(normalize_status_or_default(request.status()));
    }

    private void validate_status(String status) {
        if (status != null && !valid_statuses.contains(status)) {
            throw new IllegalArgumentException("Leave request status is invalid.");
        }
    }

    private void ensure_employee_exists(long employee_id) {
        if (!absence_repository.employee_exists(employee_id)) {
            throw new resource_not_found_exception("Employee");
        }
    }

    private void ensure_unique_code(String request_code, Long leave_request_id) {
        if (absence_repository.request_code_exists(request_code, leave_request_id)) {
            throw new field_conflict_exception("request_code", "Leave request code already exists.");
        }
    }

    private String normalize_status_or_default(String status) {
        String normalized = normalize_optional(status);
        return normalized == null ? "pending" : normalized.toLowerCase(Locale.ROOT);
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
