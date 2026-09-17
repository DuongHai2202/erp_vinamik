package vn.vinamik.erp_backend.human_resources.contract;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class human_resources_contract_service_tests {
    @Test
    void rejects_end_date_before_start_date_without_database_call() {
        human_resources_contract_service service = new human_resources_contract_service(null, (audit_event_writer) null);
        employment_contract_request request = request(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 1), new BigDecimal("10000000.00"), "draft");

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_salary_with_more_than_two_decimal_places_without_database_call() {
        human_resources_contract_service service = new human_resources_contract_service(null, (audit_event_writer) null);
        employment_contract_request request = request(LocalDate.of(2026, 3, 1), null, new BigDecimal("10000000.001"), "draft");

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_non_draft_new_contract_without_database_call() {
        human_resources_contract_service service = new human_resources_contract_service(null, (audit_event_writer) null);
        employment_contract_request request = request(LocalDate.of(2026, 3, 1), null, new BigDecimal("10000000.00"), "active");

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    private employment_contract_request request(LocalDate effective_from, LocalDate effective_to,
                                                BigDecimal base_salary, String status) {
        return new employment_contract_request(
                "contract_001", 1L, "indefinite", effective_from, effective_to, base_salary, "VND", status, null);
    }
}
