package vn.vinamik.erp_backend.platform.identity;

public record login_result(String session_secret, auth_user_response user) {
}