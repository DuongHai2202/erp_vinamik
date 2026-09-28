package vn.vinamik.erp_backend.production.bom;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void allows_active_bom_to_be_deactivated() {
        production_bom_repository repository = mock(production_bom_repository.class);
        audit_event_writer audit_writer = mock(audit_event_writer.class);
        production_bom_service service = new production_bom_service(repository, null, audit_writer);
        authenticated_user actor = new authenticated_user(9L, "admin", List.of("production_bom_approve"));
        bom_response changed = new bom_response(
                1L, "bom_001", 2L, "fg_001", "Thành phẩm mẫu", 1,
                new BigDecimal("1.000000"), "cái", LocalDate.of(2026, 1, 1), null,
                "inactive", null, List.of());

        when(repository.current_status(1L)).thenReturn("active");
        when(repository.change_status(1L, "inactive", 9L)).thenReturn(1);
        when(repository.find(1L)).thenReturn(changed);

        bom_response result = service.change_status(1L, "inactive", actor, "test-correlation");

        assertEquals("inactive", result.status());
        verify(repository).change_status(1L, "inactive", 9L);
    }

    private bom_request request(LocalDate valid_from, LocalDate valid_to, int version_number) {
        return new bom_request("bom_001", 1L, version_number, new BigDecimal("1.000000"), valid_from, valid_to, null,
                List.of(new bom_line_request(2L, new BigDecimal("0.500000"), new BigDecimal("1.00"), null)));
    }
}
