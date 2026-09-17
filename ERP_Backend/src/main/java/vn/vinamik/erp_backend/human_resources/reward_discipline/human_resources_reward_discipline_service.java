package vn.vinamik.erp_backend.human_resources.reward_discipline;
import vn.vinamik.erp_backend.platform.common.pagination_guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class human_resources_reward_discipline_service {
    private static final Logger logger = LoggerFactory.getLogger(human_resources_reward_discipline_service.class);
    private static final int max_page_size = 100;
    private static final List<String> valid_types = List.of("reward", "discipline");
    private static final List<String> valid_statuses = List.of("draft", "pending", "approved", "rejected", "cancelled");
    private final human_resources_reward_discipline_repository reward_discipline_repository;
    private final audit_event_writer audit_writer;

    public human_resources_reward_discipline_service(
            human_resources_reward_discipline_repository reward_discipline_repository,
            audit_event_writer audit_writer) {
        this.reward_discipline_repository = reward_discipline_repository;
        this.audit_writer = audit_writer;
    }

    @Transactional(readOnly = true)
    public reward_discipline_page_response search(String search, Long employee_id, String event_type, String status,
                                                  LocalDate from_date, LocalDate to_date, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = Math.min(Math.max(page_size, 1), max_page_size);
        String normalized_search = normalize_lower(search);
        String normalized_type = normalize_lower(event_type);
        String normalized_status = normalize_lower(status);
        validate_type(normalized_type);
        validate_status(normalized_status);
        validate_date_filter(from_date, to_date);
        long total = reward_discipline_repository.count(normalized_search, employee_id, normalized_type,
                normalized_status, from_date, to_date);
        List<reward_discipline_response> items = reward_discipline_repository.search(normalized_search, employee_id,
                normalized_type, normalized_status, from_date, to_date, safe_page_size, pagination_guard.offset(safe_page, safe_page_size));
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / safe_page_size);
        return new reward_discipline_page_response(items, safe_page, safe_page_size, total, total_pages);
    }

    @Transactional(readOnly = true)
    public reward_discipline_page_response search_payroll_inputs(Long employee_id, LocalDate from_date,
                                                                 LocalDate to_date, int page, int page_size) {
        return search(null, employee_id, null, "approved", from_date, to_date, page, page_size);
    }

    @Transactional(readOnly = true)
    public reward_discipline_response find_by_id(long record_id) {
        return reward_discipline_repository.find_by_id(record_id);
    }

    @Transactional
    public reward_discipline_response create(reward_discipline_request request, authenticated_user actor,
                                             String correlation_id) {
        validate_request(request);
        String requested_status = normalize_status_or_default(request.status());
        if (!requested_status.equals("draft") && !requested_status.equals("pending")) {
            throw new IllegalArgumentException("New reward or discipline records must start in draft or pending status.");
        }
        String record_code = normalize_required(request.record_code());
        ensure_unique_code(record_code, null);
        ensure_employee_exists(request.employee_id());
        long record_id = reward_discipline_repository.insert(record_code, request.employee_id(),
                normalize_required(request.event_type()), request.effective_on(), request.reason().trim(),
                request.amount(), normalize_currency(request.currency_code()), requested_status, actor.user_id());
        reward_discipline_response created = reward_discipline_repository.find_by_id(record_id);
        audit_writer.write(actor.user_id(), "hr", "reward_discipline_create", "employee_reward_discipline",
                String.valueOf(record_id), correlation_id,
                Map.of("record_code", created.record_code(), "event_type", created.event_type(),
                        "employee_id", created.employee_id()));
        logger.info("Đã tạo bản ghi thưởng hoặc kỷ luật; record_id={}, actor_user_id={}, correlation_id={}",
                record_id, actor.user_id(), correlation_id);
        return created;
    }

    @Transactional
    public reward_discipline_response update(long record_id, reward_discipline_request request,
                                             authenticated_user actor, String correlation_id) {
        validate_request(request);
        if (!reward_discipline_repository.current_status(record_id).equals("draft")) {
            throw new IllegalArgumentException("Only draft reward or discipline records can be edited.");
        }
        String requested_status = normalize_status_or_default(request.status());
        if (!requested_status.equals("draft") && !requested_status.equals("pending")) {
            throw new IllegalArgumentException("Only draft or pending status is allowed while editing a record.");
        }
        String record_code = normalize_required(request.record_code());
        ensure_unique_code(record_code, record_id);
        ensure_employee_exists(request.employee_id());
        if (reward_discipline_repository.payroll_locked(record_id)) {
            throw new IllegalArgumentException("Payroll-locked reward or discipline records cannot be edited.");
        }
        int updated = reward_discipline_repository.update(record_id, record_code, request.employee_id(),
                normalize_required(request.event_type()), request.effective_on(), request.reason().trim(),
                request.amount(), normalize_currency(request.currency_code()), requested_status, actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Reward or discipline record");
        }
        reward_discipline_response updated_record = reward_discipline_repository.find_by_id(record_id);
        audit_writer.write(actor.user_id(), "hr", "reward_discipline_update", "employee_reward_discipline",
                String.valueOf(record_id), correlation_id,
                Map.of("record_code", updated_record.record_code(), "event_type", updated_record.event_type()));
        logger.info("Đã cập nhật bản ghi thưởng hoặc kỷ luật; record_id={}, actor_user_id={}, correlation_id={}",
                record_id, actor.user_id(), correlation_id);
        return updated_record;
    }

    @Transactional
    public reward_discipline_response submit(long record_id, authenticated_user actor, String correlation_id) {
        return change_status(record_id, "pending", null, actor, correlation_id);
    }

    @Transactional
    public reward_discipline_response decide(long record_id, reward_discipline_decision_request request,
                                             authenticated_user actor, String correlation_id) {
        if (request == null) {
            throw new IllegalArgumentException("Decision request is required.");
        }
        String status = normalize_required(request.status());
        if (!status.equals("approved") && !status.equals("rejected")) {
            throw new IllegalArgumentException("Decision must be approved or rejected.");
        }
        return change_status(record_id, status, request.decision_note(), actor, correlation_id);
    }

    private reward_discipline_response change_status(long record_id, String requested_status,
                                                     String decision_note, authenticated_user actor,
                                                     String correlation_id) {
        String current = reward_discipline_repository.current_status(record_id);
        boolean allowed = current.equals("draft") && requested_status.equals("pending")
                || current.equals("pending") && (requested_status.equals("approved") || requested_status.equals("rejected"));
        if (!allowed) {
            throw new IllegalArgumentException("Reward or discipline status transition is not allowed.");
        }
        if (reward_discipline_repository.payroll_locked(record_id)) {
            throw new IllegalArgumentException("Payroll-locked reward or discipline records cannot change status.");
        }
        int updated = requested_status.equals("pending")
                ? reward_discipline_repository.submit(record_id, actor.user_id())
                : reward_discipline_repository.decide(record_id, requested_status,
                        normalize_optional(decision_note), actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Reward or discipline record");
        }
        reward_discipline_response changed = reward_discipline_repository.find_by_id(record_id);
        audit_writer.write(actor.user_id(), "hr", "reward_discipline_" + requested_status,
                "employee_reward_discipline", String.valueOf(record_id), correlation_id,
                Map.of("status", requested_status, "event_type", changed.event_type()));
        logger.info("Đã chuyển trạng thái bản ghi thưởng hoặc kỷ luật; record_id={}, status={}, actor_user_id={}, correlation_id={}",
                record_id, requested_status, actor.user_id(), correlation_id);
        return changed;
    }

    private void validate_request(reward_discipline_request request) {
        if (request == null || request.event_type() == null || request.effective_on() == null
                || request.reason() == null || request.reason().isBlank()) {
            throw new IllegalArgumentException("Event type, effective date and reason are required.");
        }
        String type = normalize_required(request.event_type());
        validate_type(type);
        if (request.amount() != null && request.amount().scale() > 2) {
            throw new IllegalArgumentException("Amount supports at most 2 decimal places.");
        }
        if (request.currency_code() != null && request.currency_code().trim().length() != 3) {
            throw new IllegalArgumentException("Currency code must contain 3 characters.");
        }
        validate_status(normalize_status_or_default(request.status()));
    }

    private void validate_date_filter(LocalDate from_date, LocalDate to_date) {
        if (from_date != null && to_date != null && to_date.isBefore(from_date)) {
            throw new IllegalArgumentException("Filter end date cannot be before filter start date.");
        }
    }

    private void validate_type(String type) {
        if (type != null && !valid_types.contains(type)) {
            throw new IllegalArgumentException("Event type must be reward or discipline.");
        }
    }

    private void validate_status(String status) {
        if (status != null && !valid_statuses.contains(status)) {
            throw new IllegalArgumentException("Reward or discipline status is invalid.");
        }
    }

    private void ensure_employee_exists(long employee_id) {
        if (!reward_discipline_repository.employee_exists(employee_id)) {
            throw new resource_not_found_exception("Employee");
        }
    }

    private void ensure_unique_code(String record_code, Long record_id) {
        if (reward_discipline_repository.record_code_exists(record_code, record_id)) {
            throw new field_conflict_exception("record_code", "Reward or discipline code already exists.");
        }
    }

    private String normalize_status_or_default(String status) {
        String normalized = normalize_optional(status);
        return normalized == null ? "draft" : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalize_currency(String currency_code) {
        return (currency_code == null || currency_code.isBlank() ? "VND" : currency_code.trim()).toUpperCase(Locale.ROOT);
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
