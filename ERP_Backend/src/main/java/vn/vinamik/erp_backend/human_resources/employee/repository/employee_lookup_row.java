package vn.vinamik.erp_backend.human_resources.employee.repository;

/** Minimal read projection used by cross-module employee selectors. */
public record employee_lookup_row(
        long employee_id,
        String employee_code,
        String full_name,
        String employment_status) {
}
