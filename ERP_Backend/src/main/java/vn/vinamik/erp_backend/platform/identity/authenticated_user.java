package vn.vinamik.erp_backend.platform.identity;

import java.util.List;

public record authenticated_user(
        long user_id,
        String username,
        List<String> permission_codes,
        boolean super_admin) {
    public authenticated_user(long user_id, String username, List<String> permission_codes) {
        this(user_id, username, permission_codes, false);
    }
}


