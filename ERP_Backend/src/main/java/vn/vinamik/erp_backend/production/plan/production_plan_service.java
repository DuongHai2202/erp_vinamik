package vn.vinamik.erp_backend.production.plan;
import vn.vinamik.erp_backend.platform.common.pagination_guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.inventory.api.inventory_material_contract;
import vn.vinamik.erp_backend.inventory.api.inventory_material_snapshot;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class production_plan_service {
    private static final Logger logger = LoggerFactory.getLogger(production_plan_service.class);
    private static final int max_page_size = 100;
    private static final List<String> statuses = List.of("draft", "approved", "released", "completed", "cancelled");

    private final production_plan_repository plan_repository;
    private final inventory_material_contract material_contract;
    private final audit_event_writer audit_writer;

    public production_plan_service(production_plan_repository plan_repository,
                                    inventory_material_contract material_contract,
                                    audit_event_writer audit_writer) {
        this.plan_repository = plan_repository;
        this.material_contract = material_contract;
        this.audit_writer = audit_writer;
    }

    @Transactional(readOnly = true)
    public production_plan_page_response search(String search, String status, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = Math.min(Math.max(page_size, 1), max_page_size);
        String normalized_search = normalize_lower(search);
        String normalized_status = normalize_lower(status);
        validate_status_filter(normalized_status);
        long total = plan_repository.count(normalized_search, normalized_status);
        List<production_plan_summary> items = plan_repository.search(normalized_search, normalized_status,
                safe_page_size, pagination_guard.offset(safe_page, safe_page_size));
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / safe_page_size);
        return new production_plan_page_response(items, safe_page, safe_page_size, total, total_pages);
    }

    @Transactional(readOnly = true)
    public production_plan_response find_by_id(long production_plan_id) {
        return plan_repository.find(production_plan_id);
    }

    @Transactional
    public production_plan_response create(production_plan_request request, authenticated_user actor,
                                           String correlation_id) {
        validate_request(request);
        String plan_code = normalize_lower_required(request.plan_code());
        ensure_unique_code(plan_code, null);
        List<inventory_material_snapshot> items = validate_inventory_lines(request.lines());
        long plan_id = plan_repository.insert(plan_code, request.plan_name().trim(), request.planned_on(),
                request.starts_on(), request.ends_on(), normalize_optional(request.notes()), actor.user_id());
        insert_lines(plan_id, request.lines(), items);
        production_plan_response created = plan_repository.find(plan_id);
        audit_writer.write(actor.user_id(), "production", "production_plan_create", "production_plan",
                String.valueOf(plan_id), correlation_id, Map.of("plan_code", created.plan_code()));
        logger.info("Đã tạo kế hoạch sản xuất; production_plan_id={}, actor_user_id={}, correlation_id={}",
                plan_id, actor.user_id(), correlation_id);
        return created;
    }

    @Transactional
    public production_plan_response update(long plan_id, production_plan_request request,
                                           authenticated_user actor, String correlation_id) {
        validate_request(request);
        String plan_code = normalize_lower_required(request.plan_code());
        ensure_unique_code(plan_code, plan_id);
        String current_status = plan_repository.current_status(plan_id);
        if (!"draft".equals(current_status)) {
            throw new IllegalArgumentException("Only draft production plans can be edited.");
        }
        List<inventory_material_snapshot> items = validate_inventory_lines(request.lines());
        production_plan_response existing_plan = plan_repository.find(plan_id);
        boolean has_orders = plan_repository.has_orders(plan_id);
        if (has_orders && !same_line_identity(existing_plan.lines(), request.lines())) {
            throw new field_conflict_exception("lines",
                    "Production plan lines already referenced by production orders cannot be replaced. Keep the existing products and line count, or create a new plan.");
        }
        int updated = plan_repository.update(plan_id, plan_code, request.plan_name().trim(),
                request.planned_on(), request.starts_on(), request.ends_on(),
                normalize_optional(request.notes()), actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Draft production plan");
        }
        if (has_orders) {
            update_existing_lines(existing_plan.lines(), request.lines(), items, plan_id);
        } else {
            plan_repository.delete_lines(plan_id);
            insert_lines(plan_id, request.lines(), items);
        }
        production_plan_response updated_plan = plan_repository.find(plan_id);
        audit_writer.write(actor.user_id(), "production", "production_plan_update", "production_plan",
                String.valueOf(plan_id), correlation_id, Map.of("plan_code", updated_plan.plan_code()));
        logger.info("Đã cập nhật kế hoạch sản xuất; production_plan_id={}, actor_user_id={}, correlation_id={}",
                plan_id, actor.user_id(), correlation_id);
        return updated_plan;
    }

    @Transactional
    public production_plan_response change_status(long plan_id, String requested_status,
                                                   authenticated_user actor, String correlation_id) {
        String target_status = normalize_lower(requested_status);
        validate_status(target_status);
        String current = plan_repository.current_status(plan_id);
        if (!is_allowed_transition(current, target_status)) {
            throw new IllegalArgumentException("The production plan status transition is not allowed.");
        }
        int updated = plan_repository.change_status(plan_id, target_status, actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Production plan");
        }
        production_plan_response changed = plan_repository.find(plan_id);
        audit_writer.write(actor.user_id(), "production", "production_plan_status_change", "production_plan",
                String.valueOf(plan_id), correlation_id,
                Map.of("from_status", current, "to_status", target_status));
        logger.info("Đã chuyển trạng thái kế hoạch sản xuất; production_plan_id={}, from_status={}, to_status={}, actor_user_id={}, correlation_id={}",
                plan_id, current, target_status, actor.user_id(), correlation_id);
        return changed;
    }

    @Transactional
    public void delete(long plan_id, authenticated_user actor, String correlation_id) {
        String current = plan_repository.current_status(plan_id);
        if (!"draft".equals(current)) {
            throw new IllegalArgumentException("Only draft production plans can be deleted.");
        }
        if (plan_repository.has_orders(plan_id)) {
            throw new IllegalArgumentException("Production plans referenced by orders cannot be deleted.");
        }
        plan_repository.delete_lines(plan_id);
        int deleted = plan_repository.delete(plan_id);
        if (deleted == 0) {
            throw new resource_not_found_exception("Draft production plan");
        }
        audit_writer.write(actor.user_id(), "production", "production_plan_delete", "production_plan",
                String.valueOf(plan_id), correlation_id, Map.of());
        logger.info("Đã xóa kế hoạch sản xuất nháp; production_plan_id={}, actor_user_id={}, correlation_id={}",
                plan_id, actor.user_id(), correlation_id);
    }

    private void ensure_unique_code(String plan_code, Long plan_id) {
        if (plan_repository.plan_code_exists(plan_code, plan_id)) {
            throw new field_conflict_exception("plan_code", "Plan code already exists.");
        }
    }

    private void insert_lines(long plan_id, List<production_plan_line_request> lines,
                              List<inventory_material_snapshot> items) {
        for (int index = 0; index < lines.size(); index++) {
            production_plan_line_request line = lines.get(index);
            inventory_material_snapshot item = items.get(index);
            plan_repository.insert_line(plan_id, index + 1, line, item.stock_item_id(),
                    item.item_code(), item.item_name(), item.unit_code(), normalize_optional(line.notes()));
        }
    }

    private boolean same_line_identity(List<production_plan_line_response> existing_lines,
                                       List<production_plan_line_request> requested_lines) {
        if (existing_lines.size() != requested_lines.size()) {
            return false;
        }
        for (int index = 0; index < requested_lines.size(); index++) {
            if (existing_lines.get(index).stock_item_id() != requested_lines.get(index).stock_item_id()) {
                return false;
            }
        }
        return true;
    }

    private void update_existing_lines(List<production_plan_line_response> existing_lines,
                                       List<production_plan_line_request> requested_lines,
                                       List<inventory_material_snapshot> items,
                                       long plan_id) {
        for (int index = 0; index < requested_lines.size(); index++) {
            production_plan_line_request line = requested_lines.get(index);
            inventory_material_snapshot item = items.get(index);
            plan_repository.update_line(existing_lines.get(index).production_plan_line_id(), plan_id, line,
                    item.stock_item_id(), item.item_code(), item.item_name(), item.unit_code(),
                    normalize_optional(line.notes()));
        }
    }

    private List<inventory_material_snapshot> validate_inventory_lines(List<production_plan_line_request> lines) {
        Set<Long> unique_item_ids = new HashSet<>();
        return lines.stream().map(line -> {
            if (!unique_item_ids.add(line.stock_item_id())) {
                throw new IllegalArgumentException("A stock item can appear only once in a production plan.");
            }
            inventory_material_snapshot snapshot = material_contract.find_active_stock_item(line.stock_item_id())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "The selected stock item is not active or does not exist."));
            if (!"finished_product".equals(snapshot.item_type())) {
                throw new IllegalArgumentException("Production plan lines must reference finished products.");
            }
            return snapshot;
        }).toList();
    }

    private boolean is_allowed_transition(String current, String target) {
        return switch (current) {
            case "draft" -> target.equals("approved") || target.equals("cancelled");
            case "approved" -> target.equals("released") || target.equals("cancelled");
            case "released" -> target.equals("completed") || target.equals("cancelled");
            default -> false;
        };
    }

    private void validate_request(production_plan_request request) {
        if (request == null) {
            throw new IllegalArgumentException("Production plan request is required.");
        }
        if (normalize_optional(request.plan_code()) == null
                || normalize_optional(request.plan_code()).length() > 60) {
            throw new IllegalArgumentException("Plan code is required and must contain at most 60 characters.");
        }
        if (normalize_optional(request.plan_name()) == null
                || normalize_optional(request.plan_name()).length() > 180) {
            throw new IllegalArgumentException("Plan name is required and must contain at most 180 characters.");
        }
        if (request.planned_on() == null || request.starts_on() == null || request.ends_on() == null) {
            throw new IllegalArgumentException("Planned date, start date and end date are required.");
        }
        if (request.ends_on().isBefore(request.starts_on())) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException("At least one production plan line is required.");
        }
        for (production_plan_line_request line : request.lines()) {
            if (line == null || line.stock_item_id() == null || line.stock_item_id() <= 0
                    || line.target_quantity() == null || line.target_quantity().signum() <= 0
                    || line.target_quantity().scale() > 6) {
                throw new IllegalArgumentException(
                        "Production plan lines require a positive stock item and quantity with at most 6 decimal places.");
            }
            if (line.required_on() != null
                    && (line.required_on().isBefore(request.starts_on())
                    || line.required_on().isAfter(request.ends_on()))) {
                throw new IllegalArgumentException("Required date must stay within the production plan dates.");
            }
        }
    }

    private void validate_status_filter(String status) {
        if (status != null && !statuses.contains(status)) {
            throw new IllegalArgumentException("Production plan status is invalid.");
        }
    }

    private void validate_status(String status) {
        if (status == null || !statuses.contains(status)) {
            throw new IllegalArgumentException("Production plan status is invalid.");
        }
    }

    private String normalize_lower_required(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalize_lower(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalize_optional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
