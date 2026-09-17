package vn.vinamik.erp_backend.platform.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Native-query gateway backed exclusively by JPA EntityManager.
 * SQL stays in the owning module's repository/service until that slice is extracted.
 */
@Repository
public class jpa_native_query_executor {
    @PersistenceContext
    private EntityManager entity_manager;

    public <T> List<T> query(String sql, BiFunction<jpa_result_row, Integer, T> mapper, Object... parameters) {
        Query query = entity_manager.createNativeQuery(index_parameters(sql), Tuple.class);
        bind(query, parameters);
        List<?> rows = query.getResultList();
        List<T> result = new ArrayList<>(rows.size());
        for (int index = 0; index < rows.size(); index++) {
            result.add(mapper.apply(new jpa_result_row((Tuple) rows.get(index)), index));
        }
        return result;
    }

    public <T> T queryForObject(String sql, Class<T> type, Object... parameters) {
        Query query = entity_manager.createNativeQuery(index_parameters(sql));
        bind(query, parameters);
        List<?> rows = query.getResultList();
        return rows.isEmpty() ? null : convert(rows.getFirst(), type);
    }

    public <T> T queryForObject(String sql, BiFunction<jpa_result_row, Integer, T> mapper, Object... parameters) {
        List<T> rows = query(sql, mapper, parameters);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public int update(String sql, Object... parameters) {
        Query query = entity_manager.createNativeQuery(index_parameters(sql));
        bind(query, parameters);
        return query.executeUpdate();
    }

    public void execute(String sql, Object... parameters) {
        Query query = entity_manager.createNativeQuery(index_parameters(sql));
        bind(query, parameters);
        query.getResultList();
    }

    private void bind(Query query, Object[] parameters) {
        for (int index = 0; index < parameters.length; index++) {
            query.setParameter(index + 1, parameters[index]);
        }
    }

    private String index_parameters(String sql) {
        StringBuilder result = new StringBuilder(sql.length() + 8);
        boolean single_quote = false;
        int parameter_index = 0;
        for (int index = 0; index < sql.length(); index++) {
            char current = sql.charAt(index);
            if (current == '\'' && (index == 0 || sql.charAt(index - 1) != '\\')) {
                single_quote = !single_quote;
            }
            if (current == '?' && !single_quote) {
                result.append('?').append(++parameter_index);
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private <T> T convert(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        if (type == Long.class || type == long.class) {
            return (T) Long.valueOf(((Number) value).longValue());
        }
        if (type == Integer.class || type == int.class) {
            return (T) Integer.valueOf(((Number) value).intValue());
        }
        if (type == BigDecimal.class) {
            return (T) (value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString()));
        }
        if (type == Boolean.class || type == boolean.class) {
            return (T) (value instanceof Boolean bool ? bool : Boolean.valueOf(value.toString()));
        }
        if (type == String.class) {
            return (T) value.toString();
        }
        return type.cast(value);
    }
}
