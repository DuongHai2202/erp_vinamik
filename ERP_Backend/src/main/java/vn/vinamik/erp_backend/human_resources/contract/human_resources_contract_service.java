package vn.vinamik.erp_backend.human_resources.contract;
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
public class human_resources_contract_service {
    private static final Logger logger = LoggerFactory.getLogger(human_resources_contract_service.class);
    private static final int max_page_size = 100;
    private static final List<String> valid_statuses = List.of("draft", "active", "expired", "terminated", "cancelled");
    private static final List<String> editable_statuses = List.of("draft");
    private final human_resources_contract_repository contract_repository;
    private final audit_event_writer audit_writer;

    public human_resources_contract_service(human_resources_contract_repository contract_repository,
                                            audit_event_writer audit_writer) {
        this.contract_repository = contract_repository;
        this.audit_writer = audit_writer;
    }

    @Transactional(readOnly = true)
    public employment_contract_page_response search(String search, Long employee_id, String status,
                                                    boolean expiring_only, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = Math.min(Math.max(page_size, 1), max_page_size);
        String normalized_search = normalize_optional(search);
        if (normalized_search != null) {
            normalized_search = normalized_search.toLowerCase(Locale.ROOT);
        }
        String normalized_status = normalize_optional(status);
        if (normalized_status != null) {
            normalized_status = normalized_status.toLowerCase(Locale.ROOT);
        }
        validate_status(normalized_status);
        return contract_repository.search(normalized_search, employee_id, normalized_status, expiring_only,
                safe_page, safe_page_size);
    }

    @Transactional(readOnly = true)
    public employment_contract_response find_by_id(long employment_contract_id) {
        return contract_repository.find_by_id(employment_contract_id);
    }

    @Transactional
    public employment_contract_response create(employment_contract_request request, authenticated_user actor,
                                               String correlation_id) {
        validate_request(request);
        String status = normalize_status_or_default(request.status());
        if (!editable_statuses.contains(status)) {
            throw new IllegalArgumentException("New contracts must start in draft status.");
        }
        String contract_code = normalize_required(request.contract_code());
        ensure_unique_code(contract_code, null);
        ensure_employee_exists(request.employee_id());
        long contract_id = contract_repository.insert(contract_code, request.employee_id(),
                normalize_required(request.contract_type()), request.effective_from(), request.effective_to(),
                request.base_salary(), normalize_currency(request.currency_code()), status,
                normalize_optional(request.notes()), actor.user_id());
        employment_contract_response created = contract_repository.find_by_id(contract_id);
        audit_writer.write(actor.user_id(), "hr", "contract_create", "employment_contract",
                String.valueOf(contract_id), correlation_id,
                Map.of("contract_code", created.contract_code(), "employee_id", created.employee_id()));
        logger.info("Đã tạo hợp đồng lao động; employment_contract_id={}, actor_user_id={}, correlation_id={}",
                contract_id, actor.user_id(), correlation_id);
        return created;
    }

    @Transactional
    public employment_contract_response update(long employment_contract_id, employment_contract_request request,
                                               authenticated_user actor, String correlation_id) {
        validate_request(request);
        String contract_code = normalize_required(request.contract_code());
        ensure_unique_code(contract_code, employment_contract_id);
        ensure_employee_exists(request.employee_id());
        String current_status = contract_repository.current_status(employment_contract_id);
        if (!editable_statuses.contains(current_status)) {
            throw new IllegalArgumentException("Only draft contracts can be edited.");
        }
        String requested_status = normalize_status_or_default(request.status());
        if (!editable_statuses.contains(requested_status)) {
            throw new IllegalArgumentException("Only draft status is allowed while editing a contract.");
        }
        int updated = contract_repository.update(employment_contract_id, contract_code, request.employee_id(),
                normalize_required(request.contract_type()), request.effective_from(), request.effective_to(),
                request.base_salary(), normalize_currency(request.currency_code()), requested_status,
                normalize_optional(request.notes()), actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Employment contract");
        }
        employment_contract_response updated_contract = contract_repository.find_by_id(employment_contract_id);
        audit_writer.write(actor.user_id(), "hr", "contract_update", "employment_contract",
                String.valueOf(employment_contract_id), correlation_id,
                Map.of("contract_code", updated_contract.contract_code(), "employee_id", updated_contract.employee_id()));
        logger.info("Đã cập nhật hợp đồng lao động; employment_contract_id={}, actor_user_id={}, correlation_id={}",
                employment_contract_id, actor.user_id(), correlation_id);
        return updated_contract;
    }

