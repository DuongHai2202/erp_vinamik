package vn.vinamik.erp_backend.human_resources.employee;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class human_resources_employee_service_tests {
    @Test
    void rejects_termination_date_before_hiring_date_without_database_call() {
        human_resources_employee_service service = new human_resources_employee_service(null, (audit_event_writer) null);
        employee_request request = new employee_request(
                "employee_001",
                "Nguyen Van A",
                null,
                null,
                null,
                null,
                null,
                null,
                "active",
                java.time.LocalDate.of(2026, 3, 2),
                java.time.LocalDate.of(2026, 3, 1),
                null);

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_non_vietnamese_phone_before_database_call() {
        human_resources_employee_service service = new human_resources_employee_service(null, (audit_event_writer) null);
        employee_request request = new employee_request(
                "employee_002",
                "Nguyen Van B",
                null,
                "0912-abcd",
                null,
                null,
                null,
                null,
                "active",
                LocalDate.of(2026, 3, 2),
                null,
                null);

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_future_date_of_birth_before_database_call() {
        human_resources_employee_service service = new human_resources_employee_service(null, (audit_event_writer) null);
        employee_request request = new employee_request(
                "employee_003",
                "Nguyen Van C",
                LocalDate.now().plusDays(1),
                null,
                null,
                null,
                null,
                null,
                "active",
                LocalDate.of(2026, 3, 2),
                null,
                null);

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }
}
