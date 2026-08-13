CREATE TABLE event_retry_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES event(id) ON DELETE CASCADE,
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
