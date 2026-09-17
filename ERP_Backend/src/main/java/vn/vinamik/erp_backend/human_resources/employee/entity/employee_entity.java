package vn.vinamik.erp_backend.human_resources.employee.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity(name = "employee_entity")
@Table(schema = "hr", name = "employee")
public class employee_entity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long employee_id;

    @Column(name = "employee_code", nullable = false, length = 40)
    private String employee_code;

    @Column(name = "full_name", nullable = false, length = 160)
    private String full_name;

    @Column(name = "date_of_birth")
    private LocalDate date_of_birth;

    @Column(name = "phone_number", length = 30)
    private String phone_number;

    @Column(name = "email", length = 254)
    private String email;

    @Column(name = "department_id")
    private Long department_id;

    @Column(name = "job_title_id")
    private Long job_title_id;

    @Column(name = "manager_employee_id")
    private Long manager_employee_id;

    @Column(name = "employment_status", nullable = false, length = 24)
    private String employment_status;

    @Column(name = "hired_on")
    private LocalDate hired_on;

    @Column(name = "terminated_on")
    private LocalDate terminated_on;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant created_at;

    @Column(name = "created_by_user_id")
    private Long created_by_user_id;

    @Column(name = "updated_at", nullable = false)
    private Instant updated_at;

    @Column(name = "updated_by_user_id")
    private Long updated_by_user_id;

    protected employee_entity() {
    }

    public employee_entity(String employee_code, String full_name, LocalDate date_of_birth,
                           String phone_number, String email,
                           Long department_id, Long job_title_id, Long manager_employee_id,
                           String employment_status, LocalDate hired_on, LocalDate terminated_on,
                           String notes, Long actor_user_id) {
        Instant now = Instant.now();
        this.employee_code = employee_code;
        this.full_name = full_name;
        this.date_of_birth = date_of_birth;
        this.phone_number = phone_number;
        this.email = email;
        this.department_id = department_id;
        this.job_title_id = job_title_id;
        this.manager_employee_id = manager_employee_id;
        this.employment_status = employment_status;
        this.hired_on = hired_on;
        this.terminated_on = terminated_on;
        this.notes = notes;
        this.created_at = now;
        this.created_by_user_id = actor_user_id;
        this.updated_at = now;
        this.updated_by_user_id = actor_user_id;
    }

    public void update_values(String employee_code, String full_name, LocalDate date_of_birth,
                              String phone_number, String email,
                              Long department_id, Long job_title_id, Long manager_employee_id,
                              String employment_status, LocalDate hired_on, LocalDate terminated_on,
                              String notes, Long actor_user_id) {
        this.employee_code = employee_code;
        this.full_name = full_name;
        this.date_of_birth = date_of_birth;
        this.phone_number = phone_number;
        this.email = email;
        this.department_id = department_id;
        this.job_title_id = job_title_id;
        this.manager_employee_id = manager_employee_id;
        this.employment_status = employment_status;
        this.hired_on = hired_on;
        this.terminated_on = terminated_on;
        this.notes = notes;
        this.updated_at = Instant.now();
        this.updated_by_user_id = actor_user_id;
    }

    public void deactivate(Long actor_user_id) {
        this.employment_status = "inactive";
        if (this.terminated_on == null) {
            this.terminated_on = LocalDate.now();
        }
        this.updated_at = Instant.now();
        this.updated_by_user_id = actor_user_id;
    }

    public Long employee_id() {
        return employee_id;
    }

    public String employee_code() {
        return employee_code;
    }
}

