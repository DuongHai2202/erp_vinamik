package vn.vinamik.erp_backend.human_resources.contract;

import java.util.List;

public record employment_contract_page_response(
        List<employment_contract_response> items,
        int page,
        int page_size,
        long total_items,
        int total_pages) {
}
