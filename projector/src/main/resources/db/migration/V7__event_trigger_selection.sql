CREATE TABLE event_trigger_selection (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscription(id) ON DELETE CASCADE,
    event_trigger_type_id SMALLINT NOT NULL REFERENCES event_trigger_type(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(320)
);
