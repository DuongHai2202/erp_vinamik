package vn.vinamik.erp_backend.human_resources;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.human_resources.absence.human_resources_absence_service;
import vn.vinamik.erp_backend.human_resources.absence.leave_decision_request;
import vn.vinamik.erp_backend.human_resources.absence.leave_request;
import vn.vinamik.erp_backend.human_resources.absence.leave_response;
import vn.vinamik.erp_backend.human_resources.contract.employment_contract_request;
import vn.vinamik.erp_backend.human_resources.contract.employment_contract_response;
import vn.vinamik.erp_backend.human_resources.contract.employment_contract_status_request;
import vn.vinamik.erp_backend.human_resources.contract.human_resources_contract_service;
import vn.vinamik.erp_backend.human_resources.employee.employee_request;
import vn.vinamik.erp_backend.human_resources.employee.employee_response;
import vn.vinamik.erp_backend.human_resources.employee.human_resources_employee_service;
import vn.vinamik.erp_backend.human_resources.payroll.payroll_period_request;
import vn.vinamik.erp_backend.human_resources.payroll.payroll_period_response;
import vn.vinamik.erp_backend.human_resources.payroll.payroll_record_detail_response;
import vn.vinamik.erp_backend.human_resources.payroll.payroll_record_response;
import vn.vinamik.erp_backend.human_resources.payroll.payroll_service;
import vn.vinamik.erp_backend.human_resources.reward_discipline.human_resources_reward_discipline_service;
import vn.vinamik.erp_backend.human_resources.reward_discipline.reward_discipline_decision_request;
import vn.vinamik.erp_backend.human_resources.reward_discipline.reward_discipline_page_response;
import vn.vinamik.erp_backend.human_resources.reward_discipline.reward_discipline_request;
import vn.vinamik.erp_backend.human_resources.reward_discipline.reward_discipline_response;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs only when ERP_RUN_INTEGRATION_TESTS=true and the database is isolated.
 * Verifies the HR input lifecycle and the approved monthly payroll formula against PostgreSQL.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "erp.bootstrap-admin.username=",
        "erp.bootstrap-admin.password="
})
@EnabledIfEnvironmentVariable(named = "ERP_RUN_INTEGRATION_TESTS", matches = "true")
class human_resources_module_integration_tests {
    @PersistenceContext
    private EntityManager entity_manager;

    @Autowired
    private human_resources_employee_service employee_service;

    @Autowired
    private human_resources_contract_service contract_service;

    @Autowired
    private human_resources_absence_service absence_service;

    @Autowired
    private human_resources_reward_discipline_service reward_discipline_service;

    @Autowired
    private payroll_service payroll_service;

