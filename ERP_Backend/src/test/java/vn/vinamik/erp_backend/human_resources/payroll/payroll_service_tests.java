package vn.vinamik.erp_backend.human_resources.payroll;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;

import java.time.LocalDate;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class payroll_service_tests {
    @Mock
    private payroll_repository payroll_repository;

    @Mock
    private audit_event_writer audit_writer;

    @InjectMocks
    private payroll_service payroll_service;

    @Test
    void standard_working_days_include_monday_through_saturday() {
        assertEquals(26, payroll_service.count_standard_working_days(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)));
        assertEquals(24, payroll_service.count_standard_working_days(
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)));
        assertEquals(25, payroll_service.count_standard_working_days(
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29)));
    }

    @Test
    void calculation_rejects_overlapping_active_contracts() {
        payroll_period_response period = draft_period(1L);
        when(payroll_repository.find_period(1L)).thenReturn(period);
        when(payroll_repository.ambiguous_contract_count(period.starts_on(), period.ends_on())).thenReturn(1L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> payroll_service.calculate(1L, null, "test-correlation"));

        assertEquals("An employee has multiple active contracts overlapping the payroll period.",
                exception.getMessage());
    }

    @Test
    void calculation_rejects_mixed_contract_currencies() {
        payroll_period_response period = draft_period(2L);
        when(payroll_repository.find_period(2L)).thenReturn(period);
        when(payroll_repository.candidate_currency_count(period.starts_on(), period.ends_on())).thenReturn(2L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> payroll_service.calculate(2L, null, "test-correlation"));

        assertEquals("All payroll contracts must use the same currency within a payroll period.",
                exception.getMessage());
    }

    private payroll_period_response draft_period(long payroll_period_id) {
        return new payroll_period_response(
                payroll_period_id,
                "payroll_2026_09",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                new BigDecimal("26.000000"),
                "monthly_mon_sat_v1",
                "draft",
                0,
                BigDecimal.ZERO,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
