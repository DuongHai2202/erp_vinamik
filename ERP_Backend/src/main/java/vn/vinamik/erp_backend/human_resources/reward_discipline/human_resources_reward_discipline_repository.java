package vn.vinamik.erp_backend.human_resources.reward_discipline;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;



import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Repository
public class human_resources_reward_discipline_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public human_resources_reward_discipline_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public long count(String search, Long employee_id, String event_type, String status,
                      LocalDate from_date, LocalDate to_date) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) " + common_where(), Long.class,
                search, search, search, search,
                employee_id, employee_id, event_type, event_type, status, status,
                from_date, from_date, to_date, to_date);
        return total == null ? 0 : total;
    }

    public List<reward_discipline_response> search(String search, Long employee_id, String event_type, String status,
                                                    LocalDate from_date, LocalDate to_date,
                                                    int page_size, int offset) {
        return jpa_query_executor.query(
                "SELECT record.employee_reward_discipline_id, record.record_code, record.employee_id, employee.employee_code, employee.full_name, "
                        + "record.event_type, record.effective_on, record.reason, record.amount, record.currency_code, record.status, "
                        + "record.approver_user_id, record.decided_at, record.decision_note "
                        + common_where()
                        + " ORDER BY record.effective_on DESC, record.record_code, record.employee_reward_discipline_id LIMIT ? OFFSET ?",
                this::map_record,
                search, search, search, search,
                employee_id, employee_id, event_type, event_type, status, status,
                from_date, from_date, to_date, to_date, page_size, offset);
    }

    public reward_discipline_response find_by_id(long record_id) {
        List<reward_discipline_response> records = jpa_query_executor.query(
                "SELECT record.employee_reward_discipline_id, record.record_code, record.employee_id, employee.employee_code, employee.full_name, "
                        + "record.event_type, record.effective_on, record.reason, record.amount, record.currency_code, record.status, "
                        + "record.approver_user_id, record.decided_at, record.decision_note "
                        + "FROM hr.employee_reward_discipline AS record "
                        + "JOIN hr.employee AS employee ON employee.employee_id = record.employee_id "
                        + "WHERE record.employee_reward_discipline_id = ?",
                this::map_record, record_id);
        if (records.isEmpty()) {
            throw new resource_not_found_exception("Reward or discipline record");
        }
        return records.getFirst();
    }

    public long insert(String record_code, long employee_id, String event_type, LocalDate effective_on,
                       String reason, java.math.BigDecimal amount, String currency_code, String status,
                       long actor_user_id) {
        Long record_id = jpa_query_executor.queryForObject(
                "INSERT INTO hr.employee_reward_discipline (record_code, employee_id, event_type, effective_on, reason, amount, currency_code, status, created_by_user_id, updated_by_user_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING employee_reward_discipline_id",
                Long.class, record_code, employee_id, event_type, effective_on, reason, amount, currency_code,
                status, actor_user_id, actor_user_id);
        if (record_id == null) {
            throw new IllegalStateException("Reward or discipline record identifier was not returned.");
        }
        return record_id;
    }

    public int update(long record_id, String record_code, long employee_id, String event_type,
                      LocalDate effective_on, String reason, java.math.BigDecimal amount, String currency_code,
                      String status, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE hr.employee_reward_discipline SET record_code = ?, employee_id = ?, event_type = ?, effective_on = ?, reason = ?, amount = ?, currency_code = ?, status = ?, updated_at = now(), updated_by_user_id = ? WHERE employee_reward_discipline_id = ?",
                record_code, employee_id, event_type, effective_on, reason, amount, currency_code, status,
                actor_user_id, record_id);
    }

    public int cancel(long record_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE hr.employee_reward_discipline SET status = 'cancelled', updated_at = now(), updated_by_user_id = ? WHERE employee_reward_discipline_id = ? AND status IN ('draft', 'pending')",
                actor_user_id, record_id);
    }

    public int delete_draft(long record_id) {
        return jpa_query_executor.update(
                "DELETE FROM hr.employee_reward_discipline WHERE employee_reward_discipline_id = ? AND status = 'draft'",
                record_id);
    }
    public int submit(long record_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE hr.employee_reward_discipline SET status = 'pending', updated_at = now(), updated_by_user_id = ? "
                        + "WHERE employee_reward_discipline_id = ? AND status = 'draft'",
                actor_user_id, record_id);
    }

    public int decide(long record_id, String status, String decision_note, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE hr.employee_reward_discipline SET status = ?, approver_user_id = ?, decided_at = now(), "
                        + "decision_note = ?, updated_at = now(), updated_by_user_id = ? "
                        + "WHERE employee_reward_discipline_id = ? AND status = 'pending'",
                status, actor_user_id, decision_note, actor_user_id, record_id);
    }

    public String current_status(long record_id) {
        List<String> statuses = jpa_query_executor.query(
                "SELECT status FROM hr.employee_reward_discipline WHERE employee_reward_discipline_id = ?",
                (result_set, row_number) -> result_set.getString("status"), record_id);
        if (statuses.isEmpty()) {
            throw new resource_not_found_exception("Reward or discipline record");
        }
        return statuses.getFirst();
    }

    public boolean payroll_locked(long record_id) {
        Integer count = jpa_query_executor.queryForObject(
                "SELECT count(*)::integer "
                        + "FROM hr.payroll_line AS line "
                        + "JOIN hr.payroll_record AS payroll_record ON payroll_record.payroll_record_id = line.payroll_record_id "
                        + "JOIN hr.payroll_period AS payroll_period ON payroll_period.payroll_period_id = payroll_record.payroll_period_id "
                        + "WHERE line.source_type = 'employee_reward_discipline' AND line.source_id = ? "
                        + "AND payroll_period.status = 'locked'",
                Integer.class, record_id);
        return count != null && count > 0;
    }

    public boolean employee_exists(long employee_id) {
        Long count = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.employee WHERE employee_id = ?", Long.class, employee_id);
        return count != null && count > 0;
    }

    public boolean record_code_exists(String record_code, Long record_id) {
        Long count = record_id == null
                ? jpa_query_executor.queryForObject(
                        "SELECT count(*) FROM hr.employee_reward_discipline WHERE record_code = ?", Long.class, record_code)
                : jpa_query_executor.queryForObject(
                        "SELECT count(*) FROM hr.employee_reward_discipline "
                                + "WHERE record_code = ? AND employee_reward_discipline_id <> ?",
                        Long.class, record_code, record_id);
        return count != null && count > 0;
    }

    private String common_where() {
        return "FROM hr.employee_reward_discipline AS record "
                + "JOIN hr.employee AS employee ON employee.employee_id = record.employee_id "
                + "WHERE (CAST(? AS text) IS NULL OR lower(record.record_code) LIKE '%' || ? || '%' "
                + "OR lower(employee.employee_code) LIKE '%' || ? || '%' "
                + "OR lower(employee.full_name) LIKE '%' || ? || '%') "
                + "AND (CAST(? AS text) IS NULL OR record.employee_id = ?) "
                + "AND (CAST(? AS text) IS NULL OR record.event_type = ?) "
                + "AND (CAST(? AS text) IS NULL OR record.status = ?) "
                + "AND (CAST(? AS text) IS NULL OR record.effective_on >= ?) "
                + "AND (CAST(? AS text) IS NULL OR record.effective_on <= ?)";
    }

    private reward_discipline_response map_record(jpa_result_row result_set, int row_number) {
        LocalDate effective_on = result_set.get_local_date("effective_on");
        Instant decided_at = result_set.get_instant("decided_at");
        return new reward_discipline_response(
                result_set.getLong("employee_reward_discipline_id"), result_set.getString("record_code"),
                result_set.getLong("employee_id"), result_set.getString("employee_code"),
                result_set.getString("full_name"), result_set.getString("event_type"),
                effective_on == null ? null : effective_on,
                result_set.getString("reason"), result_set.getBigDecimal("amount"),
                result_set.getString("currency_code"), result_set.getString("status"),
                result_set.getObject("approver_user_id", Long.class),
                decided_at == null ? null : decided_at,
                result_set.getString("decision_note"));
    }
}
