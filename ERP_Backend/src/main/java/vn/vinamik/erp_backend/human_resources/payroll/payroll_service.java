package vn.vinamik.erp_backend.human_resources.payroll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import vn.vinamik.erp_backend.platform.common.audit_event_writer;
import vn.vinamik.erp_backend.platform.common.field_conflict_exception;
import vn.vinamik.erp_backend.platform.common.master_data_page_response;
import vn.vinamik.erp_backend.platform.common.pagination_guard;
import vn.vinamik.erp_backend.platform.identity.authenticated_user;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class payroll_service {
    private static final Logger logger = LoggerFactory.getLogger(payroll_service.class);
    private static final String calculation_version = "monthly_mon_sat_v1";
    private static final int max_page_size = 100;
    private static final Set<String> valid_statuses = Set.of(
            "draft", "calculated", "approved", "rejected", "locked");
    private static final Set<String> recalculable_statuses = Set.of("draft", "calculated", "rejected");

    private final payroll_repository payroll_repository;
    private final audit_event_writer audit_writer;

    public payroll_service(payroll_repository payroll_repository, audit_event_writer audit_writer) {
        this.payroll_repository = payroll_repository;
        this.audit_writer = audit_writer;
    }

    @Transactional(readOnly = true)
    public master_data_page_response<payroll_period_response> search_periods(
            String status, int page, int page_size) {
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = Math.min(Math.max(page_size, 1), max_page_size);
        String normalized_status = normalize_optional_lower(status);
        if (normalized_status != null && !valid_statuses.contains(normalized_status)) {
            throw new IllegalArgumentException("Payroll period status is invalid.");
        }
        long total = payroll_repository.count_periods(normalized_status);
        return page_response(payroll_repository.search_periods(normalized_status, safe_page, safe_page_size),
                safe_page, safe_page_size, total);
    }

    @Transactional(readOnly = true)
    public payroll_period_response find_period(long payroll_period_id) {
        return payroll_repository.find_period(payroll_period_id);
    }

    @Transactional
    public payroll_period_response create_period(payroll_period_request request, authenticated_user actor,
                                                 String correlation_id) {
        if (request == null || request.year() == null || request.month() == null) {
            throw new IllegalArgumentException("Payroll year and month are required.");
        }
        YearMonth month = YearMonth.of(request.year(), request.month());
        LocalDate starts_on = month.atDay(1);
        LocalDate ends_on = month.atEndOfMonth();
        if (payroll_repository.period_exists(starts_on, ends_on)) {
            throw new field_conflict_exception("month", "Payroll period already exists.");
        }
        BigDecimal standard_working_days = BigDecimal.valueOf(count_standard_working_days(starts_on, ends_on));
        String period_code = "payroll_" + month.toString().replace('-', '_');
        try {
            Long payroll_period_id = payroll_repository.insert_period(period_code, starts_on, ends_on,
                    standard_working_days, calculation_version, actor.user_id());
            if (payroll_period_id == null) {
                throw new IllegalStateException("Payroll period identifier was not returned.");
            }
            payroll_period_response created = payroll_repository.find_period(payroll_period_id);
            audit_writer.write(actor.user_id(), "hr", "payroll_period_create", "payroll_period",
                    String.valueOf(payroll_period_id), correlation_id,
                    Map.of("period_code", period_code, "calculation_version", calculation_version));
            logger.info("Đã tạo kỳ lương; payroll_period_id={}, period_code={}, actor_user_id={}, correlation_id={}",
                    payroll_period_id, period_code, actor.user_id(), correlation_id);
            return created;
        } catch (DataIntegrityViolationException exception) {
            throw new field_conflict_exception("month", "Payroll period already exists.");
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public payroll_period_response calculate(long payroll_period_id, authenticated_user actor,
                                             String correlation_id) {
        payroll_repository.lock_period(payroll_period_id);
        payroll_period_response period = payroll_repository.find_period(payroll_period_id);
        if (!recalculable_statuses.contains(period.status())) {
            throw new IllegalArgumentException("Only draft, rejected or calculated payroll periods can be calculated.");
        }
        validate_calculation_inputs(period);
        long candidate_count = payroll_repository.candidate_count(period.starts_on(), period.ends_on());
        if (candidate_count == 0) {
            throw new IllegalArgumentException("No eligible employee contract covers the payroll period.");
        }

        payroll_repository.delete_calculation(payroll_period_id);
        int inserted_records = payroll_repository.insert_records(payroll_period_id, period.starts_on(), period.ends_on(),
                period.standard_working_days(), calculation_version, actor.user_id());
        if (inserted_records != candidate_count) {
            throw new IllegalStateException("Payroll candidate set changed during calculation.");
        }
        payroll_repository.insert_base_salary_lines(payroll_period_id);
        payroll_repository.insert_unpaid_leave_lines(payroll_period_id, period.starts_on(), period.ends_on());
        payroll_repository.insert_reward_discipline_lines(payroll_period_id, period.starts_on(), period.ends_on());
        int finalized_records = payroll_repository.finalize_records(payroll_period_id, actor.user_id());
        if (finalized_records != inserted_records) {
            throw new IllegalStateException("Payroll records could not be finalized consistently.");
        }
        if (payroll_repository.mark_period_calculated(payroll_period_id, actor.user_id()) == 0) {
            throw new IllegalStateException("Payroll period status changed during calculation.");
        }

        payroll_period_response calculated = payroll_repository.find_period(payroll_period_id);
        audit_writer.write(actor.user_id(), "hr", "payroll_period_calculate", "payroll_period",
                String.valueOf(payroll_period_id), correlation_id,
                Map.of("record_count", inserted_records, "calculation_version", calculation_version));
        logger.info("Đã tính bảng lương; payroll_period_id={}, record_count={}, actor_user_id={}, correlation_id={}",
                payroll_period_id, inserted_records, actor.user_id(), correlation_id);
        return calculated;
    }

    private void validate_calculation_inputs(payroll_period_response period) {
        if (payroll_repository.ambiguous_contract_count(period.starts_on(), period.ends_on()) > 0) {
            throw new IllegalArgumentException("An employee has multiple active contracts overlapping the payroll period.");
        }
        if (payroll_repository.partial_contract_count(period.starts_on(), period.ends_on()) > 0) {
            throw new IllegalArgumentException("Active contracts must cover the entire payroll period.");
        }
        if (payroll_repository.candidate_currency_count(period.starts_on(), period.ends_on()) > 1) {
            throw new IllegalArgumentException("All payroll contracts must use the same currency within a payroll period.");
        }
        if (payroll_repository.currency_mismatch_count(period.starts_on(), period.ends_on()) > 0) {
            throw new IllegalArgumentException(
                    "Approved reward or discipline currency must match the employment contract currency.");
        }
    }

    @Transactional
    public payroll_period_response approve(long payroll_period_id, authenticated_user actor,
                                           String correlation_id) {
        payroll_repository.lock_period(payroll_period_id);
        payroll_period_response period = payroll_repository.find_period(payroll_period_id);
        if ("approved".equals(period.status())) {
            return period;
        }
        require_status(period, "calculated", "Only calculated payroll periods can be approved.");
        payroll_repository.mark_records_status(payroll_period_id, "approved", actor.user_id());
        if (payroll_repository.approve_period(payroll_period_id, actor.user_id()) == 0) {
            throw new IllegalStateException("Payroll period status changed during approval.");
        }
        audit_writer.write(actor.user_id(), "hr", "payroll_period_approve", "payroll_period",
                String.valueOf(payroll_period_id), correlation_id, Map.of());
        logger.info("Đã duyệt kỳ lương; payroll_period_id={}, actor_user_id={}, correlation_id={}",
                payroll_period_id, actor.user_id(), correlation_id);
        return payroll_repository.find_period(payroll_period_id);
    }

    @Transactional
    public payroll_period_response reject(long payroll_period_id, payroll_decision_request request,
                                          authenticated_user actor, String correlation_id) {
        String note = normalize_optional(request == null ? null : request.decision_note());
        if (note == null) {
            throw new IllegalArgumentException("Decision note is required when rejecting a payroll period.");
        }
        payroll_repository.lock_period(payroll_period_id);
        payroll_period_response period = payroll_repository.find_period(payroll_period_id);
        if ("rejected".equals(period.status())) {
            return period;
        }
        require_status(period, "calculated", "Only calculated payroll periods can be rejected.");
        payroll_repository.mark_records_status(payroll_period_id, "rejected", actor.user_id());
        if (payroll_repository.reject_period(payroll_period_id, note, actor.user_id()) == 0) {
            throw new IllegalStateException("Payroll period status changed during rejection.");
        }
        audit_writer.write(actor.user_id(), "hr", "payroll_period_reject", "payroll_period",
                String.valueOf(payroll_period_id), correlation_id, Map.of("decision_note", note));
        logger.info("Đã từ chối kỳ lương; payroll_period_id={}, actor_user_id={}, correlation_id={}",
                payroll_period_id, actor.user_id(), correlation_id);
        return payroll_repository.find_period(payroll_period_id);
    }

    @Transactional
    public payroll_period_response lock(long payroll_period_id, authenticated_user actor,
                                       String correlation_id) {
        payroll_repository.lock_period(payroll_period_id);
        payroll_period_response period = payroll_repository.find_period(payroll_period_id);
        if ("locked".equals(period.status())) {
            return period;
        }
        require_status(period, "approved", "Only approved payroll periods can be locked.");
        payroll_repository.mark_records_status(payroll_period_id, "locked", actor.user_id());
        if (payroll_repository.lock_period_status(payroll_period_id, actor.user_id()) == 0) {
            throw new IllegalStateException("Payroll period status changed during locking.");
        }
        audit_writer.write(actor.user_id(), "hr", "payroll_period_lock", "payroll_period",
                String.valueOf(payroll_period_id), correlation_id, Map.of());
        logger.info("Đã khóa kỳ lương; payroll_period_id={}, actor_user_id={}, correlation_id={}",
                payroll_period_id, actor.user_id(), correlation_id);
        return payroll_repository.find_period(payroll_period_id);
    }

    @Transactional(readOnly = true)
    public master_data_page_response<payroll_record_response> search_records(
            long payroll_period_id, String search, int page, int page_size) {
        payroll_repository.find_period(payroll_period_id);
        int safe_page = pagination_guard.normalize_page(page);
        int safe_page_size = Math.min(Math.max(page_size, 1), max_page_size);
        String normalized_search = normalize_optional_lower(search);
        long total = payroll_repository.count_records(payroll_period_id, normalized_search);
        return page_response(payroll_repository.search_records(
                        payroll_period_id, normalized_search, safe_page, safe_page_size),
                safe_page, safe_page_size, total);
    }

    @Transactional(readOnly = true)
    public payroll_record_detail_response find_record(long payroll_record_id) {
        payroll_record_response record = payroll_repository.find_record(payroll_record_id);
        return new payroll_record_detail_response(record, payroll_repository.find_lines(payroll_record_id));
    }

    private void require_status(payroll_period_response period, String expected, String message) {
        if (!expected.equals(period.status())) {
            throw new IllegalArgumentException(message);
        }
    }

    int count_standard_working_days(LocalDate starts_on, LocalDate ends_on) {
        int count = 0;
        for (LocalDate date = starts_on; !date.isAfter(ends_on); date = date.plusDays(1)) {
            if (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return count;
    }

    private <T> master_data_page_response<T> page_response(
            List<T> items, int page, int page_size, long total) {
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / page_size);
        return new master_data_page_response<>(items, page, page_size, total, total_pages);
    }

    private String normalize_optional_lower(String value) {
        String normalized = normalize_optional(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalize_optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
