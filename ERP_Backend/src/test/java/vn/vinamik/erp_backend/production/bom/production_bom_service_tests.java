package vn.vinamik.erp_backend.production.bom;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class production_bom_service_tests {
    @Test
    void rejects_invalid_validity_range_without_database_call() {
        production_bom_service service = new production_bom_service(null, null, (audit_event_writer) null);
        bom_request request = request(LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 1), 1);

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_non_positive_version_without_database_call() {
        production_bom_service service = new production_bom_service(null, null, (audit_event_writer) null);
        bom_request request = request(LocalDate.of(2026, 4, 1), null, 0);

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    private bom_request request(LocalDate valid_from, LocalDate valid_to, int version_number) {
        return new bom_request("bom_001", 1L, version_number, new BigDecimal("1.000000"), valid_from, valid_to, null,
                List.of(new bom_line_request(2L, new BigDecimal("0.500000"), new BigDecimal("1.00"), null)));
    }
}