    @Test
    @Transactional
    void hr_inputs_calculate_approve_and_lock_payroll() {
        authenticated_user actor = actor();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        LocalDate today = LocalDate.now();
        YearMonth payroll_month = YearMonth.from(today).minusMonths(1);
        LocalDate payroll_start = payroll_month.atDay(1);
        LocalDate payroll_end = payroll_month.atEndOfMonth();
        LocalDate unpaid_day = first_standard_working_day(payroll_month);

        employee_response employee = employee_service.create(
                new employee_request(
                        "hr_it_" + suffix,
                        "Integration HR employee",
                        null,
                        null,
                        "hr_" + suffix + "@example.com",
                        null,
                        null,
                        null,
                        "active",
                        payroll_start.minusDays(30),
                        null,
                        "HR integration"),
                actor,
                "hr-integration");
        assertEquals("active", employee.employment_status());

        employment_contract_response contract = contract_service.create(
                new employment_contract_request(
                        "hr_contract_" + suffix,
                        employee.employee_id(),
                        "full_time",
                        payroll_start.minusDays(10),
                        today.plusDays(10),
                        new BigDecimal("12000000.00"),
                        "vnd",
                        null,
                        "HR integration contract"),
                actor,
                "hr-integration");
        assertEquals("draft", contract.status());
        employment_contract_response active_contract = contract_service.change_status(
                contract.employment_contract_id(),
                new employment_contract_status_request("active", "approved for integration"),
                actor,
                "hr-integration");
        assertEquals("active", active_contract.status());
        assertTrue(active_contract.expiring_soon());
        assertTrue(active_contract.days_until_expiry() <= 15);

        leave_response unpaid_leave = absence_service.create(
                new leave_request(
                        "hr_leave_" + suffix,
                        employee.employee_id(),
                        "unpaid_leave",
                        unpaid_day,
                        unpaid_day,
                        false,
                        "HR integration unpaid leave",
                        "pending"),
                actor,
                "hr-integration");
        assertEquals("pending", unpaid_leave.status());
        leave_response approved_leave = absence_service.decide(
                unpaid_leave.leave_request_id(),
                new leave_decision_request("approved", "approved for integration"),
                actor,
                "hr-integration");
        assertEquals("approved", approved_leave.status());
        assertEquals(1L, approved_leave.day_count());
        assertTrue(!approved_leave.is_paid());

        reward_discipline_response reward = reward_discipline_service.create(
                new reward_discipline_request(
                        "hr_reward_" + suffix,
                        employee.employee_id(),
                        "reward",
                        unpaid_day,
                        "HR integration reward",
                        new BigDecimal("500000.00"),
                        "vnd",
                        "draft"),
                actor,
                "hr-integration");
        assertEquals("draft", reward.status());
        assertEquals("pending", reward_discipline_service.submit(
                reward.employee_reward_discipline_id(), actor, "hr-integration").status());
        reward_discipline_response approved_reward = reward_discipline_service.decide(
                reward.employee_reward_discipline_id(),
                new reward_discipline_decision_request("approved", "approved for integration"),
                actor,
                "hr-integration");
        assertEquals("approved", approved_reward.status());
        assertEquals("VND", approved_reward.currency_code());

        reward_discipline_response discipline = reward_discipline_service.create(
                new reward_discipline_request(
                        "hr_discipline_" + suffix,
                        employee.employee_id(),
                        "discipline",
                        unpaid_day,
                        "HR integration discipline",
                        new BigDecimal("100000.00"),
                        "vnd",
                        "draft"),
                actor,
                "hr-integration");
        reward_discipline_service.submit(
                discipline.employee_reward_discipline_id(), actor, "hr-integration");
        reward_discipline_response approved_discipline = reward_discipline_service.decide(
                discipline.employee_reward_discipline_id(),
                new reward_discipline_decision_request("approved", "approved for integration"),
                actor,
                "hr-integration");
        assertEquals("approved", approved_discipline.status());

        reward_discipline_response pending_reward = reward_discipline_service.create(
                new reward_discipline_request(
                        "hr_pending_reward_" + suffix,
                        employee.employee_id(),
                        "reward",
                        unpaid_day,
                        "Pending amount must not affect payroll",
                        new BigDecimal("900000.00"),
                        "vnd",
                        "draft"),
                actor,
                "hr-integration");
        assertEquals("pending", reward_discipline_service.submit(
                pending_reward.employee_reward_discipline_id(), actor, "hr-integration").status());

        reward_discipline_response rejected_discipline = reward_discipline_service.create(
                new reward_discipline_request(
                        "hr_rejected_discipline_" + suffix,
                        employee.employee_id(),
                        "discipline",
                        unpaid_day,
                        "Rejected amount must not affect payroll",
                        new BigDecimal("800000.00"),
                        "vnd",
                        "draft"),
                actor,
                "hr-integration");
        reward_discipline_service.submit(
                rejected_discipline.employee_reward_discipline_id(), actor, "hr-integration");
        assertEquals("rejected", reward_discipline_service.decide(
                rejected_discipline.employee_reward_discipline_id(),
                new reward_discipline_decision_request("rejected", "rejected for integration"),
                actor,
                "hr-integration").status());

        reward_discipline_page_response payroll_inputs = reward_discipline_service.search_payroll_inputs(
                employee.employee_id(), payroll_start, payroll_end, 0, 50);
        assertEquals(2L, payroll_inputs.total_items());
        assertEquals("approved", payroll_inputs.items().getFirst().status());
        assertEquals(1L, scalar_long(
                "select count(*) from hr.leave_request "
                        + "where employee_id = :employee_id and status = 'approved' and is_paid = false",
                Map.of("employee_id", employee.employee_id())));

        payroll_period_response period = payroll_service.create_period(
                new payroll_period_request(payroll_month.getYear(), payroll_month.getMonthValue()),
                actor,
                "hr-integration");
        int standard_working_days = count_standard_working_days(payroll_month);
        assertEquals("draft", period.status());
        assert_decimal(BigDecimal.valueOf(standard_working_days), period.standard_working_days());

        payroll_period_response calculated = payroll_service.calculate(
                period.payroll_period_id(), actor, "hr-integration");
        assertEquals("calculated", calculated.status());
        assertEquals(1L, calculated.record_count());
        assertEquals("VND", calculated.currency_code());

        var records = payroll_service.search_records(
                period.payroll_period_id(), employee.employee_code(), 0, 10);
        assertEquals(1L, records.total_items());
        payroll_record_response record = records.items().getFirst();
        BigDecimal base_salary = new BigDecimal("12000000.00");
        BigDecimal unpaid_amount = base_salary
                .divide(BigDecimal.valueOf(standard_working_days), 6, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal expected_gross = new BigDecimal("12500000.00");
        BigDecimal expected_deduction = unpaid_amount.add(new BigDecimal("100000.00"));
        BigDecimal expected_net = expected_gross.subtract(expected_deduction);
        assert_decimal(BigDecimal.ONE, record.unpaid_leave_days());
        assert_decimal(unpaid_amount, record.unpaid_leave_amount());
        assert_decimal(new BigDecimal("500000.00"), record.reward_amount());
        assert_decimal(new BigDecimal("100000.00"), record.discipline_amount());
        assert_decimal(expected_gross, record.gross_amount());
        assert_decimal(expected_deduction, record.deduction_amount());
        assert_decimal(expected_net, record.net_amount());
        assert_decimal(expected_net, calculated.total_net_amount());

        payroll_record_detail_response first_detail = payroll_service.find_record(record.payroll_record_id());
        assertEquals(4, first_detail.lines().size());
        payroll_service.calculate(period.payroll_period_id(), actor, "hr-integration-recalculate");
        payroll_record_response recalculated_record = payroll_service.search_records(
                period.payroll_period_id(), employee.employee_code(), 0, 10).items().getFirst();
        assertEquals(4, payroll_service.find_record(recalculated_record.payroll_record_id()).lines().size());
        assert_decimal(expected_net, recalculated_record.net_amount());

        payroll_period_response approved = payroll_service.approve(
                period.payroll_period_id(), actor, "hr-integration");
        assertEquals("approved", approved.status());
        payroll_period_response locked = payroll_service.lock(
                period.payroll_period_id(), actor, "hr-integration");
        assertEquals("locked", locked.status());
        assertThrows(IllegalArgumentException.class,
                () -> payroll_service.calculate(period.payroll_period_id(), actor, "hr-integration"));

        employment_contract_response terminated_contract = contract_service.change_status(
                active_contract.employment_contract_id(),
                new employment_contract_status_request("terminated", "closed after locked historical payroll"),
                actor,
                "hr-integration");
        assertEquals("terminated", terminated_contract.status());
        payroll_record_response locked_record = payroll_service.find_record(
                recalculated_record.payroll_record_id()).record();
        assertEquals("locked", locked_record.status());
        assert_decimal(expected_net, locked_record.net_amount());

        YearMonth next_month = payroll_month.plusMonths(1);
        payroll_period_response unlocked_target = payroll_service.create_period(
                new payroll_period_request(next_month.getYear(), next_month.getMonthValue()),
                actor,
                "hr-integration");
        assertThrows(RuntimeException.class, () -> entity_manager.createNativeQuery(
                        "update hr.payroll_record set payroll_period_id = :target_period_id "
                                + "where payroll_record_id = :payroll_record_id")
                .setParameter("target_period_id", unlocked_target.payroll_period_id())
                .setParameter("payroll_record_id", locked_record.payroll_record_id())
                .executeUpdate());
    }

    private authenticated_user actor() {
        Long user_id = scalar_long(
                "select min(user_id) from identity.user_account where status = 'active'", Map.of());
        assertTrue(user_id != null && user_id > 0, "An active integration actor is required.");
        return new authenticated_user(user_id, "hr_integration_actor", List.of(), false);
    }

    private LocalDate first_standard_working_day(YearMonth month) {
        LocalDate date = month.atDay(1);
        return date.getDayOfWeek() == DayOfWeek.SUNDAY ? date.plusDays(1) : date;
    }

    private int count_standard_working_days(YearMonth month) {
        int result = 0;
        for (LocalDate date = month.atDay(1); !date.isAfter(month.atEndOfMonth()); date = date.plusDays(1)) {
            if (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                result++;
            }
        }
        return result;
    }

    private void assert_decimal(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual), () -> "Expected " + expected + " but was " + actual);
    }

    private Long scalar_long(String sql, Map<String, Object> parameters) {
        Query query = entity_manager.createNativeQuery(sql);
        parameters.forEach(query::setParameter);
        Object result = query.getSingleResult();
        return result == null ? null : ((Number) result).longValue();
    }
}
