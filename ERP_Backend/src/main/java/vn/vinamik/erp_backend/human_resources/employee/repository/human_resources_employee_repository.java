package vn.vinamik.erp_backend.human_resources.employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.vinamik.erp_backend.human_resources.employee.entity.employee_entity;

public interface human_resources_employee_repository extends JpaRepository<employee_entity, Long> {
    @Query("select count(e) > 0 from employee_entity e where e.employee_code = :employee_code")
    boolean exists_by_employee_code(@Param("employee_code") String employee_code);

    @Query("select count(e) > 0 from employee_entity e where e.employee_code = :employee_code and e.employee_id <> :employee_id")
    boolean exists_by_employee_code_excluding_id(@Param("employee_code") String employee_code,
                                                  @Param("employee_id") Long employee_id);
}

