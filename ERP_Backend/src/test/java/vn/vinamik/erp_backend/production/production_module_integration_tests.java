package vn.vinamik.erp_backend.production;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.inventory.receipt.inventory_receipt_service;
import vn.vinamik.erp_backend.inventory.receipt.receipt_line_request;
import vn.vinamik.erp_backend.inventory.receipt.receipt_request;
import vn.vinamik.erp_backend.inventory.receipt.receipt_response;
import vn.vinamik.erp_backend.production.bom.bom_line_request;
import vn.vinamik.erp_backend.production.bom.bom_request;
import vn.vinamik.erp_backend.production.bom.bom_response;
import vn.vinamik.erp_backend.production.bom.production_bom_service;
import vn.vinamik.erp_backend.production.assignment.production_assignment_request;
import vn.vinamik.erp_backend.production.assignment.production_assignment_response;
import vn.vinamik.erp_backend.production.assignment.production_assignment_service;
import vn.vinamik.erp_backend.production.material_needs.material_needs_response;
import vn.vinamik.erp_backend.production.material_needs.production_material_needs_service;
import vn.vinamik.erp_backend.production.material_consumption.material_consumption_line_request;
import vn.vinamik.erp_backend.production.material_consumption.material_consumption_request;
import vn.vinamik.erp_backend.production.material_consumption.material_consumption_response;
import vn.vinamik.erp_backend.production.material_consumption.production_material_consumption_service;
import vn.vinamik.erp_backend.production.output.production_output_request;
import vn.vinamik.erp_backend.production.output.production_output_response;
import vn.vinamik.erp_backend.production.output.production_output_service;
import vn.vinamik.erp_backend.production.order_progress.production_order_progress_response;
import vn.vinamik.erp_backend.production.order_progress.production_order_progress_service;
import vn.vinamik.erp_backend.production.order.production_order_request;
import vn.vinamik.erp_backend.production.order.production_order_response;
import vn.vinamik.erp_backend.production.order.production_order_service;
import vn.vinamik.erp_backend.production.plan.production_plan_line_request;
import vn.vinamik.erp_backend.production.plan.production_plan_request;
import vn.vinamik.erp_backend.production.plan.production_plan_response;
import vn.vinamik.erp_backend.production.plan.production_plan_service;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs only when ERP_RUN_INTEGRATION_TESTS=true and the database is isolated.
 * Every test is transactional and is rolled back after execution.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "erp.bootstrap-admin.username=",
        "erp.bootstrap-admin.password="
})
@EnabledIfEnvironmentVariable(named = "ERP_RUN_INTEGRATION_TESTS", matches = "true")
class production_module_integration_tests {
    @PersistenceContext
    private EntityManager entity_manager;

    @Autowired
    private production_plan_service plan_service;

    @Autowired
    private production_bom_service bom_service;

    @Autowired
    private production_order_service order_service;

    @Autowired
    private production_material_needs_service material_needs_service;

    @Autowired
    private production_material_consumption_service consumption_service;

    @Autowired
    private production_output_service output_service;

    @Autowired
    private production_assignment_service assignment_service;

    @Autowired
    private production_order_progress_service progress_service;

    @Autowired
    private inventory_receipt_service receipt_service;

