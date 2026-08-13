CREATE TABLE parameter_definition (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    data_type_id SMALLINT NOT NULL REFERENCES parameter_type(id),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    field_path VARCHAR(500) NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(320),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(320)
);
