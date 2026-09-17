package vn.vinamik.erp_backend.platform.identity;

public class invalid_credentials_exception extends RuntimeException {
    public invalid_credentials_exception() {
        super("Invalid username or password.");
    }
}