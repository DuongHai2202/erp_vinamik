package vn.vinamik.erp_backend.inventory.api;

import java.util.List;

public record inventory_receipt_result(
        long receipt_id,
        String receipt_code,
        List<inventory_receipt_line_result> lines) {
}
