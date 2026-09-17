package vn.vinamik.erp_backend.inventory.issue;
import vn.vinamik.erp_backend.platform.common.pagination_guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class inventory_issue_service {
    private static final Logger logger = LoggerFactory.getLogger(inventory_issue_service.class);
    private static final int max_page_size = 100;
    private final inventory_issue_repository issue_repository;
    private final audit_event_writer audit_writer;

    @Autowired
    public inventory_issue_service(inventory_issue_repository issue_repository, audit_event_writer audit_writer) {
        this.issue_repository = issue_repository;
        this.audit_writer = audit_writer;
    }


    @Transactional(readOnly = true)
    public issue_page_response search(String search, Long warehouse_id, String status, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = Math.min(Math.max(page_size, 1), max_page_size);
        String normalized_search = normalize_lower(search);
        String normalized_status = normalize_lower(status);
        if (normalized_status != null && !List.of("draft", "pending", "posted", "cancelled").contains(normalized_status)) {
            throw new IllegalArgumentException("Issue status is invalid.");
        }
        long total = issue_repository.count(normalized_search, warehouse_id, normalized_status);
        List<issue_summary> items = issue_repository.search(
                normalized_search, warehouse_id, normalized_status, safe_page_size, pagination_guard.offset(safe_page, safe_page_size));
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / safe_page_size);
        return new issue_page_response(items, safe_page, safe_page_size, total, total_pages);
    }

    @Transactional(readOnly = true)
    public issue_response find_by_id(long issue_id) {
        return issue_repository.find(issue_id, false);
    }

    @Transactional
    public issue_response create(issue_request request, authenticated_user actor, String correlation_id) {
        validate_request(request);
        String issue_code = normalize_required(request.issue_code());
        String idempotency_key = normalize_optional(request.idempotency_key());
        if (idempotency_key != null) {
            List<Long> existing = issue_repository.find_by_idempotency_key(idempotency_key);
            if (!existing.isEmpty()) {
                return issue_repository.find(existing.getFirst(), false);
            }
        }
        ensure_unique_issue_code(issue_code);
        ensure_warehouse_active(request.warehouse_id());
        Long issue_id = issue_repository.insert_issue(
                issue_code,
                request.warehouse_id(),
                normalize_optional(request.source_module()),
                request.source_document_id(),
                normalize_optional(request.reason_code()),
                idempotency_key,
                normalize_optional(request.notes()),
                actor.user_id());
        if (issue_id == null) {
            if (idempotency_key != null) {
                List<Long> existing = issue_repository.find_by_idempotency_key(idempotency_key);
                if (!existing.isEmpty()) {
                    return issue_repository.find(existing.getFirst(), false);
                }
            }
            throw new field_conflict_exception("issue_code", "Issue code already exists.");
        }
        for (int index = 0; index < request.lines().size(); index++) {
            issue_line_request line = request.lines().get(index);
            validate_line(request.warehouse_id(), line);
            issue_repository.insert_line(issue_id, index + 1, line);
        }
        issue_response created = issue_repository.find(issue_id, false);
        audit_writer.write(actor.user_id(), "inventory", "issue_create", "issue", String.valueOf(issue_id), correlation_id,
                Map.of("issue_code", created.issue_code(), "line_count", created.lines().size()));
        logger.info("Đã tạo phiếu xuất kho; issue_id={}, actor_user_id={}, correlation_id={}", issue_id, actor.user_id(), correlation_id);
        return created;
    }

    @Transactional
    public issue_response post(long issue_id, authenticated_user actor, String correlation_id) {
        issue_response issue = issue_repository.find(issue_id, true);
        if (issue.status().equals("posted")) {
            return issue;
        }
        if (!issue.status().equals("draft") && !issue.status().equals("pending")) {
            throw new IllegalArgumentException("Only draft or pending issues can be posted.");
        }
        if (issue.lines().isEmpty()) {
            throw new IllegalArgumentException("Issue must contain at least one line before posting.");
        }
        for (issue_line_response line : issue.lines()) {
            validate_line(issue.warehouse_id(), new issue_line_request(
                    line.stock_item_id(), line.warehouse_location_id(), line.stock_lot_id(), line.quantity()));
            post_line(issue, line, actor);
        }
        int updated = issue_repository.mark_posted(issue_id, actor.user_id());
        if (updated == 0) {
            return issue_repository.find(issue_id, false);
        }
        issue_response posted = issue_repository.find(issue_id, false);
        audit_writer.write(actor.user_id(), "inventory", "issue_post", "issue", String.valueOf(issue_id), correlation_id,
                Map.of("issue_code", posted.issue_code(), "line_count", posted.lines().size()));
        logger.info("Đã ghi sổ phiếu xuất kho; issue_id={}, actor_user_id={}, correlation_id={}", issue_id, actor.user_id(), correlation_id);
        return posted;
    }

    private void post_line(issue_response issue, issue_line_response line, authenticated_user actor) {
        String idempotency_key = "issue:" + issue.issue_id() + ":line:" + line.issue_line_id();
        if (issue_repository.movement_exists(issue.issue_id(), line.issue_line_id()) > 0) {
            return;
        }
        issue_repository.lock_balance(balance_lock_key(line.stock_item_id(), line.warehouse_location_id(), line.stock_lot_id()));
        List<BigDecimal> balances = issue_repository.lock_balance_row(line);
        if (balances.isEmpty() || balances.getFirst().compareTo(line.quantity()) < 0) {
            throw new field_conflict_exception("lines", "Insufficient stock quantity.");
        }
        issue_repository.insert_movement(issue, line, actor.user_id(), idempotency_key);
        issue_repository.decrement_balance(line);
    }

    private void validate_request(issue_request request) {
        if (request == null) {
            throw new IllegalArgumentException("Issue request is required.");
        }
        String issue_code = normalize_optional(request.issue_code());
        if (issue_code == null || issue_code.length() > 60) {
            throw new IllegalArgumentException("Issue code is required and must contain at most 60 characters.");
        }
        if (request.warehouse_id() == null || request.warehouse_id() <= 0) {
            throw new IllegalArgumentException("Warehouse is required.");
        }
        String source_module = normalize_optional(request.source_module());
        if ((source_module == null) != (request.source_document_id() == null)) {
            throw new IllegalArgumentException("Source module and source document must be provided together.");
        }
        if (request.source_document_id() != null && request.source_document_id() <= 0) {
            throw new IllegalArgumentException("Source document is invalid.");
        }
        if (normalize_optional(request.reason_code()) != null
                && normalize_optional(request.reason_code()).length() > 40) {
            throw new IllegalArgumentException("Reason code must contain at most 40 characters.");
        }
        if (normalize_optional(request.idempotency_key()) != null
                && normalize_optional(request.idempotency_key()).length() > 120) {
            throw new IllegalArgumentException("Idempotency key must contain at most 120 characters.");
        }
        if (normalize_optional(request.notes()) != null && normalize_optional(request.notes()).length() > 2000) {
            throw new IllegalArgumentException("Issue notes must contain at most 2000 characters.");
        }
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException("Issue must contain at least one line.");
        }
        for (issue_line_request line : request.lines()) {
            if (line == null || line.stock_item_id() == null || line.stock_item_id() <= 0
                    || line.warehouse_location_id() == null || line.warehouse_location_id() <= 0) {
                throw new IllegalArgumentException("Issue line is incomplete.");
            }
        }
    }

    private void validate_line(long warehouse_id, issue_line_request line) {
        if (line == null || line.stock_item_id() == null || line.warehouse_location_id() == null) {
            throw new IllegalArgumentException("Issue line is incomplete.");
        }
        if (line.quantity() == null || line.quantity().signum() <= 0 || line.quantity().scale() > 6) {
            throw new IllegalArgumentException("Issue quantity must be greater than zero with at most 6 decimal places.");
        }
        if (!issue_repository.active_stock_item(line.stock_item_id())) {
            throw new resource_not_found_exception("Active stock item");
        }
        if (!issue_repository.active_location(line.warehouse_location_id(), warehouse_id)) {
            throw new resource_not_found_exception("Active warehouse location");
        }
        if (line.stock_lot_id() != null && !issue_repository.active_lot(line.stock_lot_id(), line.stock_item_id())) {
            throw new resource_not_found_exception("Active stock lot");
        }
    }

    private void ensure_warehouse_active(long warehouse_id) {
        if (!issue_repository.active_warehouse(warehouse_id)) {
            throw new resource_not_found_exception("Active warehouse");
        }
    }

    private void ensure_unique_issue_code(String issue_code) {
        if (issue_repository.issue_code_exists(issue_code)) {
            throw new field_conflict_exception("issue_code", "Issue code already exists.");
        }
    }

    private String balance_lock_key(long stock_item_id, long warehouse_location_id, Long stock_lot_id) {
        return stock_item_id + ":" + warehouse_location_id + ":" + (stock_lot_id == null ? "none" : stock_lot_id);
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


