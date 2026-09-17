package vn.vinamik.erp_backend.production.order_progress;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class production_order_progress_service_tests {
    @Test
    void rejects_non_positive_order_id_before_database_call() {
        production_order_progress_service service = new production_order_progress_service(null);

        assertThrows(IllegalArgumentException.class, () -> service.find_by_order_id(0));
    }
}

