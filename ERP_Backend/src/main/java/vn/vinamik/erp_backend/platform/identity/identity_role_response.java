package vn.vinamik.erp_backend.platform.identity;

public record identity_role_response(
        long role_id,
        String role_code,
        String display_name,
        String description,
        String status) {
}
