package vn.vinamik.erp_backend.human_resources.employee;

/** Minimal projection for cross-module employee selectors. */
public record employee_lookup_response(
        long employee_id,
        String employee_code,
        String full_name,
        String employment_status) {
}
