package vn.vinamik.erp_backend.human_resources.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface human_resources_employee_contract {
    Optional<human_resources_employee_snapshot> find_employee(long employee_id);

    Map<Long, human_resources_employee_snapshot> find_employees(Collection<Long> employee_ids);
}
