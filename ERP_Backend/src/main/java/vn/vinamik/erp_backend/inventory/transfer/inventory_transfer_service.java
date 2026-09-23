package vn.vinamik.erp_backend.inventory.transfer;
import vn.vinamik.erp_backend.platform.common.pagination_guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

@Service
public class inventory_transfer_service {
    private static final Logger logger = LoggerFactory.getLogger(inventory_transfer_service.class);
    private static final int max_page_size = 100;
    private static final java.util.Comparator<balance_key> balance_key_order =
            java.util.Comparator.comparingLong(balance_key::stock_item_id)
                    .thenComparingLong(balance_key::location_id)
                    .thenComparing(balance_key::stock_lot_id, java.util.Comparator.nullsFirst(Long::compareTo));

    private final inventory_transfer_repository transfer_repository;
    private final audit_event_writer audit_writer;

    public inventory_transfer_service(inventory_transfer_repository transfer_repository, audit_event_writer audit_writer) {
        this.transfer_repository = transfer_repository;
        this.audit_writer = audit_writer;
    }

    @Transactional(readOnly = true)
    public transfer_page_response search(String search, Long source_warehouse_id, Long destination_warehouse_id,
                                         String status, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = Math.min(Math.max(page_size, 1), max_page_size);
        String normalized_search = normalize_lower(search);
        String normalized_status = normalize_lower(status);
        if (normalized_status != null
                && !List.of("draft", "pending", "posted", "cancelled").contains(normalized_status)) {
            throw new IllegalArgumentException("Transfer status is invalid.");
        }
        long total = transfer_repository.count(normalized_search, source_warehouse_id,
                destination_warehouse_id, normalized_status);
        List<transfer_summary> items = transfer_repository.search(normalized_search, source_warehouse_id,
                destination_warehouse_id, normalized_status, safe_page_size, pagination_guard.offset(safe_page, safe_page_size));
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / safe_page_size);
        return new transfer_page_response(items, safe_page, safe_page_size, total, total_pages);
    }

    @Transactional(readOnly = true)
    public transfer_response find_by_id(long transfer_id) {
        return transfer_repository.find(transfer_id, false);
    }

    @Transactional
    public transfer_response create(transfer_request request, authenticated_user actor, String correlation_id) {
        validate_request(request);
        String transfer_code = normalize_required(request.transfer_code());
        String idempotency_key = normalize_optional(request.idempotency_key());
        if (idempotency_key != null) {
            List<Long> existing = transfer_repository.find_by_idempotency_key(idempotency_key);
            if (!existing.isEmpty()) {
                return transfer_repository.find(existing.getFirst(), false);
            }
        }
        ensure_unique_transfer_code(transfer_code, null);
        ensure_warehouse_active(request.source_warehouse_id());
        ensure_warehouse_active(request.destination_warehouse_id());
        Long transfer_id = transfer_repository.insert_transfer(transfer_code, request.source_warehouse_id(),
                request.destination_warehouse_id(), idempotency_key, normalize_optional(request.notes()),
                actor.user_id());
        if (transfer_id == null) {
            if (idempotency_key != null) {
                List<Long> existing = transfer_repository.find_by_idempotency_key(idempotency_key);
                if (!existing.isEmpty()) {
                    return transfer_repository.find(existing.getFirst(), false);
                }
            }
            throw new field_conflict_exception("transfer_code", "Transfer code or idempotency key already exists.");
        }
        for (int index = 0; index < request.lines().size(); index++) {
            transfer_line_request line = request.lines().get(index);
            validate_line(request.source_warehouse_id(), request.destination_warehouse_id(), line);
            transfer_repository.insert_line(transfer_id, index + 1, line);
        }
        transfer_response created = transfer_repository.find(transfer_id, false);
        audit_writer.write(actor.user_id(), "inventory", "transfer_create", "transfer", String.valueOf(transfer_id),
                correlation_id, Map.of("transfer_code", created.transfer_code(), "line_count", created.lines().size()));
        logger.info("Đã tạo phiếu điều chuyển kho; transfer_id={}, actor_user_id={}, correlation_id={}",
                transfer_id, actor.user_id(), correlation_id);
        return created;
    }

    @Transactional
    public transfer_response update(long transfer_id, transfer_request request, authenticated_user actor, String correlation_id) {
        validate_request(request);
        transfer_response current = transfer_repository.find(transfer_id, true);
        if (!current.status().equals("draft")) {
            throw new IllegalArgumentException("Only draft transfers can be edited.");
        }
        String transfer_code = normalize_required(request.transfer_code());
        String idempotency_key = normalize_optional(request.idempotency_key());
        ensure_unique_transfer_code(transfer_code, transfer_id);
        if (idempotency_key != null && transfer_repository.idempotency_key_exists(idempotency_key, transfer_id)) {
            throw new field_conflict_exception("idempotency_key", "Idempotency key already belongs to another transfer.");
        }
        ensure_warehouse_active(request.source_warehouse_id());
        ensure_warehouse_active(request.destination_warehouse_id());
        for (transfer_line_request line : request.lines()) {
            validate_line(request.source_warehouse_id(), request.destination_warehouse_id(), line);
        }
        int updated = transfer_repository.update_transfer(transfer_id, transfer_code, request.source_warehouse_id(),
                request.destination_warehouse_id(), idempotency_key, normalize_optional(request.notes()), actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Draft transfer");
        }
        transfer_repository.delete_lines(transfer_id);
        for (int index = 0; index < request.lines().size(); index++) {
            transfer_repository.insert_line(transfer_id, index + 1, request.lines().get(index));
        }
        transfer_response result = transfer_repository.find(transfer_id, false);
        audit_writer.write(actor.user_id(), "inventory", "transfer_update", "transfer", String.valueOf(transfer_id), correlation_id,
                Map.of("transfer_code", result.transfer_code(), "line_count", result.lines().size()));
        logger.info("Đã cập nhật phiếu điều chuyển kho; transfer_id={}, actor_user_id={}, correlation_id={}", transfer_id, actor.user_id(), correlation_id);
        return result;
    }

    @Transactional
    public void delete(long transfer_id, authenticated_user actor, String correlation_id) {
        transfer_response current = transfer_repository.find(transfer_id, true);
        if (!current.status().equals("draft")) {
            throw new IllegalArgumentException("Only draft transfers can be deleted.");
        }
        transfer_repository.delete_lines(transfer_id);
        if (transfer_repository.delete_draft(transfer_id) == 0) {
            throw new resource_not_found_exception("Draft transfer");
        }
        audit_writer.write(actor.user_id(), "inventory", "transfer_delete", "transfer", String.valueOf(transfer_id), correlation_id,
                Map.of("transfer_code", current.transfer_code()));
        logger.info("Đã xóa bản nháp phiếu điều chuyển kho; transfer_id={}, actor_user_id={}, correlation_id={}", transfer_id, actor.user_id(), correlation_id);
    }

    @Transactional
    public transfer_response cancel(long transfer_id, authenticated_user actor, String correlation_id) {
        transfer_response current = transfer_repository.find(transfer_id, true);
        if (!current.status().equals("draft") && !current.status().equals("pending")) {
            throw new IllegalArgumentException("Only draft or pending transfers can be cancelled.");
        }
        if (transfer_repository.cancel(transfer_id, actor.user_id()) == 0) {
            throw new resource_not_found_exception("Transfer");
        }
        transfer_response result = transfer_repository.find(transfer_id, false);
        audit_writer.write(actor.user_id(), "inventory", "transfer_cancel", "transfer", String.valueOf(transfer_id), correlation_id,
                Map.of("transfer_code", result.transfer_code()));
        logger.info("Đã hủy phiếu điều chuyển kho; transfer_id={}, actor_user_id={}, correlation_id={}", transfer_id, actor.user_id(), correlation_id);
        return result;
    }
    @Transactional
    public transfer_response submit(long transfer_id, authenticated_user actor, String correlation_id) {
        transfer_response current = transfer_repository.find(transfer_id, true);
        if (current.status().equals("pending")) {
            return current;
        }
        if (!current.status().equals("draft")) {
            throw new IllegalArgumentException("Only draft transfers can be submitted.");
        }
        if (current.lines().isEmpty()) {
            throw new IllegalArgumentException("Transfer must contain at least one line before submission.");
        }
        if (transfer_repository.mark_pending(transfer_id, actor.user_id()) == 0) {
            throw new resource_not_found_exception("Draft transfer");
        }
        transfer_response result = transfer_repository.find(transfer_id, false);
        audit_writer.write(actor.user_id(), "inventory", "transfer_submit", "transfer", String.valueOf(transfer_id), correlation_id,
                Map.of("transfer_code", result.transfer_code(), "line_count", result.lines().size()));
        logger.info("Đã gửi duyệt phiếu điều chuyển kho; transfer_id={}, actor_user_id={}, correlation_id={}", transfer_id, actor.user_id(), correlation_id);
        return result;
    }

    @Transactional
    public transfer_response post(long transfer_id, authenticated_user actor, String correlation_id) {
        transfer_response transfer = transfer_repository.find(transfer_id, true);
        if (transfer.status().equals("posted")) {
            return transfer;
        }
        if (!transfer.status().equals("draft") && !transfer.status().equals("pending")) {
            throw new IllegalArgumentException("Only draft or pending transfers can be posted.");
        }
        if (transfer.lines().isEmpty()) {
            throw new IllegalArgumentException("Transfer must contain at least one line before posting.");
        }
        NavigableMap<balance_key, BigDecimal> deltas = new TreeMap<>(balance_key_order);
        for (transfer_line_response line : transfer.lines()) {
            validate_line(transfer.source_warehouse_id(), transfer.destination_warehouse_id(),
                    new transfer_line_request(line.stock_item_id(), line.stock_lot_id(), line.source_location_id(),
                            line.destination_location_id(), line.quantity()));
            deltas.merge(new balance_key(line.stock_item_id(), line.source_location_id(), line.stock_lot_id()),
                    line.quantity().negate(), BigDecimal::add);
            deltas.merge(new balance_key(line.stock_item_id(), line.destination_location_id(), line.stock_lot_id()),
                    line.quantity(), BigDecimal::add);
        }
        Map<balance_key, stock_balance_snapshot> balances = lock_balances(deltas);
        ensure_non_negative_balances(deltas, balances);
        for (transfer_line_response line : transfer.lines()) {
            post_line(transfer, line, actor);
        }
        apply_deltas(deltas, balances);
        int updated = transfer_repository.mark_posted(transfer_id, actor.user_id());
        if (updated == 0) {
            return transfer_repository.find(transfer_id, false);
        }
        transfer_response posted = transfer_repository.find(transfer_id, false);
        audit_writer.write(actor.user_id(), "inventory", "transfer_post", "transfer", String.valueOf(transfer_id),
                correlation_id, Map.of("transfer_code", posted.transfer_code(), "line_count", posted.lines().size()));
        logger.info("Đã ghi sổ phiếu điều chuyển kho; transfer_id={}, actor_user_id={}, correlation_id={}",
                transfer_id, actor.user_id(), correlation_id);
        return posted;
    }

    private Map<balance_key, stock_balance_snapshot> lock_balances(NavigableMap<balance_key, BigDecimal> deltas) {
        Map<balance_key, stock_balance_snapshot> balances = new java.util.LinkedHashMap<>();
        for (balance_key key : deltas.keySet()) {
            transfer_repository.lock_balance(key.to_lock_key());
            Optional<BigDecimal> current = transfer_repository.find_balance(
                    key.stock_item_id(), key.location_id(), key.stock_lot_id());
            balances.put(key, new stock_balance_snapshot(current.isPresent(),
                    current.orElse(BigDecimal.ZERO)));
        }
        return balances;
    }

    private void ensure_non_negative_balances(NavigableMap<balance_key, BigDecimal> deltas,
                                              Map<balance_key, stock_balance_snapshot> balances) {
        for (Map.Entry<balance_key, BigDecimal> entry : deltas.entrySet()) {
            BigDecimal current_quantity = balances.get(entry.getKey()).quantity();
            BigDecimal resulting_quantity = current_quantity.add(entry.getValue());
            if (resulting_quantity.signum() < 0) {
                throw new field_conflict_exception("lines", "Insufficient stock quantity.");
            }
        }
    }

    private void post_line(transfer_response transfer, transfer_line_response line, authenticated_user actor) {
        String source_idempotency_key = "transfer:" + transfer.transfer_id() + ":line:" + line.transfer_line_id() + ":source";
        String destination_idempotency_key = "transfer:" + transfer.transfer_id() + ":line:" + line.transfer_line_id() + ":destination";
        transfer_repository.insert_movement(transfer.transfer_id(), line.transfer_line_id(), line.stock_item_id(),
                line.source_location_id(), line.stock_lot_id(), line.quantity().negate(),
                actor.user_id(), source_idempotency_key);
        transfer_repository.insert_movement(transfer.transfer_id(), line.transfer_line_id(), line.stock_item_id(),
                line.destination_location_id(), line.stock_lot_id(), line.quantity(),
                actor.user_id(), destination_idempotency_key);
    }

    private void apply_deltas(NavigableMap<balance_key, BigDecimal> deltas,
                              Map<balance_key, stock_balance_snapshot> balances) {
        for (Map.Entry<balance_key, BigDecimal> entry : deltas.entrySet()) {
            balance_key key = entry.getKey();
            BigDecimal delta = entry.getValue();
            if (delta.signum() == 0) {
                continue;
            }
            if (balances.get(key).exists()) {
                int updated = transfer_repository.update_balance(key.stock_item_id(), key.location_id(),
                        key.stock_lot_id(), delta);
                if (updated != 1) {
                    throw new IllegalStateException("Locked stock balance could not be updated.");
                }
            } else {
                transfer_repository.insert_balance(key.stock_item_id(), key.location_id(),
                        key.stock_lot_id(), delta);
            }
        }
    }

    private void validate_request(transfer_request request) {
        if (request == null) {
            throw new IllegalArgumentException("Transfer request is required.");
        }
        String transfer_code = normalize_optional(request.transfer_code());
        if (transfer_code == null || transfer_code.length() > 60) {
            throw new IllegalArgumentException("Transfer code is required and must contain at most 60 characters.");
        }
        if (request.source_warehouse_id() == null || request.source_warehouse_id() <= 0
                || request.destination_warehouse_id() == null || request.destination_warehouse_id() <= 0) {
            throw new IllegalArgumentException("Source and destination warehouses are required.");
        }
        if (request.source_warehouse_id().equals(request.destination_warehouse_id())) {
            throw new IllegalArgumentException("Source and destination warehouses must be different.");
        }
        if (normalize_optional(request.idempotency_key()) != null
                && normalize_optional(request.idempotency_key()).length() > 120) {
            throw new IllegalArgumentException("Idempotency key must contain at most 120 characters.");
        }
        if (normalize_optional(request.notes()) != null && normalize_optional(request.notes()).length() > 2000) {
            throw new IllegalArgumentException("Transfer notes must contain at most 2000 characters.");
        }
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException("Transfer must contain at least one line.");
        }
        for (transfer_line_request line : request.lines()) {
            if (line == null || line.stock_item_id() == null || line.source_location_id() == null
                    || line.destination_location_id() == null || line.quantity() == null) {
                throw new IllegalArgumentException("Transfer line is incomplete.");
            }
            validate_line_values(line);
        }
    }

    private void validate_line(long source_warehouse_id, long destination_warehouse_id, transfer_line_request line) {
        validate_line_values(line);
        if (!transfer_repository.active_stock_item(line.stock_item_id())) {
            throw new resource_not_found_exception("Active stock item");
        }
        if (!transfer_repository.active_location(line.source_location_id(), source_warehouse_id)) {
            throw new resource_not_found_exception("Active source warehouse location");
        }
        if (!transfer_repository.active_location(line.destination_location_id(), destination_warehouse_id)) {
            throw new resource_not_found_exception("Active destination warehouse location");
        }
        if (line.stock_lot_id() != null && !transfer_repository.active_lot(line.stock_lot_id(), line.stock_item_id())) {
            throw new resource_not_found_exception("Active stock lot");
        }
    }

    private void validate_line_values(transfer_line_request line) {
        if (line.stock_item_id() == null || line.source_location_id() == null
                || line.destination_location_id() == null || line.quantity() == null) {
            throw new IllegalArgumentException("Transfer line is incomplete.");
        }
        if (line.quantity().signum() <= 0 || line.quantity().scale() > 6) {
            throw new IllegalArgumentException("Transfer quantity must be greater than zero with at most 6 decimal places.");
        }
        if (line.source_location_id().equals(line.destination_location_id())) {
            throw new IllegalArgumentException("Source and destination locations must be different.");
        }
    }

    private void ensure_warehouse_active(long warehouse_id) {
        if (!transfer_repository.active_warehouse(warehouse_id)) {
            throw new resource_not_found_exception("Active warehouse");
        }
    }

    private void ensure_unique_transfer_code(String transfer_code, Long transfer_id) {
        if (transfer_repository.transfer_code_exists(transfer_code, transfer_id)) {
            throw new field_conflict_exception("transfer_code", "Transfer code already exists.");
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

    private record balance_key(long stock_item_id, long location_id, Long stock_lot_id) {
        private String to_lock_key() {
            return stock_item_id + ":" + location_id + ":" + (stock_lot_id == null ? "none" : stock_lot_id);
        }
    }

    private record stock_balance_snapshot(boolean exists, BigDecimal quantity) {
    }
}
