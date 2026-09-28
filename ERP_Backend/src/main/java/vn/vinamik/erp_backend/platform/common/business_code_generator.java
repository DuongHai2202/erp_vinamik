package vn.vinamik.erp_backend.platform.common;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Allocates business codes in one transaction-safe place. The counter row is
 * locked by PostgreSQL's INSERT ... ON CONFLICT update, so two API requests
 * cannot receive the same number.
 */
@Service
public class business_code_generator {
    private static final DateTimeFormatter year_formatter = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter month_formatter = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter date_formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final jpa_native_query_executor query_executor;

    public business_code_generator(jpa_native_query_executor query_executor) {
        this.query_executor = query_executor;
    }

    @Transactional
    public String next_yearly(String code_type, String prefix, LocalDate date, int width) {
        LocalDate effective_date = date == null ? LocalDate.now() : date;
        return next(code_type, prefix, effective_date.format(year_formatter), width);
    }

    @Transactional
    public String next_monthly(String code_type, String prefix, YearMonth month, int width) {
        YearMonth effective_month = month == null ? YearMonth.now() : month;
        return next(code_type, prefix, effective_month.format(month_formatter), width);
    }

    @Transactional
    public String next_daily(String code_type, String prefix, LocalDate date, int width) {
        LocalDate effective_date = date == null ? LocalDate.now() : date;
        return next(code_type, prefix, effective_date.format(date_formatter), width);
    }

    @Transactional
    public String next(String code_type, String prefix, String period_key, int width) {
        String normalized_type = normalize_part(code_type, "code_type");
        String normalized_prefix = prefix == null ? "" : prefix.trim().toLowerCase(Locale.ROOT);
        String normalized_period = normalize_part(period_key, "period_key");
        if (width < 1 || width > 12) {
            throw new IllegalArgumentException("Code width must be between 1 and 12.");
        }
        Long allocated = query_executor.queryForObject(
                """
                INSERT INTO identity.business_code_sequence (code_type, period_key, last_value)
                VALUES (?, ?, 1)
                ON CONFLICT (code_type, period_key)
                DO UPDATE SET last_value = identity.business_code_sequence.last_value + 1,
                              updated_at = now()
                RETURNING last_value
                """,
                Long.class, normalized_type, normalized_period);
        if (allocated == null) {
            throw new IllegalStateException("Business code sequence could not be allocated.");
        }
        return normalized_prefix + normalized_period + "_" + String.format(Locale.ROOT, "%0" + width + "d", allocated);
    }

    private String normalize_part(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 80 || !value.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException(field + " contains an invalid value.");
        }
        return value.toLowerCase(Locale.ROOT);
    }
}
