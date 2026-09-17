package vn.vinamik.erp_backend.platform.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class audit_event_writer_tests {
    @Test
    void serializes_control_characters_as_valid_json() throws Exception {
        jpa_native_query_executor jpa_query_executor = mock(jpa_native_query_executor.class);
        audit_event_writer writer = new audit_event_writer(jpa_query_executor);

        writer.write(1L, "identity", "test", "user", "1", "correlation", Map.of(
                "message", "first\nsecond \"quoted\""));

        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
        verify(jpa_query_executor).update(anyString(), arguments.capture());
        String json = (String) arguments.getValue()[6];
        assertEquals("first\nsecond \"quoted\"", new ObjectMapper().readTree(json).get("message").asText());
        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\\""));
    }

    @Test
    void removes_sensitive_metadata_keys_before_persisting() throws Exception {
        jpa_native_query_executor jpa_query_executor = mock(jpa_native_query_executor.class);
        audit_event_writer writer = new audit_event_writer(jpa_query_executor);

        writer.write(1L, "identity", "test", "user", "1", "correlation", Map.of(
                "password", "hidden",
                "nested", Map.of("session_token", "hidden", "safe", "kept")));

        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
        verify(jpa_query_executor).update(anyString(), arguments.capture());
        String json = (String) arguments.getValue()[6];
        var node = new ObjectMapper().readTree(json);
        assertTrue(node.get("password") == null);
        assertTrue(node.get("nested").get("session_token") == null);
        assertEquals("kept", node.get("nested").get("safe").asText());
    }
}
