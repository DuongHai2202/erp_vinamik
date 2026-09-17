package vn.vinamik.erp_backend.platform.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class pagination_guard_tests {
    @Test
    void negative_page_is_normalized_to_zero() {
        assertEquals(0, pagination_guard.normalize_page(-1));
    }

    @Test
    void excessively_large_page_is_rejected_before_database_access() {
        assertThrows(IllegalArgumentException.class,
                () -> pagination_guard.normalize_page(1_000_001));
    }

    @Test
    void maximum_supported_page_is_accepted() {
        assertEquals(1_000_000, pagination_guard.normalize_page(1_000_000));
    }

    @Test
    void offset_is_calculated_without_integer_multiplication_overflow() {
        assertEquals(100_000_000, pagination_guard.offset(1_000_000, 100));
    }

    @Test
    void offset_overflow_is_rejected() {
        assertThrows(ArithmeticException.class,
                () -> pagination_guard.offset(1_000_000, Integer.MAX_VALUE));
    }
}
