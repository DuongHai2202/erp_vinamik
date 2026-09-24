package vn.vinamik.erp_backend.production.plan;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import vn.vinamik.erp_backend.inventory.api.inventory_material_contract;
import vn.vinamik.erp_backend.inventory.api.inventory_material_snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class production_plan_service_tests {
    private static final authenticated_user ACTOR =
            new authenticated_user(7L, "production_test", List.of("production.write"));
    @Test
    void rejects_missing_plan_fields_before_database_call() {
        production_plan_service service = new production_plan_service(null, null, (audit_event_writer) null);
        production_plan_request request = new production_plan_request(
                "  ", "Plan", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 2), null,
                List.of(new production_plan_line_request(1L, new BigDecimal("1.000000"), null, null)));

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_line_required_date_outside_plan_before_database_call() {
        production_plan_service service = new production_plan_service(null, null, (audit_event_writer) null);
        production_plan_request request = new production_plan_request(
                "plan_001", "Plan", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 2), null,
                List.of(new production_plan_line_request(1L, new BigDecimal("1.000000"),
                        LocalDate.of(2026, 2, 3), null)));

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_raw_material_plan_line() {
        production_plan_repository plan_repository = mock(production_plan_repository.class);
        inventory_material_contract material_contract = mock(inventory_material_contract.class);
        when(plan_repository.plan_code_exists("plan_001", null)).thenReturn(false);
        when(material_contract.find_active_stock_item(1L)).thenReturn(java.util.Optional.of(
                new inventory_material_snapshot(1L, "raw_001", "Raw material", "raw_material", "kg", "active")));
        production_plan_service service = new production_plan_service(
                plan_repository, material_contract, (audit_event_writer) null);
        production_plan_request request = new production_plan_request(
                "plan_001", "Plan", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 2), null,
                List.of(new production_plan_line_request(1L, new BigDecimal("1.000000"), null, null)));

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }
    @Test
    void creates_valid_draft_and_writes_audit() {
        production_plan_repository plan_repository = mock(production_plan_repository.class);
        inventory_material_contract material_contract = mock(inventory_material_contract.class);
        audit_event_writer audit_writer = mock(audit_event_writer.class);
        var item = new inventory_material_snapshot(
                11L, "fg_001", "Sữa tươi", "finished_product", "chai", "active");
        var request = valid_request("plan_001");
        var created = new production_plan_response(
                21L, "plan_001", "Kế hoạch", request.planned_on(), request.starts_on(), request.ends_on(),
                "draft", request.notes(), List.of(new production_plan_line_response(
                        31L, 1, 11L, "fg_001", "Sữa tươi", "chai",
                        new BigDecimal("10.000000"), request.starts_on(), null)));
        when(plan_repository.plan_code_exists("plan_001", null)).thenReturn(false);
        when(material_contract.find_active_stock_item(11L)).thenReturn(java.util.Optional.of(item));
        when(plan_repository.insert("plan_001", "Kế hoạch", request.planned_on(), request.starts_on(),
                request.ends_on(), null, 7L)).thenReturn(21L);
        when(plan_repository.find(21L)).thenReturn(created);

        production_plan_service service = new production_plan_service(plan_repository, material_contract, audit_writer);

        production_plan_response result = service.create(request, ACTOR, "corr-001");

        assertEquals("draft", result.status());
        assertEquals(1, result.lines().size());
        verify(plan_repository).insert_line(21L, 1, request.lines().getFirst(), 11L,
                "fg_001", "Sữa tươi", "chai", null);
        verify(audit_writer).write(7L, "production", "production_plan_create", "production_plan",
                "21", "corr-001", java.util.Map.of("plan_code", "plan_001"));
    }

    @Test
    void rejects_duplicate_plan_code_without_writing() {
        production_plan_repository plan_repository = mock(production_plan_repository.class);
        inventory_material_contract material_contract = mock(inventory_material_contract.class);
        when(plan_repository.plan_code_exists("plan_001", null)).thenReturn(true);
        production_plan_service service = new production_plan_service(
                plan_repository, material_contract, mock(audit_event_writer.class));

        assertThrows(vn.vinamik.erp_backend.platform.common.field_conflict_exception.class,
                () -> service.create(valid_request("plan_001"), ACTOR, "corr-001"));
        verify(plan_repository, never()).insert(
                anyString(), anyString(), any(), any(), any(), any(), anyLong());
    }

    @Test
    void rejects_update_when_plan_is_not_draft() {
        production_plan_repository plan_repository = mock(production_plan_repository.class);
        inventory_material_contract material_contract = mock(inventory_material_contract.class);
        when(plan_repository.plan_code_exists("plan_001", 21L)).thenReturn(false);
        when(plan_repository.current_status(21L)).thenReturn("approved");
        production_plan_service service = new production_plan_service(
                plan_repository, material_contract, mock(audit_event_writer.class));

        assertThrows(IllegalArgumentException.class,
                () -> service.update(21L, valid_request("plan_001"), ACTOR, "corr-001"));
        verify(plan_repository, never()).update(
                anyLong(), anyString(), anyString(), any(), any(), any(), any(), anyLong());
    }

    @Test
    void changes_draft_status_to_approved_and_writes_audit() {
        production_plan_repository plan_repository = mock(production_plan_repository.class);
        audit_event_writer audit_writer = mock(audit_event_writer.class);
        var changed = new production_plan_response(
                21L, "plan_001", "Kế hoạch",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2), "approved", null, List.of());
        when(plan_repository.current_status(21L)).thenReturn("draft");
        when(plan_repository.change_status(21L, "approved", 7L)).thenReturn(1);
        when(plan_repository.find(21L)).thenReturn(changed);
        production_plan_service service = new production_plan_service(
                plan_repository, mock(inventory_material_contract.class), audit_writer);

        production_plan_response result = service.change_status(21L, "approved", ACTOR, "corr-001");

        assertEquals("approved", result.status());
        verify(audit_writer).write(7L, "production", "production_plan_status_change", "production_plan",
                "21", "corr-001", java.util.Map.of("from_status", "draft", "to_status", "approved"));
    }

    private production_plan_request valid_request(String code) {
        return new production_plan_request(
                code, "Kế hoạch", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2), null,
                List.of(new production_plan_line_request(
                        11L, new BigDecimal("10.000000"), LocalDate.of(2026, 1, 1), null)));
    }
}