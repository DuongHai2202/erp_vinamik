package vn.vinamik.erp_backend.inventory.issue;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class inventory_issue_service_tests {
    @Test
    void rejects_source_module_without_source_document_before_database_call() {
        inventory_issue_service service = new inventory_issue_service((inventory_issue_repository) null, null);
        issue_request request = new issue_request("issue_001", 1L, "production", null, "production_use", null, null, List.of(
                new issue_line_request(1L, 1L, null, new java.math.BigDecimal("1.000000"))));

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_empty_issue_lines_before_database_call() {
        inventory_issue_service service = new inventory_issue_service((inventory_issue_repository) null, null);
        issue_request request = new issue_request("issue_001", 1L, null, null, null, null, null, List.of());

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_invalid_status_filter_before_database_call() {
        inventory_issue_service service = new inventory_issue_service((inventory_issue_repository) null, null);

        assertThrows(IllegalArgumentException.class, () -> service.search(null, null, "unknown", 0, 50));
    }

    @Test
    void rejects_null_request_before_database_call() {
        inventory_issue_service service = new inventory_issue_service((inventory_issue_repository) null, null);

        assertThrows(IllegalArgumentException.class, () -> service.create(null, null, "test-correlation"));
    }

    @Test
    void rejects_incomplete_line_before_database_call() {
        inventory_issue_service service = new inventory_issue_service((inventory_issue_repository) null, null);
        issue_request request = new issue_request("issue_001", 1L, null, null, null, null, null, List.of(
                new issue_line_request(1L, null, null, new java.math.BigDecimal("1.000000"))));

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }
}

