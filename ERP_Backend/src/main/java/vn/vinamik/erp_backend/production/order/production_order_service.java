package vn.vinamik.erp_backend.production.order;
import vn.vinamik.erp_backend.platform.common.pagination_guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.inventory.api.inventory_material_contract;
import vn.vinamik.erp_backend.inventory.api.inventory_material_snapshot;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class production_order_service {
    private static final Logger logger = LoggerFactory.getLogger(production_order_service.class);
    private static final int max_page_size = 100;
    private static final int quantity_scale = 6;
    private static final List<String> valid_statuses = List.of(
            "draft", "planned", "released", "in_progress", "paused", "completed", "cancelled");

    private final production_order_repository order_repository;
    private final inventory_material_contract material_contract;
    private final audit_event_writer audit_writer;

    public production_order_service(
            production_order_repository order_repository,
            inventory_material_contract material_contract,
            audit_event_writer audit_writer) {
        this.order_repository = order_repository;
        this.material_contract = material_contract;
        this.audit_writer = audit_writer;
    }

    @Transactional(readOnly = true)
    public production_order_page_response search(String search, String status, Long stock_item_id,
                                                 int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = Math.min(Math.max(page_size, 1), max_page_size);
        String normalized_search = normalize_lower(search);
        String normalized_status = normalize_lower(status);
        validate_status_filter(normalized_status);
        long total = order_repository.count(normalized_search, normalized_status, stock_item_id);
        List<production_order_summary> items = order_repository.search(
                normalized_search, normalized_status, stock_item_id,
                safe_page_size, pagination_guard.offset(safe_page, safe_page_size));
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / safe_page_size);
        return new production_order_page_response(items, safe_page, safe_page_size, total, total_pages);
    }

    @Transactional(readOnly = true)
    public long count_overdue() {
        return order_repository.count_overdue();
    }
    @Transactional(readOnly = true)
    public production_order_response find_by_id(long production_order_id) {
        return order_repository.find_by_id(production_order_id);
    }

    @Transactional
    public production_order_response create(production_order_request request, authenticated_user actor,
                                            String correlation_id) {
        validate_request(request);
        String order_code = normalize_required(request.order_code());
        ensure_unique_order_code(order_code, null);
        production_order_repository.plan_line_snapshot plan_line =
                order_repository.lock_plan_line(request.production_plan_line_id());
        validate_plan_for_order(plan_line);
        inventory_material_snapshot product = require_finished_product(plan_line.stock_item_id());
        validate_dates_against_plan(request, plan_line);
        production_order_repository.bom_snapshot bom =
                order_repository.load_active_bom(request.bom_id(), product.stock_item_id(),
                        request.planned_starts_on());
        ensure_target_within_plan(plan_line, request.target_quantity(), null);
        ensure_production_line_available(request, null);
        long order_id;
        try {
            order_id = order_repository.insert_order(
                    order_code, plan_line.production_plan_line_id(), product.stock_item_id(), bom.bom_id(),
                    normalize_quantity(request.target_quantity()), product.unit_code(),
                    request.planned_starts_on(), request.planned_ends_on(),
                    normalize_optional(request.production_line_name()), normalize_optional(request.notes()),
                    actor.user_id());
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("order_code", "Production order code already exists.");
        }
        insert_material_requirements(order_id, bom, request.target_quantity());
        production_order_response created = order_repository.find_by_id(order_id);
        audit_writer.write(actor.user_id(), "production", "production_order_create", "production_order",
                String.valueOf(order_id), correlation_id, Map.of("order_code", created.order_code()));
        logger.info("Đã tạo lệnh sản xuất; production_order_id={}, actor_user_id={}, correlation_id={}",
                order_id, actor.user_id(), correlation_id);
        return created;
    }

    @Transactional
    public production_order_response update(long production_order_id, production_order_request request,
                                            authenticated_user actor, String correlation_id) {
        validate_request(request);
        String current = order_repository.current_status(production_order_id);
        if (!Set.of("draft", "planned").contains(current)) {
            throw new IllegalArgumentException("Only draft or planned production orders can be edited.");
        }
        String order_code = normalize_required(request.order_code());
        ensure_unique_order_code(order_code, production_order_id);
        production_order_repository.plan_line_snapshot plan_line =
                order_repository.lock_plan_line(request.production_plan_line_id());
        validate_plan_for_order(plan_line);
        inventory_material_snapshot product = require_finished_product(plan_line.stock_item_id());
        validate_dates_against_plan(request, plan_line);
        production_order_repository.bom_snapshot bom =
                order_repository.load_active_bom(request.bom_id(), product.stock_item_id(),
                        request.planned_starts_on());
        ensure_target_within_plan(plan_line, request.target_quantity(), production_order_id);
        ensure_production_line_available(request, production_order_id);
        int updated = order_repository.update_order(
                production_order_id, order_code, plan_line.production_plan_line_id(), product.stock_item_id(),
                bom.bom_id(), normalize_quantity(request.target_quantity()), product.unit_code(),
                request.planned_starts_on(), request.planned_ends_on(),
                normalize_optional(request.production_line_name()), normalize_optional(request.notes()),
                actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Editable production order");
        }
        order_repository.delete_requirements(production_order_id);
        insert_material_requirements(production_order_id, bom, request.target_quantity());
        production_order_response changed = order_repository.find_by_id(production_order_id);
        audit_writer.write(actor.user_id(), "production", "production_order_update", "production_order",
                String.valueOf(production_order_id), correlation_id, Map.of("order_code", changed.order_code()));
        logger.info("Đã cập nhật lệnh sản xuất; production_order_id={}, actor_user_id={}, correlation_id={}",
                production_order_id, actor.user_id(), correlation_id);
        return changed;
    }

    @Transactional
    public production_order_response change_status(long production_order_id, String requested_status,
                                                   authenticated_user actor, String correlation_id) {
        String target = normalize_lower(requested_status);
        validate_status(target);
        String current = order_repository.current_status(production_order_id);
        if (!allowed_transition(current, target)) {
            throw new IllegalArgumentException("The production order status transition is not allowed.");
        }
        int updated = order_repository.change_status(production_order_id, current, target, actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Production order");
        }
        production_order_response changed = order_repository.find_by_id(production_order_id);
        audit_writer.write(actor.user_id(), "production", "production_order_status_change", "production_order",
                String.valueOf(production_order_id), correlation_id,
                Map.of("from_status", current, "to_status", target));
        logger.info("Đã chuyển trạng thái lệnh sản xuất; production_order_id={}, from_status={}, to_status={}, actor_user_id={}, correlation_id={}",
                production_order_id, current, target, actor.user_id(), correlation_id);
        return changed;
    }

    @Transactional
    public production_order_response release(long production_order_id, authenticated_user actor, String correlation_id) {
        return change_status(production_order_id, "released", actor, correlation_id);
    }

    @Transactional
    public production_order_response change_operational_status(long production_order_id, String requested_status,
                                                               authenticated_user actor, String correlation_id) {
        String target = normalize_lower(requested_status);
        if (!List.of("in_progress", "paused", "cancelled").contains(target)) {
            throw new IllegalArgumentException("Only operational production order statuses can be changed with this action.");
        }
        return change_status(production_order_id, target, actor, correlation_id);
    }

    private void insert_material_requirements(long order_id,
                                              production_order_repository.bom_snapshot bom,
                                              BigDecimal target_quantity) {
        BigDecimal base_multiplier = target_quantity.divide(
                bom.base_quantity(), quantity_scale + 8, RoundingMode.HALF_UP);
        List<production_order_repository.order_requirement_insert> requirements =
                bom.lines().stream().map(line -> {
                    BigDecimal scrap_multiplier = BigDecimal.ONE.add(
                            line.scrap_percent().divide(new BigDecimal("100"), quantity_scale + 8, RoundingMode.HALF_UP));
                    BigDecimal required_quantity = normalize_quantity(
                            line.quantity_per_base().multiply(base_multiplier).multiply(scrap_multiplier));
                    return new production_order_repository.order_requirement_insert(
                            line.bom_line_id(), line.material_stock_item_id(), line.unit_code_snapshot(),
                            line.quantity_per_base(), line.scrap_percent(), required_quantity);
                }).toList();
        order_repository.insert_requirements(order_id, bom.bom_id(), requirements);
    }

    private void ensure_target_within_plan(production_order_repository.plan_line_snapshot plan_line,
                                           BigDecimal target_quantity, Long excluded_order_id) {
        BigDecimal existing = order_repository.sum_target_quantity(
                plan_line.production_plan_line_id(), excluded_order_id);
        if (existing.add(target_quantity).compareTo(plan_line.target_quantity()) > 0) {
            throw new field_conflict_exception(
                    "target_quantity", "Production order quantity exceeds the production plan line quantity.");
        }
    }

    private void ensure_production_line_available(production_order_request request, Long excluded_order_id) {
        String line_name = normalize_optional(request.production_line_name());
        if (line_name == null) {
            return;
        }
        order_repository.lock_production_line_schedule(line_name);
        if (order_repository.production_line_overlaps(
                line_name, request.planned_starts_on(), request.planned_ends_on(), excluded_order_id)) {
            throw new field_conflict_exception(
                    "production_line_name", "The production line schedule overlaps another order.");
        }
    }

    private void validate_plan_for_order(production_order_repository.plan_line_snapshot plan_line) {
        if ("cancelled".equals(plan_line.plan_status()) || "completed".equals(plan_line.plan_status())) {
            throw new IllegalArgumentException(
                    "Production orders cannot be created from a cancelled or completed plan.");
        }
    }

    private void validate_dates_against_plan(production_order_request request,
                                             production_order_repository.plan_line_snapshot plan_line) {
        if (request.planned_ends_on().isBefore(request.planned_starts_on())) {
            throw new IllegalArgumentException("Planned end date cannot be before planned start date.");
        }
        if (request.planned_starts_on().isBefore(plan_line.plan_starts_on())
                || request.planned_ends_on().isAfter(plan_line.plan_ends_on())) {
            throw new IllegalArgumentException("Production order dates must stay within the production plan dates.");
        }
    }

    private inventory_material_snapshot require_finished_product(long stock_item_id) {
        inventory_material_snapshot product = material_contract.find_active_stock_item(stock_item_id)
                .orElseThrow(() -> new resource_not_found_exception("Active finished product"));
        if (!"finished_product".equals(product.item_type())) {
            throw new IllegalArgumentException("Production orders must reference a finished product.");
        }
        return product;
    }

    private void validate_status_filter(String status) {
        if (status != null && !valid_statuses.contains(status)) {
            throw new IllegalArgumentException("Production order status is invalid.");
        }
    }

    private void validate_status(String status) {
        if (status == null || !valid_statuses.contains(status)) {
            throw new IllegalArgumentException("Production order status is invalid.");
        }
    }

    private boolean allowed_transition(String current, String target) {
        return switch (current) {
            case "planned" -> target.equals("released") || target.equals("cancelled");
            case "released" -> target.equals("in_progress") || target.equals("cancelled");
            case "in_progress" -> target.equals("paused") || target.equals("completed");
            case "paused" -> target.equals("in_progress") || target.equals("completed");
            default -> false;
        };
    }

    private void ensure_unique_order_code(String order_code, Long production_order_id) {
        if (order_repository.order_code_exists(order_code, production_order_id)) {
            throw new field_conflict_exception("order_code", "Production order code already exists.");
        }
    }

    private void validate_request(production_order_request request) {
        if (request == null) {
            throw new IllegalArgumentException("Production order request is required.");
        }
        if (normalize_optional(request.order_code()) == null
                || normalize_optional(request.order_code()).length() > 60) {
            throw new IllegalArgumentException("Production order code is required and must contain at most 60 characters.");
        }
        if (request.production_plan_line_id() == null || request.production_plan_line_id() <= 0) {
            throw new IllegalArgumentException("Production plan line is required.");
        }
        if (request.bom_id() == null || request.bom_id() <= 0) {
            throw new IllegalArgumentException("BOM is required.");
        }
        if (request.target_quantity() == null || request.target_quantity().signum() <= 0
                || request.target_quantity().scale() > quantity_scale) {
            throw new IllegalArgumentException(
                    "Target quantity must be greater than zero with at most 6 decimal places.");
        }
        if (request.planned_starts_on() == null || request.planned_ends_on() == null
                || request.planned_ends_on().isBefore(request.planned_starts_on())) {
            throw new IllegalArgumentException("Planned end date cannot be before planned start date.");
        }
        if (normalize_optional(request.production_line_name()) != null
                && normalize_optional(request.production_line_name()).length() > 120) {
            throw new IllegalArgumentException("Production line name must contain at most 120 characters.");
        }
    }

    private BigDecimal normalize_quantity(BigDecimal value) {
        return value.setScale(quantity_scale, RoundingMode.HALF_UP);
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

