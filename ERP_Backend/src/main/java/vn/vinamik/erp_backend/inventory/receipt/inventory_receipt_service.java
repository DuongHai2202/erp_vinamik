package vn.vinamik.erp_backend.inventory.receipt;
import vn.vinamik.erp_backend.platform.common.pagination_guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class inventory_receipt_service {
    private static final Logger logger = LoggerFactory.getLogger(inventory_receipt_service.class);
    private static final int max_page_size = 100;
    private final inventory_receipt_repository receipt_repository;
    private final audit_event_writer audit_writer;

    public inventory_receipt_service(inventory_receipt_repository receipt_repository, audit_event_writer audit_writer) {
        this.receipt_repository = receipt_repository;
        this.audit_writer = audit_writer;
    }

    @Transactional(readOnly = true)
    public receipt_page_response search(String search, Long warehouse_id, String status, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = Math.min(Math.max(page_size, 1), max_page_size);
        String normalized_search = normalize_lower(search);
        String normalized_status = normalize_lower(status);
        if (normalized_status != null && !List.of("draft", "pending", "posted", "cancelled").contains(normalized_status)) {
            throw new IllegalArgumentException("Receipt status is invalid.");
        }
        long total = receipt_repository.count(normalized_search, warehouse_id, normalized_status);
        List<receipt_summary> items = receipt_repository.search(normalized_search, warehouse_id, normalized_status, safe_page_size, pagination_guard.offset(safe_page, safe_page_size));
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / safe_page_size);
        return new receipt_page_response(items, safe_page, safe_page_size, total, total_pages);
    }

    @Transactional(readOnly = true)
    public receipt_response find_by_id(long receipt_id) {
        return receipt_repository.find(receipt_id, false);
    }

    @Transactional
    public receipt_response create(receipt_request request, authenticated_user actor, String correlation_id) {
        validate_request(request);
        String receipt_code = normalize_required(request.receipt_code());
        String idempotency_key = normalize_optional(request.idempotency_key());
        if (idempotency_key != null) {
            List<Long> existing = receipt_repository.find_by_idempotency_key(idempotency_key);
            if (!existing.isEmpty()) {
                return receipt_repository.find(existing.getFirst(), false);
            }
        }
        ensure_unique_receipt_code(receipt_code, null);
        ensure_warehouse_active(request.warehouse_id());
        ensure_supplier_active(request.supplier_id());
        Long receipt_id = receipt_repository.insert_receipt(
                receipt_code, request.warehouse_id(), request.supplier_id(), normalize_optional(request.source_module()),
                request.source_document_id(), normalize_optional(request.reference_number()), idempotency_key,
                normalize_optional(request.notes()), actor.user_id());
        if (receipt_id == null) {
            if (idempotency_key != null) {
                List<Long> existing = receipt_repository.find_by_idempotency_key(idempotency_key);
                if (!existing.isEmpty()) {
                    return receipt_repository.find(existing.getFirst(), false);
                }
            }
            throw new field_conflict_exception("receipt_code", "Receipt code already exists.");
        }
        for (int index = 0; index < request.lines().size(); index++) {
            receipt_line_request line = request.lines().get(index);
            validate_line(request.warehouse_id(), line);
            receipt_repository.insert_line(receipt_id, index + 1, line);
        }
        receipt_response created = receipt_repository.find(receipt_id, false);
        audit_writer.write(actor.user_id(), "inventory", "receipt_create", "receipt", String.valueOf(receipt_id), correlation_id,
                Map.of("receipt_code", created.receipt_code(), "line_count", created.lines().size()));
        logger.info("Đã tạo phiếu nhập kho; receipt_id={}, actor_user_id={}, correlation_id={}", receipt_id, actor.user_id(), correlation_id);
        return created;
    }

    @Transactional
    public receipt_response update(long receipt_id, receipt_request request, authenticated_user actor, String correlation_id) {
        validate_request(request);
        receipt_response current = receipt_repository.find(receipt_id, true);
        if (!current.status().equals("draft")) {
            throw new IllegalArgumentException("Only draft receipts can be edited.");
        }
        String receipt_code = normalize_required(request.receipt_code());
        String idempotency_key = normalize_optional(request.idempotency_key());
        ensure_unique_receipt_code(receipt_code, receipt_id);
        if (idempotency_key != null && receipt_repository.idempotency_key_exists(idempotency_key, receipt_id)) {
            throw new field_conflict_exception("idempotency_key", "Idempotency key already belongs to another receipt.");
        }
        ensure_warehouse_active(request.warehouse_id());
        ensure_supplier_active(request.supplier_id());
        for (receipt_line_request line : request.lines()) {
            validate_line(request.warehouse_id(), line);
        }
        int updated = receipt_repository.update_receipt(receipt_id, receipt_code, request.warehouse_id(), request.supplier_id(),
                normalize_optional(request.source_module()), request.source_document_id(), normalize_optional(request.reference_number()),
                idempotency_key, normalize_optional(request.notes()), actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Draft receipt");
        }
        receipt_repository.delete_lines(receipt_id);
        for (int index = 0; index < request.lines().size(); index++) {
            receipt_repository.insert_line(receipt_id, index + 1, request.lines().get(index));
        }
        receipt_response result = receipt_repository.find(receipt_id, false);
        audit_writer.write(actor.user_id(), "inventory", "receipt_update", "receipt", String.valueOf(receipt_id), correlation_id,
                Map.of("receipt_code", result.receipt_code(), "line_count", result.lines().size()));
        logger.info("Đã cập nhật phiếu nhập kho; receipt_id={}, actor_user_id={}, correlation_id={}", receipt_id, actor.user_id(), correlation_id);
        return result;
    }

    @Transactional
    public void delete(long receipt_id, authenticated_user actor, String correlation_id) {
        receipt_response current = receipt_repository.find(receipt_id, true);
        if (!current.status().equals("draft")) {
            throw new IllegalArgumentException("Only draft receipts can be deleted.");
        }
        receipt_repository.delete_lines(receipt_id);
        if (receipt_repository.delete_draft(receipt_id) == 0) {
            throw new resource_not_found_exception("Draft receipt");
        }
        audit_writer.write(actor.user_id(), "inventory", "receipt_delete", "receipt", String.valueOf(receipt_id), correlation_id,
                Map.of("receipt_code", current.receipt_code()));
        logger.info("Đã xóa bản nháp phiếu nhập kho; receipt_id={}, actor_user_id={}, correlation_id={}", receipt_id, actor.user_id(), correlation_id);
    }

    @Transactional
    public receipt_response cancel(long receipt_id, authenticated_user actor, String correlation_id) {
        receipt_response current = receipt_repository.find(receipt_id, true);
        if (!current.status().equals("draft") && !current.status().equals("pending")) {
            throw new IllegalArgumentException("Only draft or pending receipts can be cancelled.");
        }
        if (receipt_repository.cancel(receipt_id, actor.user_id()) == 0) {
            throw new resource_not_found_exception("Receipt");
        }
        receipt_response result = receipt_repository.find(receipt_id, false);
        audit_writer.write(actor.user_id(), "inventory", "receipt_cancel", "receipt", String.valueOf(receipt_id), correlation_id,
                Map.of("receipt_code", result.receipt_code()));
        logger.info("Đã hủy phiếu nhập kho; receipt_id={}, actor_user_id={}, correlation_id={}", receipt_id, actor.user_id(), correlation_id);
        return result;
    }
    @Transactional
    public receipt_response submit(long receipt_id, authenticated_user actor, String correlation_id) {
        receipt_response current = receipt_repository.find(receipt_id, true);
        if (current.status().equals("pending")) {
            return current;
        }
        if (!current.status().equals("draft")) {
            throw new IllegalArgumentException("Only draft receipts can be submitted.");
        }
        if (current.lines().isEmpty()) {
            throw new IllegalArgumentException("Receipt must contain at least one line before submission.");
        }
        if (receipt_repository.mark_pending(receipt_id, actor.user_id()) == 0) {
            throw new resource_not_found_exception("Draft receipt");
        }
        receipt_response result = receipt_repository.find(receipt_id, false);
        audit_writer.write(actor.user_id(), "inventory", "receipt_submit", "receipt", String.valueOf(receipt_id), correlation_id,
                Map.of("receipt_code", result.receipt_code(), "line_count", result.lines().size()));
        logger.info("Đã gửi duyệt phiếu nhập kho; receipt_id={}, actor_user_id={}, correlation_id={}", receipt_id, actor.user_id(), correlation_id);
        return result;
    }

    @Transactional
    public receipt_response post(long receipt_id, authenticated_user actor, String correlation_id) {
        receipt_response receipt = receipt_repository.find(receipt_id, true);
        if (receipt.status().equals("posted")) {
            return receipt;
        }
        if (!receipt.status().equals("draft") && !receipt.status().equals("pending")) {
            throw new IllegalArgumentException("Only draft or pending receipts can be posted.");
        }
        if (receipt.lines().isEmpty()) {
            throw new IllegalArgumentException("Receipt must contain at least one line before posting.");
        }
        for (receipt_line_response line : receipt.lines()) {
            validate_line(receipt.warehouse_id(), new receipt_line_request(
                    line.stock_item_id(), line.warehouse_location_id(), line.stock_lot_id(), line.quantity()));
            post_line(receipt, line, actor);
        }
        int updated = receipt_repository.mark_posted(receipt_id, actor.user_id());
        if (updated == 0) {
            return receipt_repository.find(receipt_id, false);
        }
        receipt_response posted = receipt_repository.find(receipt_id, false);
        audit_writer.write(actor.user_id(), "inventory", "receipt_post", "receipt", String.valueOf(receipt_id), correlation_id,
                Map.of("receipt_code", posted.receipt_code(), "line_count", posted.lines().size()));
        logger.info("Đã ghi sổ phiếu nhập kho; receipt_id={}, actor_user_id={}, correlation_id={}", receipt_id, actor.user_id(), correlation_id);
        return posted;
    }

    private void post_line(receipt_response receipt, receipt_line_response line, authenticated_user actor) {
        String idempotency_key = "receipt:" + receipt.receipt_id() + ":line:" + line.receipt_line_id();
        if (receipt_repository.movement_exists(receipt.receipt_id(), line.receipt_line_id()) > 0) {
            return;
        }
        receipt_repository.lock_balance(balance_lock_key(line.stock_item_id(), line.warehouse_location_id(), line.stock_lot_id()));
        receipt_repository.insert_movement(receipt, line, actor.user_id(), idempotency_key);
        if (receipt_repository.increment_balance(line) == 0) {
            receipt_repository.insert_balance(line);
        }
    }

    private void validate_request(receipt_request request) {
        if (request == null) {
            throw new IllegalArgumentException("Receipt request is required.");
        }
        String receipt_code = normalize_optional(request.receipt_code());
        if (receipt_code == null || receipt_code.length() > 60) {
            throw new IllegalArgumentException("Receipt code is required and must contain at most 60 characters.");
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
        if (normalize_optional(request.reference_number()) != null
                && normalize_optional(request.reference_number()).length() > 100) {
            throw new IllegalArgumentException("Reference number must contain at most 100 characters.");
        }
        if (normalize_optional(request.idempotency_key()) != null
                && normalize_optional(request.idempotency_key()).length() > 120) {
            throw new IllegalArgumentException("Idempotency key must contain at most 120 characters.");
        }
        if (normalize_optional(request.notes()) != null && normalize_optional(request.notes()).length() > 2000) {
            throw new IllegalArgumentException("Receipt notes must contain at most 2000 characters.");
        }
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException("Receipt must contain at least one line.");
        }
        for (receipt_line_request line : request.lines()) {
            if (line == null || line.stock_item_id() == null || line.stock_item_id() <= 0
                    || line.warehouse_location_id() == null || line.warehouse_location_id() <= 0) {
                throw new IllegalArgumentException("Receipt line is incomplete.");
            }
        }
    }

    private void validate_line(long warehouse_id, receipt_line_request line) {
        if (line == null || line.stock_item_id() == null || line.warehouse_location_id() == null) {
            throw new IllegalArgumentException("Receipt line is incomplete.");
        }
        if (line.quantity() == null || line.quantity().signum() <= 0 || line.quantity().scale() > 6) {
            throw new IllegalArgumentException("Receipt quantity must be greater than zero with at most 6 decimal places.");
        }
        if (!receipt_repository.active_stock_item(line.stock_item_id())) {
            throw new resource_not_found_exception("Active stock item");
        }
        if (!receipt_repository.active_location(line.warehouse_location_id(), warehouse_id)) {
            throw new resource_not_found_exception("Active warehouse location");
        }
        if (line.stock_lot_id() != null && !receipt_repository.active_lot(line.stock_lot_id(), line.stock_item_id())) {
            throw new resource_not_found_exception("Active stock lot");
        }
    }

    private void ensure_warehouse_active(long warehouse_id) {
        if (!receipt_repository.active_warehouse(warehouse_id)) {
            throw new resource_not_found_exception("Active warehouse");
        }
    }

    private void ensure_supplier_active(Long supplier_id) {
        if (supplier_id != null && !receipt_repository.active_supplier(supplier_id)) {
            throw new resource_not_found_exception("Active supplier");
        }
    }

    private void ensure_unique_receipt_code(String receipt_code, Long receipt_id) {
        if (receipt_repository.receipt_code_exists(receipt_code, receipt_id)) {
            throw new field_conflict_exception("receipt_code", "Receipt code already exists.");
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

