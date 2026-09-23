package vn.vinamik.erp_backend.human_resources.contract;
import vn.vinamik.erp_backend.platform.common.pagination_guard;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;


import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Repository
public class human_resources_contract_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public human_resources_contract_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public employment_contract_page_response search(String search, Long employee_id, String status,
                                                    boolean expiring_only, int page, int page_size) {
        LocalDate today = LocalDate.now();
        String expiration_filter = expiring_only
                ? " AND contract.status = 'active' AND contract.effective_from <= ? AND contract.effective_to IS NOT NULL AND contract.effective_to >= ? AND contract.effective_to <= ?"
                : "";
        String common_where = common_where(expiration_filter);
        Object[] base_parameters = expiring_only
                ? new Object[]{search, search, search, search, employee_id, employee_id, status, status, today, today, today.plusDays(15)}
                : new Object[]{search, search, search, search, employee_id, employee_id, status, status};
        Long total_items = jpa_query_executor.queryForObject(
                "SELECT count(*) " + common_where, Long.class, base_parameters);
        Object[] page_parameters = append(base_parameters, page_size, pagination_guard.offset(page, page_size));
        List<employment_contract_response> items = jpa_query_executor.query(
                "SELECT contract.employment_contract_id, contract.contract_code, contract.employee_id, employee.employee_code, employee.full_name, "
                        + "contract.contract_type, contract.effective_from, contract.effective_to, contract.base_salary, contract.currency_code, contract.status, contract.notes "
                        + common_where
                        + " ORDER BY contract.effective_to NULLS LAST, contract.contract_code, contract.employment_contract_id LIMIT ? OFFSET ?",
                this::map_contract, page_parameters);
        long total = total_items == null ? 0 : total_items;
        int total_pages = total == 0 ? 0 : (int) Math.ceil((double) total / page_size);
        return new employment_contract_page_response(items, page, page_size, total, total_pages);
    }

    public employment_contract_response find_by_id(long employment_contract_id) {
        List<employment_contract_response> contracts = jpa_query_executor.query(
                "SELECT contract.employment_contract_id, contract.contract_code, contract.employee_id, employee.employee_code, employee.full_name, "
                        + "contract.contract_type, contract.effective_from, contract.effective_to, contract.base_salary, contract.currency_code, contract.status, contract.notes "
                        + "FROM hr.employment_contract AS contract JOIN hr.employee AS employee ON employee.employee_id = contract.employee_id "
                        + "WHERE contract.employment_contract_id = ?",
                this::map_contract, employment_contract_id);
        if (contracts.isEmpty()) {
            throw new resource_not_found_exception("Employment contract");
        }
        return contracts.getFirst();
    }

    public long insert(String contract_code, long employee_id, String contract_type, LocalDate effective_from,
                       LocalDate effective_to, java.math.BigDecimal base_salary, String currency_code,
                       String status, String notes, long actor_user_id) {
        Long contract_id = jpa_query_executor.queryForObject(
                "INSERT INTO hr.employment_contract (contract_code, employee_id, contract_type, effective_from, effective_to, base_salary, currency_code, status, notes, created_by_user_id, updated_by_user_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING employment_contract_id",
                Long.class, contract_code, employee_id, contract_type, effective_from, effective_to, base_salary,
                currency_code, status, notes, actor_user_id, actor_user_id);
        if (contract_id == null) {
            throw new IllegalStateException("Employment contract identifier was not returned.");
        }
        return contract_id;
    }

    public int update(long employment_contract_id, String contract_code, long employee_id, String contract_type,
                      LocalDate effective_from, LocalDate effective_to, java.math.BigDecimal base_salary,
                      String currency_code, String status, String notes, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE hr.employment_contract SET contract_code = ?, employee_id = ?, contract_type = ?, effective_from = ?, effective_to = ?, base_salary = ?, currency_code = ?, status = ?, notes = ?, updated_at = now(), updated_by_user_id = ? WHERE employment_contract_id = ?",
                contract_code, employee_id, contract_type, effective_from, effective_to, base_salary, currency_code,
                status, notes, actor_user_id, employment_contract_id);
    }

    public int change_status(long employment_contract_id, String status, String notes, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE hr.employment_contract SET status = ?, notes = COALESCE(?, notes), updated_at = now(), updated_by_user_id = ? WHERE employment_contract_id = ?",
                status, notes, actor_user_id, employment_contract_id);
    }

    public int delete_draft(long employment_contract_id) {
        return jpa_query_executor.update(
                "DELETE FROM hr.employment_contract WHERE employment_contract_id = ? AND status = 'draft'",
                employment_contract_id);
    }

    public String current_status(long employment_contract_id) {
        List<String> statuses = jpa_query_executor.query(
                "SELECT status FROM hr.employment_contract WHERE employment_contract_id = ?",
                (result_set, row_number) -> result_set.getString("status"), employment_contract_id);
        if (statuses.isEmpty()) {
            throw new resource_not_found_exception("Employment contract");
        }
        return statuses.getFirst();
    }

    /** Local fixture helpers. They are only used by the opt-in fixture bootstrapper. */
    public Optional<fixture_actor> active_super_admin_for_fixture() {
        return jpa_query_executor.query(
                "SELECT user_id, username FROM identity.user_account WHERE is_super_admin = true AND status = 'active' ORDER BY user_id LIMIT 1",
                (result_set, row_number) -> new fixture_actor(
                        result_set.getLong("user_id"), result_set.getString("username")))
                .stream().findFirst();
    }

    public Optional<fixture_employee> employee_by_code_for_fixture(String employee_code) {
        return jpa_query_executor.query(
                "SELECT employee_id, employee_code FROM hr.employee WHERE employee_code = ?",
                (result_set, row_number) -> new fixture_employee(
                        result_set.getLong("employee_id"), result_set.getString("employee_code")),
                employee_code).stream().findFirst();
    }

    public Optional<fixture_contract> contract_by_code_for_fixture(String contract_code) {
        return jpa_query_executor.query(
                "SELECT employment_contract_id, employee_id, status, effective_from, effective_to "
                        + "FROM hr.employment_contract WHERE contract_code = ?",
                (result_set, row_number) -> new fixture_contract(
                        result_set.getLong("employment_contract_id"),
                        result_set.getLong("employee_id"),
                        result_set.getString("status"),
                        result_set.get_local_date("effective_from"),
                        result_set.get_local_date("effective_to")),
                contract_code).stream().findFirst();
    }

    public int update_fixture_effective_to(String contract_code, LocalDate effective_to, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE hr.employment_contract SET effective_to = ?, updated_at = now(), updated_by_user_id = ? "
                        + "WHERE contract_code = ? AND status = 'active'",
                effective_to, actor_user_id, contract_code);
    }

    public boolean employee_exists(long employee_id) {
        Long count = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.employee WHERE employee_id = ?", Long.class, employee_id);
        return count != null && count > 0;
    }

    public boolean contract_code_exists(String contract_code, Long contract_id) {
        Long count = contract_id == null
                ? jpa_query_executor.queryForObject(
                        "SELECT count(*) FROM hr.employment_contract WHERE contract_code = ?", Long.class, contract_code)
                : jpa_query_executor.queryForObject(
                        "SELECT count(*) FROM hr.employment_contract WHERE contract_code = ? AND employment_contract_id <> ?",
                        Long.class, contract_code, contract_id);
        return count != null && count > 0;
    }

    private String common_where(String expiration_filter) {
        return "FROM hr.employment_contract AS contract "
                + "JOIN hr.employee AS employee ON employee.employee_id = contract.employee_id "
                + "WHERE (CAST(? AS text) IS NULL OR lower(contract.contract_code) LIKE '%' || ? || '%' "
                + "OR lower(employee.employee_code) LIKE '%' || ? || '%' "
                + "OR lower(employee.full_name) LIKE '%' || ? || '%') "
                + "AND (CAST(? AS text) IS NULL OR contract.employee_id = ?) "
                + "AND (CAST(? AS text) IS NULL OR contract.status = ?)" + expiration_filter;
    }

    private Object[] append(Object[] values, Object... suffix) {
        Object[] result = java.util.Arrays.copyOf(values, values.length + suffix.length);
        System.arraycopy(suffix, 0, result, values.length, suffix.length);
        return result;
    }

    private employment_contract_response map_contract(jpa_result_row result_set, int row_number) {
        LocalDate effective_from = result_set.get_local_date("effective_from");
        LocalDate effective_to = result_set.get_local_date("effective_to");
        LocalDate today = LocalDate.now();
        LocalDate end_date = effective_to == null ? null : effective_to;
        long days_until_expiry = end_date == null ? -1 : ChronoUnit.DAYS.between(today, end_date);
        boolean expiring_soon = "active".equals(result_set.getString("status"))
                && effective_from != null && !effective_from.isAfter(today)
                && end_date != null && days_until_expiry >= 0 && days_until_expiry <= 15;
        return new employment_contract_response(
                result_set.getLong("employment_contract_id"), result_set.getString("contract_code"),
                result_set.getLong("employee_id"), result_set.getString("employee_code"),
                result_set.getString("full_name"), result_set.getString("contract_type"),
                effective_from == null ? null : effective_from, end_date,
                result_set.getBigDecimal("base_salary"), result_set.getString("currency_code"),
                result_set.getString("status"), days_until_expiry, expiring_soon,
                result_set.getString("notes"));
    }
    public record fixture_actor(long user_id, String username) {}
    public record fixture_employee(long employee_id, String employee_code) {}
    public record fixture_contract(long employment_contract_id, long employee_id, String status,
                                   LocalDate effective_from, LocalDate effective_to) {}
}
