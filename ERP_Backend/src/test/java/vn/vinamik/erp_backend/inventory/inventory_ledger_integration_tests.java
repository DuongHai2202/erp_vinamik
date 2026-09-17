package vn.vinamik.erp_backend.inventory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.inventory.issue.inventory_issue_service;
import vn.vinamik.erp_backend.inventory.issue.issue_line_request;
import vn.vinamik.erp_backend.inventory.issue.issue_request;
import vn.vinamik.erp_backend.inventory.issue.issue_response;
import vn.vinamik.erp_backend.inventory.receipt.inventory_receipt_service;
import vn.vinamik.erp_backend.inventory.receipt.receipt_line_request;
import vn.vinamik.erp_backend.inventory.receipt.receipt_request;
import vn.vinamik.erp_backend.inventory.receipt.receipt_response;
import vn.vinamik.erp_backend.inventory.stocktake.inventory_stocktake_service;
import vn.vinamik.erp_backend.inventory.stocktake.stocktake_count_request;
import vn.vinamik.erp_backend.inventory.stocktake.stocktake_request;
import vn.vinamik.erp_backend.inventory.stocktake.stocktake_response;
import vn.vinamik.erp_backend.inventory.transfer.inventory_transfer_service;
import vn.vinamik.erp_backend.inventory.transfer.transfer_line_request;
import vn.vinamik.erp_backend.inventory.transfer.transfer_request;
import vn.vinamik.erp_backend.inventory.transfer.transfer_response;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
class inventory_ledger_integration_tests {
    @PersistenceContext
    private EntityManager entity_manager;

    @Autowired
    private inventory_receipt_service receipt_service;

    @Autowired
    private inventory_issue_service issue_service;

    @Autowired
    private inventory_transfer_service transfer_service;

    @Autowired
    private inventory_stocktake_service stocktake_service;

