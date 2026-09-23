package vn.vinamik.erp_backend.platform.identity;

import java.time.Instant;
import java.util.List;

public record identity_user_response(
        long user_id,
        String username,
        Long employee_id,
        String employee_code,
        String employee_name,
        String status,
        List<String> role_codes,
        Instant created_at,
        Instant last_login_at,
        boolean super_admin) {
}
