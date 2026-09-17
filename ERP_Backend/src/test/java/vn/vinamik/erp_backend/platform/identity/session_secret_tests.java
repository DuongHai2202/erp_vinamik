package vn.vinamik.erp_backend.platform.identity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class session_secret_tests {
    @Test
    void generates_unique_secret_and_stable_sha256_hash() {
        session_secret.generated_session_secret first = session_secret.generate();
        session_secret.generated_session_secret second = session_secret.generate();

        assertNotEquals(first.value(), second.value());
        assertEquals(first.hash(), session_secret.hash(first.value()));
        assertNotEquals(first.value(), first.hash());
        assertTrue(first.hash().matches("[a-f0-9]{64}"));
    }
}