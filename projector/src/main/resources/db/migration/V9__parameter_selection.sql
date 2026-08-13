CREATE TABLE parameter_selection (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_trigger_selection_id BIGINT NOT NULL REFERENCES event_trigger_selection(id) ON DELETE CASCADE,
    parameter_definition_id BIGINT NOT NULL REFERENCES parameter_definition(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(320)
);
