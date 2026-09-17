package vn.vinamik.erp_backend.human_resources.absence;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class human_resources_absence_service_tests {
    @Test
    void rejects_end_date_before_start_date_without_database_call() {
        human_resources_absence_service service = new human_resources_absence_service(null, (audit_event_writer) null);
        leave_request request = request(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 1), "pending");

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_invalid_initial_status_without_database_call() {
        human_resources_absence_service service = new human_resources_absence_service(null, (audit_event_writer) null);
        leave_request request = request(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2), "approved");

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_invalid_status_filter_without_database_call() {
        human_resources_absence_service service = new human_resources_absence_service(null, (audit_event_writer) null);

        assertThrows(IllegalArgumentException.class, () -> service.search(null, null, "unknown", null, null, 0, 50));
    }

    private leave_request request(LocalDate starts_on, LocalDate ends_on, String status) {
        return new leave_request("leave_001", 1L, "annual", starts_on, ends_on, true, "Family event", status);
    }
}
