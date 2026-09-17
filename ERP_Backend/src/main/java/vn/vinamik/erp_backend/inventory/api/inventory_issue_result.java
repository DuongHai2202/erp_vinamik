package vn.vinamik.erp_backend.inventory.api;

import java.util.List;

public record inventory_issue_result(
        long issue_id,
        String issue_code,
        List<inventory_issue_line_result> lines) {
}