    @Test
    @Transactional
    void production_flow_uses_inventory_contracts_and_shared_ledger() {
        production_fixture fixture = create_fixture();
        authenticated_user actor = actor();
        LocalDate today = LocalDate.now();
        LocalDate starts_on = today.plusDays(1);
        LocalDate ends_on = today.plusDays(10);

        receipt_response material_receipt = receipt_service.create(
                new receipt_request(
                        fixture.code("material_receipt"),
                        fixture.warehouse_id(),
                        null,
                        null,
                        null,
                        null,
                        fixture.code("material_receipt_key"),
                        "production integration material",
                        List.of(new receipt_line_request(
                                fixture.raw_material_id(), fixture.location_id(), null, quantity("20")))),
                actor,
                "production-integration");
        assertEquals("posted", receipt_service.post(
                material_receipt.receipt_id(), actor, "production-integration").status());

        production_plan_response plan = plan_service.create(
                new production_plan_request(
                        fixture.code("plan"),
                        "Integration production plan",
                        today,
                        starts_on,
                        ends_on,
                        "production integration",
                        List.of(new production_plan_line_request(
                                fixture.finished_product_id(), quantity("10"), starts_on, null))),
                actor,
                "production-integration");
        assertEquals("draft", plan.status());
        long plan_line_id = plan.lines().getFirst().production_plan_line_id();

        bom_response bom = bom_service.create(
                new bom_request(
                        fixture.code("bom"),
                        fixture.finished_product_id(),
                        1,
                        quantity("1"),
                        today,
                        null,
                        "integration bom",
                        List.of(new bom_line_request(
                                fixture.raw_material_id(), quantity("1"), BigDecimal.ZERO, null))),
                actor,
                "production-integration");
        assertEquals("active", bom_service.change_status(
                bom.bom_id(), "active", actor, "production-integration").status());

        production_order_response order = order_service.create(
                new production_order_request(
                        fixture.code("order"),
                        plan_line_id,
                        bom.bom_id(),
                        quantity("10"),
                        starts_on,
                        ends_on,
                        "line_a",
                        "production integration"),
                actor,
                "production-integration");
        assertEquals("planned", order.status());
        assertEquals(quantity("10"), order.material_requirements().getFirst().required_quantity());
        assertEquals("released", order_service.release(
                order.production_order_id(), actor, "production-integration").status());

        Instant assignment_start = today.atStartOfDay(ZoneOffset.UTC).plusDays(1).plusHours(2).toInstant();
        production_assignment_response assignment = assignment_service.create(
                new production_assignment_request(
                        order.production_order_id(),
                        fixture.employee_id(),
                        fixture.work_shift_id(),
                        "integration operator",
                        assignment_start,
                        assignment_start.plusSeconds(6 * 60 * 60),
                        "production integration"),
                actor,
                "production-integration");
        assertEquals("planned", assignment.status());
        assertEquals("active", assignment_service.change_status(
                assignment.production_assignment_id(), "active", actor, "production-integration").status());

        material_needs_response needs = material_needs_service.find_by_order_id(order.production_order_id());
        assertEquals(quantity("10"), needs.items().getFirst().required_quantity());
        assertEquals(quantity("20"), needs.items().getFirst().available_quantity());
        assertEquals(quantity("0"), needs.items().getFirst().shortage_quantity());

        material_consumption_response consumption = consumption_service.create(
                order.production_order_id(),
                new material_consumption_request(
                        fixture.warehouse_id(),
                        fixture.code("consumption"),
                        "production integration consumption",
                        List.of(new material_consumption_line_request(
                                fixture.raw_material_id(), fixture.location_id(), null, quantity("10")))),
                actor,
                "production-integration");
        assertEquals(1, consumption.lines().size());
        assertEquals(quantity("10"), balance(fixture.raw_material_id(), fixture.location_id(), null));

        production_output_response output = output_service.create(
                order.production_order_id(),
                new production_output_request(
                        fixture.warehouse_id(),
                        fixture.location_id(),
                        fixture.code("finished_lot"),
                        today,
                        today.plusDays(30),
                        quantity("8"),
                        quantity("2"),
                        fixture.code("output"),
                        "production integration output"),
                actor,
                "production-integration");
        assertEquals("pending_receipt", output.status());
        production_output_response received = output_service.post(
                order.production_order_id(), output.production_output_id(), actor, "production-integration");
        assertEquals("received", received.status());
        assertNotNull(received.inventory_receipt_id());
        assertNotNull(received.inventory_stock_lot_id());
        assertEquals(quantity("8"), balance(
                fixture.finished_product_id(), fixture.location_id(), received.inventory_stock_lot_id()));
        assertEquals(quantity("10"), balance(fixture.raw_material_id(), fixture.location_id(), null));

        production_output_response retried = output_service.post(
                order.production_order_id(), output.production_output_id(), actor, "production-integration");
        assertEquals("received", retried.status());
        assertEquals(1L, count_movements("receipt", received.inventory_receipt_id()));
        assertEquals(quantity("8"), balance(
                fixture.finished_product_id(), fixture.location_id(), received.inventory_stock_lot_id()));
        assertEquals(1L, count_production_issues(order.production_order_id()));
        assertEquals(1L, count_production_outputs(order.production_order_id()));
        production_order_progress_response progress = progress_service.find_by_order_id(order.production_order_id());
        assertEquals(quantity("10"), progress.planned_quantity());
        assertEquals(quantity("8"), progress.actual_good_quantity());
        assertEquals(quantity("2"), progress.actual_defective_quantity());
        assertEquals(quantity("10"), progress.actual_total_quantity());
        assertEquals(quantity("0"), progress.remaining_quantity());
        assertEquals(new BigDecimal("100.00"), progress.completion_percent());
        assertTrue(progress.events().size() >= 1);
        assertTrue(count_movements_for_item(fixture.finished_product_id()) > 0);
    }

