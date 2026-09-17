package vn.vinamik.erp_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

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
class erp_backend_application_tests {

    @Test
    void application_context_loads_without_connecting_to_a_database() {
    }
}

