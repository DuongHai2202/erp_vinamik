package vn.vinamik.erp_backend.human_resources.api;

import java.time.LocalTime;

public record human_resources_work_shift_snapshot(
        long work_shift_id,
        String shift_code,
        String shift_name,
        LocalTime starts_at,
        LocalTime ends_at,
        String status) {
}
