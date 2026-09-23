package vn.vinamik.erp_backend.human_resources.employee;

import vn.vinamik.erp_backend.platform.persistence.jpa_native_query_executor;
import org.springframework.stereotype.Component;
import vn.vinamik.erp_backend.human_resources.api.human_resources_employee_contract;
import vn.vinamik.erp_backend.human_resources.api.human_resources_employee_snapshot;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class human_resources_employee_contract_adapter implements human_resources_employee_contract {
    private final jpa_native_query_executor jpa_query_executor;

    public human_resources_employee_contract_adapter(jpa_native_query_executor jpa_query_executor) {
        this.jpa_query_executor = jpa_query_executor;
    }

    @Override
    public Optional<human_resources_employee_snapshot> find_employee(long employee_id) {
        List<human_resources_employee_snapshot> employees = jpa_query_executor.query(
                "SELECT employee_id, employee_code, full_name, employment_status FROM hr.employee WHERE employee_id = ?",
                (result_set, row_number) -> new human_resources_employee_snapshot(
                        result_set.getLong("employee_id"), result_set.getString("employee_code"),
                        result_set.getString("full_name"), result_set.getString("employment_status")), employee_id);
        return employees.stream().findFirst();
    }

    @Override
    public Map<Long, human_resources_employee_snapshot> find_employees(Collection<Long> employee_ids) {
        Map<Long, human_resources_employee_snapshot> employees = new LinkedHashMap<>();
        if (employee_ids == null || employee_ids.isEmpty()) {
            return employees;
        }
        List<Long> normalized_ids = employee_ids.stream()
                .filter(employee_id -> employee_id != null && employee_id > 0)
                .distinct()
                .toList();
        if (normalized_ids.isEmpty()) {
            return employees;
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(normalized_ids.size(), "?"));
        List<human_resources_employee_snapshot> rows = jpa_query_executor.query(
                "SELECT employee_id, employee_code, full_name, employment_status "
                        + "FROM hr.employee WHERE employee_id IN (" + placeholders + ")",
                (result_set, row_number) -> new human_resources_employee_snapshot(
                        result_set.getLong("employee_id"), result_set.getString("employee_code"),
                        result_set.getString("full_name"), result_set.getString("employment_status")),
                normalized_ids.toArray());
        rows.forEach(employee -> employees.put(employee.employee_id(), employee));
        return employees;
    }
}