    @Test
    @Transactional
    void receipt_issue_transfer_share_one_balanced_ledger() {
        inventory_fixture fixture = create_fixture();
        authenticated_user actor = actor();

        receipt_response receipt = receipt_service.create(
                new receipt_request(
                        fixture.code("receipt"),
                        fixture.source_warehouse_id(),
                        null,
                        null,
                        null,
                        null,
                        fixture.code("receipt_key"),
                        "integration receipt",
                        List.of(new receipt_line_request(
                                fixture.stock_item_id(),
                                fixture.source_location_id(),
                                null,
                                quantity("10")))),
                actor,
                "inventory-test");
        receipt_response posted_receipt = receipt_service.post(receipt.receipt_id(), actor, "inventory-test");
        assertEquals("posted", posted_receipt.status());
        assertEquals(quantity("10"), balance(fixture.stock_item_id(), fixture.source_location_id()));

        receipt_response retried_receipt = receipt_service.post(receipt.receipt_id(), actor, "inventory-test");
        assertEquals("posted", retried_receipt.status());
        assertEquals(1L, movement_count("receipt", receipt.receipt_id()));

        issue_response issue = issue_service.create(
                new issue_request(
                        fixture.code("issue"),
                        fixture.source_warehouse_id(),
                        null,
                        null,
                        "material_use",
                        fixture.code("issue_key"),
                        "integration issue",
                        List.of(new issue_line_request(
                                fixture.stock_item_id(),
                                fixture.source_location_id(),
                                null,
                                quantity("3")))),
                actor,
                "inventory-test");
        issue_response posted_issue = issue_service.post(issue.issue_id(), actor, "inventory-test");
        assertEquals("posted", posted_issue.status());
        assertEquals(quantity("7"), balance(fixture.stock_item_id(), fixture.source_location_id()));

        issue_response retried_issue = issue_service.post(issue.issue_id(), actor, "inventory-test");
        assertEquals("posted", retried_issue.status());
        assertEquals(1L, movement_count("issue", issue.issue_id()));

        transfer_response transfer = transfer_service.create(
                new transfer_request(
                        fixture.code("transfer"),
                        fixture.source_warehouse_id(),
                        fixture.destination_warehouse_id(),
                        fixture.code("transfer_key"),
                        "integration transfer",
                        List.of(new transfer_line_request(
                                fixture.stock_item_id(),
                                null,
                                fixture.source_location_id(),
                                fixture.destination_location_id(),
                                quantity("4")))),
                actor,
                "inventory-test");
        transfer_response posted_transfer = transfer_service.post(transfer.transfer_id(), actor, "inventory-test");
        assertEquals("posted", posted_transfer.status());
        assertEquals(quantity("3"), balance(fixture.stock_item_id(), fixture.source_location_id()));
        assertEquals(quantity("4"), balance(fixture.stock_item_id(), fixture.destination_location_id()));
        assertEquals(2L, movement_count("transfer", transfer.transfer_id()));

        stocktake_response stocktake = stocktake_service.create(
                new stocktake_request(fixture.code("stocktake"), fixture.source_warehouse_id(),
                        "integration stocktake"),
                actor,
                "inventory-test");
        assertEquals("counting", stocktake.status());
        assertEquals(1, stocktake.lines().size());
        long stocktake_line_id = stocktake.lines().getFirst().stocktake_line_id();
        stocktake_service.count_line(
                stocktake.stocktake_id(),
                stocktake_line_id,
                new stocktake_count_request(quantity("2")),
                actor,
                "inventory-test");
        assertEquals("submitted", stocktake_service.submit(
                stocktake.stocktake_id(), actor, "inventory-test").status());
        assertEquals("approved", stocktake_service.approve(
                stocktake.stocktake_id(), actor, "inventory-test").status());
        assertEquals("posted", stocktake_service.post(
                stocktake.stocktake_id(), actor, "inventory-test").status());
        assertEquals("posted", stocktake_service.post(
                stocktake.stocktake_id(), actor, "inventory-test").status());
        assertEquals(quantity("2"), balance(fixture.stock_item_id(), fixture.source_location_id()));
        assertEquals(quantity("4"), balance(fixture.stock_item_id(), fixture.destination_location_id()));
        assertEquals(1L, movement_count("stocktake", stocktake.stocktake_id()));

        BigDecimal ledger_total = scalar_decimal(
                "select coalesce(sum(quantity_delta), 0) "
                        + "from inventory.stock_movement where stock_item_id = :stock_item_id",
                Map.of("stock_item_id", fixture.stock_item_id()));
        assertEquals(quantity("6"), ledger_total);
        assertEquals(quantity("6"), balance(fixture.stock_item_id(), fixture.source_location_id())
                .add(balance(fixture.stock_item_id(), fixture.destination_location_id())));
        assertTrue(ledger_total.signum() >= 0);
    }

    @Test
    @Transactional
    void issue_rejects_quantity_greater_than_available_balance() {
        inventory_fixture fixture = create_fixture();
        authenticated_user actor = actor();

        receipt_response receipt = receipt_service.create(
                new receipt_request(
                        fixture.code("receipt_negative"),
                        fixture.source_warehouse_id(),
                        null,
                        null,
                        null,
                        null,
                        fixture.code("receipt_negative_key"),
                        null,
                        List.of(new receipt_line_request(
                                fixture.stock_item_id(),
                                fixture.source_location_id(),
                                null,
                                quantity("2")))),
                actor,
                "inventory-test");
        receipt_service.post(receipt.receipt_id(), actor, "inventory-test");

        issue_response issue = issue_service.create(
                new issue_request(
                        fixture.code("issue_negative"),
                        fixture.source_warehouse_id(),
                        null,
                        null,
                        "material_use",
                        fixture.code("issue_negative_key"),
                        null,
                        List.of(new issue_line_request(
                                fixture.stock_item_id(),
                                fixture.source_location_id(),
                                null,
                                quantity("3")))),
                actor,
                "inventory-test");

        assertThrows(field_conflict_exception.class,
                () -> issue_service.post(issue.issue_id(), actor, "inventory-test"));
    }

