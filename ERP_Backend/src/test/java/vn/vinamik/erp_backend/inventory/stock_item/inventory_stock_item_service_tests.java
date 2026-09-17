package vn.vinamik.erp_backend.inventory.stock_item;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;

import static org.junit.jupiter.api.Assertions.assertThrows;

class inventory_stock_item_service_tests {
    @Test
    void rejects_blank_item_code_before_database_call() {
        inventory_stock_item_service service = new inventory_stock_item_service(null, null, (audit_event_writer) null);
        stock_item_request request = new stock_item_request(
                "  ", "Material", null, 1L, false, null, "active", null);

        assertThrows(IllegalArgumentException.class,
                () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_missing_base_unit_before_database_call() {
        inventory_stock_item_service service = new inventory_stock_item_service(null, null, (audit_event_writer) null);
        stock_item_request request = new stock_item_request(
                "material_001", "Material", null, null, false, null, "active", null);

        assertThrows(IllegalArgumentException.class,
                () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_unknown_item_type_before_database_call() {
        inventory_stock_item_service service = new inventory_stock_item_service(null, null, (audit_event_writer) null);
        stock_item_request request = new stock_item_request(
                "material_001", "Material", null, 1L, false, null, "active", null, "work_in_progress");

        assertThrows(IllegalArgumentException.class,
                () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_missing_active_unit_reference_before_database_call() {
        inventory_stock_item_service service = new inventory_stock_item_service(null, null, (audit_event_writer) null);
        stock_item_request request = new stock_item_request(
                "material_001", "Material", null, 1L, false, null, "active", null);

        assertThrows(resource_not_found_exception.class,
                () -> service.create(request, null, "test-correlation"));
    }
}