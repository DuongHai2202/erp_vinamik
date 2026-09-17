package vn.vinamik.erp_backend.inventory.stocktake;
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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
public class inventory_stocktake_service {
    private static final Logger logger = LoggerFactory.getLogger(inventory_stocktake_service.class);
    private static final int max_page_size = 100;

    private final inventory_stocktake_repository stocktake_repository;
    private final audit_event_writer audit_writer;

    public inventory_stocktake_service(inventory_stocktake_repository stocktake_repository,
                                       audit_event_writer audit_writer) {
        this.stocktake_repository = stocktake_repository;
        this.audit_writer = audit_writer;
    }

    @Transactional(readOnly = true)
    public stocktake_page_response search(String search, Long warehouse_id, String status, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = Math.min(Math.max(page_size, 1), max_page_size);
        String normalized_search = normalize_lower(search);
        String normalized_status = normalize_lower(status);
        if (normalized_status != null
                && !List.of("draft", "counting", "submitted", "approved", "posted", "cancelled")
                .contains(normalized_status)) {
            throw new IllegalArgumentException("Stocktake status is invalid.");
        }
        long total = stocktake_repository.count(normalized_search, warehouse_id, normalized_status);
        List<stocktake_summary> items = stocktake_repository.search(normalized_search, warehouse_id,
                normalized_status, safe_page_size, pagination_guard.offset(safe_page, safe_page_size));
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / safe_page_size);
        return new stocktake_page_response(items, safe_page, safe_page_size, total, total_pages);
    }

    @Transactional(readOnly = true)
    public stocktake_response find_by_id(long stocktake_id) {
        return stocktake_repository.find(stocktake_id, false);
    }

    @Transactional
    public stocktake_response create(stocktake_request request, authenticated_user actor, String correlation_id) {
        validate_request(request);
        String stocktake_code = normalize_required(request.stocktake_code());
        lock_warehouse(request.warehouse_id());
        ensure_warehouse_active(request.warehouse_id());
        ensure_no_active_stocktake(request.warehouse_id());
        long stocktake_id;
        try {
            stocktake_id = stocktake_repository.insert_stocktake(stocktake_code, request.warehouse_id(),
                    normalize_optional(request.notes()), actor.user_id());
        } catch (DuplicateKeyException exception) {
            throw new field_conflict_exception("stocktake_code", "Stocktake code already exists.");
        }
        stocktake_repository.snapshot_balances(stocktake_id, request.warehouse_id());
        stocktake_response created = stocktake_repository.find(stocktake_id, false);
        audit_writer.write(actor.user_id(), "inventory", "stocktake_create", "stocktake",
                String.valueOf(stocktake_id), correlation_id,
                Map.of("stocktake_code", created.stocktake_code(), "line_count", created.lines().size()));
        logger.info("Đã tạo phiên kiểm kê và chụp số tồn hệ thống; stocktake_id={}, warehouse_id={}, line_count={}, actor_user_id={}, correlation_id={}",
                stocktake_id, request.warehouse_id(), created.lines().size(), actor.user_id(), correlation_id);
        return created;
    }

    @Transactional
    public stocktake_line_response count_line(long stocktake_id, long stocktake_line_id,
                                              stocktake_count_request request, authenticated_user actor,
                                              String correlation_id) {
        validate_count_request(request);
        stocktake_response stocktake = stocktake_repository.find(stocktake_id, true);
        if (!stocktake.status().equals("counting")) {
            throw new IllegalArgumentException("Only counting stocktake sessions accept count updates.");
        }
        int updated = stocktake_repository.update_counted_line(stocktake_id, stocktake_line_id,
                request.counted_quantity(), actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Stocktake line");
        }
        stocktake_line_response line = stocktake_repository.find_line(stocktake_id, stocktake_line_id);
        audit_writer.write(actor.user_id(), "inventory", "stocktake_count_line", "stocktake_line",
                String.valueOf(stocktake_line_id), correlation_id,
                Map.of("stocktake_id", stocktake_id, "counted_quantity", request.counted_quantity()));
        logger.info("Đã ghi số đếm kiểm kê; stocktake_id={}, stocktake_line_id={}, actor_user_id={}, correlation_id={}",
                stocktake_id, stocktake_line_id, actor.user_id(), correlation_id);
        return line;
    }

    @Transactional
    public stocktake_response submit(long stocktake_id, authenticated_user actor, String correlation_id) {
        stocktake_response stocktake = stocktake_repository.find(stocktake_id, true);
        if (!stocktake.status().equals("counting")) {
            throw new IllegalArgumentException("Only counting stocktake sessions can be submitted.");
        }
        if (stocktake_repository.count_missing_lines(stocktake_id) > 0) {
            throw new field_conflict_exception("lines", "All stocktake lines must be counted before submission.");
        }
        stocktake_repository.mark_submitted(stocktake_id, actor.user_id());
        stocktake_response submitted = stocktake_repository.find(stocktake_id, false);
        audit_writer.write(actor.user_id(), "inventory", "stocktake_submit", "stocktake",
                String.valueOf(stocktake_id), correlation_id,
                Map.of("stocktake_code", submitted.stocktake_code()));
        logger.info("Đã gửi phiên kiểm kê để duyệt; stocktake_id={}, actor_user_id={}, correlation_id={}",
                stocktake_id, actor.user_id(), correlation_id);
        return submitted;
    }

    @Transactional
    public stocktake_response approve(long stocktake_id, authenticated_user actor, String correlation_id) {
        stocktake_response stocktake = stocktake_repository.find(stocktake_id, true);
        if (!stocktake.status().equals("submitted")) {
            throw new IllegalArgumentException("Only submitted stocktake sessions can be approved.");
        }
        int updated = stocktake_repository.mark_approved(stocktake_id, actor.user_id());
        if (updated == 0) {
            throw new field_conflict_exception("status", "Stocktake session status changed before approval.");
        }
        stocktake_response approved = stocktake_repository.find(stocktake_id, false);
        audit_writer.write(actor.user_id(), "inventory", "stocktake_approve", "stocktake",
                String.valueOf(stocktake_id), correlation_id,
                Map.of("stocktake_code", approved.stocktake_code()));
        logger.info("Đã duyệt phiên kiểm kê; stocktake_id={}, approver_user_id={}, correlation_id={}",
                stocktake_id, actor.user_id(), correlation_id);
        return approved;
    }

    @Transactional
    public stocktake_response post(long stocktake_id, authenticated_user actor, String correlation_id) {
        stocktake_response stocktake = stocktake_repository.find(stocktake_id, true);
        if (stocktake.status().equals("posted")) {
            return stocktake;
        }
        if (!stocktake.status().equals("approved")) {
            throw new IllegalArgumentException("Only approved stocktake sessions can be posted.");
        }
        List<stocktake_line_response> lines = stocktake.lines();
        if (lines.stream().anyMatch(line -> line.counted_quantity() == null)) {
            throw new field_conflict_exception("lines", "All stocktake lines must be counted before posting.");
        }
        Map<stock_balance_key, stock_balance_snapshot> balances = lock_balances(lines);
        Map<stock_balance_key, BigDecimal> adjustments = calculate_adjustments(lines);
        validate_adjustments(adjustments, balances);
        for (stocktake_line_response line : lines) {
            BigDecimal difference = line.counted_quantity().subtract(line.system_quantity());
            if (difference.signum() != 0) {
                post_adjustment(stocktake, line, difference, actor);
            }
        }
        apply_adjustments(adjustments, balances);
        int updated = stocktake_repository.mark_posted(stocktake_id, actor.user_id());
        if (updated == 0) {
            throw new field_conflict_exception("status", "Stocktake session status changed before posting.");
        }
        stocktake_response posted = stocktake_repository.find(stocktake_id, false);
        audit_writer.write(actor.user_id(), "inventory", "stocktake_post", "stocktake",
                String.valueOf(stocktake_id), correlation_id,
                Map.of("stocktake_code", posted.stocktake_code(), "line_count", posted.lines().size()));
        logger.info("Đã ghi sổ điều chỉnh kiểm kê; stocktake_id={}, actor_user_id={}, correlation_id={}",
                stocktake_id, actor.user_id(), correlation_id);
        return posted;
    }

    @Transactional
    public stocktake_response cancel(long stocktake_id, authenticated_user actor, String correlation_id) {
        stocktake_response stocktake = stocktake_repository.find(stocktake_id, true);
        if (!List.of("draft", "counting", "submitted").contains(stocktake.status())) {
            throw new IllegalArgumentException("Only draft, counting or submitted stocktake sessions can be cancelled.");
        }
        stocktake_repository.mark_cancelled(stocktake_id, actor.user_id());
        stocktake_response cancelled = stocktake_repository.find(stocktake_id, false);
        audit_writer.write(actor.user_id(), "inventory", "stocktake_cancel", "stocktake",
                String.valueOf(stocktake_id), correlation_id,
                Map.of("stocktake_code", cancelled.stocktake_code()));
        logger.info("Đã hủy phiên kiểm kê; stocktake_id={}, actor_user_id={}, correlation_id={}",
                stocktake_id, actor.user_id(), correlation_id);
        return cancelled;
    }

    private Map<stock_balance_key, stock_balance_snapshot> lock_balances(List<stocktake_line_response> lines) {
        Map<stock_balance_key, stock_balance_snapshot> balances = new LinkedHashMap<>();
        TreeMap<stock_balance_key, Boolean> keys = new TreeMap<>(stock_balance_key.order);
        for (stocktake_line_response line : lines) {
            keys.put(new stock_balance_key(line.stock_item_id(), line.warehouse_location_id(), line.stock_lot_id()), true);
        }
        for (stock_balance_key key : keys.keySet()) {
            stocktake_repository.lock_balance(key.to_lock_key());
            Optional<BigDecimal> current = stocktake_repository.find_balance(
                    key.stock_item_id(), key.location_id(), key.stock_lot_id());
            balances.put(key, new stock_balance_snapshot(current.isPresent(),
                    current.orElse(BigDecimal.ZERO)));
        }
        return balances;
    }

    private Map<stock_balance_key, BigDecimal> calculate_adjustments(List<stocktake_line_response> lines) {
        Map<stock_balance_key, BigDecimal> adjustments = new TreeMap<>(stock_balance_key.order);
        for (stocktake_line_response line : lines) {
            stock_balance_key key = new stock_balance_key(line.stock_item_id(), line.warehouse_location_id(),
                    line.stock_lot_id());
            adjustments.merge(key, line.counted_quantity().subtract(line.system_quantity()), BigDecimal::add);
        }
        return adjustments;
    }

    private void validate_adjustments(Map<stock_balance_key, BigDecimal> adjustments,
                                      Map<stock_balance_key, stock_balance_snapshot> balances) {
        for (Map.Entry<stock_balance_key, BigDecimal> entry : adjustments.entrySet()) {
            BigDecimal resulting = balances.get(entry.getKey()).quantity().add(entry.getValue());
            if (resulting.signum() < 0) {
                throw new field_conflict_exception("lines", "Stocktake adjustment cannot result in negative stock.");
            }
        }
    }

    private void post_adjustment(stocktake_response stocktake, stocktake_line_response line,
                                 BigDecimal difference, authenticated_user actor) {
        String idempotency_key = "stocktake:" + stocktake.stocktake_id() + ":line:" + line.stocktake_line_id() + ":adjustment";
        stocktake_repository.insert_movement(stocktake.stocktake_id(), line.stocktake_line_id(),
                line.stock_item_id(), line.warehouse_location_id(), line.stock_lot_id(),
                difference, actor.user_id(), idempotency_key);
    }

    private void apply_adjustments(Map<stock_balance_key, BigDecimal> adjustments,
                                   Map<stock_balance_key, stock_balance_snapshot> balances) {
        for (Map.Entry<stock_balance_key, BigDecimal> entry : adjustments.entrySet()) {
            stock_balance_key key = entry.getKey();
            BigDecimal difference = entry.getValue();
            if (difference.signum() == 0) {
                continue;
            }
            if (balances.get(key).exists()) {
                int updated = stocktake_repository.update_balance(key.stock_item_id(), key.location_id(),
                        key.stock_lot_id(), difference);
                if (updated != 1) {
                    throw new IllegalStateException("Locked stock balance could not be updated.");
                }
            } else {
                stocktake_repository.insert_balance(key.stock_item_id(), key.location_id(),
                        key.stock_lot_id(), difference);
            }
        }
    }

    private void validate_request(stocktake_request request) {
        if (request == null) {
            throw new IllegalArgumentException("Stocktake request is required.");
        }
        String stocktake_code = normalize_optional(request.stocktake_code());
        if (stocktake_code == null || stocktake_code.length() > 60) {
            throw new IllegalArgumentException("Stocktake code is required and must contain at most 60 characters.");
        }
        if (request.warehouse_id() == null || request.warehouse_id() <= 0) {
            throw new IllegalArgumentException("Stocktake warehouse is required.");
        }
        if (normalize_optional(request.notes()) != null && normalize_optional(request.notes()).length() > 2000) {
            throw new IllegalArgumentException("Stocktake notes must contain at most 2000 characters.");
        }
    }

    private void validate_count_request(stocktake_count_request request) {
        if (request == null || request.counted_quantity() == null || request.counted_quantity().signum() < 0
                || request.counted_quantity().scale() > 6) {
            throw new IllegalArgumentException("Counted quantity must be zero or greater with at most 6 decimal places.");
        }
    }

    private void lock_warehouse(long warehouse_id) {
        if (!stocktake_repository.warehouse_exists_for_update(warehouse_id)) {
            throw new resource_not_found_exception("Warehouse");
        }
    }

    private void ensure_warehouse_active(long warehouse_id) {
        if (!stocktake_repository.active_warehouse(warehouse_id)) {
            throw new resource_not_found_exception("Active warehouse");
        }
    }

    private void ensure_no_active_stocktake(long warehouse_id) {
        if (stocktake_repository.has_active_stocktake(warehouse_id)) {
            throw new field_conflict_exception("warehouse_id", "An active stocktake already exists for this warehouse.");
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

    private record stock_balance_key(long stock_item_id, long location_id, Long stock_lot_id) {
        private static final java.util.Comparator<stock_balance_key> order =
                java.util.Comparator.comparingLong(stock_balance_key::stock_item_id)
                        .thenComparingLong(stock_balance_key::location_id)
                        .thenComparing(stock_balance_key::stock_lot_id, java.util.Comparator.nullsFirst(Long::compareTo));

        private String to_lock_key() {
            return stock_item_id + ":" + location_id + ":" + (stock_lot_id == null ? "none" : stock_lot_id);
        }
    }

    private record stock_balance_snapshot(boolean exists, BigDecimal quantity) {
    }
}
