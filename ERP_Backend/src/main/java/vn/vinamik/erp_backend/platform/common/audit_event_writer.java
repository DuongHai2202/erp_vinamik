package vn.vinamik.erp_backend.platform.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class audit_event_writer {
    private final jpa_native_query_executor jpa_query_executor;
    private final ObjectMapper object_mapper = new ObjectMapper();

    public audit_event_writer(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public void write(
            Long actor_user_id,
            String module_code,
            String action_code,
            String entity_type,
            String entity_id,
            String correlation_id,
            Map<String, Object> metadata) {
        jpa_query_executor.update(
                """
                INSERT INTO identity.audit_log
                    (actor_user_id, module_code, action_code, entity_type, entity_id, correlation_id, metadata)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb))
                """,
                actor_user_id,
                module_code,
                action_code,
                entity_type,
                entity_id,
                correlation_id,
                to_json(metadata));
    }

    private String to_json(Map<String, Object> metadata) {
        try {
            return object_mapper.writeValueAsString(sanitize_map(metadata, 0));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Audit metadata cannot be serialized.", exception);
        }
    }

    private Map<String, Object> sanitize_map(Map<String, Object> metadata, int depth) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        if (metadata == null || depth > 5) {
            return sanitized;
        }
        metadata.forEach((key, value) -> {
            if (key == null || is_sensitive_key(key)) {
                return;
            }
            sanitized.put(key, sanitize_value(value, depth + 1));
        });
        return sanitized;
    }

    private Object sanitize_value(Object value, int depth) {
        if (value instanceof Map<?, ?> nested_map) {
            Map<String, Object> normalized_map = new LinkedHashMap<>();
            nested_map.forEach((key, nested_value) -> {
                if (key != null && !is_sensitive_key(key.toString())) {
                    normalized_map.put(key.toString(), sanitize_value(nested_value, depth + 1));
                }
            });
            return normalized_map;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> normalized_list = new ArrayList<>();
            for (Object item : iterable) {
                normalized_list.add(sanitize_value(item, depth + 1));
            }
            return normalized_list;
        }
        return value;
    }

    private boolean is_sensitive_key(String key) {
        String normalized_key = key.toLowerCase(Locale.ROOT);
        return normalized_key.contains("password")
                || normalized_key.contains("secret")
                || normalized_key.contains("token")
                || normalized_key.contains("authorization")
                || normalized_key.contains("cookie");
    }
}
