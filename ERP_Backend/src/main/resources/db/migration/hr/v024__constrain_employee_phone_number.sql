ALTER TABLE hr.employee
    ADD CONSTRAINT ck_employee_phone_number_format
        CHECK (phone_number IS NULL OR phone_number ~ '^0[0-9]{9,10}$');
