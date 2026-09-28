CREATE TABLE identity.business_code_sequence (
    code_type varchar(80) NOT NULL,
    period_key varchar(20) NOT NULL,
    last_value bigint NOT NULL DEFAULT 0,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_business_code_sequence PRIMARY KEY (code_type, period_key),
    CONSTRAINT ck_business_code_sequence_last_value CHECK (last_value >= 0)
);

COMMENT ON TABLE identity.business_code_sequence IS
    'Concurrency-safe counters used to allocate human-readable business codes.';
