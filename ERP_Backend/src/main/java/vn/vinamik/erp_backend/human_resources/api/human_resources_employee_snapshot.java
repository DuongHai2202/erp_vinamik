package vn.vinamik.erp_backend.human_resources.api;

public record human_resources_employee_snapshot(
        long employee_id,
        String employee_code,
        String full_name,
        String employment_status) {
}
