package vn.vinamik.erp_backend.production.output;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.inventory.api.inventory_receipt_command;
import vn.vinamik.erp_backend.inventory.api.inventory_receipt_contract;
import vn.vinamik.erp_backend.inventory.api.inventory_receipt_line_command;
import vn.vinamik.erp_backend.inventory.api.inventory_receipt_result;
import vn.vinamik.erp_backend.inventory.api.inventory_stock_lot_contract;
import vn.vinamik.erp_backend.inventory.api.inventory_stock_lot_snapshot;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class production_output_service {
    private static final Logger logger = LoggerFactory.getLogger(production_output_service.class);
    private static final int quantity_scale = 6;
    private static final int max_lot_code_length = 80;
    private static final int max_idempotency_key_length = 120;
    private final production_output_repository output_repository;
    private final inventory_stock_lot_contract stock_lot_contract;
    private final inventory_receipt_contract receipt_contract;
    private final audit_event_writer audit_writer;

    public production_output_service(
            production_output_repository output_repository,
            inventory_stock_lot_contract stock_lot_contract,
            inventory_receipt_contract receipt_contract,
            audit_event_writer audit_writer) {
        this.output_repository = output_repository;
        this.stock_lot_contract = stock_lot_contract;
        this.receipt_contract = receipt_contract;
        this.audit_writer = audit_writer;
    }

    @Transactional(readOnly = true)
    public List<production_output_response> list(long production_order_id) {
        output_repository.load_order(production_order_id, false);
        return output_repository.find_by_order(production_order_id);
    }

    @Transactional
    public production_output_response create(long production_order_id, production_output_request request,
                                             authenticated_user actor, String correlation_id) {
        validate_request(request);
        production_output_repository.order_snapshot order =
                output_repository.load_order(production_order_id, true);
        String idempotency_key = request.idempotency_key().trim();
        production_output_response existing =
                output_repository.find_by_idempotency(production_order_id, idempotency_key);
        if (existing != null) {
            return existing;
        }
        if (!List.of("released", "in_progress", "paused", "completed").contains(order.status())) {
            throw new IllegalArgumentException("Production output requires a released or active production order.");
        }
        BigDecimal total_quantity = request.good_quantity().add(request.defective_quantity());
        BigDecimal existing_total = output_repository.total_output_quantity(production_order_id);
        if (existing_total.add(total_quantity).compareTo(order.target_quantity()) > 0) {
            throw new field_conflict_exception(
                    "good_quantity", "Production output exceeds the production order target quantity.");
        }
        production_output_response same_lot = output_repository.find_by_order_lot(
                production_order_id, request.lot_code().trim());
        if (same_lot != null) {
            throw new field_conflict_exception("lot_code", "The production order already has an output for this lot.");
        }
        if (output_repository.idempotency_key_exists(idempotency_key)) {
            throw new field_conflict_exception("idempotency_key", "Idempotency key is already used.");
        }
        Long output_id = output_repository.insert(
                production_order_id, order.stock_item_id(), request.lot_code().trim(),
                request.manufactured_on(), request.expires_on(),
                normalize_quantity(request.good_quantity()), normalize_quantity(request.defective_quantity()),
                request.warehouse_id(), request.warehouse_location_id(),
                request.good_quantity().signum() > 0 ? "pending_receipt" : "draft",
                idempotency_key, actor.user_id(), normalize_optional(request.notes()));
        if (output_id == null) {
            production_output_response concurrent =
                    output_repository.find_by_idempotency(production_order_id, idempotency_key);
            if (concurrent != null) {
                return concurrent;
            }
            throw new field_conflict_exception("idempotency_key", "Idempotency key is already used.");
        }
        production_output_response created = output_repository.find_by_id(output_id);
        audit_writer.write(actor.user_id(), "production", "production_output_create", "production_output",
                String.valueOf(output_id), correlation_id,
                Map.of("production_order_id", production_order_id,
                        "good_quantity", request.good_quantity(),
                        "defective_quantity", request.defective_quantity()));
        logger.info("Đã ghi nhận sản lượng thành phẩm; production_output_id={}, production_order_id={}, actor_user_id={}, correlation_id={}",
                output_id, production_order_id, actor.user_id(), correlation_id);
        return created;
    }

    @Transactional
    public production_output_response post(long production_order_id, long output_id,
                                           authenticated_user actor, String correlation_id) {
        production_output_repository.order_snapshot order =
                output_repository.load_order(production_order_id, true);
        production_output_response output =
                output_repository.find_by_id_for_update(production_order_id, output_id);
        if ("received".equals(output.status())) {
            return output;
        }
        if (!List.of("draft", "pending_receipt").contains(output.status())) {
            throw new IllegalArgumentException("Only draft or pending production outputs can be posted.");
        }
        if (!List.of("released", "in_progress", "paused", "completed").contains(order.status())) {
            throw new IllegalArgumentException("Production output can be posted only for an active production order.");
        }
        inventory_stock_lot_snapshot lot = null;
        Long receipt_id = null;
        if (output.good_quantity().signum() > 0) {
            lot = stock_lot_contract.ensure_active_lot(
                    order.stock_item_id(), output.lot_code(), output.manufactured_on(), output.expires_on(),
                    actor.user_id());
            inventory_receipt_result receipt = receipt_contract.create_and_post(
                    new inventory_receipt_command(
                            "prod_receipt_" + output.production_output_id(),
                            output.warehouse_id(),
                            "production",
                            output.production_output_id(),
                            "prod_receipt_output_" + output.production_output_id(),
                            output.notes(),
                            List.of(new inventory_receipt_line_command(
                                    order.stock_item_id(),
                                    output.warehouse_location_id(),
                                    lot.stock_lot_id(),
                                    normalize_quantity(output.good_quantity())))),
                    actor.user_id(), correlation_id);
            receipt_id = receipt.receipt_id();
        }
        int updated = output_repository.mark_received(
                production_order_id, output.production_output_id(), lot == null ? null : lot.stock_lot_id(), receipt_id);
        if (updated == 0) {
            throw new resource_not_found_exception("Postable production output");
        }
        production_output_response posted = output_repository.find_by_id(output.production_output_id());
        audit_writer.write(actor.user_id(), "production", "production_output_post", "production_output",
                String.valueOf(output.production_output_id()), correlation_id,
                Map.of("production_order_id", production_order_id,
                        "inventory_receipt_id", receipt_id == null ? "none" : receipt_id));
        logger.info("Đã bàn giao thành phẩm đạt sang Kho; production_output_id={}, production_order_id={}, inventory_receipt_id={}, actor_user_id={}, correlation_id={}",
                output.production_output_id(), production_order_id, receipt_id, actor.user_id(), correlation_id);
        return posted;
    }

    private void validate_request(production_output_request request) {
        if (request == null || request.warehouse_id() == null || request.warehouse_id() <= 0
                || request.warehouse_location_id() == null || request.warehouse_location_id() <= 0
                || request.lot_code() == null || request.lot_code().isBlank()
                || request.lot_code().trim().length() > max_lot_code_length
                || request.manufactured_on() == null
                || request.good_quantity() == null || request.defective_quantity() == null
                || request.idempotency_key() == null || request.idempotency_key().isBlank()
                || request.idempotency_key().trim().length() > max_idempotency_key_length) {
            throw new IllegalArgumentException("Production output request is incomplete.");
        }
        if (request.idempotency_key().contains(":")) {
            throw new IllegalArgumentException("Idempotency key must not contain colon.");
        }
        if (request.good_quantity().signum() < 0 || request.defective_quantity().signum() < 0
                || request.good_quantity().scale() > quantity_scale
                || request.defective_quantity().scale() > quantity_scale) {
            throw new IllegalArgumentException(
                    "Output quantities must be non-negative with at most 6 decimal places.");
        }
        if (request.good_quantity().add(request.defective_quantity()).signum() <= 0) {
            throw new IllegalArgumentException("Production output must contain good or defective quantity.");
        }
        if (request.expires_on() != null && request.expires_on().isBefore(request.manufactured_on())) {
            throw new IllegalArgumentException("Expiry date cannot be before manufactured date.");
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
