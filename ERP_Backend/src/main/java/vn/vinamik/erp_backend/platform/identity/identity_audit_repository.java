package vn.vinamik.erp_backend.platform.identity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;
import java.time.Instant;
import java.util.List;

@Repository
public class identity_audit_repository {
    private final jpa_native_query_executor jpa_query_executor;
    private final ObjectMapper object_mapper = new ObjectMapper();

    public identity_audit_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public long count(String module_code, String action_code, String entity_type, String entity_id,
                      Long actor_user_id, Instant from_at, Instant to_at) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) " + common_where(), Long.class,
                module_code, module_code, action_code, action_code,
                entity_type, entity_type, entity_id, entity_id,
                actor_user_id, actor_user_id, from_at, from_at,
                to_at, to_at);
        return total == null ? 0 : total;
    }

    public List<identity_audit_event_response> search(String module_code, String action_code, String entity_type,
                                                       String entity_id, Long actor_user_id, Instant from_at,
                                                       Instant to_at, int page_size, int offset) {
        return jpa_query_executor.query(
                "SELECT audit.audit_id, audit.actor_user_id, account.username AS actor_username, "
                        + "audit.module_code, audit.action_code, audit.entity_type, audit.entity_id, "
                        + "audit.correlation_id, audit.occurred_at, audit.metadata "
                        + common_where()
                        + " ORDER BY audit.occurred_at DESC, audit.audit_id DESC LIMIT ? OFFSET ?",
                this::map_row,
                module_code, module_code, action_code, action_code,
                entity_type, entity_type, entity_id, entity_id,
                actor_user_id, actor_user_id, from_at, from_at,
                to_at, to_at, page_size, offset);
    }

    private String common_where() {
        return "FROM identity.audit_log AS audit "
                + "LEFT JOIN identity.user_account AS account ON account.user_id = audit.actor_user_id "
                + "WHERE (CAST(? AS text) IS NULL OR audit.module_code = ?) "
                + "AND (CAST(? AS text) IS NULL OR audit.action_code = ?) "
                + "AND (CAST(? AS text) IS NULL OR audit.entity_type = ?) "
                + "AND (CAST(? AS text) IS NULL OR audit.entity_id = ?) "
                + "AND (CAST(? AS text) IS NULL OR audit.actor_user_id = ?) "
                + "AND (CAST(? AS text) IS NULL OR audit.occurred_at >= ?) "
                + "AND (CAST(? AS text) IS NULL OR audit.occurred_at <= ?)";
    }

    private identity_audit_event_response map_row(jpa_result_row result_set, int row_number) {
        return new identity_audit_event_response(
                result_set.getLong("audit_id"),
                result_set.getObject("actor_user_id", Long.class),
                result_set.getString("actor_username"),
                result_set.getString("module_code"),
                result_set.getString("action_code"),
                result_set.getString("entity_type"),
                result_set.getString("entity_id"),
                result_set.getString("correlation_id"),
                result_set.get_instant("occurred_at"),
                to_metadata(result_set.getString("metadata")));
    }

    private java.util.Map<String, Object> to_metadata(String json) {
        try {
            return object_mapper.readValue(json == null || json.isBlank() ? "{}" : json,
                    new TypeReference<java.util.Map<String, Object>>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Audit metadata is invalid.", exception);
        }
    }

}