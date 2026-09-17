package vn.vinamik.erp_backend.production.plan;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import vn.vinamik.erp_backend.inventory.api.inventory_material_contract;
import vn.vinamik.erp_backend.inventory.api.inventory_material_snapshot;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class production_plan_service_tests {
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
}