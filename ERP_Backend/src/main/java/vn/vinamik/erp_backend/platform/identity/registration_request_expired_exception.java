package vn.vinamik.erp_backend.platform.identity;

public class registration_request_expired_exception extends IllegalArgumentException {
    public registration_request_expired_exception() {
        super("Registration request has expired.");
    }
}
