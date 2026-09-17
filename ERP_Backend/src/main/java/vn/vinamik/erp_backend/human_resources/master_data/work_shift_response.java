package vn.vinamik.erp_backend.human_resources.master_data;

import java.time.LocalTime;

public record work_shift_response(
        long work_shift_id,
        String shift_code,
        String shift_name,
        LocalTime starts_at,
        LocalTime ends_at,
        String status) {
}
