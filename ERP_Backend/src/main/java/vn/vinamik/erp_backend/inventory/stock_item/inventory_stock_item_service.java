package vn.vinamik.erp_backend.inventory.stock_item;
import vn.vinamik.erp_backend.platform.common.pagination_guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.inventory.stock_item.entity.stock_item_entity;
import vn.vinamik.erp_backend.inventory.stock_item.repository.inventory_stock_item_read_repository;
import vn.vinamik.erp_backend.inventory.stock_item.repository.inventory_stock_item_repository;
import vn.vinamik.erp_backend.inventory.stock_item.repository.stock_item_read_row;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class inventory_stock_item_service {
    private static final Logger logger = LoggerFactory.getLogger(inventory_stock_item_service.class);
    private static final int max_page_size = 100;
    private final inventory_stock_item_repository stock_item_repository;
    private final inventory_stock_item_read_repository read_repository;
    private final audit_event_writer audit_writer;

    @Autowired
    public inventory_stock_item_service(
            inventory_stock_item_repository stock_item_repository,
            inventory_stock_item_read_repository read_repository,
            audit_event_writer audit_writer) {
        this.stock_item_repository = stock_item_repository;
        this.read_repository = read_repository;
        this.audit_writer = audit_writer;
    }


    @Transactional(readOnly = true)
    public stock_item_page_response search(String search, String status, String item_type, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = Math.min(Math.max(page_size, 1), max_page_size);
        String normalized_search = normalize_lower(search);
        String normalized_status = normalize_lower(status);
        String normalized_item_type = normalize_lower(item_type);
        if (normalized_item_type == null) {
            normalized_item_type = "raw_material";
        }
        if (!List.of("raw_material", "finished_product").contains(normalized_item_type)) {
            throw new IllegalArgumentException("Item type is invalid.");
        }
        if (normalized_status != null && !List.of("active", "inactive").contains(normalized_status)) {
            throw new IllegalArgumentException("Item status is invalid.");
        }
        long total = read_repository.count(normalized_search, normalized_status, normalized_item_type);
        List<stock_item_response> items = read_repository.search(
                        normalized_search, normalized_status, normalized_item_type, safe_page, safe_page_size)
                .stream()
                .map(this::to_response)
                .toList();
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / safe_page_size);
        return new stock_item_page_response(items, safe_page, safe_page_size, total, total_pages);
    }

    @Transactional(readOnly = true)
    public stock_item_response find_by_id(long stock_item_id) {
        return to_response(read_repository.find_by_id(stock_item_id));
    }

    @Transactional
    public stock_item_response create(stock_item_request request, authenticated_user actor, String correlation_id) {
        validate_request(request);
        String item_code = normalize_lower_required(request.item_code());
        String item_type = item_type_or_default(request.item_type());
        ensure_unique_code(item_code, null);
        stock_item_entity entity = new stock_item_entity(
                item_code,
                request.item_name().trim(),
                item_type,
                request.item_category_id(),
                request.base_unit_of_measure_id(),
                request.lot_controlled() == null ? Boolean.FALSE : request.lot_controlled(),
                request.minimum_stock_quantity(),
                status_or_default(request.status()),
                normalize_optional(request.description()),
                actor.user_id());
        try {
            stock_item_repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("item_code", "Item code already exists.");
        }
        stock_item_response created = find_by_id(entity.stock_item_id());
        audit_writer.write(actor.user_id(), "inventory", "stock_item_create", "stock_item",
                String.valueOf(entity.stock_item_id()), correlation_id, Map.of("item_code", created.item_code()));
        logger.info("Đã tạo mặt hàng nguyên vật liệu; stock_item_id={}, actor_user_id={}, correlation_id={}",
                entity.stock_item_id(), actor.user_id(), correlation_id);
        return created;
    }

    @Transactional
    public stock_item_response update(long stock_item_id, stock_item_request request, authenticated_user actor,
                                      String correlation_id) {
        validate_request(request);
        String item_code = normalize_lower_required(request.item_code());
        String item_type = item_type_or_default(request.item_type());
        ensure_unique_code(item_code, stock_item_id);
        stock_item_entity entity = stock_item_repository.findById(stock_item_id)
                .orElseThrow(() -> new resource_not_found_exception("Stock item"));
        if (!item_type.equals(entity.item_type())) {
            throw new field_conflict_exception("item_type", "Stock item type cannot be changed after creation.");
        }
        entity.update_values(
                item_code,
                request.item_name().trim(),
                request.item_category_id(),
                request.base_unit_of_measure_id(),
                request.lot_controlled() == null ? Boolean.FALSE : request.lot_controlled(),
                request.minimum_stock_quantity(),
                status_or_default(request.status()),
                normalize_optional(request.description()),
                actor.user_id());
        try {
            stock_item_repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("item_code", "Item code already exists.");
        }
        stock_item_response updated_item = find_by_id(stock_item_id);
        audit_writer.write(actor.user_id(), "inventory", "stock_item_update", "stock_item",
                String.valueOf(stock_item_id), correlation_id, Map.of("item_code", updated_item.item_code()));
        logger.info("Đã cập nhật mặt hàng nguyên vật liệu; stock_item_id={}, actor_user_id={}, correlation_id={}",
                stock_item_id, actor.user_id(), correlation_id);
        return updated_item;
    }

    @Transactional
    public stock_item_response deactivate(long stock_item_id, authenticated_user actor, String correlation_id) {
        stock_item_entity entity = stock_item_repository.findById(stock_item_id)
                .orElseThrow(() -> new resource_not_found_exception("Stock item"));
        entity.deactivate(actor.user_id());
        stock_item_repository.saveAndFlush(entity);
        stock_item_response deactivated = find_by_id(stock_item_id);
        audit_writer.write(actor.user_id(), "inventory", "stock_item_deactivate", "stock_item",
                String.valueOf(stock_item_id), correlation_id, Map.of("item_code", deactivated.item_code()));
        logger.info("Đã vô hiệu hóa mặt hàng; stock_item_id={}, actor_user_id={}, correlation_id={}",
                stock_item_id, actor.user_id(), correlation_id);
        return deactivated;
    }

    private void ensure_unique_code(String item_code, Long stock_item_id) {
        if (stock_item_repository == null) {
            throw new IllegalStateException("Stock item repository is not configured.");
        }
        boolean exists = stock_item_id == null
                ? stock_item_repository.exists_by_item_code(item_code)
                : stock_item_repository.exists_by_item_code_excluding_id(item_code, stock_item_id);
        if (exists) {
            throw new field_conflict_exception("item_code", "Item code already exists.");
        }
    }

    private void validate_request(stock_item_request request) {
        if (request == null) {
            throw new IllegalArgumentException("Stock item request is required.");
        }
        String item_code = normalize_lower(request.item_code());
        String item_name = normalize_optional(request.item_name());
        String item_type = item_type_or_default(request.item_type());
        if (item_code == null || item_code.length() > 60) {
            throw new IllegalArgumentException("Item code is required and must contain at most 60 characters.");
        }
        if (item_name == null || item_name.length() > 180) {
            throw new IllegalArgumentException("Item name is required and must contain at most 180 characters.");
        }
        if (!List.of("raw_material", "finished_product").contains(item_type)) {
            throw new IllegalArgumentException("Item type is invalid.");
        }
        if (request.base_unit_of_measure_id() == null || request.base_unit_of_measure_id() <= 0) {
            throw new IllegalArgumentException("Base unit of measure is required.");
        }
        if (read_repository == null || !read_repository.active_unit_of_measure(request.base_unit_of_measure_id())) {
            throw new resource_not_found_exception("Active unit of measure");
        }
        if (request.item_category_id() != null && request.item_category_id() <= 0) {
            throw new IllegalArgumentException("Item category is invalid.");
        }
        if (request.item_category_id() != null
                && !read_repository.active_item_category(request.item_category_id())) {
            throw new resource_not_found_exception("Active item category");
        }
        if (request.minimum_stock_quantity() != null
                && request.minimum_stock_quantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum stock quantity cannot be negative.");
        }
        if (request.minimum_stock_quantity() != null && request.minimum_stock_quantity().scale() > 6) {
            throw new IllegalArgumentException("Minimum stock quantity must contain at most 6 decimal places.");
        }
        String status = status_or_default(request.status());
        if (!List.of("active", "inactive").contains(status)) {
            throw new IllegalArgumentException("Item status is invalid.");
        }
    }

    private String item_type_or_default(String item_type) {
        String normalized = normalize_lower(item_type);
        return normalized == null ? "raw_material" : normalized;
    }

    private String status_or_default(String status) {
        String normalized = normalize_lower(status);
        return normalized == null ? "active" : normalized;
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

    private stock_item_response to_response(stock_item_read_row row) {
        return new stock_item_response(
                row.stock_item_id(),
                row.item_code(),
                row.item_name(),
                row.item_type(),
                row.item_category_id(),
                row.category_name(),
                row.base_unit_of_measure_id(),
                row.unit_code(),
                row.unit_name(),
                row.lot_controlled(),
                row.minimum_stock_quantity(),
                row.status(),
                row.description());
    }
}


