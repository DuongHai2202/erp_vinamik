package vn.vinamik.erp_backend.inventory.transfer;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class transfer_service_tests {
    @Test
    void rejects_empty_transfer_lines_before_database_call() {
        inventory_transfer_service service = new inventory_transfer_service(null, (audit_event_writer) null);
        transfer_request request = new transfer_request("transfer_001", 1L, 2L, null, null, List.of());

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_same_source_and_destination_location_before_database_call() {
        inventory_transfer_service service = new inventory_transfer_service(null, (audit_event_writer) null);
        transfer_request request = new transfer_request("transfer_001", 1L, 2L, null, null, List.of(
                new transfer_line_request(1L, null, 10L, 10L, new BigDecimal("1.000000"))));

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_invalid_status_filter_before_database_call() {
        inventory_transfer_service service = new inventory_transfer_service(null, (audit_event_writer) null);

        assertThrows(IllegalArgumentException.class, () -> service.search(null, null, null, "unknown", 0, 50));
    }

    @Test
    void rejects_same_source_and_destination_warehouse_before_database_call() {
        inventory_transfer_service service = new inventory_transfer_service(null, (audit_event_writer) null);
        transfer_request request = new transfer_request("transfer_001", 1L, 1L, null, null, List.of(
                new transfer_line_request(1L, null, 10L, 11L, new BigDecimal("1.000000"))));

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }
}
