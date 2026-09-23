package vn.vinamik.erp_backend.human_resources.employee;

import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import org.springframework.stereotype.Component;
import vn.vinamik.erp_backend.human_resources.api.human_resources_work_shift_contract;
import vn.vinamik.erp_backend.human_resources.api.human_resources_work_shift_snapshot;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class human_resources_work_shift_contract_adapter implements human_resources_work_shift_contract {
    private final jpa_native_query_executor jpa_query_executor;

    public human_resources_work_shift_contract_adapter(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    @Override
    public Optional<human_resources_work_shift_snapshot> find_work_shift(long work_shift_id) {
        List<human_resources_work_shift_snapshot> shifts = jpa_query_executor.query(
                "SELECT work_shift_id, shift_code, shift_name, starts_at, ends_at, status FROM hr.work_shift WHERE work_shift_id = ?",
                (result_set, row_number) -> new human_resources_work_shift_snapshot(
                        result_set.getLong("work_shift_id"), result_set.getString("shift_code"),
                        result_set.getString("shift_name"), result_set.getObject("starts_at", java.time.LocalTime.class),
                        result_set.getObject("ends_at", java.time.LocalTime.class), result_set.getString("status")), work_shift_id);
        return shifts.stream().findFirst();
    }

    @Override
    public Map<Long, human_resources_work_shift_snapshot> find_work_shifts(Collection<Long> work_shift_ids) {
        Map<Long, human_resources_work_shift_snapshot> shifts = new LinkedHashMap<>();
        if (work_shift_ids == null || work_shift_ids.isEmpty()) {
            return shifts;
        }
        List<Long> normalized_ids = work_shift_ids.stream()
                .filter(work_shift_id -> work_shift_id != null && work_shift_id > 0)
                .distinct()
                .toList();
        if (normalized_ids.isEmpty()) {
            return shifts;
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(normalized_ids.size(), "?"));
        List<human_resources_work_shift_snapshot> rows = jpa_query_executor.query(
                "SELECT work_shift_id, shift_code, shift_name, starts_at, ends_at, status "
                        + "FROM hr.work_shift WHERE work_shift_id IN (" + placeholders + ")",
                (result_set, row_number) -> new human_resources_work_shift_snapshot(
                        result_set.getLong("work_shift_id"), result_set.getString("shift_code"),
                        result_set.getString("shift_name"), result_set.getObject("starts_at", java.time.LocalTime.class),
                        result_set.getObject("ends_at", java.time.LocalTime.class),
                        result_set.getString("status")),
                normalized_ids.toArray());
        rows.forEach(shift -> shifts.put(shift.work_shift_id(), shift));
        return shifts;
    }
}