    @Transactional
    public employment_contract_response change_status(long employment_contract_id,
                                                      employment_contract_status_request request,
                                                      authenticated_user actor, String correlation_id) {
        String requested_status = normalize_required(request.status());
        validate_status(requested_status);
        String current = contract_repository.current_status(employment_contract_id);
        validate_transition(current, requested_status);
        String notes = normalize_optional(request.notes());
        int updated = contract_repository.change_status(employment_contract_id, requested_status, notes, actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Employment contract");
        }
        employment_contract_response changed = contract_repository.find_by_id(employment_contract_id);
        audit_writer.write(actor.user_id(), "hr", "contract_status_change", "employment_contract",
                String.valueOf(employment_contract_id), correlation_id,
                Map.of("from_status", current, "to_status", requested_status));
        logger.info("Đã chuyển trạng thái hợp đồng lao động; employment_contract_id={}, from_status={}, to_status={}, actor_user_id={}, correlation_id={}",
                employment_contract_id, current, requested_status, actor.user_id(), correlation_id);
        return changed;
    }

    @Transactional
    public void delete(long employment_contract_id, authenticated_user actor, String correlation_id) {
        String current_status = contract_repository.current_status(employment_contract_id);
        if (!current_status.equals("draft")) {
            throw new IllegalArgumentException("Only draft employment contracts can be deleted.");
        }
        if (contract_repository.delete_draft(employment_contract_id) == 0) {
            throw new resource_not_found_exception("Draft employment contract");
        }
        audit_writer.write(actor.user_id(), "hr", "contract_delete", "employment_contract",
                String.valueOf(employment_contract_id), correlation_id, Map.of("status", current_status));
        logger.info("Đã xóa bản nháp hợp đồng lao động; employment_contract_id={}, actor_user_id={}, correlation_id={}",
                employment_contract_id, actor.user_id(), correlation_id);
    }

    private void validate_request(employment_contract_request request) {
        if (request == null) {
            throw new IllegalArgumentException("Employment contract request is required.");
        }
        if (normalize_optional(request.contract_code()) == null
                || normalize_optional(request.contract_code()).length() > 60
                || request.employee_id() == null || request.employee_id() <= 0
                || normalize_optional(request.contract_type()) == null
                || request.effective_from() == null
                || request.base_salary() == null) {
            throw new IllegalArgumentException("Contract code, employee, type, start date and base salary are required.");
        }
        if (request.effective_to() != null && request.effective_to().isBefore(request.effective_from())) {
            throw new IllegalArgumentException("Contract end date cannot be before start date.");
        }
        if (request.base_salary().signum() < 0 || request.base_salary().scale() > 2) {
            throw new IllegalArgumentException("Base salary must be zero or greater with at most 2 decimal places.");
        }
        if (request.currency_code() != null && request.currency_code().trim().length() != 3) {
            throw new IllegalArgumentException("Currency code must contain 3 characters.");
        }
        if (normalize_optional(request.notes()) != null && normalize_optional(request.notes()).length() > 2000) {
            throw new IllegalArgumentException("Contract notes must contain at most 2000 characters.");
        }
        validate_status(normalize_status_or_default(request.status()));
    }

    private void validate_status(String status) {
        if (status != null && !valid_statuses.contains(status)) {
            throw new IllegalArgumentException("Contract status is invalid.");
        }
    }

    private void validate_transition(String current, String requested) {
        if (current.equals(requested)) {
            throw new IllegalArgumentException("Contract is already in the requested status.");
        }
        boolean allowed = (current.equals("draft") && (requested.equals("active") || requested.equals("cancelled")))
                || (current.equals("active") && (requested.equals("terminated") || requested.equals("expired")));
        if (!allowed) {
            throw new IllegalArgumentException("Contract status transition is not allowed.");
        }
    }

    private void ensure_employee_exists(long employee_id) {
        if (!contract_repository.employee_exists(employee_id)) {
            throw new resource_not_found_exception("Employee");
        }
    }

    private void ensure_unique_code(String contract_code, Long contract_id) {
        if (contract_repository.contract_code_exists(contract_code, contract_id)) {
            throw new field_conflict_exception("contract_code", "Contract code already exists.");
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

    private String normalize_optional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
