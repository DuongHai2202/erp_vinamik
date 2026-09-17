package vn.vinamik.erp_backend.human_resources.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface human_resources_work_shift_contract {
    Optional<human_resources_work_shift_snapshot> find_work_shift(long work_shift_id);

    Map<Long, human_resources_work_shift_snapshot> find_work_shifts(Collection<Long> work_shift_ids);
}
