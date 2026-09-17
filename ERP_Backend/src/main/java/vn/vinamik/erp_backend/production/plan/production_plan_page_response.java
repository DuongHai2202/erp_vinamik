package vn.vinamik.erp_backend.production.plan;

import java.util.List;

public record production_plan_page_response(
        List<production_plan_summary> items,
        int page,
        int page_size,
        long total_items,
        int total_pages) {
}