package vn.vinamik.erp_backend.platform.common;

import java.util.Map;

public record api_error_response(
        String code,
        String message,
        String correlation_id,
        Map<String, String> field_errors) {
}