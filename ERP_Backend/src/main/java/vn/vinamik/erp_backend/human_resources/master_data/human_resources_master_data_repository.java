package vn.vinamik.erp_backend.human_resources.master_data;

import org.springframework.stereotype.Repository;
import vn.vinamik.erp_backend.platform.common.pagination_guard;
import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import vn.vinamik.erp_backend.platform.persistence.jpa_result_row;

import java.time.LocalTime;
import java.util.List;

@Repository
public class human_resources_master_data_repository {
    private final jpa_native_query_executor jpa_query_executor;

    public human_resources_master_data_repository(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    public long count_departments(String search, String status) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.department "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(department_code) LIKE '%' || ? || '%' "
                        + "OR lower(department_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR status = ?)",
                Long.class, search, search, search, status, status);
        return total == null ? 0 : total;
    }

    public List<department_response> search_departments(String search, String status, int page, int page_size) {
        return jpa_query_executor.query(
                "SELECT department_id, department_code, department_name, parent_department_id, status "
                        + "FROM hr.department "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(department_code) LIKE '%' || ? || '%' "
                        + "OR lower(department_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR status = ?) "
                        + "ORDER BY department_code LIMIT ? OFFSET ?",
                this::map_department, search, search, search, status, status,
                page_size, pagination_guard.offset(page, page_size));
    }

    public department_response find_department(long department_id) {
        return jpa_query_executor.queryForObject(
                "SELECT department_id, department_code, department_name, parent_department_id, status "
                        + "FROM hr.department WHERE department_id = ?",
                this::map_department, department_id);
    }

    public boolean department_code_exists(String department_code, Long department_id) {
        Long total = department_id == null
                ? jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.department WHERE lower(department_code) = ?",
                Long.class, department_code)
                : jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.department WHERE lower(department_code) = ? AND department_id <> ?",
                Long.class, department_code, department_id);
        return total != null && total > 0;
    }

    public boolean active_department(long department_id) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.department WHERE department_id = ? AND status = 'active'",
                Long.class, department_id);
        return total != null && total > 0;
    }

    public boolean department_is_descendant(long department_id, long candidate_parent_id) {
        Long total = jpa_query_executor.queryForObject(
                "WITH RECURSIVE descendants AS ("
                        + "SELECT department_id FROM hr.department WHERE department_id = ? "
                        + "UNION ALL "
                        + "SELECT department.department_id FROM hr.department AS department "
                        + "JOIN descendants ON department.parent_department_id = descendants.department_id) "
                        + "SELECT count(*) FROM descendants WHERE department_id = ?",
                Long.class, department_id, candidate_parent_id);
        return total != null && total > 0;
    }

    public Long insert_department(String code, String name, Long parent_department_id, String status, long actor_user_id) {
        return jpa_query_executor.queryForObject(
                "INSERT INTO hr.department "
                        + "(department_code, department_name, parent_department_id, status, created_by_user_id, updated_by_user_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING department_id",
                Long.class, code, name, parent_department_id, status, actor_user_id, actor_user_id);
    }

    public int update_department(long department_id, String code, String name, Long parent_department_id,
                                 String status, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE hr.department SET department_code = ?, department_name = ?, parent_department_id = ?, "
                        + "status = ?, updated_at = now(), updated_by_user_id = ? WHERE department_id = ?",
                code, name, parent_department_id, status, actor_user_id, department_id);
    }

    public int deactivate_department(long department_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE hr.department SET status = 'inactive', updated_at = now(), updated_by_user_id = ? "
                        + "WHERE department_id = ?",
                actor_user_id, department_id);
    }

    public long count_job_titles(String search, String status) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.job_title "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(job_title_code) LIKE '%' || ? || '%' "
                        + "OR lower(job_title_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR status = ?)",
                Long.class, search, search, search, status, status);
        return total == null ? 0 : total;
    }

    public List<job_title_response> search_job_titles(String search, String status, int page, int page_size) {
        return jpa_query_executor.query(
                "SELECT job_title_id, job_title_code, job_title_name, description, status "
                        + "FROM hr.job_title "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(job_title_code) LIKE '%' || ? || '%' "
                        + "OR lower(job_title_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR status = ?) "
                        + "ORDER BY job_title_code LIMIT ? OFFSET ?",
                this::map_job_title, search, search, search, status, status,
                page_size, pagination_guard.offset(page, page_size));
    }

    public job_title_response find_job_title(long job_title_id) {
        return jpa_query_executor.queryForObject(
                "SELECT job_title_id, job_title_code, job_title_name, description, status "
                        + "FROM hr.job_title WHERE job_title_id = ?",
                this::map_job_title, job_title_id);
    }

    public boolean job_title_code_exists(String job_title_code, Long job_title_id) {
        Long total = job_title_id == null
                ? jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.job_title WHERE lower(job_title_code) = ?",
                Long.class, job_title_code)
                : jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.job_title WHERE lower(job_title_code) = ? AND job_title_id <> ?",
                Long.class, job_title_code, job_title_id);
        return total != null && total > 0;
    }

    public boolean active_job_title(long job_title_id) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.job_title WHERE job_title_id = ? AND status = 'active'",
                Long.class, job_title_id);
        return total != null && total > 0;
    }

    public Long insert_job_title(String code, String name, String description, String status, long actor_user_id) {
        return jpa_query_executor.queryForObject(
                "INSERT INTO hr.job_title "
                        + "(job_title_code, job_title_name, description, status, created_by_user_id, updated_by_user_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING job_title_id",
                Long.class, code, name, description, status, actor_user_id, actor_user_id);
    }

    public int update_job_title(long job_title_id, String code, String name, String description,
                                String status, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE hr.job_title SET job_title_code = ?, job_title_name = ?, description = ?, status = ?, "
                        + "updated_at = now(), updated_by_user_id = ? WHERE job_title_id = ?",
                code, name, description, status, actor_user_id, job_title_id);
    }

    public int deactivate_job_title(long job_title_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE hr.job_title SET status = 'inactive', updated_at = now(), updated_by_user_id = ? "
                        + "WHERE job_title_id = ?",
                actor_user_id, job_title_id);
    }

    public long count_work_shifts(String search, String status) {
        Long total = jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.work_shift "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(shift_code) LIKE '%' || ? || '%' "
                        + "OR lower(shift_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR status = ?)",
                Long.class, search, search, search, status, status);
        return total == null ? 0 : total;
    }

    public List<work_shift_response> search_work_shifts(String search, String status, int page, int page_size) {
        return jpa_query_executor.query(
                "SELECT work_shift_id, shift_code, shift_name, starts_at, ends_at, status "
                        + "FROM hr.work_shift "
                        + "WHERE (CAST(? AS text) IS NULL OR lower(shift_code) LIKE '%' || ? || '%' "
                        + "OR lower(shift_name) LIKE '%' || ? || '%') "
                        + "AND (CAST(? AS text) IS NULL OR status = ?) "
                        + "ORDER BY shift_code LIMIT ? OFFSET ?",
                this::map_work_shift, search, search, search, status, status,
                page_size, pagination_guard.offset(page, page_size));
    }

    public List<work_shift_response> find_active_work_shifts() {
        return jpa_query_executor.query(
                "SELECT work_shift_id, shift_code, shift_name, starts_at, ends_at, status "
                        + "FROM hr.work_shift WHERE status = 'active' ORDER BY shift_code",
                this::map_work_shift);
    }

    public work_shift_response find_work_shift(long work_shift_id) {
        return jpa_query_executor.queryForObject(
                "SELECT work_shift_id, shift_code, shift_name, starts_at, ends_at, status "
                        + "FROM hr.work_shift WHERE work_shift_id = ?",
                this::map_work_shift, work_shift_id);
    }

    public boolean work_shift_code_exists(String shift_code, Long work_shift_id) {
        Long total = work_shift_id == null
                ? jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.work_shift WHERE lower(shift_code) = ?",
                Long.class, shift_code)
                : jpa_query_executor.queryForObject(
                "SELECT count(*) FROM hr.work_shift WHERE lower(shift_code) = ? AND work_shift_id <> ?",
                Long.class, shift_code, work_shift_id);
        return total != null && total > 0;
    }

    public Long insert_work_shift(String code, String name, LocalTime starts_at, LocalTime ends_at,
                                  String status, long actor_user_id) {
        return jpa_query_executor.queryForObject(
                "INSERT INTO hr.work_shift "
                        + "(shift_code, shift_name, starts_at, ends_at, status, created_by_user_id, updated_by_user_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING work_shift_id",
                Long.class, code, name, starts_at, ends_at, status, actor_user_id, actor_user_id);
    }

    public int update_work_shift(long work_shift_id, String code, String name, LocalTime starts_at,
                                 LocalTime ends_at, String status, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE hr.work_shift SET shift_code = ?, shift_name = ?, starts_at = ?, ends_at = ?, status = ?, "
                        + "updated_at = now(), updated_by_user_id = ? WHERE work_shift_id = ?",
                code, name, starts_at, ends_at, status, actor_user_id, work_shift_id);
    }

    public int deactivate_work_shift(long work_shift_id, long actor_user_id) {
        return jpa_query_executor.update(
                "UPDATE hr.work_shift SET status = 'inactive', updated_at = now(), updated_by_user_id = ? "
                        + "WHERE work_shift_id = ?",
                actor_user_id, work_shift_id);
    }

    private department_response map_department(jpa_result_row row, int row_number) {
        return new department_response(row.getLong("department_id"), row.getString("department_code"),
                row.getString("department_name"), row.getObject("parent_department_id", Long.class),
                row.getString("status"));
    }

    private job_title_response map_job_title(jpa_result_row row, int row_number) {
        return new job_title_response(row.getLong("job_title_id"), row.getString("job_title_code"),
                row.getString("job_title_name"), row.getString("description"), row.getString("status"));
    }

    private work_shift_response map_work_shift(jpa_result_row row, int row_number) {
        return new work_shift_response(row.getLong("work_shift_id"), row.getString("shift_code"),
                row.getString("shift_name"), row.getObject("starts_at", LocalTime.class),
                row.getObject("ends_at", LocalTime.class), row.getString("status"));
    }
}
