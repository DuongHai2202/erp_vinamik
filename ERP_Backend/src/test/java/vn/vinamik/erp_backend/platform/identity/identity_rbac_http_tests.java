package vn.vinamik.erp_backend.platform.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/erp_test_disabled",
        "spring.datasource.username=erp_test",
        "spring.datasource.password=test_only",
        "erp.bootstrap-admin.username=",
        "erp.bootstrap-admin.password="
})
@AutoConfigureMockMvc
class identity_rbac_http_tests {
    @Autowired
    private MockMvc mock_mvc;

    @Test
    void unauthenticated_api_request_returns_401() throws Exception {
        mock_mvc.perform(get("/api/v1/inventory/balances"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void unauthenticated_audit_request_returns_401() throws Exception {
        mock_mvc.perform(get("/api/v1/identity/audit_events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void unauthenticated_hr_request_returns_401() throws Exception {
        mock_mvc.perform(get("/api/v1/human_resources/employees"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void unauthenticated_payroll_request_returns_401() throws Exception {
        mock_mvc.perform(get("/api/v1/human_resources/payroll/periods"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void read_only_authority_cannot_read_audit_events() throws Exception {
        mock_mvc.perform(get("/api/v1/identity/audit_events")
                        .with(user("reader").authorities(() -> "identity_user_read")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void read_only_authority_cannot_post_inventory_receipt() throws Exception {
        mock_mvc.perform(post("/api/v1/inventory/receipts")
                        .with(user("reader").authorities(() -> "inventory_receipt_read"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receipt_code\":\"test_receipt\",\"warehouse_id\":1,\"lines\":[{\"stock_item_id\":1,\"warehouse_location_id\":1,\"quantity\":1}]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void read_only_authority_cannot_post_inventory_issue() throws Exception {
        mock_mvc.perform(post("/api/v1/inventory/issues/1/post")
                        .with(user("reader").authorities(() -> "inventory_issue_read"))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void read_only_authority_cannot_post_inventory_transfer() throws Exception {
        mock_mvc.perform(post("/api/v1/inventory/transfers/1/post")
                        .with(user("reader").authorities(() -> "inventory_transfer_read"))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void read_only_authority_cannot_post_inventory_stocktake_adjustment() throws Exception {
        mock_mvc.perform(post("/api/v1/inventory/stocktakes/1/post")
                        .with(user("reader").authorities(() -> "inventory_stocktake_read"))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void read_only_authority_cannot_create_hr_employee() throws Exception {
        mock_mvc.perform(post("/api/v1/human_resources/employees")
                        .with(user("reader").authorities(() -> "hr_employee_read"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employee_code\":\"test_employee\",\"full_name\":\"Test employee\",\"employment_status\":\"active\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void payroll_read_authority_cannot_calculate_payroll() throws Exception {
        mock_mvc.perform(post("/api/v1/human_resources/payroll/periods/1/calculate")
                        .with(user("reader").authorities(() -> "hr_payroll_read"))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void read_only_authority_cannot_create_production_order() throws Exception {
        mock_mvc.perform(post("/api/v1/production/orders")
                        .with(user("reader").authorities(() -> "production_order_read"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_code\":\"test_order\",\"production_plan_line_id\":1,\"bom_id\":1,\"target_quantity\":1,\"planned_starts_on\":\"2026-01-01\",\"planned_ends_on\":\"2026-01-02\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void production_read_authority_cannot_post_finished_output() throws Exception {
        mock_mvc.perform(post("/api/v1/production/orders/1/outputs/1/post")
                        .with(user("reader").authorities(() -> "production_output_read"))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
    @Test
    void unauthenticated_master_data_request_returns_401() throws Exception {
        mock_mvc.perform(get("/api/v1/inventory/master_data/units"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void read_only_master_authority_cannot_create_hr_department() throws Exception {
        mock_mvc.perform(post("/api/v1/human_resources/master_data/departments")
                        .with(user("reader").authorities(() -> "hr_master_read"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"department_code\":\"test_department\",\"department_name\":\"Test department\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void read_only_master_authority_cannot_create_inventory_warehouse() throws Exception {
        mock_mvc.perform(post("/api/v1/inventory/master_data/warehouses")
                        .with(user("reader").authorities(() -> "inventory_master_read"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouse_code\":\"test_warehouse\",\"warehouse_name\":\"Test warehouse\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}
