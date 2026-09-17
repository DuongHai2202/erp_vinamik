package vn.vinamik.erp_backend.platform.identity;

import java.time.Instant;

public record identity_account_snapshot(
        long user_id,
        String username,
        String password_hash,
        String status,
        Instant locked_until,
        boolean super_admin) {
}
