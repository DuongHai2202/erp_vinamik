package vn.vinamik.erp_backend.human_resources.master_data;

public record department_response(
        long department_id,
        String department_code,
        String department_name,
        Long parent_department_id,
        String status) {
}
