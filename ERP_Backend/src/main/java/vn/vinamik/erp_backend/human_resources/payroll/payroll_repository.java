package vn.vinamik.erp_backend.human_resources.payroll;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.pagination_guard;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public class payroll_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public payroll_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public long count_periods(String status) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.payroll_period WHERE (CAST(? AS text) IS NULL OR status = ?)",
                Long.class, status, status);
        return total == null ? 0 : total;
    }

    public List<payroll_period_response> search_periods(String status, int page, int page_size) {
        return jpa_query_executor.query(period_select()
                        + " WHERE (CAST(? AS text) IS NULL OR period.status = ?) "
                        + "ORDER BY period.starts_on DESC LIMIT ? OFFSET ?",
                this::map_period, status, status, page_size, pagination_guard.offset(page, page_size));
    }

    public payroll_period_response find_period(long payroll_period_id) {
        payroll_period_response result = jpa_query_executor.queryForObject(
                period_select() + " WHERE period.payroll_period_id = ?",
                this::map_period, payroll_period_id);
        if (result == null) {
            throw new resource_not_found_exception("Payroll period");
        }
        return result;
    }

    public void lock_period(long payroll_period_id) {
        Long result = jpa_query_executor.queryForObject(
                "SELECT payroll_period_id FROM hr.payroll_period WHERE payroll_period_id = ? FOR UPDATE",
                Long.class, payroll_period_id);
        if (result == null) {
            throw new resource_not_found_exception("Payroll period");
        }
    }

    public Long insert_period(String period_code, LocalDate starts_on, LocalDate ends_on,
                              BigDecimal standard_working_days, String calculation_version,
                              long actor_user_id) {
        return jpa_query_executor.queryForObject(
                "INSERT INTO hr.payroll_period "
                        + "(period_code, starts_on, ends_on, standard_working_days, calculation_version, status, created_by_user_id, updated_by_user_id) "
                        + "VALUES (?, ?, ?, ?, ?, 'draft', ?, ?) RETURNING payroll_period_id",
                Long.class, period_code, starts_on, ends_on, standard_working_days,
                calculation_version, actor_user_id, actor_user_id);
    }

    public boolean period_exists(LocalDate starts_on, LocalDate ends_on) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.payroll_period WHERE starts_on = ? AND ends_on = ?",
                Long.class, starts_on, ends_on);
        return total != null && total > 0;
    }

    public long ambiguous_contract_count(LocalDate starts_on, LocalDate ends_on) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM ("
                        + "SELECT contract.employee_id FROM hr.employment_contract AS contract "
                        + "JOIN hr.employee AS employee ON employee.employee_id = contract.employee_id "
                        + "WHERE contract.status = 'active' AND contract.effective_from <= ? "
                        + "AND (contract.effective_to IS NULL OR contract.effective_to >= ?) "
                        + "AND employee.employment_status <> 'inactive' "
                        + "AND (employee.hired_on IS NULL OR employee.hired_on <= ?) "
                        + "AND (employee.terminated_on IS NULL OR employee.terminated_on >= ?) "
                        + "GROUP BY contract.employee_id HAVING count(*) > 1) AS ambiguous",
                Long.class, ends_on, starts_on, starts_on, ends_on);
        return total == null ? 0 : total;
    }

    public long partial_contract_count(LocalDate starts_on, LocalDate ends_on) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(DISTINCT overlap.employee_id) FROM hr.employment_contract AS overlap "
                        + "JOIN hr.employee AS employee ON employee.employee_id = overlap.employee_id "
                        + "WHERE overlap.status = 'active' AND overlap.effective_from <= ? "
                        + "AND (overlap.effective_to IS NULL OR overlap.effective_to >= ?) "
                        + "AND employee.employment_status <> 'inactive' "
                        + "AND (employee.hired_on IS NULL OR employee.hired_on <= ?) "
                        + "AND (employee.terminated_on IS NULL OR employee.terminated_on >= ?) "
                        + "AND NOT EXISTS (SELECT 1 FROM hr.employment_contract AS covering "
                        + "WHERE covering.employee_id = overlap.employee_id AND covering.status = 'active' "
                        + "AND covering.effective_from <= ? AND (covering.effective_to IS NULL OR covering.effective_to >= ?))",
                Long.class, ends_on, starts_on, starts_on, ends_on, starts_on, ends_on);
        return total == null ? 0 : total;
    }

    public long candidate_count(LocalDate starts_on, LocalDate ends_on) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.employment_contract AS contract "
                        + "JOIN hr.employee AS employee ON employee.employee_id = contract.employee_id "
                        + "WHERE contract.status = 'active' AND contract.effective_from <= ? "
                        + "AND (contract.effective_to IS NULL OR contract.effective_to >= ?) "
                        + "AND employee.employment_status <> 'inactive' "
                        + "AND (employee.hired_on IS NULL OR employee.hired_on <= ?) "
                        + "AND (employee.terminated_on IS NULL OR employee.terminated_on >= ?)",
                Long.class, starts_on, ends_on, starts_on, ends_on);
        return total == null ? 0 : total;
    }

    public long candidate_currency_count(LocalDate starts_on, LocalDate ends_on) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(DISTINCT contract.currency_code) FROM hr.employment_contract AS contract "
                        + "JOIN hr.employee AS employee ON employee.employee_id = contract.employee_id "
                        + "WHERE contract.status = 'active' AND contract.effective_from <= ? "
                        + "AND (contract.effective_to IS NULL OR contract.effective_to >= ?) "
                        + "AND employee.employment_status <> 'inactive' "
                        + "AND (employee.hired_on IS NULL OR employee.hired_on <= ?) "
                        + "AND (employee.terminated_on IS NULL OR employee.terminated_on >= ?)",
                Long.class, starts_on, ends_on, starts_on, ends_on);
        return total == null ? 0 : total;
    }

    public long currency_mismatch_count(LocalDate starts_on, LocalDate ends_on) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.employee_reward_discipline AS event "
                        + "JOIN hr.employment_contract AS contract ON contract.employee_id = event.employee_id "
                        + "AND contract.status = 'active' AND contract.effective_from <= ? "
                        + "AND (contract.effective_to IS NULL OR contract.effective_to >= ?) "
                        + "JOIN hr.employee AS employee ON employee.employee_id = contract.employee_id "
                        + "WHERE event.status = 'approved' AND event.amount IS NOT NULL AND event.amount > 0 "
                        + "AND event.effective_on BETWEEN ? AND ? AND event.currency_code <> contract.currency_code "
                        + "AND employee.employment_status <> 'inactive' "
                        + "AND (employee.hired_on IS NULL OR employee.hired_on <= ?) "
                        + "AND (employee.terminated_on IS NULL OR employee.terminated_on >= ?)",
                Long.class, starts_on, ends_on, starts_on, ends_on, starts_on, ends_on);
        return total == null ? 0 : total;
    }

    public void delete_calculation(long payroll_period_id) {
        jpa_query_executor.update(
                "DELETE FROM hr.payroll_line WHERE payroll_record_id IN "
                        + "(SELECT payroll_record_id FROM hr.payroll_record WHERE payroll_period_id = ?)",
                payroll_period_id);
        jpa_query_executor.update(
                "DELETE FROM hr.payroll_record WHERE payroll_period_id = ?", payroll_period_id);
    }

    public int insert_records(long payroll_period_id, LocalDate starts_on, LocalDate ends_on,
                              BigDecimal standard_working_days, String calculation_version,
                              long actor_user_id) {
        return jpa_query_executor.update(
                "INSERT INTO hr.payroll_record "
                        + "(payroll_period_id, employee_id, employment_contract_id, base_salary_snapshot, "
                        + "currency_code_snapshot, standard_working_days_snapshot, calculation_version, status, "
                        + "created_by_user_id, updated_by_user_id) "
                        + "SELECT ?, contract.employee_id, contract.employment_contract_id, contract.base_salary, "
                        + "contract.currency_code, ?, ?, 'draft', ?, ? "
                        + "FROM hr.employment_contract AS contract "
                        + "JOIN hr.employee AS employee ON employee.employee_id = contract.employee_id "
                        + "WHERE contract.status = 'active' AND contract.effective_from <= ? "
                        + "AND (contract.effective_to IS NULL OR contract.effective_to >= ?) "
                        + "AND employee.employment_status <> 'inactive' "
                        + "AND (employee.hired_on IS NULL OR employee.hired_on <= ?) "
                        + "AND (employee.terminated_on IS NULL OR employee.terminated_on >= ?)",
                payroll_period_id, standard_working_days, calculation_version,
                actor_user_id, actor_user_id, starts_on, ends_on, starts_on, ends_on);
    }

    public int insert_base_salary_lines(long payroll_period_id) {
        return jpa_query_executor.update(
                "INSERT INTO hr.payroll_line "
                        + "(payroll_record_id, line_type, direction, line_label, quantity, unit_amount, amount, source_type, source_id) "
                        + "SELECT payroll_record_id, 'base_salary', 'earning', 'Base salary', 1, "
                        + "base_salary_snapshot, base_salary_snapshot, 'employment_contract', employment_contract_id "
                        + "FROM hr.payroll_record WHERE payroll_period_id = ?",
                payroll_period_id);
    }

    public int insert_unpaid_leave_lines(long payroll_period_id, LocalDate starts_on, LocalDate ends_on) {
        return jpa_query_executor.update(
                "INSERT INTO hr.payroll_line "
                        + "(payroll_record_id, line_type, direction, line_label, quantity, unit_amount, amount, source_type, source_id) "
                        + "SELECT payroll_record.payroll_record_id, 'unpaid_leave', 'deduction', "
                        + "'Unpaid leave: ' || leave_request.request_code, workdays.day_count, "
                        + "round(payroll_record.base_salary_snapshot / payroll_record.standard_working_days_snapshot, 6), "
                        + "round(round(payroll_record.base_salary_snapshot / payroll_record.standard_working_days_snapshot, 6) * workdays.day_count, 2), "
                        + "'leave_request', leave_request.leave_request_id "
                        + "FROM hr.payroll_record AS payroll_record "
                        + "JOIN hr.leave_request AS leave_request ON leave_request.employee_id = payroll_record.employee_id "
                        + "JOIN LATERAL (SELECT count(*)::numeric(18, 6) AS day_count "
                        + "FROM generate_series(GREATEST(leave_request.starts_on, CAST(? AS date)), "
                        + "LEAST(leave_request.ends_on, CAST(? AS date)), interval '1 day') AS workday(day_value) "
                        + "WHERE extract(isodow FROM workday.day_value) BETWEEN 1 AND 6) AS workdays ON workdays.day_count > 0 "
                        + "WHERE payroll_record.payroll_period_id = ? AND leave_request.status = 'approved' "
                        + "AND leave_request.is_paid = false AND leave_request.starts_on <= ? AND leave_request.ends_on >= ?",
                starts_on, ends_on, payroll_period_id, ends_on, starts_on);
    }

    public int insert_reward_discipline_lines(long payroll_period_id, LocalDate starts_on, LocalDate ends_on) {
        return jpa_query_executor.update(
                "INSERT INTO hr.payroll_line "
                        + "(payroll_record_id, line_type, direction, line_label, amount, source_type, source_id) "
                        + "SELECT payroll_record.payroll_record_id, event.event_type, "
                        + "CASE WHEN event.event_type = 'reward' THEN 'earning' ELSE 'deduction' END, "
                        + "CASE WHEN event.event_type = 'reward' THEN 'Reward: ' ELSE 'Discipline: ' END || event.record_code, "
                        + "event.amount, 'employee_reward_discipline', event.employee_reward_discipline_id "
                        + "FROM hr.payroll_record AS payroll_record "
                        + "JOIN hr.employee_reward_discipline AS event ON event.employee_id = payroll_record.employee_id "
                        + "WHERE payroll_record.payroll_period_id = ? AND event.status = 'approved' "
                        + "AND event.amount IS NOT NULL AND event.amount > 0 AND event.effective_on BETWEEN ? AND ? "
                        + "AND event.currency_code = payroll_record.currency_code_snapshot",
                payroll_period_id, starts_on, ends_on);
    }

    public int finalize_records(long payroll_period_id, long actor_user_id) {
        return jpa_query_executor.update(
                "WITH totals AS (SELECT payroll_record_id, "
                        + "COALESCE(sum(quantity) FILTER (WHERE line_type = 'unpaid_leave'), 0) AS unpaid_days, "
                        + "COALESCE(sum(amount) FILTER (WHERE line_type = 'unpaid_leave'), 0) AS unpaid_amount, "
                        + "COALESCE(sum(amount) FILTER (WHERE line_type = 'reward'), 0) AS reward_amount, "
                        + "COALESCE(sum(amount) FILTER (WHERE line_type = 'discipline'), 0) AS discipline_amount "
                        + "FROM hr.payroll_line GROUP BY payroll_record_id) "
                        + "UPDATE hr.payroll_record AS payroll_record SET "
                        + "unpaid_leave_days = totals.unpaid_days, unpaid_leave_amount = totals.unpaid_amount, "
                        + "reward_amount = totals.reward_amount, discipline_amount = totals.discipline_amount, "
                        + "gross_amount = payroll_record.base_salary_snapshot + totals.reward_amount, "
                        + "deduction_amount = totals.unpaid_amount + totals.discipline_amount, "
                        + "net_amount = payroll_record.base_salary_snapshot + totals.reward_amount - totals.unpaid_amount - totals.discipline_amount, "
                        + "status = 'calculated', calculated_at = now(), updated_at = now(), updated_by_user_id = ? "
                        + "FROM totals WHERE payroll_record.payroll_record_id = totals.payroll_record_id "
                        + "AND payroll_record.payroll_period_id = ?",
                actor_user_id, payroll_period_id);
    }

    public int mark_period_calculated(long payroll_period_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE hr.payroll_period SET status = 'calculated', calculated_at = now(), "
                        + "approved_by_user_id = NULL, approved_at = NULL, locked_at = NULL, decision_note = NULL, "
                        + "updated_at = now(), updated_by_user_id = ? WHERE payroll_period_id = ? "
                        + "AND status IN ('draft', 'calculated', 'rejected')",
                actor_user_id, payroll_period_id);
    }

    public int mark_records_status(long payroll_period_id, String status, long actor_user_id) {
        String approval_fields = "approved".equals(status)
                ? ", approved_by_user_id = ?, approved_at = now()"
                : "locked".equals(status) ? ", locked_at = now()" : "";
        if ("approved".equals(status)) {
            return jpa_query_executor.update(
                    "UPDATE hr.payroll_record SET status = ?" + approval_fields
                            + ", updated_at = now(), updated_by_user_id = ? WHERE payroll_period_id = ?",
                    status, actor_user_id, actor_user_id, payroll_period_id);
        }
        return jpa_query_executor.update(
                "UPDATE hr.payroll_record SET status = ?" + approval_fields
                        + ", updated_at = now(), updated_by_user_id = ? WHERE payroll_period_id = ?",
                status, actor_user_id, payroll_period_id);
    }

    public int approve_period(long payroll_period_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE hr.payroll_period SET status = 'approved', approved_by_user_id = ?, approved_at = now(), "
                        + "decision_note = NULL, updated_at = now(), updated_by_user_id = ? "
                        + "WHERE payroll_period_id = ? AND status = 'calculated'",
                actor_user_id, actor_user_id, payroll_period_id);
    }

    public int reject_period(long payroll_period_id, String decision_note, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE hr.payroll_period SET status = 'rejected', approved_by_user_id = ?, approved_at = now(), "
                        + "decision_note = ?, updated_at = now(), updated_by_user_id = ? "
                        + "WHERE payroll_period_id = ? AND status = 'calculated'",
                actor_user_id, decision_note, actor_user_id, payroll_period_id);
    }

    public int lock_period_status(long payroll_period_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE hr.payroll_period SET status = 'locked', locked_at = now(), updated_at = now(), "
                        + "updated_by_user_id = ? WHERE payroll_period_id = ? AND status = 'approved'",
                actor_user_id, payroll_period_id);
    }

    public long count_records(long payroll_period_id, String search) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) " + record_from_where(), Long.class,
                payroll_period_id, search, search, search, search);
        return total == null ? 0 : total;
    }

    public List<payroll_record_response> search_records(long payroll_period_id, String search,
                                                        int page, int page_size) {
        return jpa_query_executor.query(record_select() + record_from_where()
                        + " ORDER BY employee.employee_code LIMIT ? OFFSET ?",
                this::map_record, payroll_period_id, search, search, search, search,
                page_size, pagination_guard.offset(page, page_size));
    }

    public payroll_record_response find_record(long payroll_record_id) {
        payroll_record_response result = jpa_query_executor.queryForObject(
                record_select() + " FROM hr.payroll_record AS payroll_record "
                        + "JOIN hr.employee AS employee ON employee.employee_id = payroll_record.employee_id "
                        + "JOIN hr.employment_contract AS contract ON contract.employment_contract_id = payroll_record.employment_contract_id "
                        + "WHERE payroll_record.payroll_record_id = ?",
                this::map_record, payroll_record_id);
        if (result == null) {
            throw new resource_not_found_exception("Payroll record");
        }
        return result;
    }

    public List<payroll_line_response> find_lines(long payroll_record_id) {
        return jpa_query_executor.query(
                "SELECT payroll_line_id, line_type, direction, line_label, quantity, unit_amount, amount, source_type, source_id "
                        + "FROM hr.payroll_line WHERE payroll_record_id = ? ORDER BY payroll_line_id",
                this::map_line, payroll_record_id);
    }

    private String period_select() {
        return "SELECT period.payroll_period_id, period.period_code, period.starts_on, period.ends_on, "
                + "period.standard_working_days, period.calculation_version, period.status, "
                + "(SELECT count(*) FROM hr.payroll_record AS payroll_record WHERE payroll_record.payroll_period_id = period.payroll_period_id) AS record_count, "
                + "(SELECT COALESCE(sum(net_amount), 0) FROM hr.payroll_record AS payroll_record WHERE payroll_record.payroll_period_id = period.payroll_period_id) AS total_net_amount, "
                + "(SELECT min(currency_code_snapshot) FROM hr.payroll_record AS payroll_record WHERE payroll_record.payroll_period_id = period.payroll_period_id) AS currency_code, "
                + "period.approved_by_user_id, period.calculated_at, period.approved_at, period.locked_at, period.decision_note "
                + "FROM hr.payroll_period AS period";
    }

    private String record_select() {
        return "SELECT payroll_record.payroll_record_id, payroll_record.payroll_period_id, payroll_record.employee_id, "
                + "employee.employee_code, employee.full_name, payroll_record.employment_contract_id, contract.contract_code, "
                + "payroll_record.base_salary_snapshot, payroll_record.currency_code_snapshot, payroll_record.standard_working_days_snapshot, "
                + "payroll_record.unpaid_leave_days, payroll_record.unpaid_leave_amount, payroll_record.reward_amount, "
                + "payroll_record.discipline_amount, payroll_record.gross_amount, payroll_record.deduction_amount, payroll_record.net_amount, "
                + "payroll_record.calculation_version, payroll_record.status, payroll_record.calculated_at, "
                + "payroll_record.approved_by_user_id, payroll_record.approved_at, payroll_record.locked_at ";
    }

    private String record_from_where() {
        return "FROM hr.payroll_record AS payroll_record "
                + "JOIN hr.employee AS employee ON employee.employee_id = payroll_record.employee_id "
                + "JOIN hr.employment_contract AS contract ON contract.employment_contract_id = payroll_record.employment_contract_id "
                + "WHERE payroll_record.payroll_period_id = ? "
                + "AND (CAST(? AS text) IS NULL OR lower(employee.employee_code) LIKE '%' || ? || '%' "
                + "OR lower(employee.full_name) LIKE '%' || ? || '%' OR lower(contract.contract_code) LIKE '%' || ? || '%')";
    }

    private payroll_period_response map_period(jpa_result_row row, int row_number) {
        return new payroll_period_response(row.getLong("payroll_period_id"), row.getString("period_code"),
                row.get_local_date("starts_on"), row.get_local_date("ends_on"),
                row.getBigDecimal("standard_working_days"), row.getString("calculation_version"),
                row.getString("status"), row.getLong("record_count"), row.getBigDecimal("total_net_amount"),
                row.getString("currency_code"), row.getObject("approved_by_user_id", Long.class), row.get_instant("calculated_at"),
                row.get_instant("approved_at"), row.get_instant("locked_at"), row.getString("decision_note"));
    }

    private payroll_record_response map_record(jpa_result_row row, int row_number) {
        return new payroll_record_response(row.getLong("payroll_record_id"), row.getLong("payroll_period_id"),
                row.getLong("employee_id"), row.getString("employee_code"), row.getString("full_name"),
                row.getLong("employment_contract_id"), row.getString("contract_code"),
                row.getBigDecimal("base_salary_snapshot"), row.getString("currency_code_snapshot"),
                row.getBigDecimal("standard_working_days_snapshot"), row.getBigDecimal("unpaid_leave_days"),
                row.getBigDecimal("unpaid_leave_amount"), row.getBigDecimal("reward_amount"),
                row.getBigDecimal("discipline_amount"), row.getBigDecimal("gross_amount"),
                row.getBigDecimal("deduction_amount"), row.getBigDecimal("net_amount"),
                row.getString("calculation_version"), row.getString("status"), row.get_instant("calculated_at"),
                row.getObject("approved_by_user_id", Long.class), row.get_instant("approved_at"),
                row.get_instant("locked_at"));
    }

    private payroll_line_response map_line(jpa_result_row row, int row_number) {
        return new payroll_line_response(row.getLong("payroll_line_id"), row.getString("line_type"),
                row.getString("direction"), row.getString("line_label"), row.getBigDecimal("quantity"),
                row.getBigDecimal("unit_amount"), row.getBigDecimal("amount"), row.getString("source_type"),
                row.getObject("source_id", Long.class));
    }
}