    private inventory_fixture create_fixture() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        long unit_id = insert_id(
                "insert into inventory.unit_of_measure (unit_code, unit_name) "
                        + "values (:code, :name) returning unit_of_measure_id",
                Map.of("code", "it_" + suffix, "name", "Integration unit"));
        long stock_item_id = insert_id(
                "insert into inventory.stock_item "
                        + "(item_code, item_name, item_type, base_unit_of_measure_id, lot_controlled) "
                        + "values (:code, :name, 'raw_material', :unit_id, false) returning stock_item_id",
                Map.of("code", "it_" + suffix, "name", "Integration material", "unit_id", unit_id));
        long source_warehouse_id = insert_id(
                "insert into inventory.warehouse (warehouse_code, warehouse_name) "
                        + "values (:code, :name) returning warehouse_id",
                Map.of("code", "src_" + suffix, "name", "Integration source warehouse"));
        long destination_warehouse_id = insert_id(
                "insert into inventory.warehouse (warehouse_code, warehouse_name) "
                        + "values (:code, :name) returning warehouse_id",
                Map.of("code", "dst_" + suffix, "name", "Integration destination warehouse"));
        long source_location_id = insert_id(
                "insert into inventory.warehouse_location "
                        + "(warehouse_id, location_code, location_name) "
                        + "values (:warehouse_id, :code, :name) returning warehouse_location_id",
                Map.of("warehouse_id", source_warehouse_id, "code", "src_bin_" + suffix, "name", "Source bin"));
        long destination_location_id = insert_id(
                "insert into inventory.warehouse_location "
                        + "(warehouse_id, location_code, location_name) "
                        + "values (:warehouse_id, :code, :name) returning warehouse_location_id",
                Map.of("warehouse_id", destination_warehouse_id, "code", "dst_bin_" + suffix, "name", "Destination bin"));
        return new inventory_fixture(
                stock_item_id,
                source_warehouse_id,
                destination_warehouse_id,
                source_location_id,
                destination_location_id,
                suffix);
    }

    private authenticated_user actor() {
        Long user_id = scalar_long(
                "select min(user_id) from identity.user_account where status = 'active'", Map.of());
        assertTrue(user_id != null && user_id > 0, "An active integration actor is required.");
        return new authenticated_user(user_id, "integration_actor", List.of(), false);
    }

    private long insert_id(String sql, Map<String, Object> parameters) {
        Query query = entity_manager.createNativeQuery(sql);
        parameters.forEach(query::setParameter);
        return ((Number) query.getSingleResult()).longValue();
    }

    private BigDecimal balance(long stock_item_id, long location_id) {
        return scalar_decimal(
                "select coalesce((select on_hand_quantity "
                        + "from inventory.stock_balance "
                        + "where stock_item_id = :stock_item_id "
                        + "and warehouse_location_id = :location_id "
                        + "and stock_lot_id is null), 0)",
                Map.of("stock_item_id", stock_item_id, "location_id", location_id));
    }

    private long movement_count(String source_document_type, long source_document_id) {
        return scalar_long(
                "select count(*) from inventory.stock_movement "
                        + "where source_document_type = :source_document_type "
                        + "and source_document_id = :source_document_id",
                Map.of("source_document_type", source_document_type, "source_document_id", source_document_id));
    }

    private BigDecimal scalar_decimal(String sql, Map<String, Object> parameters) {
        Query query = entity_manager.createNativeQuery(sql);
        parameters.forEach(query::setParameter);
        return (BigDecimal) query.getSingleResult();
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

    private record inventory_fixture(
            long stock_item_id,
            long source_warehouse_id,
            long destination_warehouse_id,
            long source_location_id,
            long destination_location_id,
            String suffix) {
        private String code(String prefix) {
            return prefix + "_" + suffix;
        }
    }
}