    private production_fixture create_fixture() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        long unit_id = insert_id(
                "insert into inventory.unit_of_measure (unit_code, unit_name, decimal_places) "
                        + "values (:code, :name, 6) returning unit_of_measure_id",
                Map.of("code", "pr_" + suffix, "name", "Production integration unit"));
        long raw_material_id = insert_id(
                "insert into inventory.stock_item "
                        + "(item_code, item_name, item_type, base_unit_of_measure_id, lot_controlled) "
                        + "values (:code, :name, 'raw_material', :unit_id, false) returning stock_item_id",
                Map.of("code", "pr_raw_" + suffix, "name", "Production integration raw material", "unit_id", unit_id));
        long finished_product_id = insert_id(
                "insert into inventory.stock_item "
                        + "(item_code, item_name, item_type, base_unit_of_measure_id, lot_controlled) "
                        + "values (:code, :name, 'finished_product', :unit_id, true) returning stock_item_id",
                Map.of("code", "pr_finished_" + suffix, "name", "Production integration finished product", "unit_id", unit_id));
        long warehouse_id = insert_id(
                "insert into inventory.warehouse (warehouse_code, warehouse_name) "
                        + "values (:code, :name) returning warehouse_id",
                Map.of("code", "pr_wh_" + suffix, "name", "Production integration warehouse"));
        long location_id = insert_id(
                "insert into inventory.warehouse_location "
                        + "(warehouse_id, location_code, location_name) "
                        + "values (:warehouse_id, :code, :name) returning warehouse_location_id",
                Map.of("warehouse_id", warehouse_id, "code", "pr_bin_" + suffix, "name", "Production integration bin"));
        long employee_id = insert_id(
                "insert into hr.employee (employee_code, full_name, employment_status) "
                        + "values (:code, :name, 'active') returning employee_id",
                Map.of("code", "pr_emp_" + suffix, "name", "Production integration operator"));
        long work_shift_id = insert_id(
                "insert into hr.work_shift (shift_code, shift_name, starts_at, ends_at) "
                        + "values (:code, :name, '08:00', '16:00') returning work_shift_id",
                Map.of("code", "pr_shift_" + suffix, "name", "Production integration shift"));
        return new production_fixture(raw_material_id, finished_product_id, warehouse_id, location_id,
                employee_id, work_shift_id, suffix);
    }

    private authenticated_user actor() {
        Long user_id = scalar_long(
                "select min(user_id) from identity.user_account where status = 'active'", Map.of());
        assertTrue(user_id != null && user_id > 0, "An active integration actor is required.");
        return new authenticated_user(user_id, "production_integration_actor", List.of(), false);
    }

    private long insert_id(String sql, Map<String, Object> parameters) {
        Query query = entity_manager.createNativeQuery(sql);
        parameters.forEach(query::setParameter);
        return ((Number) query.getSingleResult()).longValue();
    }

    private BigDecimal balance(long stock_item_id, long location_id, Long stock_lot_id) {
        Query query = entity_manager.createNativeQuery(
                "select coalesce((select on_hand_quantity from inventory.stock_balance "
                        + "where stock_item_id = :stock_item_id and warehouse_location_id = :location_id "
                        + "and stock_lot_id is not distinct from :stock_lot_id), 0)");
        query.setParameter("stock_item_id", stock_item_id);
        query.setParameter("location_id", location_id);
        query.setParameter("stock_lot_id", stock_lot_id);
        return (BigDecimal) query.getSingleResult();
    }

    private long count_movements(String source_document_type, long source_document_id) {
        return scalar_long(
                "select count(*) from inventory.stock_movement "
                        + "where source_document_type = :source_document_type and source_document_id = :source_document_id",
                Map.of("source_document_type", source_document_type, "source_document_id", source_document_id));
    }

    private long count_production_issues(long production_order_id) {
        return scalar_long(
                "select count(*) from inventory.issue "
                        + "where source_module = 'production' and source_document_id = :production_order_id",
                Map.of("production_order_id", production_order_id));
    }

    private long count_production_outputs(long production_order_id) {
        return scalar_long(
                "select count(*) from production.production_output where production_order_id = :production_order_id",
                Map.of("production_order_id", production_order_id));
    }

    private long count_movements_for_item(long stock_item_id) {
        return scalar_long(
                "select count(*) from inventory.stock_movement where stock_item_id = :stock_item_id",
                Map.of("stock_item_id", stock_item_id));
    }

    private Long scalar_long(String sql, Map<String, Object> parameters) {
        Query query = entity_manager.createNativeQuery(sql);
        parameters.forEach(query::setParameter);
        Object result = query.getSingleResult();
        return result == null ? null : ((Number) result).longValue();
    }

    private BigDecimal quantity(String value) {
        return new BigDecimal(value).setScale(6);
    }

    private record production_fixture(
            long raw_material_id,
            long finished_product_id,
            long warehouse_id,
            long location_id,
            long employee_id,
            long work_shift_id,
            String suffix) {
        private String code(String prefix) {
            return prefix + "_" + suffix;
        }
    }
}