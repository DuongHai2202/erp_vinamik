package vn.vinamik.erp_backend.human_resources.employee.repository;

import java.time.LocalDate;

public record employee_read_row(
        long employee_id,
        String employee_code,
        String full_name,
        LocalDate date_of_birth,
        String phone_number,
        String email,
        Long department_id,
        String department_name,
        Long job_title_id,
        String job_title_name,
        Long manager_employee_id,
        String employment_status,
        LocalDate hired_on,
        LocalDate terminated_on,
        String notes) {
}

