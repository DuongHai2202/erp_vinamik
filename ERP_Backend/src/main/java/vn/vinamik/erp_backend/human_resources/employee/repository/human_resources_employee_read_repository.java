package vn.vinamik.erp_backend.human_resources.employee.repository;
import vn.vinamik.erp_backend.platform.common.pagination_guard;

import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.resource_not_found_exception;

import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;

import java.time.LocalDate;
import java.util.List;

@Repository
public class human_resources_employee_read_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public human_resources_employee_read_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public long count(String search, String status) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.employee AS employee "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(employee.employee_code) LIKE '%' || ? || '%' "
                        + "OR lower(employee.full_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR employee.employment_status = ?)",
                Long.class,
                search, search, search, status, status);
        return total == null ? 0 : total;
    }

    public List<employee_read_row> search(String search, String status, int page, int page_size) {
        return jpa_query_executor.query(
                "SELECT employee.employee_id, employee.employee_code, employee.full_name, "
                        + "employee.date_of_birth, employee.phone_number, employee.email, employee.department_id, "
                        + "department.department_name, employee.job_title_id, job_title.job_title_name, "
                        + "employee.manager_employee_id, employee.employment_status, employee.hired_on, "
                        + "employee.terminated_on, employee.notes "
                        + "FROM hr.employee AS employee "
                        + "LEFT JOIN hr.department AS department ON department.department_id = employee.department_id "
                        + "LEFT JOIN hr.job_title AS job_title ON job_title.job_title_id = employee.job_title_id "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(employee.employee_code) LIKE '%' || ? || '%' "
                        + "OR lower(employee.full_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR employee.employment_status = ?) "
                        + "ORDER BY employee.employee_code LIMIT ? OFFSET ?",
                this::map_row,
                search, search, search, status, status, page_size, pagination_guard.offset(page, page_size));
    }

    public employee_read_row find_by_id(long employee_id) {
        List<employee_read_row> rows = jpa_query_executor.query(
                "SELECT employee.employee_id, employee.employee_code, employee.full_name, "
                        + "employee.date_of_birth, employee.phone_number, employee.email, employee.department_id, "
                        + "department.department_name, employee.job_title_id, job_title.job_title_name, "
                        + "employee.manager_employee_id, employee.employment_status, employee.hired_on, "
                        + "employee.terminated_on, employee.notes "
                        + "FROM hr.employee AS employee "
                        + "LEFT JOIN hr.department AS department ON department.department_id = employee.department_id "
                        + "LEFT JOIN hr.job_title AS job_title ON job_title.job_title_id = employee.job_title_id "
                        + "WHERE employee.employee_id = ?",
                this::map_row,
                employee_id);
        if (rows.isEmpty()) {
            throw new resource_not_found_exception("Employee");
        }
        return rows.getFirst();
    }

    private employee_read_row map_row(jpa_result_row result_set, int row_number) {
        return new employee_read_row(
                result_set.getLong("employee_id"),
                result_set.getString("employee_code"),
                result_set.getString("full_name"),
                result_set.getObject("date_of_birth", LocalDate.class),
                result_set.getString("phone_number"),
                result_set.getString("email"),
                result_set.getObject("department_id", Long.class),
                result_set.getString("department_name"),
                result_set.getObject("job_title_id", Long.class),
                result_set.getString("job_title_name"),
                result_set.getObject("manager_employee_id", Long.class),
                result_set.getString("employment_status"),
                result_set.getObject("hired_on", LocalDate.class),
                result_set.getObject("terminated_on", LocalDate.class),
                result_set.getString("notes"));
    }
    public boolean active_department(long department_id) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.department WHERE department_id = ? AND status = 'active'",
                Long.class, department_id);
        return total != null && total > 0;
    }

    public boolean active_job_title(long job_title_id) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.job_title WHERE job_title_id = ? AND status = 'active'",
                Long.class, job_title_id);
        return total != null && total > 0;
    }
}
