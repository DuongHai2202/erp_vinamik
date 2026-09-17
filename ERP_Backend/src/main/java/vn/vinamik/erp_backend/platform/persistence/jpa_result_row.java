package vn.vinamik.erp_backend.platform.persistence;

import jakarta.persistence.Tuple;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A small read-only row adapter for native JPA projections.
 * It keeps module mappers independent of database-driver result APIs and driver-specific value types.
 */
public final class jpa_result_row {
    private final Tuple tuple;

    public jpa_result_row(Tuple tuple) {
        this.tuple = tuple;
    }

    public Object getObject(String alias) {
        try {
            return tuple.get(alias);
        } catch (IllegalArgumentException exception) {
            return tuple.getElements().stream()
                    .filter(element -> element.getAlias() != null
                            && element.getAlias().equalsIgnoreCase(alias))
                    .findFirst()
                    .map(element -> tuple.get(element.getAlias()))
                    .orElse(null);
        }
    }

    public Object getObject(int column) {
        return tuple.get(column - 1);
    }

    @SuppressWarnings("unchecked")
    public <T> T getObject(String alias, Class<T> type) {
        Object value = getObject(alias);
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
        if (type == LocalDate.class) {
            return (T) get_local_date(alias);
        }
        if (type == Instant.class) {
            return (T) get_instant(alias);
        }
        return type.cast(value);
    }

    public String getString(String alias) {
        Object value = getObject(alias);
        return value == null ? null : value.toString();
    }

    public String getString(int column) {
        Object value = getObject(column);
        return value == null ? null : value.toString();
    }

    public long getLong(String alias) {
        return to_number(getObject(alias)).longValue();
    }

    public long getLong(int column) {
        return to_number(getObject(column)).longValue();
    }

    public int getInt(String alias) {
        return to_number(getObject(alias)).intValue();
    }

    public int getInt(int column) {
        return to_number(getObject(column)).intValue();
    }

    public short getShort(String alias) {
        return to_number(getObject(alias)).shortValue();
    }

    public boolean getBoolean(String alias) {
        Object value = getObject(alias);
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
    }

    public BigDecimal getBigDecimal(String alias) {
        Object value = getObject(alias);
        return value == null ? null : value instanceof BigDecimal decimal
                ? decimal : new BigDecimal(value.toString());
    }

    public Instant get_instant(String alias) {
        return to_instant(getObject(alias));
    }

    public LocalDate get_local_date(String alias) {
        return to_local_date(getObject(alias));
    }

    private Instant to_instant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offset_date_time) {
            return offset_date_time.toInstant();
        }
        if (value instanceof ZonedDateTime zoned_date_time) {
            return zoned_date_time.toInstant();
        }
        if (value instanceof LocalDateTime local_date_time) {
            return local_date_time.toInstant(ZoneOffset.UTC);
        }
        if (value instanceof java.util.Date date) {
            return Instant.ofEpochMilli(date.getTime());
        }
        String text = value.toString().trim();
        try {
            return Instant.parse(text);
        } catch (RuntimeException ignored) {
            try {
                return OffsetDateTime.parse(text).toInstant();
            } catch (RuntimeException ignored_again) {
                return LocalDateTime.parse(text.replace(' ', 'T'), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .toInstant(ZoneOffset.UTC);
            }
        }
    }

    private LocalDate to_local_date(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate local_date) {
            return local_date;
        }
        if (value instanceof LocalDateTime local_date_time) {
            return local_date_time.toLocalDate();
        }
        if (value instanceof OffsetDateTime offset_date_time) {
            return offset_date_time.toLocalDate();
        }
        if (value instanceof ZonedDateTime zoned_date_time) {
            return zoned_date_time.toLocalDate();
        }
        if (value instanceof java.util.Date date) {
            return Instant.ofEpochMilli(date.getTime()).atZone(ZoneOffset.UTC).toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }

    private Number to_number(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number;
        }
        return new BigDecimal(value.toString());
    }
}