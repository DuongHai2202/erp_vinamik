package vn.vinamik.erp_backend.production.material_consumption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.inventory.api.inventory_issue_command;
import vn.vinamik.erp_backend.inventory.api.inventory_issue_contract;
import vn.vinamik.erp_backend.inventory.api.inventory_issue_line_command;
import vn.vinamik.erp_backend.inventory.api.inventory_issue_result;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class production_material_consumption_service {
    private static final Logger logger = LoggerFactory.getLogger(production_material_consumption_service.class);
    private static final int quantity_scale = 6;
    private final production_material_consumption_repository consumption_repository;
    private final inventory_issue_contract issue_contract;
    private final audit_event_writer audit_writer;

    public production_material_consumption_service(
            production_material_consumption_repository consumption_repository,
            inventory_issue_contract issue_contract,
            audit_event_writer audit_writer) {
        this.consumption_repository = consumption_repository;
        this.issue_contract = issue_contract;
        this.audit_writer = audit_writer;
    }

    @Transactional
    public material_consumption_response create(long production_order_id, material_consumption_request request,
                                                authenticated_user actor, String correlation_id) {
        validate_request(request);
        production_material_consumption_repository.production_order_snapshot order =
                consumption_repository.load_order(production_order_id, true);
        String request_key = request.idempotency_key().trim();
        material_consumption_response existing =
                consumption_repository.find_by_key(production_order_id, request_key, order.order_code());
        if (existing != null) {
            return existing;
        }
        if (!List.of("released", "in_progress", "paused").contains(order.status())) {
            throw new IllegalArgumentException("Material consumption requires a released or active production order.");
        }
        Map<Long, production_material_consumption_repository.material_requirement_snapshot> requirements =
                consumption_repository.load_requirements(production_order_id);
        Map<Long, BigDecimal> consumed_quantities = consumption_repository.consumed_quantities(production_order_id);
        validate_lines(request.lines(), requirements, consumed_quantities);
        List<inventory_issue_line_command> issue_lines = request.lines().stream()
                .map(line -> new inventory_issue_line_command(
                        line.material_stock_item_id(),
                        line.warehouse_location_id(),
                        line.stock_lot_id(),
                        normalize_quantity(line.consumed_quantity())))
                .toList();
        inventory_issue_result issue = issue_contract.create_and_post(
                new inventory_issue_command(
                        build_issue_code(production_order_id, request_key),
                        request.warehouse_id(),
                        "production",
                        production_order_id,
                        "production_consumption",
                        request_key,
                        request.notes(),
                        issue_lines),
                actor.user_id(), correlation_id);
        if (issue.lines().size() != request.lines().size()) {
            throw new IllegalStateException("Inventory issue response does not match the requested consumption lines.");
        }
        for (int index = 0; index < request.lines().size(); index++) {
            material_consumption_line_request line = request.lines().get(index);
            production_material_consumption_repository.material_requirement_snapshot requirement =
                    requirements.get(line.material_stock_item_id());
            consumption_repository.insert(production_order_id, line.material_stock_item_id(),
                    issue.lines().get(index).issue_line_id(), requirement.unit_code(),
                    normalize_quantity(line.consumed_quantity()), actor.user_id(), request_key,
                    index + 1, normalize_optional(request.notes()));
        }
        material_consumption_response created =
                consumption_repository.find_by_key(production_order_id, request_key, order.order_code());
        if (created == null) {
            throw new IllegalStateException("Material consumption was not persisted.");
        }
        audit_writer.write(actor.user_id(), "production", "material_consumption_create", "production_order",
                String.valueOf(production_order_id), correlation_id,
                Map.of("idempotency_key", request_key, "line_count", created.lines().size()));
        logger.info("Đã ghi nhận tiêu hao vật tư và liên kết phiếu xuất kho; production_order_id={}, line_count={}, actor_user_id={}, correlation_id={}",
                production_order_id, created.lines().size(), actor.user_id(), correlation_id);
        return created;
    }

    @Transactional(readOnly = true)
    public List<material_consumption_response> list(long production_order_id) {
        production_material_consumption_repository.production_order_snapshot order =
                consumption_repository.load_order(production_order_id, false);
        return consumption_repository.list_by_order(production_order_id, order.order_code());
    }

    private void validate_lines(List<material_consumption_line_request> lines,
                                Map<Long, production_material_consumption_repository.material_requirement_snapshot> requirements,
                                Map<Long, BigDecimal> consumed_quantities) {
        for (material_consumption_line_request line : lines) {
            production_material_consumption_repository.material_requirement_snapshot requirement =
                    requirements.get(line.material_stock_item_id());
            if (requirement == null) {
                throw new resource_not_found_exception("Material requirement");
            }
            BigDecimal already_consumed = consumed_quantities.getOrDefault(line.material_stock_item_id(), BigDecimal.ZERO);
            if (already_consumed.add(normalize_quantity(line.consumed_quantity()))
                    .compareTo(requirement.required_quantity()) > 0) {
                throw new field_conflict_exception("consumed_quantity", "Consumed quantity exceeds the material requirement.");
            }
        }
    }

    private void validate_request(material_consumption_request request) {
        if (request == null || request.warehouse_id() == null || request.warehouse_id() <= 0
                || request.idempotency_key() == null || request.idempotency_key().isBlank()
                || request.idempotency_key().trim().length() > 120
                || request.lines() == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException("Material consumption request is incomplete.");
        }
        Set<Long> unique_items = new HashSet<>();
        for (material_consumption_line_request line : request.lines()) {
            if (line == null || line.material_stock_item_id() == null || line.material_stock_item_id() <= 0
                    || line.warehouse_location_id() == null || line.warehouse_location_id() <= 0
                    || line.consumed_quantity() == null || line.consumed_quantity().signum() <= 0
                    || line.consumed_quantity().scale() > quantity_scale
                    || (line.stock_lot_id() != null && line.stock_lot_id() <= 0)) {
                throw new IllegalArgumentException("Material consumption line is invalid.");
            }
            if (!unique_items.add(line.material_stock_item_id())) {
                throw new IllegalArgumentException("A material can appear only once in a consumption request.");
            }
        }
    }

    private String build_issue_code(long order_id, String request_key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(request_key.getBytes(StandardCharsets.UTF_8));
            String suffix = java.util.HexFormat.of().formatHex(digest).substring(0, 16);
            return "prod_issue_" + order_id + "_" + suffix;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private BigDecimal normalize_quantity(BigDecimal value) {
        return value.setScale(quantity_scale, java.math.RoundingMode.HALF_UP);
    }

    private String normalize_optional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
