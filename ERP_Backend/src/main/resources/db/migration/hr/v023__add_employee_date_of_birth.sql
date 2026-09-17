ALTER TABLE hr.employee
    ADD COLUMN date_of_birth date NULL;

ALTER TABLE hr.employee
    ADD CONSTRAINT ck_employee_birth_date_before_hire
        CHECK (date_of_birth IS NULL OR hired_on IS NULL OR date_of_birth <= hired_on);

CREATE INDEX ix_employee_date_of_birth
    ON hr.employee (date_of_birth);
