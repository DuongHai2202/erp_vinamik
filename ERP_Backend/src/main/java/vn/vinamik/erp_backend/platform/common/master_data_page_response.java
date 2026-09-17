package vn.vinamik.erp_backend.platform.common;

import java.util.List;

public record master_data_page_response<T>(
        List<T> items,
        int page,
        int page_size,
        long total_items,
        int total_pages) {
}
