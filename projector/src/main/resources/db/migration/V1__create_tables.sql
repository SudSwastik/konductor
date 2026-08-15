CREATE TABLE IF NOT EXISTS subscription_type (
    id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(320)
);

CREATE TABLE IF NOT EXISTS subscription_status (
    id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(320)
);

CREATE TABLE IF NOT EXISTS event_trigger_type (
    id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(320)
);

CREATE TABLE IF NOT EXISTS event_status (
    id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(320)
);

CREATE TABLE IF NOT EXISTS parameter_data_type (
    id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(320)
);

CREATE TABLE IF NOT EXISTS subscription (
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

CREATE TABLE IF NOT EXISTS event_trigger_selection (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscription(id),
    event_trigger_type_id SMALLINT NOT NULL REFERENCES event_trigger_type(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(320)
);

CREATE TABLE IF NOT EXISTS parameter_definition (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    data_type_id SMALLINT NOT NULL REFERENCES parameter_data_type(id),
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

CREATE TABLE IF NOT EXISTS parameter_selection (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_trigger_selection_id BIGINT NOT NULL REFERENCES event_trigger_selection(id),
    parameter_definition_id BIGINT NOT NULL REFERENCES parameter_definition(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(320)
);

CREATE TABLE IF NOT EXISTS delivery_config (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscription(id),
    delivery_type VARCHAR(50) NOT NULL,
    endpoint_url TEXT,
    http_method VARCHAR(10),
    auth_type VARCHAR(50) NOT NULL DEFAULT 'NONE',
    auth_header_name VARCHAR(100),
    auth_secret_ref VARCHAR(300),
    timeout_seconds INTEGER NOT NULL DEFAULT 30,
    max_retry_count INTEGER NOT NULL DEFAULT 3,
    retry_backoff_seconds INTEGER NOT NULL DEFAULT 60,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(320),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(320),
    CONSTRAINT ck_delivery_timeout_positive CHECK (timeout_seconds > 0),
    CONSTRAINT ck_delivery_retry_count_non_negative CHECK (max_retry_count >= 0),
    CONSTRAINT ck_delivery_backoff_positive CHECK (retry_backoff_seconds > 0)
);

CREATE TABLE IF NOT EXISTS event (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_uid VARCHAR(150) NOT NULL UNIQUE,
    source_event_id VARCHAR(150) NOT NULL,
    event_trigger_type_id SMALLINT NOT NULL REFERENCES event_trigger_type(id),
    subscription_id BIGINT NOT NULL REFERENCES subscription(id),
    event_trigger_selection_id BIGINT NOT NULL REFERENCES event_trigger_selection(id),
    delivery_config_id BIGINT REFERENCES delivery_config(id),
    event_status_id SMALLINT NOT NULL REFERENCES event_status(id),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    next_retry_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    response_status_code INTEGER,
    error_message TEXT,
    payload_hash VARCHAR(128),
    payload_size_bytes BIGINT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(320),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(320),
    CONSTRAINT ck_event_attempt_count_non_negative CHECK (attempt_count >= 0),
    CONSTRAINT ck_event_payload_size_non_negative CHECK (payload_size_bytes IS NULL OR payload_size_bytes >= 0)
);

CREATE TABLE IF NOT EXISTS event_retry_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES event(id),
    attempt_number INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    response_status_code INTEGER,
    response_body TEXT,
    error_message TEXT,
    duration_ms BIGINT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(320),
    CONSTRAINT uk_event_retry_attempt UNIQUE (event_id, attempt_number),
    CONSTRAINT ck_event_retry_attempt_positive CHECK (attempt_number > 0),
    CONSTRAINT ck_event_retry_duration_non_negative CHECK (duration_ms IS NULL OR duration_ms >= 0)
);

CREATE TABLE IF NOT EXISTS subscription_audit_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subscription_id BIGINT REFERENCES subscription(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(150),
    old_value JSONB,
    new_value JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(320)
);

CREATE INDEX IF NOT EXISTS idx_subscription_status_id ON subscription(subscription_status_id);
CREATE INDEX IF NOT EXISTS idx_subscription_type_id ON subscription(subscription_type_id);
CREATE INDEX IF NOT EXISTS idx_event_trigger_selection_subscription_id ON event_trigger_selection(subscription_id);
CREATE INDEX IF NOT EXISTS idx_event_trigger_selection_event_trigger_type_id ON event_trigger_selection(event_trigger_type_id);
CREATE INDEX IF NOT EXISTS idx_parameter_definition_data_type_id ON parameter_definition(data_type_id);
CREATE INDEX IF NOT EXISTS idx_parameter_selection_event_trigger_selection_id ON parameter_selection(event_trigger_selection_id);
CREATE INDEX IF NOT EXISTS idx_parameter_selection_parameter_definition_id ON parameter_selection(parameter_definition_id);
CREATE INDEX IF NOT EXISTS idx_delivery_config_subscription_id ON delivery_config(subscription_id);
CREATE INDEX IF NOT EXISTS idx_event_source_event_id ON event(source_event_id);
CREATE INDEX IF NOT EXISTS idx_event_subscription_id ON event(subscription_id);
CREATE INDEX IF NOT EXISTS idx_event_event_status_id ON event(event_status_id);
CREATE INDEX IF NOT EXISTS idx_event_next_retry_at ON event(next_retry_at);
CREATE INDEX IF NOT EXISTS idx_event_retry_log_event_id ON event_retry_log(event_id);
CREATE INDEX IF NOT EXISTS idx_subscription_audit_log_subscription_id ON subscription_audit_log(subscription_id);
CREATE INDEX IF NOT EXISTS idx_subscription_audit_log_created_at ON subscription_audit_log(created_at);

CREATE UNIQUE INDEX IF NOT EXISTS uk_event_trigger_selection_active
    ON event_trigger_selection(subscription_id, event_trigger_type_id)
    WHERE is_active = TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_parameter_definition_field_path_active
    ON parameter_definition(field_path)
    WHERE is_active = TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_parameter_selection_active
    ON parameter_selection(event_trigger_selection_id, parameter_definition_id)
    WHERE is_active = TRUE;
