package vn.vinamik.erp_backend.platform.identity;

import java.util.List;

public record identity_user_page_response(
        List<identity_user_response> items,
        int page,
        int page_size,
        long total_items,
        int total_pages) {
}
