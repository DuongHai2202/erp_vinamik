package vn.vinamik.erp_backend.inventory.master_data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;
import vn.vinamik.erp_backend.platform.common.master_data_page_response;
import vn.vinamik.erp_backend.platform.common.pagination_guard;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class inventory_master_data_service {
    private static final Logger logger = LoggerFactory.getLogger(inventory_master_data_service.class);
    private static final int max_page_size = 100;
    private static final List<String> valid_statuses = List.of("active", "inactive");

    private final inventory_master_data_repository master_data_repository;
    private final audit_event_writer audit_writer;

    public inventory_master_data_service(inventory_master_data_repository master_data_repository,
                                         audit_event_writer audit_writer) {
        this.master_data_repository = master_data_repository;
        this.audit_writer = audit_writer;
    }

    @Transactional(readOnly = true)
    public master_data_page_response<inventory_unit_response> search_units(
            String search, String status, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = safe_page_size(page_size);
        String normalized_search = normalize_lower(search);
        String normalized_status = normalize_lower(status);
        validate_status(normalized_status);
        long total = master_data_repository.count_units(normalized_search, normalized_status);
        return page_response(master_data_repository.search_units(normalized_search, normalized_status,
                safe_page, safe_page_size), safe_page, safe_page_size, total);
    }

    @Transactional(readOnly = true)
    public inventory_unit_response find_unit(long id) {
        return require_unit(id);
    }

    @Transactional
    public inventory_unit_response create_unit(inventory_unit_request request, authenticated_user actor,
                                               String correlation_id) {
        validate_unit(request);
        String code = normalize_required(request.unit_code());
        ensure_unit_code_available(code, null);
        try {
            Long id = master_data_repository.insert_unit(code, normalize_text(request.unit_name()),
                    decimal_places(request), status_or_default(request.status()), actor.user_id());
            inventory_unit_response result = require_unit(id);
            audit_writer.write(actor.user_id(), "inventory", "unit_create", "unit_of_measure",
                    String.valueOf(id), correlation_id, Map.of("unit_code", code));
            logger.info("Đã tạo đơn vị tính; unit_of_measure_id={}, actor_user_id={}, correlation_id={}",
                    id, actor.user_id(), correlation_id);
            return result;
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("unit_code", "Unit code already exists.");
        }
    }

    @Transactional
    public inventory_unit_response update_unit(long id, inventory_unit_request request,
                                               authenticated_user actor, String correlation_id) {
        validate_unit(request);
        String code = normalize_required(request.unit_code());
        ensure_unit_code_available(code, id);
        require_unit(id);
        try {
            int updated = master_data_repository.update_unit(id, code, normalize_text(request.unit_name()),
                    decimal_places(request), status_or_default(request.status()), actor.user_id());
            if (updated == 0) {
                throw new resource_not_found_exception("Unit of measure");
            }
            inventory_unit_response result = require_unit(id);
            audit_writer.write(actor.user_id(), "inventory", "unit_update", "unit_of_measure",
                    String.valueOf(id), correlation_id, Map.of("unit_code", code));
            logger.info("Đã cập nhật đơn vị tính; unit_of_measure_id={}, actor_user_id={}, correlation_id={}",
                    id, actor.user_id(), correlation_id);
            return result;
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("unit_code", "Unit code already exists.");
        }
    }

    @Transactional
    public inventory_unit_response deactivate_unit(long id, authenticated_user actor, String correlation_id) {
        inventory_unit_response current = require_unit(id);
        int updated = master_data_repository.deactivate_unit(id, actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Unit of measure");
        }
        inventory_unit_response result = require_unit(id);
        audit_writer.write(actor.user_id(), "inventory", "unit_deactivate", "unit_of_measure",
                String.valueOf(id), correlation_id, Map.of("unit_code", current.unit_code()));
        logger.info("Đã vô hiệu hóa đơn vị tính; unit_of_measure_id={}, actor_user_id={}, correlation_id={}",
                id, actor.user_id(), correlation_id);
        return result;
    }

    @Transactional(readOnly = true)
    public master_data_page_response<inventory_category_response> search_categories(
            String search, String status, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = safe_page_size(page_size);
        String normalized_search = normalize_lower(search);
        String normalized_status = normalize_lower(status);
        validate_status(normalized_status);
        long total = master_data_repository.count_categories(normalized_search, normalized_status);
        return page_response(master_data_repository.search_categories(normalized_search, normalized_status,
                safe_page, safe_page_size), safe_page, safe_page_size, total);
    }

    @Transactional(readOnly = true)
    public inventory_category_response find_category(long id) {
        return require_category(id);
    }

    @Transactional
    public inventory_category_response create_category(inventory_category_request request,
                                                       authenticated_user actor, String correlation_id) {
        validate_category(request);
        String code = normalize_required(request.category_code());
        ensure_category_code_available(code, null);
        try {
            Long id = master_data_repository.insert_category(code, normalize_text(request.category_name()),
                    status_or_default(request.status()), actor.user_id());
            inventory_category_response result = require_category(id);
            audit_writer.write(actor.user_id(), "inventory", "category_create", "item_category",
                    String.valueOf(id), correlation_id, Map.of("category_code", code));
            logger.info("Đã tạo nhóm mặt hàng; item_category_id={}, actor_user_id={}, correlation_id={}",
                    id, actor.user_id(), correlation_id);
            return result;
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("category_code", "Category code already exists.");
        }
    }

    @Transactional
    public inventory_category_response update_category(long id, inventory_category_request request,
                                                       authenticated_user actor, String correlation_id) {
        validate_category(request);
        String code = normalize_required(request.category_code());
        ensure_category_code_available(code, id);
        require_category(id);
        try {
            int updated = master_data_repository.update_category(id, code,
                    normalize_text(request.category_name()), status_or_default(request.status()), actor.user_id());
            if (updated == 0) {
                throw new resource_not_found_exception("Item category");
            }
            inventory_category_response result = require_category(id);
            audit_writer.write(actor.user_id(), "inventory", "category_update", "item_category",
                    String.valueOf(id), correlation_id, Map.of("category_code", code));
            logger.info("Đã cập nhật nhóm mặt hàng; item_category_id={}, actor_user_id={}, correlation_id={}",
                    id, actor.user_id(), correlation_id);
            return result;
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("category_code", "Category code already exists.");
        }
    }

    @Transactional
    public inventory_category_response deactivate_category(long id, authenticated_user actor,
                                                            String correlation_id) {
        inventory_category_response current = require_category(id);
        int updated = master_data_repository.deactivate_category(id, actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Item category");
        }
        inventory_category_response result = require_category(id);
        audit_writer.write(actor.user_id(), "inventory", "category_deactivate", "item_category",
                String.valueOf(id), correlation_id, Map.of("category_code", current.category_code()));
        logger.info("Đã vô hiệu hóa nhóm mặt hàng; item_category_id={}, actor_user_id={}, correlation_id={}",
                id, actor.user_id(), correlation_id);
        return result;
    }

    @Transactional(readOnly = true)
    public master_data_page_response<inventory_supplier_response> search_suppliers(
            String search, String status, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = safe_page_size(page_size);
        String normalized_search = normalize_lower(search);
        String normalized_status = normalize_lower(status);
        validate_status(normalized_status);
        long total = master_data_repository.count_suppliers(normalized_search, normalized_status);
        return page_response(master_data_repository.search_suppliers(normalized_search, normalized_status,
                safe_page, safe_page_size), safe_page, safe_page_size, total);
    }

    @Transactional(readOnly = true)
    public inventory_supplier_response find_supplier(long id) {
        return require_supplier(id);
    }

    @Transactional
    public inventory_supplier_response create_supplier(inventory_supplier_request request,
                                                       authenticated_user actor, String correlation_id) {
        validate_supplier(request);
        String code = normalize_required(request.supplier_code());
        ensure_supplier_code_available(code, null);
        try {
            Long id = master_data_repository.insert_supplier(code, normalize_text(request.supplier_name()),
                    normalize_optional(request.phone_number()), normalize_email(request.email()),
                    normalize_optional(request.address()), status_or_default(request.status()), actor.user_id());
            inventory_supplier_response result = require_supplier(id);
            audit_writer.write(actor.user_id(), "inventory", "supplier_create", "supplier",
                    String.valueOf(id), correlation_id, Map.of("supplier_code", code));
            logger.info("Đã tạo nhà cung cấp; supplier_id={}, actor_user_id={}, correlation_id={}",
                    id, actor.user_id(), correlation_id);
            return result;
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("supplier_code", "Supplier code already exists.");
        }
    }

    @Transactional
    public inventory_supplier_response update_supplier(long id, inventory_supplier_request request,
                                                       authenticated_user actor, String correlation_id) {
        validate_supplier(request);
        String code = normalize_required(request.supplier_code());
        ensure_supplier_code_available(code, id);
        require_supplier(id);
        try {
            int updated = master_data_repository.update_supplier(id, code, normalize_text(request.supplier_name()),
                    normalize_optional(request.phone_number()), normalize_email(request.email()),
                    normalize_optional(request.address()), status_or_default(request.status()), actor.user_id());
            if (updated == 0) {
                throw new resource_not_found_exception("Supplier");
            }
            inventory_supplier_response result = require_supplier(id);
            audit_writer.write(actor.user_id(), "inventory", "supplier_update", "supplier",
                    String.valueOf(id), correlation_id, Map.of("supplier_code", code));
            logger.info("Đã cập nhật nhà cung cấp; supplier_id={}, actor_user_id={}, correlation_id={}",
                    id, actor.user_id(), correlation_id);
            return result;
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("supplier_code", "Supplier code already exists.");
        }
    }

    @Transactional
    public inventory_supplier_response deactivate_supplier(long id, authenticated_user actor,
                                                            String correlation_id) {
        inventory_supplier_response current = require_supplier(id);
        int updated = master_data_repository.deactivate_supplier(id, actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Supplier");
        }
        inventory_supplier_response result = require_supplier(id);
        audit_writer.write(actor.user_id(), "inventory", "supplier_deactivate", "supplier",
                String.valueOf(id), correlation_id, Map.of("supplier_code", current.supplier_code()));
        logger.info("Đã vô hiệu hóa nhà cung cấp; supplier_id={}, actor_user_id={}, correlation_id={}",
                id, actor.user_id(), correlation_id);
        return result;
    }

    @Transactional(readOnly = true)
    public master_data_page_response<inventory_warehouse_response> search_warehouses(
            String search, String status, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = safe_page_size(page_size);
        String normalized_search = normalize_lower(search);
        String normalized_status = normalize_lower(status);
        validate_status(normalized_status);
        long total = master_data_repository.count_warehouses(normalized_search, normalized_status);
        return page_response(master_data_repository.search_warehouses(normalized_search, normalized_status,
                safe_page, safe_page_size), safe_page, safe_page_size, total);
    }

    @Transactional(readOnly = true)
    public inventory_warehouse_response find_warehouse(long id) {
        return require_warehouse(id);
    }

    @Transactional
    public inventory_warehouse_response create_warehouse(inventory_warehouse_request request,
                                                         authenticated_user actor, String correlation_id) {
        validate_warehouse(request);
        String code = normalize_required(request.warehouse_code());
        ensure_warehouse_code_available(code, null);
        try {
            Long id = master_data_repository.insert_warehouse(code, normalize_text(request.warehouse_name()),
                    normalize_optional(request.address()), status_or_default(request.status()), actor.user_id());
            inventory_warehouse_response result = require_warehouse(id);
            audit_writer.write(actor.user_id(), "inventory", "warehouse_create", "warehouse",
                    String.valueOf(id), correlation_id, Map.of("warehouse_code", code));
            logger.info("Đã tạo kho; warehouse_id={}, actor_user_id={}, correlation_id={}",
                    id, actor.user_id(), correlation_id);
            return result;
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("warehouse_code", "Warehouse code already exists.");
        }
    }

    @Transactional
    public inventory_warehouse_response update_warehouse(long id, inventory_warehouse_request request,
                                                         authenticated_user actor, String correlation_id) {
        validate_warehouse(request);
        String code = normalize_required(request.warehouse_code());
        ensure_warehouse_code_available(code, id);
        require_warehouse(id);
        try {
            int updated = master_data_repository.update_warehouse(id, code,
                    normalize_text(request.warehouse_name()), normalize_optional(request.address()),
                    status_or_default(request.status()), actor.user_id());
            if (updated == 0) {
                throw new resource_not_found_exception("Warehouse");
            }
            inventory_warehouse_response result = require_warehouse(id);
            audit_writer.write(actor.user_id(), "inventory", "warehouse_update", "warehouse",
                    String.valueOf(id), correlation_id, Map.of("warehouse_code", code));
            logger.info("Đã cập nhật kho; warehouse_id={}, actor_user_id={}, correlation_id={}",
                    id, actor.user_id(), correlation_id);
            return result;
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("warehouse_code", "Warehouse code already exists.");
        }
    }

    @Transactional
    public inventory_warehouse_response deactivate_warehouse(long id, authenticated_user actor,
                                                              String correlation_id) {
        inventory_warehouse_response current = require_warehouse(id);
        int updated = master_data_repository.deactivate_warehouse(id, actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Warehouse");
        }
        inventory_warehouse_response result = require_warehouse(id);
        audit_writer.write(actor.user_id(), "inventory", "warehouse_deactivate", "warehouse",
                String.valueOf(id), correlation_id, Map.of("warehouse_code", current.warehouse_code()));
        logger.info("Đã vô hiệu hóa kho; warehouse_id={}, actor_user_id={}, correlation_id={}",
                id, actor.user_id(), correlation_id);
        return result;
    }

    @Transactional(readOnly = true)
    public master_data_page_response<inventory_location_response> search_locations(
            long warehouse_id, String search, String status, int page, int page_size) {
        require_warehouse(warehouse_id);
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = safe_page_size(page_size);
        String normalized_search = normalize_lower(search);
        String normalized_status = normalize_lower(status);
        validate_status(normalized_status);
        long total = master_data_repository.count_locations(warehouse_id, normalized_search, normalized_status);
        return page_response(master_data_repository.search_locations(warehouse_id, normalized_search, normalized_status,
                safe_page, safe_page_size), safe_page, safe_page_size, total);
    }

    @Transactional(readOnly = true)
    public inventory_location_response find_location(long id) {
        return require_location(id);
    }

    @Transactional
    public inventory_location_response create_location(inventory_location_request request,
                                                       authenticated_user actor, String correlation_id) {
        validate_location(request);
        ensure_active_warehouse(request.warehouse_id());
        String code = normalize_required(request.location_code());
        ensure_location_code_available(request.warehouse_id(), code, null);
        try {
            Long id = master_data_repository.insert_location(request.warehouse_id(), code,
                    normalize_text(request.location_name()), status_or_default(request.status()), actor.user_id());
            inventory_location_response result = require_location(id);
            audit_writer.write(actor.user_id(), "inventory", "location_create", "warehouse_location",
                    String.valueOf(id), correlation_id, Map.of("location_code", code));
            logger.info("Đã tạo vị trí kho; warehouse_location_id={}, actor_user_id={}, correlation_id={}",
                    id, actor.user_id(), correlation_id);
            return result;
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("location_code", "Location code already exists in the warehouse.");
        }
    }

    @Transactional
    public inventory_location_response update_location(long id, inventory_location_request request,
                                                       authenticated_user actor, String correlation_id) {
        validate_location(request);
        inventory_location_response current = require_location(id);
        if (current.warehouse_id() != request.warehouse_id()) {
            throw new field_conflict_exception("warehouse_id", "Warehouse location cannot be moved after creation.");
        }
        String code = normalize_required(request.location_code());
        ensure_location_code_available(request.warehouse_id(), code, id);
        try {
            int updated = master_data_repository.update_location(id, code,
                    normalize_text(request.location_name()), status_or_default(request.status()), actor.user_id());
            if (updated == 0) {
                throw new resource_not_found_exception("Warehouse location");
            }
            inventory_location_response result = require_location(id);
            audit_writer.write(actor.user_id(), "inventory", "location_update", "warehouse_location",
                    String.valueOf(id), correlation_id, Map.of("location_code", code));
            logger.info("Đã cập nhật vị trí kho; warehouse_location_id={}, actor_user_id={}, correlation_id={}",
                    id, actor.user_id(), correlation_id);
            return result;
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("location_code", "Location code already exists in the warehouse.");
        }
    }

    @Transactional
    public inventory_location_response deactivate_location(long id, authenticated_user actor,
                                                            String correlation_id) {
        inventory_location_response current = require_location(id);
        int updated = master_data_repository.deactivate_location(id, actor.user_id());
        if (updated == 0) {
            throw new resource_not_found_exception("Warehouse location");
        }
        inventory_location_response result = require_location(id);
        audit_writer.write(actor.user_id(), "inventory", "location_deactivate", "warehouse_location",
                String.valueOf(id), correlation_id, Map.of("location_code", current.location_code()));
        logger.info("Đã vô hiệu hóa vị trí kho; warehouse_location_id={}, actor_user_id={}, correlation_id={}",
                id, actor.user_id(), correlation_id);
        return result;
    }

    private void validate_unit(inventory_unit_request request) {
        if (request == null) {
            throw new IllegalArgumentException("Unit request is required.");
        }
        require_text(request.unit_code(), "Unit code", 32);
        require_text(request.unit_name(), "Unit name", 100);
        short decimal_places = decimal_places(request);
        if (decimal_places < 0 || decimal_places > 6) {
            throw new IllegalArgumentException("Decimal places must be between 0 and 6.");
        }
        validate_status(status_or_default(request.status()));
    }

    private void validate_category(inventory_category_request request) {
        if (request == null) {
            throw new IllegalArgumentException("Category request is required.");
        }
        require_text(request.category_code(), "Category code", 40);
        require_text(request.category_name(), "Category name", 120);
        validate_status(status_or_default(request.status()));
    }

    private void validate_supplier(inventory_supplier_request request) {
        if (request == null) {
            throw new IllegalArgumentException("Supplier request is required.");
        }
        require_text(request.supplier_code(), "Supplier code", 60);
        require_text(request.supplier_name(), "Supplier name", 180);
        if (normalize_optional(request.phone_number()) != null && normalize_optional(request.phone_number()).length() > 30) {
            throw new IllegalArgumentException("Phone number must contain at most 30 characters.");
        }
        if (normalize_optional(request.email()) != null && normalize_optional(request.email()).length() > 254) {
            throw new IllegalArgumentException("Email must contain at most 254 characters.");
        }
        if (normalize_optional(request.address()) != null && normalize_optional(request.address()).length() > 2000) {
            throw new IllegalArgumentException("Supplier address must contain at most 2000 characters.");
        }
        validate_status(status_or_default(request.status()));
    }

    private void validate_warehouse(inventory_warehouse_request request) {
        if (request == null) {
            throw new IllegalArgumentException("Warehouse request is required.");
        }
        require_text(request.warehouse_code(), "Warehouse code", 40);
        require_text(request.warehouse_name(), "Warehouse name", 160);
        if (normalize_optional(request.address()) != null && normalize_optional(request.address()).length() > 2000) {
            throw new IllegalArgumentException("Warehouse address must contain at most 2000 characters.");
        }
        validate_status(status_or_default(request.status()));
    }

    private void validate_location(inventory_location_request request) {
        if (request == null) {
            throw new IllegalArgumentException("Warehouse location request is required.");
        }
        if (request.warehouse_id() == null || request.warehouse_id() <= 0) {
            throw new IllegalArgumentException("Warehouse is required.");
        }
        require_text(request.location_code(), "Location code", 60);
        require_text(request.location_name(), "Location name", 160);
        validate_status(status_or_default(request.status()));
    }

    private short decimal_places(inventory_unit_request request) {
        return request.decimal_places() == null ? 3 : request.decimal_places();
    }

    private void ensure_active_warehouse(long warehouse_id) {
        if (!master_data_repository.active_warehouse(warehouse_id)) {
            throw new resource_not_found_exception("Active warehouse");
        }
    }

    private void ensure_unit_code_available(String code, Long id) {
        if (master_data_repository.unit_code_exists(code, id)) {
            throw new field_conflict_exception("unit_code", "Unit code already exists.");
        }
    }

    private void ensure_category_code_available(String code, Long id) {
        if (master_data_repository.category_code_exists(code, id)) {
            throw new field_conflict_exception("category_code", "Category code already exists.");
        }
    }

    private void ensure_supplier_code_available(String code, Long id) {
        if (master_data_repository.supplier_code_exists(code, id)) {
            throw new field_conflict_exception("supplier_code", "Supplier code already exists.");
        }
    }

    private void ensure_warehouse_code_available(String code, Long id) {
        if (master_data_repository.warehouse_code_exists(code, id)) {
            throw new field_conflict_exception("warehouse_code", "Warehouse code already exists.");
        }
    }

    private void ensure_location_code_available(long warehouse_id, String code, Long id) {
        if (master_data_repository.location_code_exists(warehouse_id, code, id)) {
            throw new field_conflict_exception("location_code", "Location code already exists in the warehouse.");
        }
    }

    private inventory_unit_response require_unit(Long id) {
        if (id == null) {
            throw new resource_not_found_exception("Unit of measure");
        }
        inventory_unit_response result = master_data_repository.find_unit(id);
        if (result == null) {
            throw new resource_not_found_exception("Unit of measure");
        }
        return result;
    }

    private inventory_category_response require_category(Long id) {
        if (id == null) {
            throw new resource_not_found_exception("Item category");
        }
        inventory_category_response result = master_data_repository.find_category(id);
        if (result == null) {
            throw new resource_not_found_exception("Item category");
        }
        return result;
    }

    private inventory_supplier_response require_supplier(Long id) {
        if (id == null) {
            throw new resource_not_found_exception("Supplier");
        }
        inventory_supplier_response result = master_data_repository.find_supplier(id);
        if (result == null) {
            throw new resource_not_found_exception("Supplier");
        }
        return result;
    }

    private inventory_warehouse_response require_warehouse(Long id) {
        if (id == null) {
            throw new resource_not_found_exception("Warehouse");
        }
        inventory_warehouse_response result = master_data_repository.find_warehouse(id);
        if (result == null) {
            throw new resource_not_found_exception("Warehouse");
        }
        return result;
    }

    private inventory_location_response require_location(Long id) {
        if (id == null) {
            throw new resource_not_found_exception("Warehouse location");
        }
        inventory_location_response result = master_data_repository.find_location(id);
        if (result == null) {
            throw new resource_not_found_exception("Warehouse location");
        }
        return result;
    }

    private <T> master_data_page_response<T> page_response(List<T> items, int page, int page_size, long total) {
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / page_size);
        return new master_data_page_response<>(items, page, page_size, total, total_pages);
    }

    private int safe_page_size(int requested) {
        return Math.min(Math.max(requested, 1), max_page_size);
    }

    private void require_text(String value, String label, int max_length) {
        String normalized = normalize_optional(value);
        if (normalized == null || normalized.length() > max_length) {
            throw new IllegalArgumentException(label + " is required and must contain at most " + max_length + " characters.");
        }
    }

    private void validate_status(String status) {
        if (status != null && !valid_statuses.contains(status)) {
            throw new IllegalArgumentException("Master data status is invalid.");
        }
    }

    private String status_or_default(String status) {
        String normalized = normalize_lower(status);
        return normalized == null ? "active" : normalized;
    }

    private String normalize_text(String value) {
        return value == null ? null : value.trim();
    }

    private String normalize_required(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalize_email(String value) {
        String normalized = normalize_optional(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
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
