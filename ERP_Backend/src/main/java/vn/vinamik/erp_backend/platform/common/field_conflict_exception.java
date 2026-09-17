package vn.vinamik.erp_backend.platform.common;

public class field_conflict_exception extends RuntimeException {
    private final String field_name;

    public field_conflict_exception(String field_name, String message) {
        super(message);
        this.field_name = field_name;
    }

    public String field_name() {
        return field_name;
    }
}