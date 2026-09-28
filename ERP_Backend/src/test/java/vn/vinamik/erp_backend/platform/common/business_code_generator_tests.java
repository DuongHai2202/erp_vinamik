package vn.vinamik.erp_backend.platform.common;

import org.junit.jupiter.api.Test;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class business_code_generator_tests {
    @Test
    void formats_yearly_code_from_allocated_counter() {
        jpa_native_query_executor executor = mock(jpa_native_query_executor.class);
        when(executor.queryForObject(any(String.class), eq(Long.class), any(), any()))
                .thenReturn(7L);

        business_code_generator generator = new business_code_generator(executor);

        assertEquals("plan_2026_000007",
                generator.next_yearly("production_plan", "plan_", LocalDate.of(2026, 9, 28), 6));
    }
}
