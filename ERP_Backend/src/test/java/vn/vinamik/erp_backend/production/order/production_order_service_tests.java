package vn.vinamik.erp_backend.production.order;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class production_order_service_tests {
    @Test
    void rejects_invalid_date_range_before_database_call() {
        production_order_service service = new production_order_service(null, null, (audit_event_writer) null);
        production_order_request request = new production_order_request(
                "order_001", 1L, 1L, new BigDecimal("10.000000"),
                LocalDate.of(2026, 2, 2), LocalDate.of(2026, 2, 1), null, null);

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_invalid_quantity_scale_before_database_call() {
        production_order_service service = new production_order_service(null, null, (audit_event_writer) null);
        production_order_request request = new production_order_request(
                "order_001", 1L, 1L, new BigDecimal("10.1234567"),
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1), null, null);

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_missing_plan_or_bom_reference_before_database_call() {
        production_order_service service = new production_order_service(null, null, (audit_event_writer) null);
        production_order_request missing_plan = new production_order_request(
                "order_001", null, 1L, new BigDecimal("10.000000"),
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1), null, null);
        production_order_request missing_bom = new production_order_request(
                "order_001", 1L, null, new BigDecimal("10.000000"),
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1), null, null);

        assertThrows(IllegalArgumentException.class,
                () -> service.create(missing_plan, null, "test-correlation"));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(missing_bom, null, "test-correlation"));
    }

    @Test
    void rejects_missing_order_code_before_database_call() {
        production_order_service service = new production_order_service(null, null, (audit_event_writer) null);
        production_order_request request = new production_order_request(
                "  ", 1L, 1L, new BigDecimal("10.000000"),
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1), null, null);

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }
    @Test
    void rejects_invalid_status_filter_before_database_call() {
        production_order_service service = new production_order_service(null, null, (audit_event_writer) null);

        assertThrows(IllegalArgumentException.class, () -> service.search(null, "unknown", null, 0, 50));
    }
}
