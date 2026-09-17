package vn.vinamik.erp_backend.human_resources.reward_discipline;

import java.util.List;

public record reward_discipline_page_response(
        List<reward_discipline_response> items,
        int page,
        int page_size,
        long total_items,
        int total_pages) {
}
