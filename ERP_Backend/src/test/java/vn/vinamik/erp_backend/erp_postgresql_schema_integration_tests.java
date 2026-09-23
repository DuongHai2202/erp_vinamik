package vn.vinamik.erp_backend;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.platform.identity.identity_audit_service;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs only when ERP_RUN_INTEGRATION_TESTS=true and the supplied database
 * environment points to an isolated PostgreSQL test database.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "erp.bootstrap-admin.username=",
        "erp.bootstrap-admin.password="
})
@EnabledIfEnvironmentVariable(named = "ERP_RUN_INTEGRATION_TESTS", matches = "true")
class erp_postgresql_schema_integration_tests {
    @PersistenceContext
    private EntityManager entity_manager;

    @Autowired
    private identity_audit_service audit_service;

    @Test
    @Transactional(readOnly = true)
    void migrations_schemas_and_permission_seed_are_available() {
        assertEquals(25L, scalar_long(
                "select count(*) from public.flyway_schema_history where success = true"));
        assertEquals(1L, scalar_long(
                "select count(*) from pg_indexes where schemaname = 'hr' "
                        + "and indexname = 'ix_contract_active_period'"));
        assertEquals(1L, scalar_long(
                "select count(*) from pg_indexes where schemaname = 'hr' "
                        + "and indexname = 'ix_leave_approved_unpaid_period'"));
        assertEquals(1L, scalar_long(
                "select count(*) from pg_indexes where schemaname = 'hr' "
                        + "and indexname = 'ix_reward_approved_period'"));
        assertEquals(1L, scalar_long(
                "select count(*) from pg_indexes where schemaname = 'hr' "
                        + "and indexname = 'ix_employee_date_of_birth'"));
        assertEquals(1L, scalar_long(
                "select count(*) from pg_constraint "
                        + "where conname = 'ck_employee_phone_number_format' "
                        + "and conrelid = 'hr.employee'::regclass"));
        assertEquals(4L, scalar_long(
                "select count(*) from pg_namespace "
                        + "where nspname in ('identity', 'hr', 'inventory', 'production')"));
        assertTrue(scalar_long(
                "select count(*) from identity.permission where status = 'active'") > 0);
        assertEquals(1L, scalar_long(
                "select count(*) from identity.permission "
                        + "where permission_code = 'identity_audit_read' and status = 'active'"));
        assertTrue(scalar_long(
                "select count(*) from inventory.unit_of_measure where status = 'active'") > 0);
    }

    @Test
    @Transactional(readOnly = true)
    void audit_query_supports_time_filters_and_paging() {
        Instant now = Instant.now();
        var page = audit_service.search(
                "identity", null, null, null, null,
                now.minusSeconds(86400), now.plusSeconds(60), 0, 20);
        assertEquals(0, page.page());
        assertEquals(20, page.page_size());
        assertTrue(page.total_items() >= 0);
        assertTrue(page.items().size() <= 20);
        page.items().forEach(item -> assertNotNull(item.metadata()));
    }

    @Test
    @Transactional(readOnly = true)
    void schema_constraints_protect_identity_and_inventory_invariants() {
        assertEquals(1L, scalar_long(
                "select count(*) from pg_constraint "
                        + "where conname = 'uq_user_account_username' "
                        + "and conrelid = 'identity.user_account'::regclass"));
        assertEquals(1L, scalar_long(
                "select count(*) from pg_constraint "
                        + "where conname = 'ck_stock_item_minimum_quantity' "
                        + "and conrelid = 'inventory.stock_item'::regclass"));
        assertEquals(1L, scalar_long(
                "select count(*) from pg_index "
                        + "where indexrelid = 'identity.uq_user_account_single_super_admin'::regclass"));
        assertEquals(1L, scalar_long(
                "select count(*) from identity.user_account where is_super_admin"));
        assertEquals(1L, scalar_long(
                "select count(*) from pg_trigger "
                        + "where tgname = 'trg_user_account_protect_super_admin' "
                        + "and tgrelid = 'identity.user_account'::regclass"));
        assertEquals(1L, scalar_long(
                "select count(*) from pg_trigger "
                        + "where tgname = 'trg_user_role_protect_super_admin' "
                        + "and tgrelid = 'identity.user_role'::regclass"));
        assertEquals(1L, scalar_long(
                "select count(*) from pg_trigger "
                        + "where tgname = 'trg_stock_movement_append_only' "
                        + "and tgrelid = 'inventory.stock_movement'::regclass"));
        assertEquals(1L, scalar_long(
                "select count(*) from pg_trigger "
                        + "where tgname = 'trg_audit_log_append_only' "
                        + "and tgrelid = 'identity.audit_log'::regclass"));
        assertEquals(1L, scalar_long(
                "select count(*) from pg_trigger "
                        + "where tgname = 'trg_reject_locked_payroll_period_mutation' "
                        + "and tgrelid = 'hr.payroll_period'::regclass"));
        assertEquals(1L, scalar_long(
                "select count(*) from pg_trigger "
                        + "where tgname = 'trg_reject_locked_payroll_record_mutation' "
                        + "and tgrelid = 'hr.payroll_record'::regclass"));
        assertEquals(1L, scalar_long(
                "select count(*) from pg_trigger "
                        + "where tgname = 'trg_reject_locked_payroll_line_mutation' "
                        + "and tgrelid = 'hr.payroll_line'::regclass"));
        assertEquals(0L, scalar_long(
                "select count(*) from pg_trigger "
                        + "where tgname in ('trg_reject_locked_contract_mutation', "
                        + "'trg_reject_locked_leave_mutation', 'trg_reject_locked_reward_mutation')"));
        assertTrue(scalar_long(
                "select count(*) from identity.user_role "
                        + "where user_id = (select user_id from identity.user_account where is_super_admin) "
                        + "and role_id = (select role_id from identity.role where role_code = 'system_admin')") > 0);
    }

    @Test
    @Transactional
    void database_rejects_removing_super_admin_system_admin_role() {
        assertThrows(RuntimeException.class, () -> entity_manager.createNativeQuery(
                "DELETE FROM identity.user_role WHERE user_id = (SELECT user_id FROM identity.user_account WHERE is_super_admin) "
                        + "AND role_id = (SELECT role_id FROM identity.role WHERE role_code = 'system_admin')"
        ).executeUpdate());
    }

    private long scalar_long(String sql) {
        Object result = entity_manager.createNativeQuery(sql).getSingleResult();
        return ((Number) result).longValue();
    }
}
