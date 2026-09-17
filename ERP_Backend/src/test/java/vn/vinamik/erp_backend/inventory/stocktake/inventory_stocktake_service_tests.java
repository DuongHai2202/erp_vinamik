package vn.vinamik.erp_backend.inventory.stocktake;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

class inventory_stocktake_service_tests {
    @Test
    void rejects_missing_warehouse_before_database_call() {
        inventory_stocktake_service service = new inventory_stocktake_service(null, (audit_event_writer) null);

        assertThrows(IllegalArgumentException.class, () -> service.create(
                new stocktake_request("stocktake_001", null, null), null, "test-correlation"));
    }

    @Test
    void rejects_invalid_status_filter_before_database_call() {
        inventory_stocktake_service service = new inventory_stocktake_service(null, (audit_event_writer) null);

        assertThrows(IllegalArgumentException.class, () -> service.search(null, null, "unknown", 0, 50));
    }

    @Test
    void rejects_negative_counted_quantity_before_database_call() {
        inventory_stocktake_service service = new inventory_stocktake_service(null, (audit_event_writer) null);

        assertThrows(IllegalArgumentException.class, () -> service.count_line(
                1L, 1L, new stocktake_count_request(new BigDecimal("-1.000000")), null, "test-correlation"));
    }

    @Test
    void rejects_blank_stocktake_code_before_database_call() {
        inventory_stocktake_service service = new inventory_stocktake_service(null, (audit_event_writer) null);

        assertThrows(IllegalArgumentException.class, () -> service.create(
                new stocktake_request("  ", 1L, null), null, "test-correlation"));
    }
}
