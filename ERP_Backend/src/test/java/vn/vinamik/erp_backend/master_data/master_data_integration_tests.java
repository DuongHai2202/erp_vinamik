package vn.vinamik.erp_backend.master_data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.human_resources.master_data.department_request;
import vn.vinamik.erp_backend.human_resources.master_data.department_response;
import vn.vinamik.erp_backend.human_resources.master_data.human_resources_master_data_service;
import vn.vinamik.erp_backend.human_resources.master_data.job_title_request;
import vn.vinamik.erp_backend.human_resources.master_data.job_title_response;
import vn.vinamik.erp_backend.human_resources.master_data.work_shift_request;
import vn.vinamik.erp_backend.human_resources.master_data.work_shift_response;
import vn.vinamik.erp_backend.inventory.master_data.inventory_category_request;
import vn.vinamik.erp_backend.inventory.master_data.inventory_category_response;
import vn.vinamik.erp_backend.inventory.master_data.inventory_location_request;
import vn.vinamik.erp_backend.inventory.master_data.inventory_location_response;
import vn.vinamik.erp_backend.inventory.master_data.inventory_master_data_service;
import vn.vinamik.erp_backend.inventory.master_data.inventory_supplier_request;
import vn.vinamik.erp_backend.inventory.master_data.inventory_supplier_response;
import vn.vinamik.erp_backend.inventory.master_data.inventory_unit_request;
import vn.vinamik.erp_backend.inventory.master_data.inventory_unit_response;
import vn.vinamik.erp_backend.inventory.master_data.inventory_warehouse_request;
import vn.vinamik.erp_backend.inventory.master_data.inventory_warehouse_response;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "erp.bootstrap-admin.username=",
        "erp.bootstrap-admin.password="
})
@EnabledIfEnvironmentVariable(named = "ERP_RUN_INTEGRATION_TESTS", matches = "true")
class master_data_integration_tests {
    @PersistenceContext
    private EntityManager entity_manager;

    @Autowired
    private human_resources_master_data_service hr_master_data_service;

    @Autowired
    private inventory_master_data_service inventory_master_data_service;

    @Test
    @Transactional
    void authorized_master_data_services_create_search_and_deactivate_reference_data() {
        authenticated_user actor = actor();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        department_response parent = hr_master_data_service.create_department(
                new department_request("dept_" + suffix, "Integration department", null, "active"),
                actor, "master-data-integration");
        department_response child = hr_master_data_service.create_department(
                new department_request("dept_child_" + suffix, "Integration child department",
                        parent.department_id(), "active"), actor, "master-data-integration");
        assertEquals(parent.department_id(), child.parent_department_id());
        assertTrue(hr_master_data_service.search_departments("integration", "active", 0, 50).total_items() >= 2);

        job_title_response job_title = hr_master_data_service.create_job_title(
                new job_title_request("job_" + suffix, "Integration job title", null, "active"),
                actor, "master-data-integration");
        assertEquals(job_title.job_title_id(), hr_master_data_service.find_job_title(job_title.job_title_id()).job_title_id());

        work_shift_response shift = hr_master_data_service.create_work_shift(
                new work_shift_request("shift_" + suffix, "Integration shift",
                        LocalTime.of(8, 0), LocalTime.of(17, 0), "active"), actor, "master-data-integration");
        assertTrue(hr_master_data_service.find_active_work_shifts().stream()
                .anyMatch(item -> item.work_shift_id() == shift.work_shift_id()));

        inventory_unit_response unit = inventory_master_data_service.create_unit(
                new inventory_unit_request("u_" + suffix, "Integration unit", (short) 3, "active"),
                actor, "master-data-integration");
        inventory_category_response category = inventory_master_data_service.create_category(
                new inventory_category_request("cat_" + suffix, "Integration category", "active"),
                actor, "master-data-integration");
        inventory_supplier_response supplier = inventory_master_data_service.create_supplier(
                new inventory_supplier_request("sup_" + suffix, "Integration supplier", null,
                        "supplier_" + suffix + "@example.com", null, "active"), actor, "master-data-integration");
        inventory_warehouse_response warehouse = inventory_master_data_service.create_warehouse(
                new inventory_warehouse_request("wh_" + suffix, "Integration warehouse", null, "active"),
                actor, "master-data-integration");
        inventory_location_response location = inventory_master_data_service.create_location(
                new inventory_location_request(warehouse.warehouse_id(), "loc_" + suffix,
                        "Integration location", "active"), actor, "master-data-integration");

        assertEquals(unit.unit_of_measure_id(), inventory_master_data_service.find_unit(unit.unit_of_measure_id()).unit_of_measure_id());
        assertEquals(category.item_category_id(), inventory_master_data_service.find_category(category.item_category_id()).item_category_id());
        assertEquals(supplier.supplier_id(), inventory_master_data_service.find_supplier(supplier.supplier_id()).supplier_id());
        assertEquals(location.warehouse_id(), warehouse.warehouse_id());
        assertEquals(1L, inventory_master_data_service.search_locations(warehouse.warehouse_id(), null, "active", 0, 50).total_items());

        inventory_master_data_service.deactivate_location(location.warehouse_location_id(), actor, "master-data-integration");
        assertEquals("inactive", inventory_master_data_service.find_location(location.warehouse_location_id()).status());

        assertThrows(IllegalArgumentException.class, () -> hr_master_data_service.update_department(
                parent.department_id(),
                new department_request(parent.department_code(), parent.department_name(),
                        child.department_id(), "active"),
                actor, "master-data-integration"));
    }

    private authenticated_user actor() {
        Long user_id = ((Number) entity_manager.createNativeQuery(
                "select min(user_id) from identity.user_account where status = 'active'").getSingleResult()).longValue();
        return new authenticated_user(user_id, "master_data_integration_actor", List.of(), false);
    }
}
