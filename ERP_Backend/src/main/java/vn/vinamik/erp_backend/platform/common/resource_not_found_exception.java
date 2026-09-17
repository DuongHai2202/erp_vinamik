package vn.vinamik.erp_backend.platform.common;

public class resource_not_found_exception extends RuntimeException {
    public resource_not_found_exception(String resource_name) {
        super(resource_name + " was not found.");
    }
}