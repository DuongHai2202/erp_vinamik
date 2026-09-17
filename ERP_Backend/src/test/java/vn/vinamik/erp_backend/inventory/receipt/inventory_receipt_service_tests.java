package vn.vinamik.erp_backend.inventory.receipt;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class inventory_receipt_service_tests {
    @Test
    void rejects_source_module_without_source_document_before_database_call() {
        inventory_receipt_service service = new inventory_receipt_service(null, (audit_event_writer) null);
        receipt_request request = new receipt_request("receipt_001", 1L, null, "production", null, null, null, null, List.of(
                new receipt_line_request(1L, 1L, null, new java.math.BigDecimal("1.000000"))));

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_empty_receipt_lines_before_database_call() {
        inventory_receipt_service service = new inventory_receipt_service(null, (audit_event_writer) null);
        receipt_request request = new receipt_request("receipt_001", 1L, null, null, null, null, null, null, List.of());

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_invalid_status_filter_before_database_call() {
        inventory_receipt_service service = new inventory_receipt_service(null, (audit_event_writer) null);

        assertThrows(IllegalArgumentException.class, () -> service.search(null, null, "unknown", 0, 50));
    }

    @Test
    void rejects_null_request_before_database_call() {
        inventory_receipt_service service = new inventory_receipt_service(null, (audit_event_writer) null);

        assertThrows(IllegalArgumentException.class, () -> service.create(null, null, "test-correlation"));
    }

    @Test
    void rejects_incomplete_line_before_database_call() {
        inventory_receipt_service service = new inventory_receipt_service(null, (audit_event_writer) null);
        receipt_request request = new receipt_request("receipt_001", 1L, null, null, null, null, null, null,
                List.of(new receipt_line_request(null, 1L, null, new java.math.BigDecimal("1.000000"))));

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }
}
