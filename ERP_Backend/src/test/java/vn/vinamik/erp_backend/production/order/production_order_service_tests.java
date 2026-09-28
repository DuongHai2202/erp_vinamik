package vn.vinamik.erp_backend.production.order;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void rejects_non_draft_initial_status_before_database_call() {
        production_order_service service = new production_order_service(null, null, (audit_event_writer) null);
        production_order_request request = new production_order_request(
                "order_001", 1L, 1L, new BigDecimal("10.000000"),
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1), null, null, "released");

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void forwards_output_ready_filter_to_repository() {
        production_order_repository repository = mock(production_order_repository.class);
        when(repository.count(null, null, null, true)).thenReturn(0L);
        when(repository.search(null, null, null, true, 50, 0)).thenReturn(List.of());
        production_order_service service = new production_order_service(repository, null, (audit_event_writer) null);

        production_order_page_response result = service.search(null, null, null, true, 0, 50);

        assertEquals(0L, result.total_items());
        verify(repository).count(null, null, null, true);
        verify(repository).search(null, null, null, true, 50, 0);
    }
}
