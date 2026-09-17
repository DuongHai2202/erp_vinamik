package vn.vinamik.erp_backend.platform.identity;

import java.util.List;

public record identity_audit_event_page_response(
        List<identity_audit_event_response> items,
        int page,
        int page_size,
        long total_items,
        int total_pages) {
}