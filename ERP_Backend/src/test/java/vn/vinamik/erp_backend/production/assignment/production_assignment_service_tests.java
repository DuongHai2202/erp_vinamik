package vn.vinamik.erp_backend.production.assignment;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.human_resources.api.human_resources_employee_contract;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import vn.vinamik.erp_backend.human_resources.api.human_resources_work_shift_contract;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

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
    @Test
    void creates_valid_assignment_and_writes_audit() {
        production_assignment_repository assignment_repository = mock(production_assignment_repository.class);
        human_resources_employee_contract employee_contract = mock(human_resources_employee_contract.class);
        human_resources_work_shift_contract shift_contract = mock(human_resources_work_shift_contract.class);
        audit_event_writer audit_writer = mock(audit_event_writer.class);
        var employee = new vn.vinamik.erp_backend.human_resources.api.human_resources_employee_snapshot(
                2L, "vmk0002", "Nguyễn Văn A", "active");
        var shift = new vn.vinamik.erp_backend.human_resources.api.human_resources_work_shift_snapshot(
                3L, "ca_sang", "Ca sáng", java.time.LocalTime.of(6, 0),
                java.time.LocalTime.of(14, 0), "active");
        var request = valid_request();
        var row = new assignment_row(
                10L, 1L, "order_001", 2L, 3L, "Vận hành thiết bị",
                request.starts_at(), request.ends_at(), "planned", "test");
        when(assignment_repository.find_order(1L))
                .thenReturn(new production_order_snapshot(1L, "order_001", "released"));
        when(employee_contract.find_employee(2L)).thenReturn(java.util.Optional.of(employee));
        when(shift_contract.find_work_shift(3L)).thenReturn(java.util.Optional.of(shift));
        when(assignment_repository.has_overlap(2L, request.starts_at(), request.ends_at(), null))
                .thenReturn(false);
        when(assignment_repository.insert(1L, 2L, 3L, "Vận hành thiết bị",
                request.starts_at(), request.ends_at(), "test", 7L)).thenReturn(10L);
        when(assignment_repository.find(10L)).thenReturn(row);

        production_assignment_service service = new production_assignment_service(
                assignment_repository, employee_contract, shift_contract, audit_writer);

        production_assignment_response result = service.create(
                request, new authenticated_user(7L, "production_test", java.util.List.of()), "corr-001");

        assertEquals("planned", result.status());
        assertEquals("Nguyễn Văn A", result.employee_name());
        verify(audit_writer).write(7L, "production", "production_assignment_create",
                "production_assignment", "10", "corr-001",
                java.util.Map.of("production_order_id", 1L, "employee_id", 2L));
    }

    @Test
    void rejects_assignment_for_terminated_employee() {
        production_assignment_repository assignment_repository = mock(production_assignment_repository.class);
        human_resources_employee_contract employee_contract = mock(human_resources_employee_contract.class);
        var employee = new vn.vinamik.erp_backend.human_resources.api.human_resources_employee_snapshot(
                2L, "vmk0002", "Nguyễn Văn A", "terminated");
        when(assignment_repository.find_order(1L))
                .thenReturn(new production_order_snapshot(1L, "order_001", "released"));
        when(employee_contract.find_employee(2L)).thenReturn(java.util.Optional.of(employee));

        production_assignment_service service = new production_assignment_service(
                assignment_repository, employee_contract, null, mock(audit_event_writer.class));

        assertThrows(IllegalArgumentException.class,
                () -> service.create(valid_request(),
                        new authenticated_user(7L, "production_test", java.util.List.of()), "corr-001"));
        verify(assignment_repository, never()).insert(
                anyLong(), anyLong(), any(), anyString(), any(), any(), anyString(), anyLong());
    }

    @Test
    void rejects_overlapping_assignment() {
        production_assignment_repository assignment_repository = mock(production_assignment_repository.class);
        human_resources_employee_contract employee_contract = mock(human_resources_employee_contract.class);
        human_resources_work_shift_contract shift_contract = mock(human_resources_work_shift_contract.class);
        var employee = new vn.vinamik.erp_backend.human_resources.api.human_resources_employee_snapshot(
                2L, "vmk0002", "Nguyễn Văn A", "active");
        var shift = new vn.vinamik.erp_backend.human_resources.api.human_resources_work_shift_snapshot(
                3L, "ca_sang", "Ca sáng", java.time.LocalTime.of(6, 0),
                java.time.LocalTime.of(14, 0), "active");
        var request = valid_request();
        when(assignment_repository.find_order(1L))
                .thenReturn(new production_order_snapshot(1L, "order_001", "released"));
        when(employee_contract.find_employee(2L)).thenReturn(java.util.Optional.of(employee));
        when(shift_contract.find_work_shift(3L)).thenReturn(java.util.Optional.of(shift));
        when(assignment_repository.has_overlap(2L, request.starts_at(), request.ends_at(), null))
                .thenReturn(true);

        production_assignment_service service = new production_assignment_service(
                assignment_repository, employee_contract, shift_contract, mock(audit_event_writer.class));

        assertThrows(vn.vinamik.erp_backend.platform.common.field_conflict_exception.class,
                () -> service.create(request,
                        new authenticated_user(7L, "production_test", java.util.List.of()), "corr-001"));
        verify(assignment_repository, never()).insert(
                anyLong(), anyLong(), any(), anyString(), any(), any(), anyString(), anyLong());
    }

    private production_assignment_request valid_request() {
        return new production_assignment_request(
                1L, 2L, 3L, "Vận hành thiết bị",
                Instant.parse("2026-01-01T06:00:00Z"),
                Instant.parse("2026-01-01T14:00:00Z"), "test");
    }
}