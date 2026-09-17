package vn.vinamik.erp_backend.human_resources.reward_discipline;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class human_resources_reward_discipline_service_tests {
    @Test
    void rejects_unknown_event_type_without_database_call() {
        human_resources_reward_discipline_service service = new human_resources_reward_discipline_service(null, (audit_event_writer) null);
        reward_discipline_request request = request("unknown", new BigDecimal("100000.00"), "draft");

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_amount_with_more_than_two_decimal_places_without_database_call() {
        human_resources_reward_discipline_service service = new human_resources_reward_discipline_service(null, (audit_event_writer) null);
        reward_discipline_request request = request("reward", new BigDecimal("100000.001"), "draft");

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    @Test
    void rejects_approved_initial_status_without_database_call() {
        human_resources_reward_discipline_service service = new human_resources_reward_discipline_service(null, (audit_event_writer) null);
        reward_discipline_request request = request("reward", new BigDecimal("100000.00"), "approved");

        assertThrows(IllegalArgumentException.class, () -> service.create(request, null, "test-correlation"));
    }

    private reward_discipline_request request(String event_type, BigDecimal amount, String status) {
        return new reward_discipline_request(
                "reward_001", 1L, event_type, LocalDate.of(2026, 3, 1), "Performance result", amount, "VND", status);
    }
}
