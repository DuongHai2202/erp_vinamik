package vn.vinamik.erp_backend.production.assignment;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.human_resources.api.human_resources_employee_contract;
import vn.vinamik.erp_backend.human_resources.api.human_resources_work_shift_contract;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

class production_assignment_service_tests {
    @Test
    void rejects_end_time_before_start_time_before_database_call() {
        production_assignment_service service = new production_assignment_service(
                null, (human_resources_employee_contract) null, (human_resources_work_shift_contract) null,
                (audit_event_writer) null);
        production_assignment_request request = new production_assignment_request(
                1L, 1L, null, null, Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T09:00:00Z"), null);

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_invalid_status_filter_before_database_call() {
        production_assignment_service service = new production_assignment_service(
                null, (human_resources_employee_contract) null, (human_resources_work_shift_contract) null,
                (audit_event_writer) null);

        assertThrows(IllegalArgumentException.class, () -> service.search(null, null, "unknown", null, null, 0, 50));
    }

    @Test
    void rejects_invalid_time_filter_before_database_call() {
        production_assignment_service service = new production_assignment_service(
                null, (human_resources_employee_contract) null, (human_resources_work_shift_contract) null,
                (audit_event_writer) null);

        assertThrows(IllegalArgumentException.class, () -> service.search(null, null, null,
                Instant.parse("2026-01-02T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"), 0, 50));
    }
}
