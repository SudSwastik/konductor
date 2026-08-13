CREATE TABLE subscription (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subscription_uid VARCHAR(100) NOT NULL UNIQUE,
    subscription_type_id SMALLINT NOT NULL REFERENCES subscription_type(id),
    subscription_status_id SMALLINT NOT NULL REFERENCES subscription_status(id),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    activated_at TIMESTAMPTZ,
    deactivated_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(320),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(320)
);
