package vn.vinamik.erp_backend.production.material_consumption;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class production_material_consumption_service_tests {
    @Test
    void rejects_incomplete_request_before_database_call() {
        production_material_consumption_service service =
                new production_material_consumption_service(null, null, (audit_event_writer) null);
        material_consumption_request request = new material_consumption_request(
                1L, "request_001", null, List.of());

        assertThrows(IllegalArgumentException.class,
                () -> service.create(1, request, null, "test-correlation"));
    }

    @Test
    void rejects_duplicate_material_lines_before_database_call() {
        production_material_consumption_service service =
                new production_material_consumption_service(null, null, (audit_event_writer) null);
        material_consumption_line_request line = new material_consumption_line_request(
                4L, 2L, null, new BigDecimal("1.000000"));
        material_consumption_request request = new material_consumption_request(
                1L, "request_001", null, List.of(line, line));

        assertThrows(IllegalArgumentException.class,
                () -> service.create(1, request, null, "test-correlation"));
    }

    @Test
    void rejects_quantity_with_more_than_six_decimal_places_before_database_call() {
        production_material_consumption_service service =
                new production_material_consumption_service(null, null, (audit_event_writer) null);
        material_consumption_request request = new material_consumption_request(
                1L, "request_001", null,
                List.of(new material_consumption_line_request(
                        4L, 2L, null, new BigDecimal("1.0000001"))));

        assertThrows(IllegalArgumentException.class,
                () -> service.create(1, request, null, "test-correlation"));
    }
}

