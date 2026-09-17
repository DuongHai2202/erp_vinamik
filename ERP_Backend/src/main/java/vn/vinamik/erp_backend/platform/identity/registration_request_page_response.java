package vn.vinamik.erp_backend.platform.identity;

import java.util.List;

public record registration_request_page_response(
        List<registration_request_response> items,
        int page,
        int page_size,
        long total_items,
        int total_pages) {
}
