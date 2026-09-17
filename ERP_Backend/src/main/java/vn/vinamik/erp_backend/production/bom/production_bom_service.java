package vn.vinamik.erp_backend.production.bom;
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

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class production_bom_service {
    private static final Logger logger = LoggerFactory.getLogger(production_bom_service.class);
    private static final int max_page_size = 100;
    private static final List<String> valid_statuses = List.of("draft", "active", "inactive");

    private final production_bom_repository bom_repository;
    private final inventory_material_contract material_contract;
    private final audit_event_writer audit_writer;

    public production_bom_service(production_bom_repository bom_repository,
                                  inventory_material_contract material_contract,
                                  audit_event_writer audit_writer) {
        this.bom_repository = bom_repository;
        this.material_contract = material_contract;
        this.audit_writer = audit_writer;
    }

    @Transactional(readOnly = true)
    public bom_page_response search(String search, Long stock_item_id, String status, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = Math.min(Math.max(page_size, 1), max_page_size);
        String normalized_search = normalize_lower(search);
        String normalized_status = normalize_lower(status);
        validate_status(normalized_status);
        long total = bom_repository.count(normalized_search, stock_item_id, normalized_status);
        List<bom_summary> items = bom_repository.search(normalized_search, stock_item_id, normalized_status,
                safe_page_size, pagination_guard.offset(safe_page, safe_page_size));
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / safe_page_size);
        return new bom_page_response(items, safe_page, safe_page_size, total, total_pages);
    }

    @Transactional(readOnly = true)
    public bom_response find_by_id(long bom_id) {
        return bom_repository.find(bom_id);
    }

    @Transactional
    public bom_response create(bom_request request, authenticated_user actor, String correlation_id) {
        validate_request(request);
        inventory_material_snapshot product = require_material(request.stock_item_id(), "finished_product");
        List<inventory_material_snapshot> materials = validate_material_lines(request.lines());
        ensure_unique_bom(request.bom_code(), request.stock_item_id(), request.version_number(), null);
        long bom_id = bom_repository.insert(normalize_required(request.bom_code()), request.stock_item_id(),
                product.item_code(), product.item_name(), request.version_number(), request.base_quantity(),
                product.unit_code(), request.valid_from(), request.valid_to(), normalize_optional(request.notes()),
                actor.user_id());
        insert_lines(bom_id, request.lines(), materials);
        bom_response created = bom_repository.find(bom_id);
        audit_writer.write(actor.user_id(), "production", "bom_create", "bom", String.valueOf(bom_id),
                correlation_id, Map.of("bom_code", created.bom_code(), "version_number", created.version_number()));
        logger.info("Đã tạo định mức sản xuất; bom_id={}, actor_user_id={}, correlation_id={}",
                bom_id, actor.user_id(), correlation_id);
        return created;
    }

    @Transactional
    public bom_response update(long bom_id, bom_request request, authenticated_user actor,
                               String correlation_id) {
        validate_request(request);
        String current_status = bom_repository.current_status(bom_id);
        if (!current_status.equals("draft")) {
            throw new IllegalArgumentException("Only draft BOMs can be edited.");
        }
        inventory_material_snapshot product = require_material(request.stock_item_id(), "finished_product");
        List<inventory_material_snapshot> materials = validate_material_lines(request.lines());
        ensure_unique_bom(request.bom_code(), request.stock_item_id(), request.version_number(), bom_id);
        int updated = bom_repository.update(bom_id, normalize_required(request.bom_code()),
                request.stock_item_id(), product.item_code(), product.item_name(), request.version_number(),
                request.base_quantity(), product.unit_code(), request.valid_from(), request.valid_to(),
                normalize_optional(request.notes()), actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Draft BOM");
        }
        bom_repository.delete_lines(bom_id);
        insert_lines(bom_id, request.lines(), materials);
        bom_response changed = bom_repository.find(bom_id);
        audit_writer.write(actor.user_id(), "production", "bom_update", "bom", String.valueOf(bom_id),
                correlation_id, Map.of("bom_code", changed.bom_code(), "version_number", changed.version_number()));
        logger.info("Đã cập nhật định mức sản xuất; bom_id={}, actor_user_id={}, correlation_id={}",
                bom_id, actor.user_id(), correlation_id);
        return changed;
    }

    @Transactional
    public bom_response change_status(long bom_id, String requested_status, authenticated_user actor,
                                      String correlation_id) {
        String status = normalize_required(requested_status);
        validate_status(status);
        String current = bom_repository.current_status(bom_id);
        if (current.equals(status)) {
            throw new IllegalArgumentException("BOM is already in the requested status.");
        }
        if (!((current.equals("draft") && (status.equals("active") || status.equals("inactive")))
                || (current.equals("inactive") && status.equals("active")))) {
            throw new IllegalArgumentException("BOM status transition is not allowed.");
        }
        if (status.equals("active") && bom_repository.has_active_overlap(bom_id)) {
            throw new field_conflict_exception("valid_from", "Another active BOM overlaps this validity period.");
        }
        int updated = bom_repository.change_status(bom_id, status, actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("BOM");
        }
        bom_response changed = bom_repository.find(bom_id);
        audit_writer.write(actor.user_id(), "production", "bom_status_change", "bom",
                String.valueOf(bom_id), correlation_id,
                Map.of("from_status", current, "to_status", status));
        logger.info("Đã chuyển trạng thái định mức sản xuất; bom_id={}, from_status={}, to_status={}, actor_user_id={}, correlation_id={}",
                bom_id, current, status, actor.user_id(), correlation_id);
        return changed;
    }

    private void insert_lines(long bom_id, List<bom_line_request> lines,
                              List<inventory_material_snapshot> materials) {
        for (int index = 0; index < lines.size(); index++) {
            bom_line_request line = lines.get(index);
            inventory_material_snapshot material = materials.get(index);
            bom_repository.insert_line(bom_id, index + 1, line, material.item_code(), material.item_name(),
                    material.unit_code(), line.scrap_percent() == null ? BigDecimal.ZERO : line.scrap_percent(),
                    normalize_optional(line.notes()));
        }
    }

    private inventory_material_snapshot require_material(long stock_item_id, String expected_type) {
        inventory_material_snapshot snapshot = material_contract.find_active_stock_item(stock_item_id)
                .orElseThrow(() -> new resource_not_found_exception("Active stock item"));
        if (!expected_type.equals(snapshot.item_type())) {
            throw new IllegalArgumentException("Selected stock item must be a " + expected_type + ".");
        }
        return snapshot;
    }

    private List<inventory_material_snapshot> validate_material_lines(List<bom_line_request> lines) {
        Set<Long> unique_item_ids = new HashSet<>();
        return lines.stream().map(line -> {
            validate_line(line);
            if (!unique_item_ids.add(line.material_stock_item_id())) {
                throw new IllegalArgumentException("A material can appear only once in a BOM.");
            }
            inventory_material_snapshot snapshot = material_contract.find_active_stock_item(line.material_stock_item_id())
                    .orElseThrow(() -> new resource_not_found_exception("Active material stock item"));
            if (!"raw_material".equals(snapshot.item_type())) {
                throw new IllegalArgumentException("BOM lines must reference raw materials.");
            }
            return snapshot;
        }).toList();
    }

    private void ensure_unique_bom(String bom_code, long stock_item_id, int version_number, Long bom_id) {
        if (bom_repository.bom_code_version_exists(normalize_required(bom_code), version_number, bom_id)) {
            throw new field_conflict_exception("bom_code", "BOM code and version already exist.");
        }
        if (stock_item_id <= 0) {
            throw new IllegalArgumentException("Finished product is invalid.");
        }
    }

    private void validate_request(bom_request request) {
        if (request == null) {
            throw new IllegalArgumentException("BOM request is required.");
        }
        if (normalize_optional(request.bom_code()) == null
                || normalize_optional(request.bom_code()).length() > 60) {
            throw new IllegalArgumentException("BOM code is required and must contain at most 60 characters.");
        }
        if (request.stock_item_id() == null || request.stock_item_id() <= 0
                || request.version_number() == null || request.version_number() < 1
                || request.valid_from() == null) {
            throw new IllegalArgumentException("Finished product, BOM version and valid-from date are required.");
        }
        if (request.valid_to() != null && request.valid_to().isBefore(request.valid_from())) {
            throw new IllegalArgumentException("BOM valid-to date cannot be before valid-from date.");
        }
        if (request.base_quantity() == null || request.base_quantity().signum() <= 0
                || request.base_quantity().scale() > 6) {
            throw new IllegalArgumentException("BOM base quantity must be positive with at most 6 decimal places.");
        }
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException("At least one BOM line is required.");
        }
    }

    private void validate_line(bom_line_request line) {
        if (line == null || line.material_stock_item_id() == null || line.quantity_per_base() == null
                || line.quantity_per_base().signum() <= 0 || line.quantity_per_base().scale() > 6) {
            throw new IllegalArgumentException("BOM material quantity must be positive with at most 6 decimal places.");
        }
        if (line.scrap_percent() != null && (line.scrap_percent().signum() < 0
                || line.scrap_percent().compareTo(new BigDecimal("100.00")) > 0
                || line.scrap_percent().scale() > 2)) {
            throw new IllegalArgumentException("Scrap percentage must be between 0 and 100 with at most 2 decimal places.");
        }
    }

    private void validate_status(String status) {
        if (status != null && !valid_statuses.contains(status)) {
            throw new IllegalArgumentException("BOM status is invalid.");
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
}
